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
 * {@code SpawnEggItem}.
 */
public class SpawnEggItem extends Item {

	private static final Map<EntityType<?>, SpawnEggItem> SPAWN_EGGS = Maps.newIdentityHashMap();

	public SpawnEggItem(Item.Settings settings) {
		super(settings);
		TypedEntityData<EntityType<?>> typedEntityData = this.getComponents().get(DataComponentTypes.ENTITY_DATA);
		if (typedEntityData != null) {
			SPAWN_EGGS.put(typedEntityData.getType(), this);
		}
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.SUCCESS;
		}
		else {
			ItemStack itemStack = context.getStack();
			BlockPos blockPos = context.getBlockPos();
			Direction direction = context.getSide();
			BlockState blockState = world.getBlockState(blockPos);
			if (world.getBlockEntity(blockPos) instanceof Spawner spawner) {
				EntityType<?> entityType = this.getEntityType(itemStack);
				if (entityType == null) {
					return ActionResult.FAIL;
				}
				else if (!serverWorld.areSpawnerBlocksEnabled()) {
					if (context.getPlayer() instanceof ServerPlayerEntity serverPlayerEntity) {
						serverPlayerEntity.sendMessage(Text.translatable("advMode.notEnabled.spawner"));
					}

					return ActionResult.FAIL;
				}
				else {
					spawner.setEntityType(entityType, world.getRandom());
					world.updateListeners(blockPos, blockState, blockState, 3);
					world.emitGameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, blockPos);
					itemStack.decrement(1);
					return ActionResult.SUCCESS;
				}
			}
			else {
				BlockPos blockPos2;
				if (blockState.getCollisionShape(world, blockPos).isEmpty()) {
					blockPos2 = blockPos;
				}
				else {
					blockPos2 = blockPos.offset(direction);
				}

				return this.spawnMobEntity(
						context.getPlayer(),
						itemStack,
						world,
						blockPos2,
						true,
						!Objects.equals(blockPos, blockPos2) && direction == Direction.UP
				);
			}
		}
	}

	private ActionResult spawnMobEntity(
			@Nullable LivingEntity entity,
			ItemStack stack,
			World world,
			BlockPos pos,
			boolean bl,
			boolean bl2
	) {
		EntityType<?> entityType = this.getEntityType(stack);
		if (entityType == null) {
			return ActionResult.FAIL;
		}
		else if (!entityType.isAllowedInPeaceful() && world.getDifficulty() == Difficulty.PEACEFUL) {
			return ActionResult.FAIL;
		}
		else {
			if (entityType.spawnFromItemStack(
					(ServerWorld) world,
					stack,
					entity,
					pos,
					SpawnReason.SPAWN_ITEM_USE,
					bl,
					bl2
			) != null) {
				stack.decrementUnlessCreative(1, entity);
				world.emitGameEvent(entity, GameEvent.ENTITY_PLACE, pos);
			}

			return ActionResult.SUCCESS;
		}
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		BlockHitResult blockHitResult = raycast(world, user, RaycastContext.FluidHandling.SOURCE_ONLY);
		if (blockHitResult.getType() != HitResult.Type.BLOCK) {
			return ActionResult.PASS;
		}
		else if (world instanceof ServerWorld serverWorld) {
			BlockPos blockPos = blockHitResult.getBlockPos();
			if (!(world.getBlockState(blockPos).getBlock() instanceof FluidBlock)) {
				return ActionResult.PASS;
			}
			else if (world.canEntityModifyAt(user, blockPos) && user.canPlaceOn(
					blockPos,
					blockHitResult.getSide(),
					itemStack
			)) {
				ActionResult actionResult = this.spawnMobEntity(user, itemStack, world, blockPos, false, false);
				if (actionResult == ActionResult.SUCCESS) {
					user.incrementStat(Stats.USED.getOrCreateStat(this));
				}

				return actionResult;
			}
			else {
				return ActionResult.FAIL;
			}
		}
		else {
			return ActionResult.SUCCESS;
		}
	}

	public boolean isOfSameEntityType(ItemStack stack, EntityType<?> entityType) {
		return Objects.equals(this.getEntityType(stack), entityType);
	}

	public static @Nullable SpawnEggItem forEntity(@Nullable EntityType<?> type) {
		return SPAWN_EGGS.get(type);
	}

	public static Iterable<SpawnEggItem> getAll() {
		return Iterables.unmodifiableIterable(SPAWN_EGGS.values());
	}

	public @Nullable EntityType<?> getEntityType(ItemStack stack) {
		TypedEntityData<EntityType<?>> typedEntityData = stack.get(DataComponentTypes.ENTITY_DATA);
		return typedEntityData != null ? typedEntityData.getType() : null;
	}

	@Override
	public FeatureSet getRequiredFeatures() {
		return Optional.ofNullable(this.getComponents().get(DataComponentTypes.ENTITY_DATA))
		               .map(TypedEntityData::getType)
		               .map(EntityType::getRequiredFeatures)
		               .orElseGet(FeatureSet::empty);
	}

	public Optional<MobEntity> spawnBaby(
			PlayerEntity user,
			MobEntity entity,
			EntityType<? extends MobEntity> entityType,
			ServerWorld world,
			Vec3d pos,
			ItemStack stack
	) {
		if (!this.isOfSameEntityType(stack, entityType)) {
			return Optional.empty();
		}
		else {
			MobEntity mobEntity;
			if (entity instanceof PassiveEntity) {
				mobEntity = ((PassiveEntity) entity).createChild(world, (PassiveEntity) entity);
			}
			else {
				mobEntity = entityType.create(world, SpawnReason.SPAWN_ITEM_USE);
			}

			if (mobEntity == null) {
				return Optional.empty();
			}
			else {
				mobEntity.setBaby(true);
				if (!mobEntity.isBaby()) {
					return Optional.empty();
				}
				else {
					mobEntity.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
					mobEntity.copyComponentsFrom(stack);
					world.spawnEntityAndPassengers(mobEntity);
					stack.decrementUnlessCreative(1, user);
					return Optional.of(mobEntity);
				}
			}
		}
	}

	@Override
	public boolean shouldShowOperatorBlockWarnings(ItemStack stack, @Nullable PlayerEntity player) {
		if (player != null && player.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS)) {
			TypedEntityData<EntityType<?>> typedEntityData = stack.get(DataComponentTypes.ENTITY_DATA);
			if (typedEntityData != null) {
				return typedEntityData.getType().canPotentiallyExecuteCommands();
			}
		}

		return false;
	}
}
