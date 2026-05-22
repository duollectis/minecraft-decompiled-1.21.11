package net.minecraft.test;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.FillBiomeCommand;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.*;
import java.util.stream.LongStream;

/**
 * Контекст выполнения игрового теста.
 * <p>
 * Предоставляет богатый API для взаимодействия с миром в рамках тестовой структуры:
 * спавн сущностей, проверка блоков, управление временем, создание игроков-заглушек и т.д.
 * Все позиции, принимаемые методами, являются <em>относительными</em> (внутри структуры);
 * преобразование в абсолютные координаты выполняется автоматически.
 */
public class TestContext {

	private final GameTestState test;
	private boolean hasFinalClause;

	public TestContext(GameTestState test) {
		this.test = test;
	}

	public GameTestException createError(Text message) {
		return new GameTestException(message, test.getTick());
	}

	public GameTestException createError(String translationKey, Object... args) {
		return createError(Text.stringifiedTranslatable(translationKey, args));
	}

	public PositionedException createError(BlockPos pos, Text message) {
		return new PositionedException(message, getAbsolutePos(pos), pos, test.getTick());
	}

	public PositionedException createError(BlockPos pos, String translationKey, Object... args) {
		return createError(pos, Text.stringifiedTranslatable(translationKey, args));
	}

	public ServerWorld getWorld() {
		return test.getWorld();
	}

	public BlockState getBlockState(BlockPos pos) {
		return getWorld().getBlockState(getAbsolutePos(pos));
	}

	/**
	 * Возвращает блок-сущность по относительной позиции, приведённую к заданному типу.
	 *
	 * @throws PositionedException если блок-сущность отсутствует или имеет неверный тип
	 */
	public <T extends BlockEntity> T getBlockEntity(BlockPos pos, Class<T> clazz) {
		BlockEntity blockEntity = getWorld().getBlockEntity(getAbsolutePos(pos));
		if (blockEntity == null) {
			throw createError(pos, "test.error.missing_block_entity");
		}

		if (!clazz.isInstance(blockEntity)) {
			throw createError(
					pos,
					"test.error.wrong_block_entity",
					blockEntity.getType().getRegistryEntry().getIdAsString()
			);
		}

		return clazz.cast(blockEntity);
	}

	public void killAllEntities() {
		killAllEntities(Entity.class);
	}

	public void killAllEntities(Class<? extends Entity> entityClass) {
		Box box = getTestBox();
		List<? extends Entity> entities = getWorld().getEntitiesByClass(
				entityClass,
				box.expand(1.0),
				entity -> !(entity instanceof PlayerEntity)
		);
		entities.forEach(entity -> entity.kill(getWorld()));
	}

	public ItemEntity spawnItem(Item item, Vec3d pos) {
		ServerWorld world = getWorld();
		Vec3d absolute = getAbsolute(pos);
		ItemEntity itemEntity = new ItemEntity(world, absolute.x, absolute.y, absolute.z, new ItemStack(item, 1));
		itemEntity.setVelocity(0.0, 0.0, 0.0);
		world.spawnEntity(itemEntity);
		return itemEntity;
	}

	public ItemEntity spawnItem(Item item, float x, float y, float z) {
		return spawnItem(item, new Vec3d(x, y, z));
	}

	public ItemEntity spawnItem(Item item, BlockPos pos) {
		return spawnItem(item, pos.getX(), pos.getY(), pos.getZ());
	}

	public <E extends Entity> E spawnEntity(EntityType<E> type, BlockPos pos) {
		return spawnEntity(type, Vec3d.ofBottomCenter(pos));
	}

	public <E extends Entity> List<E> spawnEntities(EntityType<E> type, BlockPos pos, int count) {
		return spawnEntities(type, Vec3d.ofBottomCenter(pos), count);
	}

	public <E extends Entity> List<E> spawnEntities(EntityType<E> type, Vec3d pos, int count) {
		List<E> entities = new ArrayList<>();
		for (int index = 0; index < count; index++) {
			entities.add(spawnEntity(type, pos));
		}

		return entities;
	}

	public <E extends Entity> E spawnEntity(EntityType<E> type, Vec3d pos) {
		return spawnEntity(type, pos, null);
	}

	/**
	 * Спавнит сущность заданного типа в относительной позиции с опциональной причиной спавна.
	 * Для {@link MobEntity} автоматически устанавливает флаг persistent и применяет поворот.
	 *
	 * @throws PositionedException если сущность не удалось создать
	 */
	public <E extends Entity> E spawnEntity(EntityType<E> type, Vec3d pos, @Nullable SpawnReason reason) {
		ServerWorld world = getWorld();
		E entity = type.create(world, SpawnReason.STRUCTURE);
		if (entity == null) {
			throw createError(
					BlockPos.ofFloored(pos),
					"test.error.spawn_failure",
					type.getRegistryEntry().getIdAsString()
			);
		}

		if (entity instanceof MobEntity mob) {
			mob.setPersistent();
		}

		Vec3d absolute = getAbsolute(pos);
		float yaw = entity.applyRotation(getRotation());
		entity.refreshPositionAndAngles(absolute.x, absolute.y, absolute.z, yaw, entity.getPitch());
		entity.setBodyYaw(yaw);
		entity.setHeadYaw(yaw);

		if (reason != null && entity instanceof MobEntity mob) {
			mob.initialize(
					world,
					world.getLocalDifficulty(mob.getBlockPos()),
					reason,
					null
			);
		}

		world.spawnEntityAndPassengers(entity);
		return entity;
	}

