package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;

import java.util.function.Predicate;

/**
 * {@code CamelBrain}.
 */
public class CamelBrain {

	private static final float WALK_SPEED = 4.0F;
	private static final float STROLL_SPEED = 2.0F;
	private static final float TEMPT_SPEED = 2.5F;
	private static final float WALK_TOWARD_ADULT_SPEED = 2.5F;
	private static final float BREED_SPEED = 1.0F;
	private static final UniformIntProvider WALK_TOWARD_ADULT_RANGE = UniformIntProvider.create(5, 16);
	private static final ImmutableList<SensorType<? extends Sensor<? super CamelEntity>>> SENSORS = ImmutableList.of(
			SensorType.NEAREST_LIVING_ENTITIES,
			SensorType.HURT_BY,
			SensorType.FOOD_TEMPTATIONS,
			SensorType.NEAREST_ADULT
	);
	private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
			MemoryModuleType.IS_PANICKING,
			MemoryModuleType.HURT_BY,
			MemoryModuleType.HURT_BY_ENTITY,
			MemoryModuleType.WALK_TARGET,
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			MemoryModuleType.PATH,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.TEMPTING_PLAYER,
			MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
			MemoryModuleType.GAZE_COOLDOWN_TICKS,
			MemoryModuleType.IS_TEMPTED,
			new MemoryModuleType[]{MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_VISIBLE_ADULT}
	);

	/**
	 * Инициализирует ialize.
	 *
	 * @param camel camel
	 * @param random random
	 */
	protected static void initialize(CamelEntity camel, Random random) {
	}

	public static Brain.Profile<CamelEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULES, SENSORS);
	}

	/**
	 * Create.
	 *
	 * @param brain brain
	 *
	 * @return Brain — результат операции
	 */
	protected static Brain<?> create(Brain<CamelEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreActivities(Brain<CamelEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.<Task<? super CamelEntity>>of(
						new StayAboveWaterTask(0.8F),
						new CamelBrain.CamelWalkTask(4.0F),
						new UpdateLookControlTask(45, 90),
						new MoveToTargetTask(),
						new TickCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
						new TickCooldownTask(MemoryModuleType.GAZE_COOLDOWN_TICKS)
				)
		);
	}

	private static void addIdleActivities(Brain<CamelEntity> brain) {
		brain.setTaskList(
				Activity.IDLE,
				ImmutableList.of(
						Pair.of(
								0,
								LookAtMobWithIntervalTask.follow(
										EntityType.PLAYER,
										6.0F,
										UniformIntProvider.create(30, 60)
								)
						),
						Pair.of(1, new BreedTask(EntityType.CAMEL)),
						Pair.of(
								2,
								new RandomTask(
										ImmutableList.of(
												Pair.of(
														new TemptTask(
																entity -> TEMPT_SPEED,
																entity -> entity.isBaby() ? 2.5 : 3.5
														), 1
												),
												Pair.of(
														TaskTriggerer.runIf(
																Predicate.not(CamelEntity::isStationary),
																WalkTowardsEntityTask.createNearestVisibleAdult(WALK_TOWARD_ADULT_RANGE, WALK_TOWARD_ADULT_SPEED)
														),
														1
												)
										)
								)
						),
						Pair.of(3, new LookAroundTask(UniformIntProvider.create(150, 250), 30.0F, 0.0F, 0.0F)),
						Pair.of(
								4,
								new RandomTask(
										ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
										ImmutableList.of(
												Pair.of(
														TaskTriggerer.runIf(
																Predicate.not(CamelEntity::isStationary),
																StrollTask.create(STROLL_SPEED)
														), 1
												),
												Pair.of(
														TaskTriggerer.runIf(
																Predicate.not(CamelEntity::isStationary),
																GoToLookTargetTask.create(STROLL_SPEED, 3)
														), 1
												),
												Pair.of(new CamelBrain.SitOrStandTask(20), 1),
												Pair.of(new WaitTask(30, 60), 1)
										)
								)
						)
				)
		);
	}

	/**
	 * Обновляет activities.
	 *
	 * @param camel camel
	 */
	public static void updateActivities(CamelEntity camel) {
		camel.getBrain().resetPossibleActivities(ImmutableList.of(Activity.IDLE));
	}

	/**
	 * {@code CamelWalkTask}.
	 */
	public static class CamelWalkTask extends FleeTask<CamelEntity> {

		public CamelWalkTask(float f) {
			super(f);
		}

		/**
		 * Определяет, следует ли run.
		 *
		 * @param serverWorld server world
		 * @param camelEntity camel entity
		 *
		 * @return boolean — результат операции
		 */
		protected boolean shouldRun(ServerWorld serverWorld, CamelEntity camelEntity) {
			return super.shouldRun(serverWorld, camelEntity) && !camelEntity.isControlledByMob();
		}

		/**
		 * Run.
		 *
		 * @param serverWorld server world
		 * @param camelEntity camel entity
		 * @param l l
		 */
		protected void run(ServerWorld serverWorld, CamelEntity camelEntity, long l) {
			camelEntity.setStanding();
			super.run(serverWorld, camelEntity, l);
		}
	}

	/**
	 * {@code SitOrStandTask}.
	 */
	public static class SitOrStandTask extends MultiTickTask<CamelEntity> {

		private final int lastTimeSinceLastPoseTick;

		public SitOrStandTask(int lastPoseSecondsDelta) {
			super(ImmutableMap.of());
			this.lastTimeSinceLastPoseTick = lastPoseSecondsDelta * 20;
		}

		/**
		 * Определяет, следует ли run.
		 *
		 * @param serverWorld server world
		 * @param camelEntity camel entity
		 *
		 * @return boolean — результат операции
		 */
		protected boolean shouldRun(ServerWorld serverWorld, CamelEntity camelEntity) {
			return !camelEntity.isTouchingWater()
					&& camelEntity.getTimeSinceLastPoseTick() >= this.lastTimeSinceLastPoseTick
					&& !camelEntity.isLeashed()
					&& camelEntity.isOnGround()
					&& !camelEntity.hasControllingPassenger()
					&& camelEntity.canChangePose();
		}

		/**
		 * Run.
		 *
		 * @param serverWorld server world
		 * @param camelEntity camel entity
		 * @param l l
		 */
		protected void run(ServerWorld serverWorld, CamelEntity camelEntity, long l) {
			if (camelEntity.isSitting()) {
				camelEntity.startStanding();
			}
			else if (!camelEntity.isPanicking()) {
				camelEntity.startSitting();
			}
		}
	}
}
