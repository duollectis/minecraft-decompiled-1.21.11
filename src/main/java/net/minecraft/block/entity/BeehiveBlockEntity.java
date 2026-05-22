package net.minecraft.block.entity;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BeesComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.debug.data.BeeHiveDebugData;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Блок-сущность улья/пчелиного гнезда. Управляет хранением пчёл внутри,
 * их выходом по таймеру, сбором нектара и уровнем мёда.
 */
public class BeehiveBlockEntity extends BlockEntity {

	static final Logger LOGGER = LogUtils.getLogger();
	private static final String FLOWER_POS_KEY = "flower_pos";
	private static final String BEES_KEY = "bees";
	static final List<String> IRRELEVANT_BEE_NBT_KEYS = Arrays.asList(
			"Air",
			"drop_chances",
			"equipment",
			"Brain",
			"CanPickUpLoot",
			"DeathTime",
			"fall_distance",
			"FallFlying",
			"Fire",
			"HurtByTimestamp",
			"HurtTime",
			"LeftHanded",
			"Motion",
			"NoGravity",
			"OnGround",
			"PortalCooldown",
			"Pos",
			"Rotation",
			"sleeping_pos",
			"CannotEnterHiveTicks",
			"TicksSincePollination",
			"CropsGrownSincePollination",
			"hive_pos",
			"Passengers",
			"leash",
			"UUID"
	);
	public static final int MAX_BEE_COUNT = 3;
	private static final int ANGERED_CANNOT_ENTER_HIVE_TICKS = 400;
	private static final int MIN_OCCUPATION_TICKS_WITH_NECTAR = 2400;
	public static final int MIN_OCCUPATION_TICKS_WITHOUT_NECTAR = 600;
	private final List<BeehiveBlockEntity.Bee> bees = Lists.newArrayList();
	private @Nullable BlockPos flowerPos;

	public BeehiveBlockEntity(BlockPos pos, BlockState state) {
		super(BlockEntityType.BEEHIVE, pos, state);
	}

	@Override
	public void markDirty() {
		if (isNearFire()) {
			angerBees(null, world.getBlockState(getPos()), BeehiveBlockEntity.BeeState.EMERGENCY);
		}

		super.markDirty();
	}

	public boolean isNearFire() {
		if (world == null) {
			return false;
		}

		for (BlockPos blockPos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
			if (world.getBlockState(blockPos).getBlock() instanceof FireBlock) {
				return true;
			}
		}

		return false;
	}

	public boolean hasNoBees() {
		return bees.isEmpty();
	}

	public boolean isFullOfBees() {
		return bees.size() == MAX_BEE_COUNT;
	}

	/**
	 * Выпускает всех пчёл из улья в агрессивном состоянии.
	 * Если улей задымлён, пчёлы не атакуют игрока, но получают таймер невозможности войти обратно.
	 */
	public void angerBees(@Nullable PlayerEntity player, BlockState state, BeehiveBlockEntity.BeeState beeState) {
		List<Entity> released = tryReleaseBee(state, beeState);

		if (player == null) {
			return;
		}

		for (Entity entity : released) {
			if (entity instanceof BeeEntity beeEntity
					&& player.getEntityPos().squaredDistanceTo(entity.getEntityPos()) <= 16.0) {
				if (isSmoked()) {
					beeEntity.setCannotEnterHiveTicks(ANGERED_CANNOT_ENTER_HIVE_TICKS);
				} else {
					beeEntity.setTarget(player);
				}
			}
		}
	}

	private List<Entity> tryReleaseBee(BlockState state, BeehiveBlockEntity.BeeState beeState) {
		List<Entity> released = Lists.newArrayList();
		bees.removeIf(bee -> releaseBee(world, pos, state, bee.createData(), released, beeState, flowerPos));

		if (!released.isEmpty()) {
			super.markDirty();
		}

		return released;
	}

	@Debug
	public int getBeeCount() {
		return bees.size();
	}

	public static int getHoneyLevel(BlockState state) {
		return state.get(BeehiveBlock.HONEY_LEVEL);
	}

