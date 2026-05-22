package net.minecraft.entity.mob;

import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.World;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.debug.data.BreezeDebugData;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Бриз — моб, атакующий порывами ветра и активирующий механизмы.
 */
public class BreezeEntity extends HostileEntity {

	private static final int ANIMATION_TICKS = 20;
	private static final int MIN_PARTICLE_COUNT = 1;
	private static final int SOUND_INTERVAL_TICKS = 20;
	private static final int JUMP_PARTICLE_COUNT = 3;
	private static final int MAX_JUMP_PARTICLE_COUNT = 5;
	private static final int WHIRL_SOUND_TICKS = 10;
	private static final float MOVEMENT_SPEED = 3.0F;
	private static final int MIN_WHIRL_SOUND_INTERVAL = 1;
	private static final int MAX_WHIRL_SOUND_INTERVAL = 80;
	public AnimationState idleAnimationState = new AnimationState();
	public AnimationState slidingAnimationState = new AnimationState();
	public AnimationState slidingBackAnimationState = new AnimationState();
	public AnimationState longJumpingAnimationState = new AnimationState();
	public AnimationState shootingAnimationState = new AnimationState();
	public AnimationState inhalingAnimationState = new AnimationState();
	private int longJumpingParticleAddCount = 0;
	private int ticksUntilWhirlSound = 0;
	private static final ProjectileDeflection PROJECTILE_DEFLECTOR = (projectile, hitEntity, random) -> {
		hitEntity
				.getEntityWorld()
				.playSoundFromEntity(
						null,
						hitEntity,
						SoundEvents.ENTITY_BREEZE_DEFLECT,
						hitEntity.getSoundCategory(),
						1.0F,
						1.0F
				);
		ProjectileDeflection.SIMPLE.deflect(projectile, hitEntity, random);
	};

	public static DefaultAttributeContainer.Builder createBreezeAttributes() {
		return MobEntity.createMobAttributes()
		                .add(EntityAttributes.MOVEMENT_SPEED, 0.63F)
		                .add(EntityAttributes.MAX_HEALTH, 30.0)
		                .add(EntityAttributes.FOLLOW_RANGE, 24.0)
		                .add(EntityAttributes.ATTACK_DAMAGE, 3.0);
	}

	public BreezeEntity(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
		setPathfindingPenalty(PathNodeType.DANGER_TRAPDOOR, -1.0F);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
		experiencePoints = WHIRL_SOUND_TICKS;
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		return BreezeBrain.create(this, this.createBrainProfile().deserialize(dynamic));
	}

	@Override
	public Brain<BreezeEntity> getBrain() {
		return (Brain<BreezeEntity>) super.getBrain();
	}

	@Override
	protected Brain.Profile<BreezeEntity> createBrainProfile() {
		return Brain.createProfile(BreezeBrain.MEMORY_MODULES, BreezeBrain.SENSORS);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (getEntityWorld().isClient() && POSE.equals(data)) {
			stopAnimations();
			switch (getPose()) {
				case SHOOTING -> shootingAnimationState.startIfNotRunning(age);
				case INHALING -> inhalingAnimationState.startIfNotRunning(age);
				case SLIDING -> slidingAnimationState.startIfNotRunning(age);
				default -> {}
			}
		}

		super.onTrackedDataSet(data);
	}

	private void stopAnimations() {
		shootingAnimationState.stop();
		idleAnimationState.stop();
		inhalingAnimationState.stop();
		longJumpingAnimationState.stop();
	}

	@Override
	public void tick() {
		EntityPose pose = getPose();
		switch (pose) {
			case SHOOTING, INHALING, STANDING ->
					resetLongJumpingParticleAddCount().addBlockParticles(1 + getRandom().nextInt(1));
			case SLIDING -> addBlockParticles(ANIMATION_TICKS);
			case LONG_JUMPING -> {
				longJumpingAnimationState.startIfNotRunning(age);
				addLongJumpingParticles();
			}
			default -> {}
		}

		idleAnimationState.startIfNotRunning(age);
		if (pose != EntityPose.SLIDING && slidingAnimationState.isRunning()) {
			slidingBackAnimationState.start(age);
			slidingAnimationState.stop();
		}

		ticksUntilWhirlSound = ticksUntilWhirlSound == 0
				? random.nextBetween(MIN_WHIRL_SOUND_INTERVAL, MAX_WHIRL_SOUND_INTERVAL)
				: ticksUntilWhirlSound - 1;
		if (ticksUntilWhirlSound == 0) {
			playWhirlSound();
		}

		super.tick();
	}

	public BreezeEntity resetLongJumpingParticleAddCount() {
		longJumpingParticleAddCount = 0;
		return this;
	}

