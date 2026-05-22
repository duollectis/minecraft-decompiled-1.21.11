package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.Set;

/**
 * Мозг счастливого гаста: регистрирует сенсоры и задачи поведения.
 */
public class HappyGhastBrain {

	private static final float STROLL_SPEED = 1.0F;
	private static final float TEMPT_SPEED = 1.25F;
	private static final float MOVE_TOWARDS_FRIENDLY_ENTITY_SPEED = 1.1F;
	private static final double TEMPT_DISTANCE = 3.0;
	private static final UniformIntProvider MOVE_TOWARDS_FRIENDLY_ENTITY_RANGE = UniformIntProvider.create(3, 16);
	private static final ImmutableList<SensorType<? extends Sensor<? super HappyGhastEntity>>>
			SENSORS =
			ImmutableList.of(
					SensorType.NEAREST_LIVING_ENTITIES,
					SensorType.HURT_BY,
					SensorType.FOOD_TEMPTATIONS,
					SensorType.NEAREST_ADULT_ANY_TYPE,
					SensorType.NEAREST_PLAYERS
			);
	private static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
			MemoryModuleType.WALK_TARGET,
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			MemoryModuleType.PATH,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.TEMPTING_PLAYER,
			MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
			MemoryModuleType.IS_TEMPTED,
			MemoryModuleType.BREED_TARGET,
			MemoryModuleType.IS_PANICKING,
			MemoryModuleType.HURT_BY,
			MemoryModuleType.NEAREST_VISIBLE_ADULT,
			new MemoryModuleType[]{
					MemoryModuleType.NEAREST_PLAYERS,
					MemoryModuleType.NEAREST_VISIBLE_PLAYER,
					MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
					MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYERS
			}
	);

	public static Brain.Profile<HappyGhastEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULES, SENSORS);
	}

	/**
	 * Create.
	 *
	 * @param brain brain
	 *
	 * @return Brain — результат операции
	 */
	protected static Brain<?> create(Brain<HappyGhastEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		addPanicActivities(brain);
		brain.setCoreActivities(Set.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreActivities(Brain<HappyGhastEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.<Task<? super HappyGhastEntity>>of(
						new StayAboveWaterTask(0.8F),
						new FleeTask(2.0F, 0),
						new UpdateLookControlTask(45, 90),
						new MoveToTargetTask(),
						new TickCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
				)
		);
	}

	private static void addIdleActivities(Brain<HappyGhastEntity> brain) {
		brain.setTaskList(
				Activity.IDLE,
				ImmutableList.of(
						Pair.of(1, new TemptTask(entity -> TEMPT_SPEED, entity -> TEMPT_DISTANCE, true)),
						Pair.of(
								2,
								WalkTowardsEntityTask.create(
										MOVE_TOWARDS_FRIENDLY_ENTITY_RANGE,
										player -> MOVE_TOWARDS_FRIENDLY_ENTITY_SPEED,
										MemoryModuleType.NEAREST_VISIBLE_PLAYER,
										true
								)
						),
						Pair.of(
								3,
								WalkTowardsEntityTask.create(
										MOVE_TOWARDS_FRIENDLY_ENTITY_RANGE,
										adult -> MOVE_TOWARDS_FRIENDLY_ENTITY_SPEED,
										MemoryModuleType.NEAREST_VISIBLE_ADULT,
										true
								)
						),
						Pair.of(
								4,
								new RandomTask(ImmutableList.of(
										Pair.of(StrollTask.createSolidTargeting(STROLL_SPEED), 1),
										Pair.of(GoToLookTargetTask.create(STROLL_SPEED, 3), 1)
								))
						)
				)
		);
	}

	private static void addPanicActivities(Brain<HappyGhastEntity> brain) {
		brain.setTaskList(
				Activity.PANIC,
				ImmutableList.of(),
				Set.of(Pair.of(MemoryModuleType.IS_PANICKING, MemoryModuleState.VALUE_PRESENT))
		);
	}

	/**
	 * Обновляет activities.
	 *
	 * @param happyGhast happy ghast
	 */
	public static void updateActivities(HappyGhastEntity happyGhast) {
		happyGhast.getBrain().resetPossibleActivities(ImmutableList.of(Activity.PANIC, Activity.IDLE));
	}
}
