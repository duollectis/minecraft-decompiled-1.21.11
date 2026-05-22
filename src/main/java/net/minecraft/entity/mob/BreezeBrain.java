package net.minecraft.entity.mob;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Set;

/**
 * Мозг (Brain) для моба Бриз. Управляет поведением и памятью.
 */
public class BreezeBrain {

	public static final float WALK_SPEED = 0.6F;
	public static final float JUMP_SPEED = 4.0F;
	public static final float ATTACK_SPEED = 8.0F;
	public static final float ATTACK_RANGE = 24.0F;
	static final List<SensorType<? extends Sensor<? super BreezeEntity>>> SENSORS = ImmutableList.of(
			SensorType.NEAREST_LIVING_ENTITIES,
			SensorType.HURT_BY,
			SensorType.NEAREST_PLAYERS,
			SensorType.BREEZE_ATTACK_ENTITY_SENSOR
	);
	static final List<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.NEAREST_ATTACKABLE,
			MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			MemoryModuleType.ATTACK_TARGET,
			MemoryModuleType.WALK_TARGET,
			MemoryModuleType.BREEZE_JUMP_COOLDOWN,
			MemoryModuleType.BREEZE_JUMP_INHALING,
			MemoryModuleType.BREEZE_SHOOT,
			MemoryModuleType.BREEZE_SHOOT_CHARGING,
			MemoryModuleType.BREEZE_SHOOT_RECOVER,
			MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
			new MemoryModuleType[]{
					MemoryModuleType.BREEZE_JUMP_TARGET,
					MemoryModuleType.BREEZE_LEAVING_WATER,
					MemoryModuleType.HURT_BY,
					MemoryModuleType.HURT_BY_ENTITY,
					MemoryModuleType.PATH
			}
	);
	private static final int TIME_BEFORE_FORGETTING_TARGET = 100;

	protected static Brain<?> create(BreezeEntity breeze, Brain<BreezeEntity> brain) {
		addCoreTasks(brain);
		addIdleTasks(brain);
		addFightTasks(breeze, brain);
		brain.setCoreActivities(Set.of(Activity.CORE));
		brain.setDefaultActivity(Activity.FIGHT);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreTasks(Brain<BreezeEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.<Task<? super BreezeEntity>>of(
						new StayAboveWaterTask(0.8F),
						new UpdateLookControlTask(45, 90)
				)
		);
	}

	private static void addIdleTasks(Brain<BreezeEntity> brain) {
		brain.setTaskList(
				Activity.IDLE,
				ImmutableList.of(
						Pair.of(
								0,
								UpdateAttackTargetTask.create((world, breeze) -> breeze
										.getBrain()
										.getOptionalRegisteredMemory(MemoryModuleType.NEAREST_ATTACKABLE))
						),
						Pair.of(1, UpdateAttackTargetTask.create((world, breeze) -> breeze.getHurtBy())),
						Pair.of(2, new BreezeBrain.SlideAroundTask(20, 40)),
						Pair.of(
								3,
								new RandomTask(ImmutableList.of(
										Pair.of(new WaitTask(20, TIME_BEFORE_FORGETTING_TARGET), 1),
										Pair.of(StrollTask.create(WALK_SPEED), 2)
								))
						)
				)
		);
	}

	private static void addFightTasks(BreezeEntity breeze, Brain<BreezeEntity> brain) {
		brain.setTaskList(
				Activity.FIGHT,
				ImmutableList.of(
						Pair.of(
								0,
								ForgetAttackTargetTask.create(Sensor
										.hasTargetBeenAttackableRecently(breeze, TIME_BEFORE_FORGETTING_TARGET)
										.negate()::test)
						),
						Pair.of(1, new BreezeShootTask()),
						Pair.of(2, new BreezeJumpTask()),
						Pair.of(3, new BreezeShootIfStuckTask()),
						Pair.of(4, new BreezeSlideTowardsTargetTask())
				),
				ImmutableSet.of(
						Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleState.VALUE_PRESENT),
						Pair.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT)
				)
		);
	}

	static void updateActivities(BreezeEntity breeze) {
		breeze.getBrain().resetPossibleActivities(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
	}

	/** Задача скольжения бриза вокруг цели — перемещение по дуге для уклонения и позиционирования. */
	public static class SlideAroundTask extends MoveToTargetTask {

		@VisibleForTesting
			public SlideAroundTask(int minRunTime, int maxRunTime) {
				super(minRunTime, maxRunTime);
			}

		@Override
		protected void run(ServerWorld serverWorld, MobEntity mobEntity, long l) {
			super.run(serverWorld, mobEntity, l);
			mobEntity.playSoundIfNotSilent(SoundEvents.ENTITY_BREEZE_SLIDE);
			mobEntity.setPose(EntityPose.SLIDING);
		}

		@Override
		protected void finishRunning(ServerWorld serverWorld, MobEntity mobEntity, long l) {
			super.finishRunning(serverWorld, mobEntity, l);
			mobEntity.setPose(EntityPose.STANDING);
			if (mobEntity.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET)) {
				mobEntity.getBrain().remember(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 60L);
			}
		}
	}
}
