package net.minecraft.entity.mob;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Зоглин — зомбифицированная версия хоглина, появляющаяся при попадании хоглина
 * в Верхний мир. Агрессивен ко всем существам кроме зоглинов и криперов.
 * Использует Brain-архитектуру с активностями CORE, IDLE и FIGHT.
 */
public class ZoglinEntity extends HostileEntity implements Hoglin {

	private static final TrackedData<Boolean>
			BABY =
			DataTracker.registerData(ZoglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final int ADULT_MELEE_ATTACK_COOLDOWN = 40;
	private static final int BABY_MELEE_ATTACK_COOLDOWN = 15;
	private static final int ATTACK_MOVEMENT_COOLDOWN = 10;
	private static final int ATTACK_TARGET_DURATION = 200;
	private static final int DEFAULT_ATTACK_KNOCKBACK = 1;
	private static final int DEFAULT_ATTACK_DAMAGE = 6;
	private static final float DEFAULT_KNOCKBACK_RESISTANCE = 0.6F;
	private static final float DEFAULT_MOVEMENT_SPEED = 0.3F;
	private static final float BABY_MOVEMENT_SPEED = 0.4F;
	private static final float BABY_ATTACK_DAMAGE = 0.5F;
	private static final float BABY_SPAWN_CHANCE = 0.2F;
	private static final byte ATTACK_STATUS = 4;
	private int movementCooldownTicks;
	protected static final ImmutableList<? extends SensorType<? extends Sensor<? super ZoglinEntity>>>
			USED_SENSORS =
			ImmutableList.of(
					SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS
			);
	protected static final ImmutableList<? extends MemoryModuleType<?>> USED_MEMORY_MODULES = ImmutableList.of(
			MemoryModuleType.MOBS,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.NEAREST_VISIBLE_PLAYER,
			MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.WALK_TARGET,
			MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			MemoryModuleType.PATH,
			MemoryModuleType.ATTACK_TARGET,
			MemoryModuleType.ATTACK_COOLING_DOWN
	);

	public ZoglinEntity(EntityType<? extends ZoglinEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
	}

	@Override
	protected Brain.Profile<ZoglinEntity> createBrainProfile() {
		return Brain.createProfile(USED_MEMORY_MODULES, USED_SENSORS);
	}

	@Override
	protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
		Brain<ZoglinEntity> brain = createBrainProfile().deserialize(dynamic);
		addCoreTasks(brain);
		addIdleTasks(brain);
		addFightTasks(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreTasks(Brain<ZoglinEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.of(new UpdateLookControlTask(45, 90), new MoveToTargetTask())
		);
	}

	private static void addIdleTasks(Brain<ZoglinEntity> brain) {
		brain.setTaskList(
				Activity.IDLE,
				10,
				ImmutableList.<Task<? super ZoglinEntity>>of(
						UpdateAttackTargetTask.create((world, target) -> ((ZoglinEntity) target).getHoglinTarget(world)),
						LookAtMobWithIntervalTask.follow(8.0F, UniformIntProvider.create(30, 60)),
						new RandomTask<>(
								ImmutableList.of(
										Pair.of(StrollTask.create(BABY_MOVEMENT_SPEED), 2),
										Pair.of(GoToLookTargetTask.create(BABY_MOVEMENT_SPEED, 3), 2),
										Pair.of(new WaitTask(30, 60), 1)
								)
						)
				)
		);
	}

	private static void addFightTasks(Brain<ZoglinEntity> brain) {
		brain.setTaskList(
				Activity.FIGHT,
				10,
				ImmutableList.of(
						RangedApproachTask.create(1.0F),
						TaskTriggerer.runIf(ZoglinEntity::isAdult, MeleeAttackTask.create(ADULT_MELEE_ATTACK_COOLDOWN)),
						TaskTriggerer.runIf(ZoglinEntity::isBaby, MeleeAttackTask.create(BABY_MELEE_ATTACK_COOLDOWN)),
						ForgetAttackTargetTask.create()
				),
				MemoryModuleType.ATTACK_TARGET
		);
	}

	public Optional<? extends LivingEntity> getHoglinTarget(ServerWorld world) {
		return getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				.orElse(LivingTargetCache.empty())
				.findFirst(target -> shouldAttack(world, target));
	}

	private boolean shouldAttack(ServerWorld world, LivingEntity target) {
		EntityType<?> entityType = target.getType();
		return entityType != EntityType.ZOGLIN && entityType != EntityType.CREEPER
				&& Sensor.testAttackableTargetPredicate(world, this, target);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BABY, false);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (BABY.equals(data)) {
			calculateDimensions();
		}
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		if (world.getRandom().nextFloat() < BABY_SPAWN_CHANCE) {
			setBaby(true);
		}

		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	public static DefaultAttributeContainer.Builder createZoglinAttributes() {
		return HostileEntity.createHostileAttributes()
		                    .add(EntityAttributes.MAX_HEALTH, 40.0)
		                    .add(EntityAttributes.MOVEMENT_SPEED, DEFAULT_MOVEMENT_SPEED)
		                    .add(EntityAttributes.KNOCKBACK_RESISTANCE, DEFAULT_KNOCKBACK_RESISTANCE)
		                    .add(EntityAttributes.ATTACK_KNOCKBACK, 1.0)
		                    .add(EntityAttributes.ATTACK_DAMAGE, 6.0);
	}

	public boolean isAdult() {
		return !isBaby();
	}

	@Override
	public boolean tryAttack(ServerWorld world, Entity target) {
		if (!(target instanceof LivingEntity livingEntity)) {
			return false;
		}

		movementCooldownTicks = ATTACK_MOVEMENT_COOLDOWN;
		world.sendEntityStatus(this, ATTACK_STATUS);
		playSound(SoundEvents.ENTITY_ZOGLIN_ATTACK);
		return Hoglin.tryAttack(world, this, livingEntity);
	}

	@Override
	public boolean canBeLeashed() {
		return true;
	}

	@Override
	protected void knockback(LivingEntity target) {
		if (!isBaby()) {
			Hoglin.knockback(this, target);
		}
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		boolean damaged = super.damage(world, source, amount);
		if (damaged && source.getAttacker() instanceof LivingEntity attacker) {
			if (canTarget(attacker) && !TargetUtil.isNewTargetTooFar(this, attacker, 4.0)) {
				setAttackTarget(attacker);
			}

			return true;
		}

		return damaged;
	}

	private void setAttackTarget(LivingEntity entity) {
		brain.forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		brain.remember(MemoryModuleType.ATTACK_TARGET, entity, ATTACK_TARGET_DURATION);
	}

	@Override
	public Brain<ZoglinEntity> getBrain() {
		return (Brain<ZoglinEntity>) super.getBrain();
	}

	/**
	 * Переключает активность brain между FIGHT и IDLE, воспроизводит звук ярости при переходе в бой.
	 */
	protected void tickBrain() {
		Activity prevActivity = brain.getFirstPossibleNonCoreActivity().orElse(null);
		brain.resetPossibleActivities(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
		Activity currentActivity = brain.getFirstPossibleNonCoreActivity().orElse(null);

		if (currentActivity == Activity.FIGHT && prevActivity != Activity.FIGHT) {
			playAngrySound();
		}

		setAttacking(brain.hasMemoryModule(MemoryModuleType.ATTACK_TARGET));
	}

	@Override
	protected void mobTick(ServerWorld world) {
		Profiler profiler = Profilers.get();
		profiler.push("zoglinBrain");
		getBrain().tick(world, this);
		profiler.pop();
		tickBrain();
	}

	@Override
	public void setBaby(boolean baby) {
		getDataTracker().set(BABY, baby);
		if (!getEntityWorld().isClient() && baby) {
			getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(BABY_ATTACK_DAMAGE);
		}
	}

	@Override
	public boolean isBaby() {
		return getDataTracker().get(BABY);
	}

	@Override
	public void tickMovement() {
		if (movementCooldownTicks > 0) {
			movementCooldownTicks--;
		}

		super.tickMovement();
	}

	@Override
	public void handleStatus(byte status) {
		if (status != ATTACK_STATUS) {
			super.handleStatus(status);
			return;
		}

		movementCooldownTicks = ATTACK_MOVEMENT_COOLDOWN;
		playSound(SoundEvents.ENTITY_ZOGLIN_ATTACK);
	}

	@Override
	public int getMovementCooldownTicks() {
		return movementCooldownTicks;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		if (getEntityWorld().isClient()) {
			return null;
		}

		return brain.hasMemoryModule(MemoryModuleType.ATTACK_TARGET)
				? SoundEvents.ENTITY_ZOGLIN_ANGRY
				: SoundEvents.ENTITY_ZOGLIN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_ZOGLIN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_ZOGLIN_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.ENTITY_ZOGLIN_STEP, 0.15F, 1.0F);
	}

	protected void playAngrySound() {
		playSound(SoundEvents.ENTITY_ZOGLIN_ANGRY);
	}

	@Override
	public @Nullable LivingEntity getTarget() {
		return getTargetInBrain();
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putBoolean("IsBaby", isBaby());
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setBaby(view.getBoolean("IsBaby", false));
	}
}