	public <E extends MobEntity> E spawnEntity(EntityType<E> type, int x, int y, int z, SpawnReason reason) {
		return spawnEntity(type, new Vec3d(x, y, z), reason);
	}

	public void damage(Entity entity, DamageSource damageSource, float amount) {
		entity.damage(getWorld(), damageSource, amount);
	}

	public void killEntity(Entity entity) {
		entity.kill(getWorld());
	}

	public <E extends Entity> E expectEntityInWorld(EntityType<E> type) {
		return expectEntity(type, 0, 0, 0, 2.147483647E9);
	}

	/**
	 * Ищет ровно одну сущность заданного типа в радиусе от точки.
	 * Бросает исключение если сущностей нет или их больше одной.
	 */
	public <E extends Entity> E expectEntity(EntityType<E> type, int x, int y, int z, double margin) {
		List<E> entities = getEntitiesAround(type, x, y, z, margin);
		if (entities.isEmpty()) {
			throw createError("test.error.expected_entity_around", type.getName(), x, y, z);
		}

		if (entities.size() > 1) {
			throw createError("test.error.too_many_entities", type.getUntranslatedName(), x, y, z, entities.size());
		}

		Vec3d center = getAbsolute(new Vec3d(x, y, z));
		entities.sort((a, b) -> Double.compare(
				a.getEntityPos().distanceTo(center),
				b.getEntityPos().distanceTo(center)
		));
		return entities.get(0);
	}

	public <E extends Entity> List<E> getEntitiesAround(EntityType<E> type, int x, int y, int z, double margin) {
		return getEntitiesAround(type, Vec3d.ofBottomCenter(new BlockPos(x, y, z)), margin);
	}

	public <E extends Entity> List<E> getEntitiesAround(EntityType<E> type, Vec3d pos, double margin) {
		Vec3d absolute = getAbsolute(pos);
		Box testBox = test.getBoundingBox();
		Box searchBox = new Box(
				absolute.add(-margin, -margin, -margin),
				absolute.add(margin, margin, margin)
		);
		return getWorld().getEntitiesByType(
				type,
				testBox,
				entity -> entity.getBoundingBox().intersects(searchBox) && entity.isAlive()
		);
	}

	public <E extends Entity> E spawnEntity(EntityType<E> type, int x, int y, int z) {
		return spawnEntity(type, new BlockPos(x, y, z));
	}

	public <E extends Entity> E spawnEntity(EntityType<E> type, float x, float y, float z) {
		return spawnEntity(type, new Vec3d(x, y, z));
	}

	public <E extends MobEntity> E spawnMob(EntityType<E> type, BlockPos pos) {
		@SuppressWarnings("unchecked")
		E mob = (E) spawnEntity(type, pos);
		mob.clearGoalsAndTasks();
		return mob;
	}

	public <E extends MobEntity> E spawnMob(EntityType<E> type, int x, int y, int z) {
		return spawnMob(type, new BlockPos(x, y, z));
	}

	public <E extends MobEntity> E spawnMob(EntityType<E> type, Vec3d pos) {
		@SuppressWarnings("unchecked")
		E mob = (E) spawnEntity(type, pos);
		mob.clearGoalsAndTasks();
		return mob;
	}

	public <E extends MobEntity> E spawnMob(EntityType<E> type, float x, float y, float z) {
		return spawnMob(type, new Vec3d(x, y, z));
	}

	public void setEntityPos(MobEntity entity, float x, float y, float z) {
		Vec3d absolute = getAbsolute(new Vec3d(x, y, z));
		entity.refreshPositionAndAngles(absolute.x, absolute.y, absolute.z, entity.getYaw(), entity.getPitch());
	}

	public TimedTaskRunner startMovingTowards(MobEntity entity, BlockPos pos, float speed) {
		return createTimedTaskRunner().expectMinDurationAndRun(2, () -> {
			Path path = entity.getNavigation().findPathTo(getAbsolutePos(pos), 0);
			entity.getNavigation().startMovingAlong(path, speed);
		});
	}

	public void pushButton(int x, int y, int z) {
		pushButton(new BlockPos(x, y, z));
	}

	public void pushButton(BlockPos pos) {
		expectBlockIn(BlockTags.BUTTONS, pos);
		BlockPos absolute = getAbsolutePos(pos);
		BlockState state = getWorld().getBlockState(absolute);
		ButtonBlock button = (ButtonBlock) state.getBlock();
		button.powerOn(state, getWorld(), absolute, null);
	}

	public void useBlock(BlockPos pos) {
		useBlock(pos, createMockPlayer(GameMode.CREATIVE));
	}

	public void useBlock(BlockPos pos, PlayerEntity player) {
		BlockPos absolute = getAbsolutePos(pos);
		useBlock(pos, player, new BlockHitResult(Vec3d.ofCenter(absolute), Direction.NORTH, absolute, true));
	}

