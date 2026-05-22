package net.minecraft.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.Spawner;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Предмет «Яйцо призыва». При использовании на блоке-спавнере меняет тип существа.
 * При использовании на жидкости или твёрдом блоке — спавнит существо рядом.
 * Поддерживает спавн детёнышей при использовании на уже существующем существе.
 */
public class SpawnEggItem extends Item {

	private static final Map<EntityType<?>, SpawnEggItem> SPAWN_EGGS = Maps.newIdentityHashMap();

	public SpawnEggItem(Item.Settings settings) {
		super(settings);
		TypedEntityData<EntityType<?>> entityData = getComponents().get(DataComponentTypes.ENTITY_DATA);

		if (entityData != null) {
			SPAWN_EGGS.put(entityData.getType(), this);
		}
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();

		if (world instanceof ServerWorld serverWorld) {
			return useOnBlockServer(context, serverWorld);
		}

		return ActionResult.SUCCESS;
	}

	private ActionResult useOnBlockServer(ItemUsageContext context, ServerWorld serverWorld) {
		ItemStack stack = context.getStack();
		BlockPos pos = context.getBlockPos();
		Direction side = context.getSide();
		BlockState blockState = serverWorld.getBlockState(pos);

		if (serverWorld.getBlockEntity(pos) instanceof Spawner spawner) {
			return useOnSpawner(context, serverWorld, stack, pos, blockState, spawner);
		}

		BlockPos spawnPos = blockState.getCollisionShape(serverWorld, pos).isEmpty()
			? pos
			: pos.offset(side);

		return spawnMobEntity(
			context.getPlayer(),
			stack,
			serverWorld,
			spawnPos,
			true,
			!Objects.equals(pos, spawnPos) && side == Direction.UP
		);
	}

	private ActionResult useOnSpawner(
		ItemUsageContext context,
		ServerWorld serverWorld,
		ItemStack stack,
		BlockPos pos,
		BlockState blockState,
		Spawner spawner
	) {
		EntityType<?> entityType = getEntityType(stack);

		if (entityType == null) {
			return ActionResult.FAIL;
		}

		if (!serverWorld.areSpawnerBlocksEnabled()) {
			if (context.getPlayer() instanceof ServerPlayerEntity serverPlayer) {
				serverPlayer.sendMessage(Text.translatable("advMode.notEnabled.spawner"));
			}

			return ActionResult.FAIL;
		}

		spawner.setEntityType(entityType, serverWorld.getRandom());
		serverWorld.updateListeners(pos, blockState, blockState, 3);
		serverWorld.emitGameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, pos);
		stack.decrement(1);

		return ActionResult.SUCCESS;
	}

	private ActionResult spawnMobEntity(
		@Nullable LivingEntity entity,
		ItemStack stack,
		World world,
		BlockPos pos,
		boolean onGround,
		boolean offsetY
	) {
		EntityType<?> entityType = getEntityType(stack);

		if (entityType == null) {
			return ActionResult.FAIL;
		}

		if (!entityType.isAllowedInPeaceful() && world.getDifficulty() == Difficulty.PEACEFUL) {
			return ActionResult.FAIL;
		}

		boolean spawned = entityType.spawnFromItemStack(
			(ServerWorld) world,
			stack,
			entity,
			pos,
			SpawnReason.SPAWN_ITEM_USE,
			onGround,
			offsetY
		) != null;

		if (spawned) {
			stack.decrementUnlessCreative(1, entity);
			world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, pos);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		BlockHitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.SOURCE_ONLY);

		if (hitResult.getType() != HitResult.Type.BLOCK) {
			return ActionResult.PASS;
		}

		if (world instanceof ServerWorld serverWorld) {
			return useOnFluidServer(serverWorld, user, stack, hitResult);
		}

		return ActionResult.SUCCESS;
	}

	private ActionResult useOnFluidServer(
		ServerWorld serverWorld,
		PlayerEntity user,
		ItemStack stack,
		BlockHitResult hitResult
	) {
		BlockPos pos = hitResult.getBlockPos();

		if (!(serverWorld.getBlockState(pos).getBlock() instanceof FluidBlock)) {
			return ActionResult.PASS;
		}

		if (!serverWorld.canEntityModifyAt(user, pos)
			|| !user.canPlaceOn(pos, hitResult.getSide(), stack)
		) {
			return ActionResult.FAIL;
		}

		ActionResult result = spawnMobEntity(user, stack, serverWorld, pos, false, false);

		if (result == ActionResult.SUCCESS) {
			user.incrementStat(Stats.USED.getOrCreateStat(this));
		}

		return result;
	}

	public boolean isOfSameEntityType(ItemStack stack, EntityType<?> entityType) {
		return Objects.equals(getEntityType(stack), entityType);
	}

	/**
	 * Возвращает яйцо призыва для указанного типа сущности, или {@code null} если не найдено.
	 *
	 * @param type тип сущности
	 * @return яйцо призыва или {@code null}
	 */
	public static @Nullable SpawnEggItem forEntity(@Nullable EntityType<?> type) {
		return SPAWN_EGGS.get(type);
	}

	public static Iterable<SpawnEggItem> getAll() {
		return Iterables.unmodifiableIterable(SPAWN_EGGS.values());
	}

	public @Nullable EntityType<?> getEntityType(ItemStack stack) {
		TypedEntityData<EntityType<?>> entityData = stack.get(DataComponentTypes.ENTITY_DATA);
		return entityData != null ? entityData.getType() : null;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return Optional.ofNullable(getComponents().get(DataComponentTypes.ENTITY_DATA))
			.map(TypedEntityData::getType)
			.map(EntityType::getRequiredFeatures)
			.orElseGet(FeatureSet::empty);
	}

	/**
	 * Спавнит детёныша существа при использовании яйца на взрослой особи.
	 * Если тип яйца не совпадает с типом существа — возвращает пустой Optional.
	 *
	 * @param user       игрок, использующий яйцо
	 * @param entity     существо, на которое использовано яйцо
	 * @param entityType тип существа для спавна детёныша
	 * @param world      серверный мир
	 * @param pos        позиция спавна
	 * @param stack      стек яйца призыва
	 * @return Optional с заспавненным детёнышем или пустой
	 */
	public Optional<MobEntity> spawnBaby(
		PlayerEntity user,
		MobEntity entity,
		EntityType<? extends MobEntity> entityType,
		ServerWorld world,
		Vec3d pos,
		ItemStack stack
	) {
		if (!isOfSameEntityType(stack, entityType)) {
			return Optional.empty();
		}

		MobEntity baby = entity instanceof PassiveEntity passive
			? passive.createChild(world, passive)
			: entityType.create(world, SpawnReason.SPAWN_ITEM_USE);

		if (baby == null) {
			return Optional.empty();
		}

		baby.setBaby(true);

		if (!baby.isBaby()) {
			return Optional.empty();
		}

		baby.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
		baby.copyComponentsFrom(stack);
		world.spawnEntityAndPassengers(baby);
		stack.decrementUnlessCreative(1, user);

		return Optional.of(baby);
	}

	@Override
	public boolean shouldShowOperatorBlockWarnings(ItemStack stack, @Nullable PlayerEntity player) {
		if (player == null || !player.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS)) {
			return false;
		}

		TypedEntityData<EntityType<?>> entityData = stack.get(DataComponentTypes.ENTITY_DATA);

		return entityData != null && entityData.getType().canPotentiallyExecuteCommands();
	}
}
