package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.util.math.intprovider.UniformIntProvider;

/**
 * {@code TadpoleBrain}.
 */
public class TadpoleBrain {

	private static final float FLEE_SPEED = 2.0F;
	private static final float STROLL_SPEED = 0.5F;
	private static final float TEMPT_SPEED = 1.25F;

	protected static Brain<?> create(Brain<TadpoleEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreActivities(Brain<TadpoleEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.<Task<? super TadpoleEntity>>of(
						new FleeTask(2.0F),
						new UpdateLookControlTask(45, 90),
						new MoveToTargetTask(),
						new TickCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
				)
		);
	}

	private static void addIdleActivities(Brain<TadpoleEntity> brain) {
		brain.setTaskList(
				Activity.IDLE,
				ImmutableList.of(
						Pair.of(0,
								LookAtMobWithIntervalTask.follow(
										EntityType.PLAYER,
										6.0F,
										UniformIntProvider.create(30, 60)
								)
						),
						Pair.of(1, new TemptTask(livingEntity -> 1.25F)),
						Pair.of(
								2,
								new CompositeTask(
										ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
										ImmutableSet.of(),
										CompositeTask.Order.ORDERED,
										CompositeTask.RunMode.TRY_ALL,
										ImmutableList.of(
												Pair.of(StrollTask.createDynamicRadius(STROLL_SPEED), 2),
												Pair.of(GoToLookTargetTask.create(STROLL_SPEED, 3), 3),
												Pair.of(TaskTriggerer.predicate(Entity::isTouchingWater), 5)
										)
								)
						)
				)
		);
	}

	public static void updateActivities(TadpoleEntity tadpole) {
		tadpole.getBrain().resetPossibleActivities(ImmutableList.of(Activity.IDLE));
	}
}
