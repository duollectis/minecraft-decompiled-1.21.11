package net.minecraft.entity.mob;

import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Утопленник — водный зомби, умеющий плавать под водой и атаковать трезубцем на расстоянии.
 * Ночью выходит на сушу. Иммунен к утоплению. Может спавниться с наутилусом в левой руке.
 */
public class DrownedEntity extends ZombieEntity implements RangedAttackMob {

	public static final float NAUTILUS_SHELL_CHANCE = 0.03F;
	private static final float TRIDENT_CHANCE = 0.5F;
	private static final float WEAPON_SPAWN_CHANCE = 0.9F;
	boolean targetingUnderwater;

	public DrownedEntity(EntityType<? extends DrownedEntity> entityType, World world) {
		super(entityType, world);
		moveControl = new DrownedEntity.DrownedMoveControl(this);
		setPathfindingPenalty(PathNodeType.WATER, 0.0F);
	}

	public static DefaultAttributeContainer.Builder createDrownedAttributes() {
		return ZombieEntity.createZombieAttributes().add(EntityAttributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected EntityNavigation createNavigation(World world) {
		return new AmphibiousSwimNavigation(this, world);
	}

	@Override
	protected void initCustomGoals() {
		goalSelector.add(1, new DrownedEntity.WanderAroundOnSurfaceGoal(this, 1.0));
		goalSelector.add(2, new DrownedEntity.TridentAttackGoal(this, 1.0, 40, 10.0F));
		goalSelector.add(2, new DrownedEntity.DrownedAttackGoal(this, 1.0, false));
		goalSelector.add(5, new DrownedEntity.LeaveWaterGoal(this, 1.0));
		goalSelector.add(6, new DrownedEntity.TargetAboveWaterGoal(this, 1.0, getEntityWorld().getSeaLevel()));
		goalSelector.add(7, new WanderAroundGoal(this, 1.0));
		targetSelector.add(1, new RevengeGoal(this, DrownedEntity.class).setGroupRevenge(ZombifiedPiglinEntity.class));
		targetSelector.add(
			2,
			new ActiveTargetGoal<>(
				this,
				PlayerEntity.class,
				10,
				true,
				false,
				(target, world) -> canDrownedAttackTarget(target)
			)
		);
		targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, false));
		targetSelector.add(3, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
		targetSelector.add(3, new ActiveTargetGoal<>(this, AxolotlEntity.class, true, false));
		targetSelector.add(
			5,
			new ActiveTargetGoal<>(
				this,
				TurtleEntity.class,
				10,
				true,
				false,
				TurtleEntity.BABY_TURTLE_ON_LAND_FILTER
			)
		);
	}

	@Override
	public EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		entityData = super.initialize(world, difficulty, spawnReason, entityData);

		if (getEquippedStack(EquipmentSlot.OFFHAND).isEmpty() && world.getRandom().nextFloat() < NAUTILUS_SHELL_CHANCE) {
			equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.NAUTILUS_SHELL));
			setDropGuaranteed(EquipmentSlot.OFFHAND);
		}

		if ((spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.STRUCTURE)
				&& getMainHandStack().isOf(Items.TRIDENT)
				&& world.getRandom().nextFloat() < TRIDENT_CHANCE
				&& !isBaby()
				&& !world.getBiome(getBlockPos()).isIn(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)
		) {
			ZombieNautilusEntity nautilusRider = EntityType.ZOMBIE_NAUTILUS.create(getEntityWorld(), SpawnReason.JOCKEY);

			if (nautilusRider != null) {
				if (spawnReason == SpawnReason.STRUCTURE) {
					nautilusRider.setPersistent();
				}

				nautilusRider.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0F);
				nautilusRider.initialize(world, difficulty, spawnReason, null);
				startRiding(nautilusRider, false, false);
				world.spawnEntity(nautilusRider);
			}
		}

		return entityData;
	}

	public static boolean canSpawn(
			EntityType<DrownedEntity> type,
			ServerWorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		if (!world.getFluidState(pos.down()).isIn(FluidTags.WATER) && !SpawnReason.isAnySpawner(spawnReason)) {
			return false;
		}

		RegistryEntry<Biome> biome = world.getBiome(pos);
		boolean validConditions = world.getDifficulty() != Difficulty.PEACEFUL
				&& (SpawnReason.isTrialSpawner(spawnReason) || isSpawnDark(world, pos, random))
				&& (SpawnReason.isAnySpawner(spawnReason) || world.getFluidState(pos).isIn(FluidTags.WATER));

		if (validConditions && (SpawnReason.isAnySpawner(spawnReason) || spawnReason == SpawnReason.REINFORCEMENT)) {
			return true;
		}

		return biome.isIn(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)
				? random.nextInt(15) == 0 && validConditions
				: random.nextInt(40) == 0 && isValidSpawnDepth(world, pos) && validConditions;
	}

	private static boolean isValidSpawnDepth(WorldAccess world, BlockPos pos) {
		return pos.getY() < world.getSeaLevel() - 5;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isTouchingWater() ? SoundEvents.ENTITY_DROWNED_AMBIENT_WATER : SoundEvents.ENTITY_DROWNED_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isTouchingWater() ? SoundEvents.ENTITY_DROWNED_HURT_WATER : SoundEvents.ENTITY_DROWNED_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isTouchingWater() ? SoundEvents.ENTITY_DROWNED_DEATH_WATER : SoundEvents.ENTITY_DROWNED_DEATH;
	}

	@Override
	protected SoundEvent getStepSound() {
		return SoundEvents.ENTITY_DROWNED_STEP;
	}

	@Override
	protected SoundEvent getSwimSound() {
		return SoundEvents.ENTITY_DROWNED_SWIM;
	}

	@Override
	protected boolean canSpawnAsReinforcementInFluid() {
		return true;
	}

	@Override
	protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
		if (random.nextFloat() > WEAPON_SPAWN_CHANCE) {
			int roll = random.nextInt(16);
			Item weapon = roll < 10 ? Items.TRIDENT : Items.FISHING_ROD;
			equipStack(EquipmentSlot.MAINHAND, new ItemStack(weapon));
		}
	}

	@Override
	protected boolean prefersNewEquipment(ItemStack newStack, ItemStack currentStack, EquipmentSlot slot) {
		if (currentStack.isOf(Items.NAUTILUS_SHELL)) {
			return false;
		}

		return super.prefersNewEquipment(newStack, currentStack, slot);
	}

	@Override
	protected boolean canConvertInWater() {
		return false;
	}

	@Override
	public boolean canSpawn(WorldView world) {
		return world.doesNotIntersectEntities(this);
	}

	public boolean canDrownedAttackTarget(@Nullable LivingEntity target) {
		return target != null && (!getEntityWorld().isDay() || target.isTouchingWater());
	}

	@Override
	public boolean isPushedByFluids() {
		return !isSwimming();
	}

	boolean isTargetingUnderwater() {
		if (targetingUnderwater) {
			return true;
		}

		LivingEntity target = getTarget();
		return target != null && target.isTouchingWater();
	}

	@Override
	protected void travelInWater(Vec3d movementInput, double gravity, boolean falling, double y) {
		if (isSubmergedInWater() && isTargetingUnderwater()) {
			updateVelocity(0.01F, movementInput);
			move(MovementType.SELF, getVelocity());
			setVelocity(getVelocity().multiply(0.9));
		}
		else {
			super.travelInWater(movementInput, gravity, falling, y);
		}
	}

	@Override
	public void updateSwimming() {
		if (!getEntityWorld().isClient()) {
			setSwimming(canActVoluntarily() && isSubmergedInWater() && isTargetingUnderwater());
		}
	}

	@Override
	public boolean isInSwimmingPose() {
		return isSwimming() && !hasVehicle();
	}

	protected boolean hasFinishedCurrentPath() {
		Path path = getNavigation().getCurrentPath();
		if (path == null) {
			return false;
		}

		BlockPos target = path.getTarget();
		if (target == null) {
			return false;
		}

		double distSq = squaredDistanceTo(target.getX(), target.getY(), target.getZ());
		return distSq < 4.0;
	}

	/**
	 * Бросает трезубец в цель. Угол возвышения корректируется на основе горизонтального расстояния,
	 * чтобы снаряд попал в тело цели, а не в ноги.
	 */
	@Override
	public void shootAt(LivingEntity target, float pullProgress) {
		ItemStack heldStack = getMainHandStack();
		ItemStack tridentStack = heldStack.isOf(Items.TRIDENT) ? heldStack : new ItemStack(Items.TRIDENT);
		TridentEntity trident = new TridentEntity(getEntityWorld(), this, tridentStack);

		double dx = target.getX() - getX();
		double dy = target.getBodyY(0.3333333333333333) - trident.getY();
		double dz = target.getZ() - getZ();
		double horizDist = Math.sqrt(dx * dx + dz * dz);

		if (getEntityWorld() instanceof ServerWorld serverWorld) {
			ProjectileEntity.spawnWithVelocity(
					trident,
					serverWorld,
					tridentStack,
					dx,
					dy + horizDist * 0.2F,
					dz,
					1.6F,
					14 - getEntityWorld().getDifficulty().getId() * 4
			);
		}

		playSound(SoundEvents.ENTITY_DROWNED_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	public TagKey<Item> getPreferredWeapons() {
		return ItemTags.DROWNED_PREFERRED_WEAPONS;
	}

	public void setTargetingUnderwater(boolean targetingUnderwater) {
		this.targetingUnderwater = targetingUnderwater;
	}

	@Override
	public void tickRiding() {
		super.tickRiding();
		if (getControllingVehicle() instanceof PathAwareEntity pathAwareEntity) {
			bodyYaw = pathAwareEntity.bodyYaw;
		}
	}

	@Override
	public boolean canGather(ServerWorld world, ItemStack stack) {
		if (stack.isIn(ItemTags.SPEARS)) {
			return false;
		}

		return super.canGather(world, stack);
	}

	static class DrownedAttackGoal extends ZombieAttackGoal {

		private final DrownedEntity drowned;

		public DrownedAttackGoal(DrownedEntity drowned, double speed, boolean pauseWhenMobIdle) {
			super(drowned, speed, pauseWhenMobIdle);
			this.drowned = drowned;
		}

		@Override
		public boolean canStart() {
			return super.canStart() && drowned.canDrownedAttackTarget(drowned.getTarget());
		}

		@Override
		public boolean shouldContinue() {
			return super.shouldContinue() && drowned.canDrownedAttackTarget(drowned.getTarget());
		}
	}

	static class DrownedMoveControl extends MoveControl {

		private final DrownedEntity drowned;

		public DrownedMoveControl(DrownedEntity drowned) {
			super(drowned);
			this.drowned = drowned;
		}

		@Override
		public void tick() {
			LivingEntity target = drowned.getTarget();

			if (drowned.isTargetingUnderwater() && drowned.isTouchingWater()) {
				if (target != null && target.getY() > drowned.getY() || drowned.targetingUnderwater) {
					drowned.setVelocity(drowned.getVelocity().add(0.0, 0.002, 0.0));
				}

				if (state != MoveControl.State.MOVE_TO || drowned.getNavigation().isIdle()) {
					drowned.setMovementSpeed(0.0F);
					return;
				}

				double dx = targetX - drowned.getX();
				double dy = targetY - drowned.getY();
				double dz = targetZ - drowned.getZ();
				double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
				dy /= dist;
				float targetYaw = (float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
				drowned.setYaw(wrapDegrees(drowned.getYaw(), targetYaw, 90.0F));
				drowned.bodyYaw = drowned.getYaw();
				float targetSpeed = (float) (speed * drowned.getAttributeValue(EntityAttributes.MOVEMENT_SPEED));
				float lerpedSpeed = MathHelper.lerp(0.125F, drowned.getMovementSpeed(), targetSpeed);
				drowned.setMovementSpeed(lerpedSpeed);
				drowned.setVelocity(drowned.getVelocity().add(lerpedSpeed * dx * 0.005, lerpedSpeed * dy * 0.1, lerpedSpeed * dz * 0.005));
			}
			else {
				if (!drowned.isOnGround()) {
					drowned.setVelocity(drowned.getVelocity().add(0.0, -0.008, 0.0));
				}

				super.tick();
			}
		}
	}

	static class LeaveWaterGoal extends MoveToTargetPosGoal {

		private final DrownedEntity drowned;

		public LeaveWaterGoal(DrownedEntity drowned, double speed) {
			super(drowned, speed, 8, 2);
			this.drowned = drowned;
		}

		@Override
		public boolean canStart() {
			return super.canStart()
					&& !drowned.getEntityWorld().isDay()
					&& drowned.isTouchingWater()
					&& drowned.getY() >= drowned.getEntityWorld().getSeaLevel() - 3;
		}

		@Override
		public boolean shouldContinue() {
			return super.shouldContinue();
		}

		@Override
		protected boolean isTargetPos(WorldView world, BlockPos pos) {
			BlockPos above = pos.up();

			if (!world.isAir(above) || !world.isAir(above.up())) {
				return false;
			}

			return world.getBlockState(pos).hasSolidTopSurface(world, pos, drowned);
		}

		@Override
		public void start() {
			drowned.setTargetingUnderwater(false);
			super.start();
		}

		@Override
		public void stop() {
			super.stop();
		}
	}

	static class TargetAboveWaterGoal extends Goal {

		private final DrownedEntity drowned;
		private final double speed;
		private final int minY;
		private boolean foundTarget;

		public TargetAboveWaterGoal(DrownedEntity drowned, double speed, int minY) {
			this.drowned = drowned;
			this.speed = speed;
			this.minY = minY;
		}

		@Override
		public boolean canStart() {
			return !drowned.getEntityWorld().isDay()
					&& drowned.isTouchingWater()
					&& drowned.getY() < minY - 2;
		}

		@Override
		public boolean shouldContinue() {
			return canStart() && !foundTarget;
		}

		@Override
		public void tick() {
			if (drowned.getY() >= minY - 1) {
				return;
			}

			if (!drowned.getNavigation().isIdle() && !drowned.hasFinishedCurrentPath()) {
				return;
			}

			Vec3d destination = NoPenaltyTargeting.findTo(
					drowned,
					4,
					8,
					new Vec3d(drowned.getX(), minY - 1, drowned.getZ()),
					(float) (Math.PI / 2)
			);

			if (destination == null) {
				foundTarget = true;
				return;
			}

			drowned.getNavigation().startMovingTo(destination.x, destination.y, destination.z, speed);
		}

		@Override
		public void start() {
			drowned.setTargetingUnderwater(true);
			foundTarget = false;
		}

		@Override
		public void stop() {
			drowned.setTargetingUnderwater(false);
		}
	}

	static class TridentAttackGoal extends ProjectileAttackGoal {

		private final DrownedEntity drowned;

		public TridentAttackGoal(RangedAttackMob rangedAttackMob, double speed, int attackInterval, float attackRange) {
			super(rangedAttackMob, speed, attackInterval, attackRange);
			this.drowned = (DrownedEntity) rangedAttackMob;
		}

		@Override
		public boolean canStart() {
			return super.canStart() && drowned.getMainHandStack().isOf(Items.TRIDENT);
		}

		@Override
		public void start() {
			super.start();
			drowned.setAttacking(true);
			drowned.setCurrentHand(Hand.MAIN_HAND);
		}

		@Override
		public void stop() {
			super.stop();
			drowned.clearActiveItem();
			drowned.setAttacking(false);
		}
	}

	static class WanderAroundOnSurfaceGoal extends Goal {

		private final PathAwareEntity mob;
		private double targetX;
		private double targetY;
		private double targetZ;
		private final double speed;
		private final World world;

		public WanderAroundOnSurfaceGoal(PathAwareEntity mob, double speed) {
			this.mob = mob;
			this.speed = speed;
			this.world = mob.getEntityWorld();
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			if (!world.isDay()) {
				return false;
			}

			if (mob.isTouchingWater()) {
				return false;
			}

			Vec3d wanderPos = getWanderTarget();
			if (wanderPos == null) {
				return false;
			}

			targetX = wanderPos.x;
			targetY = wanderPos.y;
			targetZ = wanderPos.z;
			return true;
		}

		@Override
		public boolean shouldContinue() {
			return !mob.getNavigation().isIdle();
		}

		@Override
		public void start() {
			mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
		}

		private @Nullable Vec3d getWanderTarget() {
			Random random = mob.getRandom();
			BlockPos origin = mob.getBlockPos();

			for (int attempt = 0; attempt < 10; attempt++) {
				BlockPos candidate = origin.add(
						random.nextInt(20) - 10,
						2 - random.nextInt(8),
						random.nextInt(20) - 10
				);

				if (world.getBlockState(candidate).isOf(Blocks.WATER)) {
					return Vec3d.ofBottomCenter(candidate);
				}
			}

			return null;
		}
	}
}