	@Debug
	public boolean isSmoked() {
		return CampfireBlock.isLitCampfireInRange(world, getPos());
	}

	/**
	 * Пытается поместить пчелу в улей, если есть свободное место.
	 * Обновляет позицию цветка, воспроизводит звук и испускает игровое событие.
	 */
	public void tryEnterHive(BeeEntity entity) {
		if (bees.size() >= MAX_BEE_COUNT) {
			return;
		}

		entity.stopRiding();
		entity.removeAllPassengers();
		entity.detachLeash();
		addBee(BeehiveBlockEntity.BeeData.of(entity));

		if (world != null) {
			if (entity.hasFlower() && (!hasFlowerPos() || world.random.nextBoolean())) {
				flowerPos = entity.getFlowerPos();
			}

			BlockPos blockPos = getPos();
			world.playSound(
					null,
					(double) blockPos.getX(),
					(double) blockPos.getY(),
					(double) blockPos.getZ(),
					SoundEvents.BLOCK_BEEHIVE_ENTER,
					SoundCategory.BLOCKS,
					1.0F,
					1.0F
			);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(entity, getCachedState()));
		}

		entity.discard();
		super.markDirty();
	}

	public void addBee(BeehiveBlockEntity.BeeData bee) {
		bees.add(new BeehiveBlockEntity.Bee(bee));
	}

	private static boolean releaseBee(
			World world,
			BlockPos pos,
			BlockState state,
			BeehiveBlockEntity.BeeData bee,
			@Nullable List<Entity> entities,
			BeehiveBlockEntity.BeeState beeState,
			@Nullable BlockPos flowerPos
	) {
		if (world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.BEES_STAY_IN_HIVE_GAMEPLAY, pos)
				&& beeState != BeehiveBlockEntity.BeeState.EMERGENCY) {
			return false;
		}

		Direction direction = state.get(BeehiveBlock.FACING);
		BlockPos exitPos = pos.offset(direction);
		boolean exitBlocked = !world.getBlockState(exitPos).getCollisionShape(world, exitPos).isEmpty();

		if (exitBlocked && beeState != BeehiveBlockEntity.BeeState.EMERGENCY) {
			return false;
		}

		Entity entity = bee.loadEntity(world, pos);
		if (entity == null) {
			return false;
		}

		if (entity instanceof BeeEntity beeEntity) {
			if (flowerPos != null && !beeEntity.hasFlower() && world.random.nextFloat() < 0.9F) {
				beeEntity.setFlowerPos(flowerPos);
			}

			if (beeState == BeehiveBlockEntity.BeeState.HONEY_DELIVERED) {
				beeEntity.onHoneyDelivered();

				if (state.isIn(BlockTags.BEEHIVES, statex -> statex.contains(BeehiveBlock.HONEY_LEVEL))) {
					int honeyLevel = getHoneyLevel(state);

					if (honeyLevel < 5) {
						int honeyIncrement = world.random.nextInt(100) == 0 ? 2 : 1;
						if (honeyLevel + honeyIncrement > 5) {
							honeyIncrement--;
						}

						world.setBlockState(pos, state.with(BeehiveBlock.HONEY_LEVEL, honeyLevel + honeyIncrement));
					}
				}
			}

			if (entities != null) {
				entities.add(beeEntity);
			}

			float entityWidth = entity.getWidth();
			double offset = exitBlocked ? 0.0 : 0.55 + entityWidth / 2.0F;
			double spawnX = pos.getX() + 0.5 + offset * direction.getOffsetX();
			double spawnY = pos.getY() + 0.5 - entity.getHeight() / 2.0F;
			double spawnZ = pos.getZ() + 0.5 + offset * direction.getOffsetZ();
			entity.refreshPositionAndAngles(spawnX, spawnY, spawnZ, entity.getYaw(), entity.getPitch());
		}

		world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.BLOCKS, 1.0F, 1.0F);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(entity, world.getBlockState(pos)));
		return world.spawnEntity(entity);
	}

	private boolean hasFlowerPos() {
		return flowerPos != null;
	}

	private static void tickBees(
			World world,
			BlockPos pos,
			BlockState state,
			List<BeehiveBlockEntity.Bee> bees,
			@Nullable BlockPos flowerPos
	) {
		boolean anyReleased = false;
		Iterator<BeehiveBlockEntity.Bee> iterator = bees.iterator();

		while (iterator.hasNext()) {
			BeehiveBlockEntity.Bee bee = iterator.next();

			if (bee.canExitHive()) {
				BeehiveBlockEntity.BeeState beeState = bee.hasNectar()
						? BeehiveBlockEntity.BeeState.HONEY_DELIVERED
						: BeehiveBlockEntity.BeeState.BEE_RELEASED;

				if (releaseBee(world, pos, state, bee.createData(), null, beeState, flowerPos)) {
					anyReleased = true;
					iterator.remove();
				}
			}
		}

		if (anyReleased) {
			markDirty(world, pos, state);
		}
	}

	public static void serverTick(World world, BlockPos pos, BlockState state, BeehiveBlockEntity blockEntity) {
		tickBees(world, pos, state, blockEntity.bees, blockEntity.flowerPos);

		if (!blockEntity.bees.isEmpty() && world.getRandom().nextDouble() < 0.005) {
			double centerX = pos.getX() + 0.5;
			double centerY = pos.getY();
			double centerZ = pos.getZ() + 0.5;
			world.playSound(null, centerX, centerY, centerZ, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
	}

	@Override
	protected void readData(ReadView view) {
		super.readData(view);
		bees.clear();
		view.<List<BeehiveBlockEntity.BeeData>>read(BEES_KEY, BeehiveBlockEntity.BeeData.LIST_CODEC)
				.orElse(List.of())
				.forEach(this::addBee);
		flowerPos = view.<BlockPos>read(FLOWER_POS_KEY, BlockPos.CODEC).orElse(null);
	}

	@Override
	protected void writeData(WriteView view) {
		super.writeData(view);
		view.put(BEES_KEY, BeehiveBlockEntity.BeeData.LIST_CODEC, createBeesData());
		view.putNullable(FLOWER_POS_KEY, BlockPos.CODEC, flowerPos);
	}

	@Override
	protected void readComponents(ComponentsAccess components) {
		super.readComponents(components);
		bees.clear();
		components.getOrDefault(DataComponentTypes.BEES, BeesComponent.DEFAULT)
				.bees()
				.forEach(this::addBee);
	}

	@Override
	protected void addComponents(ComponentMap.Builder builder) {
		super.addComponents(builder);
		builder.add(DataComponentTypes.BEES, new BeesComponent(createBeesData()));
	}

	@Override
	public void removeFromCopiedStackData(WriteView view) {
		super.removeFromCopiedStackData(view);
		view.remove(BEES_KEY);
	}

	private List<BeehiveBlockEntity.BeeData> createBeesData() {
		return bees.stream().map(BeehiveBlockEntity.Bee::createData).toList();
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
		tracker.track(DebugSubscriptionTypes.BEE_HIVES, () -> BeeHiveDebugData.fromBeehive(this));
	}

	static class Bee {

		private final BeehiveBlockEntity.BeeData data;
		private int ticksInHive;

		Bee(BeehiveBlockEntity.BeeData data) {
			this.data = data;
			ticksInHive = data.ticksInHive();
		}

		public boolean canExitHive() {
			return ticksInHive++ > data.minTicksInHive;
		}

		public BeehiveBlockEntity.BeeData createData() {
			return new BeehiveBlockEntity.BeeData(data.entityData, ticksInHive, data.minTicksInHive);
		}

		public boolean hasNectar() {
			return data.entityData.getNbtWithoutId().getBoolean("HasNectar", false);
		}
	}

	/**
	 * Сериализуемые данные пчелы, хранящейся в улье: NBT-данные сущности,
	 * количество тиков внутри и минимальный порог для выхода.
	 */
	public record BeeData(TypedEntityData<EntityType<?>> entityData, int ticksInHive, int minTicksInHive) {

		public static final Codec<BeehiveBlockEntity.BeeData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    TypedEntityData
								                    .createCodec(EntityType.CODEC)
								                    .fieldOf("entity_data")
								                    .forGetter(BeehiveBlockEntity.BeeData::entityData),
						                    Codec.INT.fieldOf("ticks_in_hive").forGetter(BeehiveBlockEntity.BeeData::ticksInHive),
						                    Codec.INT.fieldOf("min_ticks_in_hive").forGetter(BeehiveBlockEntity.BeeData::minTicksInHive)
				                    )
				                    .apply(instance, BeehiveBlockEntity.BeeData::new)
		);
		public static final Codec<List<BeehiveBlockEntity.BeeData>> LIST_CODEC = CODEC.listOf();
		public static final PacketCodec<RegistryByteBuf, BeehiveBlockEntity.BeeData> PACKET_CODEC = PacketCodec.tuple(
				TypedEntityData.createPacketCodec(EntityType.PACKET_CODEC),
				BeehiveBlockEntity.BeeData::entityData,
				PacketCodecs.VAR_INT,
				BeehiveBlockEntity.BeeData::ticksInHive,
				PacketCodecs.VAR_INT,
				BeehiveBlockEntity.BeeData::minTicksInHive,
				BeehiveBlockEntity.BeeData::new
		);

		public static BeehiveBlockEntity.BeeData of(Entity entity) {
			BeehiveBlockEntity.BeeData result;
			try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
					entity.getErrorReporterContext(),
					BeehiveBlockEntity.LOGGER
			)) {
				NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
				entity.saveData(nbtWriteView);
				BeehiveBlockEntity.IRRELEVANT_BEE_NBT_KEYS.forEach(nbtWriteView::remove);
				NbtCompound nbtCompound = nbtWriteView.getNbt();
				boolean hasNectar = nbtCompound.getBoolean("HasNectar", false);
				result = new BeehiveBlockEntity.BeeData(
						TypedEntityData.create(entity.getType(), nbtCompound),
						0,
						hasNectar ? MIN_OCCUPATION_TICKS_WITH_NECTAR : MIN_OCCUPATION_TICKS_WITHOUT_NECTAR
				);
			}

			return result;
		}

		public static BeehiveBlockEntity.BeeData create(int ticksInHive) {
			return new BeehiveBlockEntity.BeeData(
					TypedEntityData.create(EntityType.BEE, new NbtCompound()),
					ticksInHive,
					MIN_OCCUPATION_TICKS_WITHOUT_NECTAR
			);
		}

		public @Nullable Entity loadEntity(World world, BlockPos pos) {
			NbtCompound nbtCompound = entityData.copyNbtWithoutId();
			BeehiveBlockEntity.IRRELEVANT_BEE_NBT_KEYS.forEach(nbtCompound::remove);
			Entity entity = EntityType.loadEntityWithPassengers(
					entityData.getType(),
					nbtCompound,
					world,
					SpawnReason.LOAD,
					LoadedEntityProcessor.NOOP
			);

			if (entity == null || !entity.getType().isIn(EntityTypeTags.BEEHIVE_INHABITORS)) {
				return null;
			}

			entity.setNoGravity(true);

			if (entity instanceof BeeEntity beeEntity) {
				beeEntity.setHivePos(pos);
				tickEntity(ticksInHive, beeEntity);
			}

			return entity;
		}

		private static void tickEntity(int ticksInHive, BeeEntity beeEntity) {
			int breedingAge = beeEntity.getBreedingAge();

			if (breedingAge < 0) {
				beeEntity.setBreedingAge(Math.min(0, breedingAge + ticksInHive));
			}

			if (breedingAge > 0) {
				beeEntity.setBreedingAge(Math.max(0, breedingAge - ticksInHive));
			}

			beeEntity.setLoveTicks(Math.max(0, beeEntity.getLoveTicks() - ticksInHive));
		}
	}

	public enum BeeState {
		HONEY_DELIVERED,
		BEE_RELEASED,
		EMERGENCY;
	}
}