	public void useBlock(BlockPos pos, PlayerEntity player, BlockHitResult result) {
		BlockPos absolute = getAbsolutePos(pos);
		BlockState state = getWorld().getBlockState(absolute);
		Hand hand = Hand.MAIN_HAND;
		ActionResult actionResult = state.onUseWithItem(player.getStackInHand(hand), getWorld(), player, hand, result);

		if (!actionResult.isAccepted()) {
			if (!(actionResult instanceof ActionResult.PassToDefaultBlockAction)
					|| !state.onUse(getWorld(), player, result).isAccepted()) {
				ItemUsageContext usageContext = new ItemUsageContext(player, hand, result);
				player.getStackInHand(hand).useOnBlock(usageContext);
			}
		}
	}

	public LivingEntity drown(LivingEntity entity) {
		entity.setAir(0);
		entity.setHealth(0.25F);
		return entity;
	}

	public LivingEntity setHealthLow(LivingEntity entity) {
		entity.setHealth(0.25F);
		return entity;
	}

	public PlayerEntity createMockPlayer(GameMode gameMode) {
		return new PlayerEntity(getWorld(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
			@Override
			public GameMode getGameMode() {
				return gameMode;
			}

			@Override
			public boolean isControlledByPlayer() {
				return false;
			}
		};
	}

	@Deprecated(forRemoval = true)
	public ServerPlayerEntity createMockCreativeServerPlayerInWorld() {
		ConnectedClientData clientData = ConnectedClientData.createDefault(
				new GameProfile(UUID.randomUUID(), "test-mock-player"),
				false
		);
		ServerPlayerEntity player = new ServerPlayerEntity(
				getWorld().getServer(),
				getWorld(),
				clientData.gameProfile(),
				clientData.syncedOptions()
		) {
			@Override
			public GameMode getGameMode() {
				return GameMode.CREATIVE;
			}
		};
		ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
		new EmbeddedChannel(new ChannelHandler[]{connection});
		getWorld().getServer().getPlayerManager().onPlayerConnect(connection, player, clientData);
		return player;
	}

	public void toggleLever(int x, int y, int z) {
		toggleLever(new BlockPos(x, y, z));
	}

	public void toggleLever(BlockPos pos) {
		expectBlock(Blocks.LEVER, pos);
		BlockPos absolute = getAbsolutePos(pos);
		BlockState state = getWorld().getBlockState(absolute);
		LeverBlock lever = (LeverBlock) state.getBlock();
		lever.togglePower(state, getWorld(), absolute, null);
	}

	public void putAndRemoveRedstoneBlock(BlockPos pos, long delay) {
		setBlockState(pos, Blocks.REDSTONE_BLOCK);
		waitAndRun(delay, () -> setBlockState(pos, Blocks.AIR));
	}

	public void removeBlock(BlockPos pos) {
		getWorld().breakBlock(getAbsolutePos(pos), false, null);
	}

	public void setBlockState(int x, int y, int z, Block block) {
		setBlockState(new BlockPos(x, y, z), block);
	}

	public void setBlockState(int x, int y, int z, BlockState state) {
		setBlockState(new BlockPos(x, y, z), state);
	}

	public void setBlockState(BlockPos pos, Block block) {
		setBlockState(pos, block.getDefaultState());
	}

	public void setBlockState(BlockPos pos, BlockState state) {
		getWorld().setBlockState(getAbsolutePos(pos), state, 3);
	}

	public void setBlockFacing(BlockPos pos, Block block, Direction facing) {
		setBlockFacing(pos, block.getDefaultState(), facing);
	}

	public void setBlockFacing(BlockPos pos, BlockState block, Direction facing) {
		BlockState state = block;
		if (block.contains(HorizontalFacingBlock.FACING)) {
			state = block.with(HorizontalFacingBlock.FACING, facing);
		}

		if (block.contains(Properties.FACING)) {
			state = block.with(Properties.FACING, facing);
		}

		getWorld().setBlockState(getAbsolutePos(pos), state, 3);
	}

	public void expectBlock(Block block, int x, int y, int z) {
		expectBlock(block, new BlockPos(x, y, z));
	}

	public void expectBlock(Block block, BlockPos pos) {
		BlockState state = getBlockState(pos);
		checkBlock(
				pos,
				b -> state.isOf(block),
				actual -> Text.translatable("test.error.expected_block", block.getName(), actual.getName())
		);
	}

	public void dontExpectBlock(Block block, int x, int y, int z) {
		dontExpectBlock(block, new BlockPos(x, y, z));
	}

	public void dontExpectBlock(Block block, BlockPos pos) {
		checkBlock(
				pos,
				b -> !getBlockState(pos).isOf(block),
				actual -> Text.translatable("test.error.unexpected_block", block.getName())
		);
	}

	public void expectBlockIn(TagKey<Block> tag, BlockPos pos) {
		checkBlockState(
				pos,
				state -> state.isIn(tag),
				state -> Text.translatable(
						"test.error.expected_block_tag",
						Text.of(tag.id()),
						state.getBlock().getName()
				)
		);
	}

	public void expectBlockAtEnd(Block block, int x, int y, int z) {
		expectBlockAtEnd(block, new BlockPos(x, y, z));
	}

	public void expectBlockAtEnd(Block block, BlockPos pos) {
		addInstantFinalTask(() -> expectBlock(block, pos));
	}

	public void checkBlock(BlockPos pos, Predicate<Block> predicate, Function<Block, Text> messageGetter) {
		checkBlockState(
				pos,
				state -> predicate.test(state.getBlock()),
				state -> messageGetter.apply(state.getBlock())
		);
	}

	public <T extends Comparable<T>> void expectBlockProperty(BlockPos pos, Property<T> property, T value) {
		BlockState state = getBlockState(pos);
		if (!state.contains(property)) {
			throw createError(pos, "test.error.block_property_missing", property.getName(), value);
		}

		if (!state.<T>get(property).equals(value)) {
			throw createError(
					pos,
					"test.error.block_property_mismatch",
					property.getName(),
					value,
					state.get(property)
			);
		}
	}

	public <T extends Comparable<T>> void checkBlockProperty(
			BlockPos pos,
			Property<T> property,
			Predicate<T> predicate,
			Text message
	) {
		checkBlockState(pos, state -> {
			if (!state.contains(property)) {
				return false;
			}

			return predicate.test(state.get(property));
		}, state -> message);
	}

	public void expectBlockState(BlockPos pos, BlockState state) {
		BlockState actual = getBlockState(pos);
		if (!actual.equals(state)) {
			throw createError(pos, "test.error.state_not_equal", state, actual);
		}
	}

	public void checkBlockState(
			BlockPos pos,
			Predicate<BlockState> predicate,
			Function<BlockState, Text> messageGetter
	) {
		BlockState state = getBlockState(pos);
		if (!predicate.test(state)) {
			throw createError(pos, messageGetter.apply(state));
		}
	}

	public <T extends BlockEntity> void checkBlockEntity(
			BlockPos pos,
			Class<T> clazz,
			Predicate<T> predicate,
			Supplier<Text> messageGetter
	) {
		T blockEntity = getBlockEntity(pos, clazz);
		if (!predicate.test(blockEntity)) {
			throw createError(pos, messageGetter.get());
		}
	}

	public void expectRedstonePower(
			BlockPos pos,
			Direction direction,
			IntPredicate powerPredicate,
			Supplier<Text> messageGetter
	) {
		BlockPos absolute = getAbsolutePos(pos);
		ServerWorld world = getWorld();
		BlockState state = world.getBlockState(absolute);
		int power = state.getWeakRedstonePower(world, absolute, direction);
		if (!powerPredicate.test(power)) {
			throw createError(pos, messageGetter.get());
		}
	}

	public void expectEntity(EntityType<?> type) {
		if (!getWorld().hasEntities(type, getTestBox(), Entity::isAlive)) {
			throw createError("test.error.expected_entity_in_test", type.getName());
		}
	}

	public void expectEntityAt(EntityType<?> type, int x, int y, int z) {
		expectEntityAt(type, new BlockPos(x, y, z));
	}

	public void expectEntityAt(EntityType<?> type, BlockPos pos) {
		BlockPos absolute = getAbsolutePos(pos);
		if (!getWorld().hasEntities(type, new Box(absolute), Entity::isAlive)) {
			throw createError(pos, "test.error.expected_entity", type.getName());
		}
	}

	public void expectEntityInside(EntityType<?> type, Box box) {
		Box absolute = getAbsolute(box);
		if (!getWorld().hasEntities(type, absolute, Entity::isAlive)) {
			throw createError(BlockPos.ofFloored(box.getCenter()), "test.error.expected_entity", type.getName());
		}
	}

	public void expectEntityIn(EntityType<?> type, Box box, Text message) {
		Box absolute = getAbsolute(box);
		if (!getWorld().hasEntities(type, absolute, Entity::isAlive)) {
			throw createError(BlockPos.ofFloored(box.getCenter()), message);
		}
	}

	public void expectEntities(EntityType<?> type, int amount) {
		List<? extends Entity> entities = getWorld().getEntitiesByType(type, getTestBox(), Entity::isAlive);
		if (entities.size() != amount) {
			throw createError("test.error.expected_entity_count", amount, type.getName(), entities.size());
		}
	}

	public void expectEntitiesAround(EntityType<?> type, BlockPos pos, int amount, double radius) {
		List<? extends Entity> entities = getEntitiesAround((EntityType<? extends Entity>) type, pos, radius);
		if (entities.size() != amount) {
			throw createError(pos, "test.error.expected_entity_count", amount, type.getName(), entities.size());
		}
	}

	public void expectEntityAround(EntityType<?> type, BlockPos pos, double radius) {
		List<? extends Entity> entities = getEntitiesAround((EntityType<? extends Entity>) type, pos, radius);
		if (entities.isEmpty()) {
			throw createError(pos, "test.error.expected_entity", type.getName());
		}
	}

	public <T extends Entity> List<T> getEntitiesAround(EntityType<T> type, BlockPos pos, double radius) {
		BlockPos absolute = getAbsolutePos(pos);
		return getWorld().getEntitiesByType(type, new Box(absolute).expand(radius), Entity::isAlive);
	}

	public <T extends Entity> List<T> getEntities(EntityType<T> type) {
		return getWorld().getEntitiesByType(type, getTestBox(), Entity::isAlive);
	}

	public void expectEntityAt(Entity entity, int x, int y, int z) {
		expectEntityAt(entity, new BlockPos(x, y, z));
	}

	public void expectEntityAt(Entity entity, BlockPos pos) {
		BlockPos absolute = getAbsolutePos(pos);
		List<? extends Entity> entities = getWorld().getEntitiesByType(
				entity.getType(),
				new Box(absolute),
				Entity::isAlive
		);
		entities.stream()
				.filter(e -> e == entity)
				.findFirst()
				.orElseThrow(() -> createError(pos, "test.error.expected_entity", entity.getType().getName()));
	}

	public void expectItemsAt(Item item, BlockPos pos, double radius, int amount) {
		BlockPos absolute = getAbsolutePos(pos);
		List<ItemEntity> itemEntities = getWorld().getEntitiesByType(
				EntityType.ITEM,
				new Box(absolute).expand(radius),
				Entity::isAlive
		);
		int totalCount = 0;
		for (ItemEntity itemEntity : itemEntities) {
			ItemStack stack = itemEntity.getStack();
			if (stack.isOf(item)) {
				totalCount += stack.getCount();
			}
		}

		if (totalCount != amount) {
			throw createError(pos, "test.error.expected_items_count", amount, item.getName(), totalCount);
		}
	}

	public void expectItemAt(Item item, BlockPos pos, double radius) {
		BlockPos absolute = getAbsolutePos(pos);
		Predicate<ItemEntity> predicate = entity -> entity.isAlive() && entity.getStack().isOf(item);
		if (!getWorld().hasEntities(EntityType.ITEM, new Box(absolute).expand(radius), predicate)) {
			throw createError(pos, "test.error.expected_item", item.getName());
		}
	}

	public void dontExpectItemAt(Item item, BlockPos pos, double radius) {
		BlockPos absolute = getAbsolutePos(pos);
		Predicate<ItemEntity> predicate = entity -> entity.isAlive() && entity.getStack().isOf(item);
		if (getWorld().hasEntities(EntityType.ITEM, new Box(absolute).expand(radius), predicate)) {
			throw createError(pos, "test.error.unexpected_item", item.getName());
		}
	}

	public void expectItem(Item item) {
		Predicate<ItemEntity> predicate = entity -> entity.isAlive() && entity.getStack().isOf(item);
		if (!getWorld().hasEntities(EntityType.ITEM, getTestBox(), predicate)) {
			throw createError("test.error.expected_item", item.getName());
		}
	}

	public void dontExpectItem(Item item) {
		Predicate<ItemEntity> predicate = entity -> entity.isAlive() && entity.getStack().isOf(item);
		if (getWorld().hasEntities(EntityType.ITEM, getTestBox(), predicate)) {
			throw createError("test.error.unexpected_item", item.getName());
		}
	}

	public void dontExpectEntity(EntityType<?> type) {
		List<? extends Entity> entities = getWorld().getEntitiesByType(type, getTestBox(), Entity::isAlive);
		if (!entities.isEmpty()) {
			throw createError(entities.getFirst().getBlockPos(), "test.error.unexpected_entity", type.getName());
		}
	}

	public void dontExpectEntityAt(EntityType<?> type, int x, int y, int z) {
		dontExpectEntityAt(type, new BlockPos(x, y, z));
	}

	public void dontExpectEntityAt(EntityType<?> type, BlockPos pos) {
		BlockPos absolute = getAbsolutePos(pos);
		if (getWorld().hasEntities(type, new Box(absolute), Entity::isAlive)) {
			throw createError(pos, "test.error.unexpected_entity", type.getName());
		}
	}

	public void dontExpectEntityBetween(EntityType<?> type, Box box) {
		Box absolute = getAbsolute(box);
		List<? extends Entity> entities = getWorld().getEntitiesByType(type, absolute, Entity::isAlive);
		if (!entities.isEmpty()) {
			throw createError(entities.getFirst().getBlockPos(), "test.error.unexpected_entity", type.getName());
		}
	}

	public void expectEntityToTouch(EntityType<?> type, double x, double y, double z) {
		Vec3d relative = new Vec3d(x, y, z);
		Vec3d absolute = getAbsolute(relative);
		Predicate<? super Entity> predicate = entity -> entity.getBoundingBox().intersects(absolute, absolute);
		if (!getWorld().hasEntities(type, getTestBox(), predicate)) {
			throw createError(
					"test.error.expected_entity_touching",
					type.getName(),
					absolute.getX(),
					absolute.getY(),
					absolute.getZ(),
					x,
					y,
					z
			);
		}
	}

	public void dontExpectEntityToTouch(EntityType<?> type, double x, double y, double z) {
		Vec3d relative = new Vec3d(x, y, z);
		Vec3d absolute = getAbsolute(relative);
		Predicate<? super Entity> predicate = entity -> !entity.getBoundingBox().intersects(absolute, absolute);
		if (!getWorld().hasEntities(type, getTestBox(), predicate)) {
			throw createError(
					"test.error.expected_entity_not_touching",
					type.getName(),
					absolute.getX(),
					absolute.getY(),
					absolute.getZ(),
					x,
					y,
					z
			);
		}
	}

	public <E extends Entity, T> void expectEntity(BlockPos pos, EntityType<E> type, Predicate<E> predicate) {
		BlockPos absolute = getAbsolutePos(pos);
		List<E> entities = getWorld().getEntitiesByType(type, new Box(absolute), Entity::isAlive);
		if (entities.isEmpty()) {
			throw createError(pos, "test.error.expected_entity", type.getName());
		}

		for (E entity : entities) {
			if (!predicate.test(entity)) {
				throw createError(
						entity.getBlockPos(),
						"test.error.expected_entity_data_predicate",
						entity.getName()
				);
			}
		}
	}

	public <E extends Entity, T> void expectEntityWithData(
			BlockPos pos,
			EntityType<E> type,
			Function<? super E, T> entityDataGetter,
			@Nullable T data
	) {
		expectEntityWithData(new Box(pos), type, entityDataGetter, data);
	}

	public <E extends Entity, T> void expectEntityWithData(
			Box box,
			EntityType<E> type,
			Function<? super E, T> entityDataGetter,
			@Nullable T data
	) {
		List<E> entities = getWorld().getEntitiesByType(type, getAbsolute(box), Entity::isAlive);
		if (entities.isEmpty()) {
			throw createError(
					BlockPos.ofFloored(box.getHorizontalCenter()),
					"test.error.expected_entity",
					type.getName()
			);
		}

		for (E entity : entities) {
			T actual = entityDataGetter.apply(entity);
			if (!Objects.equals(actual, data)) {
				throw createError(
						BlockPos.ofFloored(box.getHorizontalCenter()),
						"test.error.expected_entity_data",
						data,
						actual
				);
			}
		}
	}

	public <E extends LivingEntity> void expectEntityHoldingItem(BlockPos pos, EntityType<E> entityType, Item item) {
		BlockPos absolute = getAbsolutePos(pos);
		List<E> entities = getWorld().getEntitiesByType(entityType, new Box(absolute), Entity::isAlive);
		if (entities.isEmpty()) {
			throw createError(pos, "test.error.expected_entity", entityType.getName());
		}

		for (E entity : entities) {
			if (entity.isHolding(item)) {
				return;
			}
		}

		throw createError(pos, "test.error.expected_entity_holding", item.getName());
	}

	public <E extends Entity & InventoryOwner> void expectEntityWithItem(
			BlockPos pos,
			EntityType<E> entityType,
			Item item
	) {
		BlockPos absolute = getAbsolutePos(pos);
		List<E> entities = getWorld().getEntitiesByType(
				entityType,
				new Box(absolute),
				entity -> ((Entity) entity).isAlive()
		);
		if (entities.isEmpty()) {
			throw createError(pos, "test.error.expected_entity", entityType.getName());
		}

		for (E entity : entities) {
			if (entity.getInventory().containsAny(stack -> stack.isOf(item))) {
				return;
			}
		}

		throw createError(pos, "test.error.expected_entity_having", item.getName());
	}

	public void expectEmptyContainer(BlockPos pos) {
		LockableContainerBlockEntity container = getBlockEntity(pos, LockableContainerBlockEntity.class);
		if (!container.isEmpty()) {
			throw createError(pos, "test.error.expected_empty_container");
		}
	}

	public void expectContainerWithSingle(BlockPos pos, Item item) {
		LockableContainerBlockEntity container = getBlockEntity(pos, LockableContainerBlockEntity.class);
		if (container.count(item) != 1) {
			throw createError(pos, "test.error.expected_container_contents_single", item.getName());
		}
	}

	public void expectContainerWith(BlockPos pos, Item item) {
		LockableContainerBlockEntity container = getBlockEntity(pos, LockableContainerBlockEntity.class);
		if (container.count(item) == 0) {
			throw createError(pos, "test.error.expected_container_contents", item.getName());
		}
	}

	public void expectSameStates(BlockBox checkedBlockBox, BlockPos correctStatePos) {
		BlockPos.stream(checkedBlockBox).forEach(checkedPos -> {
			BlockPos referencePos = correctStatePos.add(
					checkedPos.getX() - checkedBlockBox.getMinX(),
					checkedPos.getY() - checkedBlockBox.getMinY(),
					checkedPos.getZ() - checkedBlockBox.getMinZ()
			);
			expectSameStates(checkedPos, referencePos);
		});
	}

	public void expectSameStates(BlockPos checkedPos, BlockPos correctStatePos) {
		BlockState actual = getBlockState(checkedPos);
		BlockState expected = getBlockState(correctStatePos);
		if (actual != expected) {
			throw createError(checkedPos, "test.error.state_not_equal", expected, actual);
		}
	}

	public void expectContainerWith(long delay, BlockPos pos, Item item) {
		runAtTick(delay, () -> expectContainerWithSingle(pos, item));
	}

	public void expectEmptyContainer(long delay, BlockPos pos) {
		runAtTick(delay, () -> expectEmptyContainer(pos));
	}

	public <E extends Entity, T> void expectEntityWithDataEnd(
			BlockPos pos,
			EntityType<E> type,
			Function<E, T> entityDataGetter,
			T data
	) {
		addInstantFinalTask(() -> expectEntityWithData(pos, type, entityDataGetter, data));
	}

	public <E extends Entity> void testEntity(E entity, Predicate<E> predicate, Text message) {
		if (!predicate.test(entity)) {
			throw createError(entity.getBlockPos(), "test.error.entity_property", entity.getName(), message);
		}
	}

	public <E extends Entity, T> void testEntityProperty(
			E entity,
			Function<E, T> propertyGetter,
			T value,
			Text message
	) {
		T actual = propertyGetter.apply(entity);
		if (!actual.equals(value)) {
			throw createError(
					entity.getBlockPos(),
					"test.error.entity_property_details",
					entity.getName(),
					message,
					actual,
					value
			);
		}
	}

	public void expectEntityHasEffect(LivingEntity entity, RegistryEntry<StatusEffect> effect, int amplifier) {
		StatusEffectInstance instance = entity.getStatusEffect(effect);
		if (instance == null || instance.getAmplifier() != amplifier) {
			throw createError(
					"test.error.expected_entity_effect",
					entity.getName(),
					PotionContentsComponent.getEffectText(effect, amplifier)
			);
		}
	}

	public void expectEntityAtEnd(EntityType<?> type, int x, int y, int z) {
		expectEntityAtEnd(type, new BlockPos(x, y, z));
	}

	public void expectEntityAtEnd(EntityType<?> type, BlockPos pos) {
		addInstantFinalTask(() -> expectEntityAt(type, pos));
	}

	public void dontExpectEntityAtEnd(EntityType<?> type, int x, int y, int z) {
		dontExpectEntityAtEnd(type, new BlockPos(x, y, z));
	}

	public void dontExpectEntityAtEnd(EntityType<?> type, BlockPos pos) {
		addInstantFinalTask(() -> dontExpectEntityAt(type, pos));
	}

	public void complete() {
		test.completeIfSuccessful();
	}

	public void addFinalTask(Runnable runnable) {
		markFinalCause();
		test.createTimedTaskRunner().createAndAdd(0L, runnable).completeIfSuccessful();
	}

	public void addInstantFinalTask(Runnable runnable) {
		markFinalCause();
		test.createTimedTaskRunner().createAndAdd(runnable).completeIfSuccessful();
	}

	public void addFinalTaskWithDuration(int duration, Runnable runnable) {
		markFinalCause();
		test.createTimedTaskRunner().createAndAdd(duration, runnable).completeIfSuccessful();
	}

	public void runAtTick(long tick, Runnable runnable) {
		test.runAtTick(tick, runnable);
	}

	public void waitAndRun(long ticks, Runnable runnable) {
		runAtTick(test.getTick() + ticks, runnable);
	}

	public void forceRandomTick(BlockPos pos) {
		BlockPos absolute = getAbsolutePos(pos);
		ServerWorld world = getWorld();
		world.getBlockState(absolute).randomTick(world, absolute, world.random);
	}

	public void forceScheduledTick(BlockPos pos) {
		BlockPos absolute = getAbsolutePos(pos);
		ServerWorld world = getWorld();
		world.getBlockState(absolute).scheduledTick(world, absolute, world.random);
	}

	public void forceTickIceAndSnow(BlockPos pos) {
		BlockPos absolute = getAbsolutePos(pos);
		ServerWorld world = getWorld();
		world.tickIceAndSnow(absolute);
	}

	public void forceTickIceAndSnow() {
		Box box = getRelativeTestBox();
		int maxX = (int) Math.floor(box.maxX);
		int maxZ = (int) Math.floor(box.maxZ);
		int maxY = (int) Math.floor(box.maxY);

		for (int x = (int) Math.floor(box.minX); x < maxX; x++) {
			for (int z = (int) Math.floor(box.minZ); z < maxZ; z++) {
				forceTickIceAndSnow(new BlockPos(x, maxY, z));
			}
		}
	}

	public int getRelativeTopY(Heightmap.Type heightmap, int x, int z) {
		BlockPos absolute = getAbsolutePos(new BlockPos(x, 0, z));
		return getRelativePos(getWorld().getTopPosition(heightmap, absolute)).getY();
	}

	public void throwPositionedException(Text message, BlockPos pos) {
		throw createError(pos, message);
	}

	public void throwPositionedException(Text message, Entity entity) {
		throw createError(entity.getBlockPos(), message);
	}

	public void throwGameTestException(Text message) {
		throw createError(message);
	}

	public void throwGameTestException(String message) {
		throw createError(Text.literal(message));
	}

	public void addTask(Runnable task) {
		test.createTimedTaskRunner().createAndAdd(task).fail(() -> createError("test.error.fail"));
	}

	public void runAtEveryTick(Runnable task) {
		LongStream.range(test.getTick(), test.getTickLimit())
				.forEach(tick -> test.runAtTick(tick, task::run));
	}

	public TimedTaskRunner createTimedTaskRunner() {
		return test.createTimedTaskRunner();
	}

	/**
	 * Преобразует относительную позицию блока в абсолютную с учётом поворота структуры.
	 */
	public BlockPos getAbsolutePos(BlockPos pos) {
		BlockPos origin = test.getOrigin();
		BlockPos shifted = origin.add(pos);
		return StructureTemplate.transformAround(shifted, BlockMirror.NONE, test.getRotation(), origin);
	}

	/**
	 * Преобразует абсолютную позицию блока в относительную (внутри структуры).
	 */
	public BlockPos getRelativePos(BlockPos pos) {
		BlockPos origin = test.getOrigin();
		BlockRotation inverse = test.getRotation().rotate(BlockRotation.CLOCKWISE_180);
		BlockPos transformed = StructureTemplate.transformAround(pos, BlockMirror.NONE, inverse, origin);
		return transformed.subtract(origin);
	}

	public Box getAbsolute(Box box) {
		Vec3d min = getAbsolute(box.getMinPos());
		Vec3d max = getAbsolute(box.getMaxPos());
		return new Box(min, max);
	}

	public Box getRelative(Box box) {
		Vec3d min = getRelative(box.getMinPos());
		Vec3d max = getRelative(box.getMaxPos());
		return new Box(min, max);
	}

	public Vec3d getAbsolute(Vec3d pos) {
		Vec3d origin = Vec3d.of(test.getOrigin());
		return StructureTemplate.transformAround(
				origin.add(pos),
				BlockMirror.NONE,
				test.getRotation(),
				test.getOrigin()
		);
	}

	public Vec3d getRelative(Vec3d pos) {
		Vec3d origin = Vec3d.of(test.getOrigin());
		return StructureTemplate.transformAround(
				pos.subtract(origin),
				BlockMirror.NONE,
				test.getRotation(),
				test.getOrigin()
		);
	}

	public BlockRotation getRotation() {
		return test.getRotation();
	}

	public Direction getDirection() {
		return test.getRotation().rotate(Direction.SOUTH);
	}

	public Direction rotate(Direction direction) {
		return getRotation().rotate(direction);
	}

	public void assertTrue(boolean condition, Text message) {
		if (!condition) {
			throw createError(message);
		}
	}

	public void assertTrue(boolean condition, String message) {
		assertTrue(condition, Text.literal(message));
	}

	public <N> void assertEquals(N expected, N value, String message) {
		assertEquals(expected, value, Text.literal(message));
	}

	public <N> void assertEquals(N expected, N value, Text message) {
		if (!expected.equals(value)) {
			throw createError("test.error.value_not_equal", message, expected, value);
		}
	}

	public void assertFalse(boolean condition, Text message) {
		assertTrue(!condition, message);
	}

	public void assertFalse(boolean condition, String message) {
		assertFalse(condition, Text.literal(message));
	}

	public long getTick() {
		return test.getTick();
	}

	public Box getTestBox() {
		return test.getBoundingBox();
	}

	public Box getRelativeTestBox() {
		Box box = test.getBoundingBox();
		BlockRotation rotation = test.getRotation();
		return switch (rotation) {
			case COUNTERCLOCKWISE_90, CLOCKWISE_90 ->
					new Box(0.0, 0.0, 0.0, box.getLengthZ(), box.getLengthY(), box.getLengthX());
			default ->
					new Box(0.0, 0.0, 0.0, box.getLengthX(), box.getLengthY(), box.getLengthZ());
		};
	}

	public void forEachRelativePos(Consumer<BlockPos> posConsumer) {
		Box box = getRelativeTestBox().shrink(1.0, 1.0, 1.0);
		BlockPos.Mutable.stream(box).forEach(posConsumer);
	}

	public void forEachRemainingTick(Runnable runnable) {
		LongStream.range(test.getTick(), test.getTickLimit())
				.forEach(tick -> test.runAtTick(tick, runnable::run));
	}

	public void useStackOnBlock(PlayerEntity player, ItemStack stack, BlockPos pos, Direction direction) {
		BlockPos absolute = getAbsolutePos(pos.offset(direction));
		BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(absolute), direction, absolute, false);
		ItemUsageContext usageContext = new ItemUsageContext(player, Hand.MAIN_HAND, hitResult);
		stack.useOnBlock(usageContext);
	}

	/**
	 * Заполняет биом в границах тестовой структуры.
	 *
	 * @throws GameTestException если команда заполнения биома завершилась ошибкой
	 */
	public void setBiome(RegistryKey<Biome> biome) {
		Box box = getTestBox();
		BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
		BlockPos max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);
		Either<Integer, CommandSyntaxException> result = FillBiomeCommand.fillBiome(
				getWorld(),
				min,
				max,
				getWorld().getRegistryManager().getOrThrow(RegistryKeys.BIOME).getOrThrow(biome)
		);
		if (result.right().isPresent()) {
			throw createError("test.error.set_biome");
		}
	}

	private void markFinalCause() {
		if (hasFinalClause) {
			throw new IllegalStateException("This test already has final clause");
		}

		hasFinalClause = true;
	}
}
