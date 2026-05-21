package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.control.AquaticMoveControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.spawn.SpawnContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * {@code FrogEntity}.
 */
public class FrogEntity extends AnimalEntity {

	protected static final ImmutableList<SensorType<? extends Sensor<? super FrogEntity>>> SENSORS = ImmutableList.of(
			SensorType.NEAREST_LIVING_ENTITIES,
			SensorType.HURT_BY,
			SensorType.FROG_ATTACKABLES,
			SensorType.FROG_TEMPTATIONS,
			SensorType.IS_IN_WATER
	);
	protected static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.MOBS,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.WALK_TARGET,
			MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			MemoryModuleType.PATH,
			MemoryModuleType.BREED_TARGET,
			MemoryModuleType.LONG_JUMP_COOLING_DOWN,
			MemoryModuleType.LONG_JUMP_MID_JUMP,
			MemoryModuleType.ATTACK_TARGET,
			MemoryModuleType.TEMPTING_PLAYER,
			MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
			new MemoryModuleType[]{
					MemoryModuleType.IS_TEMPTED,
					MemoryModuleType.HURT_BY,
					MemoryModuleType.HURT_BY_ENTITY,
					MemoryModuleType.NEAREST_ATTACKABLE,
					MemoryModuleType.IS_IN_WATER,
					MemoryModuleType.IS_PREGNANT,
					MemoryModuleType.IS_PANICKING,
					MemoryModuleType.UNREACHABLE_TONGUE_TARGETS
			}
	);
	private static final TrackedData<RegistryEntry<FrogVariant>>
			VARIANT =
			DataTracker.registerData(FrogEntity.class, TrackedDataHandlerRegistry.FROG_VARIANT);
	private static final TrackedData<OptionalInt>
			TARGET =
			DataTracker.registerData(FrogEntity.class, TrackedDataHandlerRegistry.OPTIONAL_INT);
	private static final int MAX_HEAD_ROTATION = 5;
	private static final RegistryKey<FrogVariant> DEFAULT_VARIANT_KEY = FrogVariants.TEMPERATE;
	public final AnimationState longJumpingAnimationState = new AnimationState();
	public final AnimationState croakingAnimationState = new AnimationState();
	public final AnimationState usingTongueAnimationState = new AnimationState();
	public final AnimationState idlingInWaterAnimationState = new AnimationState();

	public FrogEntity(EntityType<? extends AnimalEntity> entityType, World world) {
		super(entityType, world);
		this.lookControl = new FrogEntity.FrogLookControl(this);
		this.setPathfindingPenalty(PathNodeType.WATER, 4.0F);
		this.setPathfindingPenalty(PathNodeType.TRAPDOOR, -1.0F);
		this.moveControl = new AquaticMoveControl(this, 85, 10, 0.02F, 0.1F, true);
	}

	@Override
	protected Brain.Profile<FrogEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULES, SENSORS);
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return FrogBrain.create(this.createBrainProfile().deserialize(dynamic));
	}

	@Override
	public Brain<FrogEntity> getBrain() {
		return (Brain<FrogEntity>) super.getBrain();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		Registry<FrogVariant> registry = this.getRegistryManager().getOrThrow(RegistryKeys.FROG_VARIANT);
		builder.add(VARIANT, Variants.getOrDefaultOrThrow(this.getRegistryManager(), DEFAULT_VARIANT_KEY));
		builder.add(TARGET, OptionalInt.empty());
	}

	public void clearFrogTarget() {
		this.dataTracker.set(TARGET, OptionalInt.empty());
	}

	public Optional<Entity> getFrogTarget() {
		return this.dataTracker
				.get(TARGET)
				.stream()
				.mapToObj(this.getEntityWorld()::getEntityById)
				.filter(Objects::nonNull)
				.findFirst();
	}

	public void setFrogTarget(Entity entity) {
		this.dataTracker.set(TARGET, OptionalInt.of(entity.getId()));
	}

	@Override
	public int getMaxLookYawChange() {
		return 35;
	}

	@Override
	public int getMaxHeadRotation() {
		return 5;
	}

	public RegistryEntry<FrogVariant> getVariant() {
		return this.dataTracker.get(VARIANT);
	}

	private void setVariant(RegistryEntry<FrogVariant> variant) {
		this.dataTracker.set(VARIANT, variant);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.FROG_VARIANT ? castComponentValue((ComponentType<T>) type, this.getVariant())
		                                               : super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		this.copyComponentFrom(from, DataComponentTypes.FROG_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.FROG_VARIANT) {
			this.setVariant(castComponentValue(DataComponentTypes.FROG_VARIANT, value));
			return true;
		}
		else {
			return super.setApplicableComponent(type, value);
		}
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		Variants.writeData(view, this.getVariant());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		Variants.fromData(view, RegistryKeys.FROG_VARIANT).ifPresent(this::setVariant);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("frogBrain");
		this.getBrain().tick(world, this);
		profiler.pop();
		profiler.push("frogActivityUpdate");
		FrogBrain.updateActivities(this);
		profiler.pop();
		super.mobTick(world);
	}

	@Override
	public void tick() {
		if (this.getEntityWorld().isClient()) {
			this.idlingInWaterAnimationState.setRunning(
					this.isTouchingWater() && !this.limbAnimator.isLimbMoving(),
					this.age
			);
		}

		super.tick();
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (POSE.equals(data)) {
			EntityPose entityPose = this.getPose();
			if (entityPose == EntityPose.LONG_JUMPING) {
				this.longJumpingAnimationState.start(this.age);
			}
			else {
				this.longJumpingAnimationState.stop();
			}

			if (entityPose == EntityPose.CROAKING) {
				this.croakingAnimationState.start(this.age);
			}
			else {
				this.croakingAnimationState.stop();
			}

			if (entityPose == EntityPose.USING_TONGUE) {
				this.usingTongueAnimationState.start(this.age);
			}
			else {
				this.usingTongueAnimationState.stop();
			}
		}

		super.onTrackedDataSet(data);
	}

	@Override
	protected void updateLimbs(float posDelta) {
		float f;
		if (this.longJumpingAnimationState.isRunning()) {
			f = 0.0F;
		}
		else {
			f = Math.min(posDelta * 25.0F, 1.0F);
		}

		this.limbAnimator.updateLimbs(f, 0.4F, this.isBaby() ? 3.0F : 1.0F);
	}

	@Override
	public void playEatSound() {
		this
				.getEntityWorld()
				.playSoundFromEntity(null, this, SoundEvents.ENTITY_FROG_EAT, SoundCategory.NEUTRAL, 2.0F, 1.0F);
	}

	@Override
	public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
		FrogEntity frogEntity = EntityType.FROG.create(world, SpawnReason.BREEDING);
		if (frogEntity != null) {
			FrogBrain.coolDownLongJump(frogEntity, world.getRandom());
		}

		return frogEntity;
	}

	@Override
	public boolean isBaby() {
		return false;
	}

	@Override
	public void setBaby(boolean baby) {
	}

	@Override
	public void breed(ServerWorld world, AnimalEntity other) {
		this.breed(world, other, null);
		this.getBrain().remember(MemoryModuleType.IS_PREGNANT, Unit.INSTANCE);
	}

	@Override
	public EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Variants
				.select(SpawnContext.of(world, this.getBlockPos()), RegistryKeys.FROG_VARIANT)
				.ifPresent(this::setVariant);
		FrogBrain.coolDownLongJump(this, world.getRandom());
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public static DefaultAttributeContainer.Builder createFrogAttributes() {
		return AnimalEntity.createAnimalAttributes()
		                   .add(EntityAttributes.MOVEMENT_SPEED, 1.0)
		                   .add(EntityAttributes.MAX_HEALTH, 10.0)
		                   .add(EntityAttributes.ATTACK_DAMAGE, 10.0)
		                   .add(EntityAttributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_FROG_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_FROG_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_FROG_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(SoundEvents.ENTITY_FROG_STEP, 0.15F, 1.0F);
	}

	@Override
	public boolean isPushedByFluids() {
		return false;
	}

	@Override
	protected int computeFallDamage(double fallDistance, float damagePerDistance) {
		return super.computeFallDamage(fallDistance, damagePerDistance) - 5;
	}

	@Override
	protected void travelInWater(Vec3d movementInput, double gravity, boolean falling, double y) {
		this.updateVelocity(this.getMovementSpeed(), movementInput);
		this.move(MovementType.SELF, this.getVelocity());
		this.setVelocity(this.getVelocity().multiply(0.9));
	}

	public static boolean isValidFrogFood(LivingEntity entity) {
		return entity instanceof SlimeEntity slimeEntity && slimeEntity.getSize() != 1 ? false : entity
		                                                                                         .getType()
		                                                                                         .isIn(EntityTypeTags.FROG_FOOD);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new FrogEntity.FrogSwimNavigation(this, world);
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return this.getTargetInBrain();
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.FROG_FOOD);
	}

	public static boolean canSpawn(
			EntityType<? extends AnimalEntity> type,
			WorldAccess world,
			SpawnReason reason,
			BlockPos pos,
			Random random
	) {
		return world.getBlockState(pos.down()).isIn(BlockTags.FROGS_SPAWNABLE_ON) && isLightLevelValidForNaturalSpawn(
				world,
				pos
		);
	}

	/**
	 * {@code FrogLookControl}.
	 */
	class FrogLookControl extends LookControl {

		FrogLookControl(final MobEntity entity) {
			super(entity);
		}

		@Override
		protected boolean shouldStayHorizontal() {
			return FrogEntity.this.getFrogTarget().isEmpty();
		}
	}

	/**
	 * {@code FrogSwimNavigation}.
	 */
	static class FrogSwimNavigation extends AmphibiousSwimNavigation {

		FrogSwimNavigation(FrogEntity frog, World world) {
			super(frog, world);
		}

		@Override
		public boolean canJumpToNext(PathNodeType nodeType) {
			return nodeType != PathNodeType.WATER_BORDER && super.canJumpToNext(nodeType);
		}

		@Override
		protected PathNodeNavigator createPathNodeNavigator(int range) {
			this.nodeMaker = new FrogEntity.FrogSwimPathNodeMaker(true);
			return new PathNodeNavigator(this.nodeMaker, range);
		}
	}

	/**
	 * {@code FrogSwimPathNodeMaker}.
	 */
	static class FrogSwimPathNodeMaker extends AmphibiousPathNodeMaker {

		private final BlockPos.Mutable pos = new BlockPos.Mutable();

		public FrogSwimPathNodeMaker(boolean bl) {
			super(bl);
		}

		@Override
		public PathNode getStart() {
			return !this.entity.isTouchingWater()
			       ? super.getStart()
			       : this.getStart(
					       new BlockPos(
							       MathHelper.floor(this.entity.getBoundingBox().minX),
							       MathHelper.floor(this.entity.getBoundingBox().minY),
							       MathHelper.floor(this.entity.getBoundingBox().minZ)
					       )
			       );
		}

		@Override
		public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
			this.pos.set(x, y - 1, z);
			BlockState blockState = context.getBlockState(this.pos);
			return blockState.isIn(BlockTags.FROG_PREFER_JUMP_TO) ? PathNodeType.OPEN
			                                                      : super.getDefaultNodeType(context, x, y, z);
		}
	}
}