	public void addLongJumpingParticles() {
		if (++longJumpingParticleAddCount > MAX_JUMP_PARTICLE_COUNT) {
			return;
		}

		BlockState blockState = !getBlockStateAtPos().isAir()
				? getBlockStateAtPos()
				: getSteppingBlockState();
		Vec3d velocity = getVelocity();
		Vec3d spawnPos = getEntityPos().add(velocity).add(0.0, 0.1F, 0.0);

		for (int particleIndex = 0; particleIndex < JUMP_PARTICLE_COUNT; particleIndex++) {
			getEntityWorld()
					.addParticleClient(
							new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
							spawnPos.x,
							spawnPos.y,
							spawnPos.z,
							0.0,
							0.0,
							0.0
					);
		}
	}

	public void addBlockParticles(int count) {
		if (hasVehicle()) {
			return;
		}

		Vec3d center = getBoundingBox().getCenter();
		Vec3d spawnPos = new Vec3d(center.x, getEntityPos().y, center.z);
		BlockState blockState = !getBlockStateAtPos().isAir()
				? getBlockStateAtPos()
				: getSteppingBlockState();
		if (blockState.getRenderType() == BlockRenderType.INVISIBLE) {
			return;
		}

		for (int particleIndex = 0; particleIndex < count; particleIndex++) {
			getEntityWorld()
					.addParticleClient(
							new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState),
							spawnPos.x,
							spawnPos.y,
							spawnPos.z,
							0.0,
							0.0,
							0.0
					);
		}
	}

	@Override
	public void playAmbientSound() {
		if (getTarget() == null || !isOnGround()) {
			getEntityWorld()
					.playSoundFromEntityClient(this, getAmbientSound(), getSoundCategory(), 1.0F, 1.0F);
		}
	}

	public void playWhirlSound() {
		float pitch = 0.7F + 0.4F * random.nextFloat();
		float volume = 0.8F + 0.2F * random.nextFloat();
		getEntityWorld()
				.playSoundFromEntityClient(this, SoundEvents.ENTITY_BREEZE_WHIRL, getSoundCategory(), volume, pitch);
	}

	@Override
	public ProjectileDeflection getProjectileDeflection(ProjectileEntity projectile) {
		if (projectile.getType() == EntityType.BREEZE_WIND_CHARGE || projectile.getType() == EntityType.WIND_CHARGE) {
			return ProjectileDeflection.NONE;
		}

		return getType().isIn(EntityTypeTags.DEFLECTS_PROJECTILES) ? PROJECTILE_DEFLECTOR : ProjectileDeflection.NONE;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_BREEZE_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_BREEZE_HURT;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.isOnGround() ? SoundEvents.ENTITY_BREEZE_IDLE_GROUND : SoundEvents.ENTITY_BREEZE_IDLE_AIR;
	}

	public Optional<LivingEntity> getHurtBy() {
		return getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.HURT_BY)
				.map(DamageSource::getAttacker)
				.filter(attacker -> attacker instanceof LivingEntity)
				.map(livingAttacker -> (LivingEntity) livingAttacker);
	}

	public boolean isWithinShortRange(Vec3d pos) {
		Vec3d blockCenter = getBlockPos().toCenterPos();
		return pos.isWithinRangeOf(blockCenter, 4.0, 10.0);
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("breezeBrain");
		getBrain().tick(world, this);
		profiler.swap("breezeActivityUpdate");
		BreezeBrain.updateActivities(this);
		profiler.pop();
		super.mobTick(world);
	}

	@Override
	public boolean canTarget(EntityType<?> type) {
		return type == EntityType.PLAYER || type == EntityType.IRON_GOLEM;
	}

	@Override
	public int getMaxHeadRotation() {
		return 30;
	}

	@Override
	public int getMaxLookYawChange() {
		return 25;
	}

	public double getChargeY() {
		return getY() + getHeight() / 2.0F + 0.3F;
	}

	@Override
	public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
		return source.getAttacker() instanceof BreezeEntity || super.isInvulnerableTo(world, source);
	}

	@Override
	public double getSwimHeight() {
		return getStandingEyeHeight();
	}

	@Override
	public boolean handleFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
		if (fallDistance > 3.0) {
			playSound(SoundEvents.ENTITY_BREEZE_LAND, 1.0F, 1.0F);
		}

		return super.handleFallDamage(fallDistance, damagePerDistance, damageSource);
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return getTargetInBrain();
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
		super.registerTracking(world, tracker);
		tracker.track(
				DebugSubscriptionTypes.BREEZES,
				() -> new BreezeDebugData(
						getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).map(Entity::getId),
						getBrain().getOptionalRegisteredMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
				)
		);
	}
}
