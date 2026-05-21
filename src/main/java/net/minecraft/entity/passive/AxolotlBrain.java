package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * {@code AxolotlBrain}.
 */
public class AxolotlBrain {

	private static final UniformIntProvider WALK_TOWARD_ADULT_RANGE = UniformIntProvider.create(5, 16);
	private static final float BREEDING_SPEED = 0.2F;
	private static final float ON_LAND_SPEED = 0.15F;
	private static final float IDLE_SPEED = 0.5F;
	private static final float TARGET_APPROACHING_SPEED = 0.6F;
	private static final float ADULT_FOLLOWING_SPEED = 0.6F;

	protected static Brain<?> create(Brain<AxolotlEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		addFightActivities(brain);
		addPlayDeadActivities(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addPlayDeadActivities(Brain<AxolotlEntity> brain) {
		brain.setTaskList(
				Activity.PLAY_DEAD,
				ImmutableList.of(
						Pair.of(0, new PlayDeadTask()),
						Pair.of(1, ForgetTask.create(TargetUtil::hasBreedTarget, MemoryModuleType.PLAY_DEAD_TICKS))
				),
				ImmutableSet.of(Pair.of(MemoryModuleType.PLAY_DEAD_TICKS, MemoryModuleState.VALUE_PRESENT)),
				ImmutableSet.of(MemoryModuleType.PLAY_DEAD_TICKS)
		);
	}

	private static void addFightActivities(Brain<AxolotlEntity> brain) {
		brain.setTaskList(
				Activity.FIGHT,
				0,
				ImmutableList.of(
						ForgetAttackTargetTask.create(AxolotlEntity::appreciatePlayer),
						RangedApproachTask.create(AxolotlBrain::getTargetApproachingSpeed),
						MeleeAttackTask.create(20),
						ForgetTask.create(TargetUtil::hasBreedTarget, MemoryModuleType.ATTACK_TARGET)
				),
				MemoryModuleType.ATTACK_TARGET
		);
	}

	private static void addCoreActivities(Brain<AxolotlEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.of(
						new UpdateLookControlTask(45, 90),
						new MoveToTargetTask(),
						PlayDeadTimerTask.create(),
						new TickCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)
				)
		);
	}

	private static void addIdleActivities(Brain<AxolotlEntity> brain) {
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
						Pair.of(1, new BreedTask(EntityType.AXOLOTL, 0.2F, 2)),
						Pair.of(
								2,
								new RandomTask(
										ImmutableList.of(
												Pair.of(new TemptTask(AxolotlBrain::getTemptedSpeed), 1),
												Pair.of(
														WalkTowardsEntityTask.create(
																WALK_TOWARD_ADULT_RANGE,
																AxolotlBrain::getAdultFollowingSpeed,
																MemoryModuleType.NEAREST_VISIBLE_ADULT,
																false
														),
														1
												)
										)
								)
						),
						Pair.of(3, UpdateAttackTargetTask.create(AxolotlBrain::getAttackTarget)),
						Pair.of(3, SeekWaterTask.create(6, 0.15F)),
						Pair.of(
								4,
								new CompositeTask(
										ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
										ImmutableSet.of(),
										CompositeTask.Order.ORDERED,
										CompositeTask.RunMode.TRY_ALL,
										ImmutableList.of(
												Pair.of(StrollTask.createDynamicRadius(0.5F), 2),
												Pair.of(StrollTask.create(0.15F, false), 2),
												Pair.of(
														GoToLookTargetTask.create(
																AxolotlBrain::canGoToLookTarget,
																AxolotlBrain::getTemptedSpeed,
																3
														), 3
												),
												Pair.of(TaskTriggerer.predicate(Entity::isTouchingWater), 5),
												Pair.of(TaskTriggerer.predicate(Entity::isOnGround), 5)
										)
								)
						)
				)
		);
	}

	private static boolean canGoToLookTarget(LivingEntity entity) {
		World world = entity.getEntityWorld();
		Optional<LookTarget> optional = entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.LOOK_TARGET);
		if (optional.isPresent()) {
			BlockPos blockPos = optional.get().getBlockPos();
			return world.isWater(blockPos) == entity.isTouchingWater();
		}
		else {
			return false;
		}
	}

	public static void updateActivities(AxolotlEntity axolotl) {
		Brain<AxolotlEntity> brain = axolotl.getBrain();
		Activity activity = brain.getFirstPossibleNonCoreActivity().orElse(null);
		if (activity != Activity.PLAY_DEAD) {
			brain.resetPossibleActivities(ImmutableList.of(Activity.PLAY_DEAD, Activity.FIGHT, Activity.IDLE));
			if (activity == Activity.FIGHT && brain.getFirstPossibleNonCoreActivity().orElse(null) != Activity.FIGHT) {
				brain.remember(MemoryModuleType.HAS_HUNTING_COOLDOWN, true, 2400L);
			}
		}
	}

	private static float getTargetApproachingSpeed(LivingEntity entity) {
		return entity.isTouchingWater() ? 0.6F : 0.15F;
	}

	private static float getAdultFollowingSpeed(LivingEntity entity) {
		return entity.isTouchingWater() ? 0.6F : 0.15F;
	}

	private static float getTemptedSpeed(LivingEntity entity) {
		return entity.isTouchingWater() ? 0.5F : 0.15F;
	}

	private static Optional<? extends LivingEntity> getAttackTarget(ServerWorld world, AxolotlEntity axolotl) {
		return TargetUtil.hasBreedTarget(axolotl) ? Optional.empty() : axolotl
		                                                               .getBrain()
		                                                               .getOptionalRegisteredMemory(MemoryModuleType.NEAREST_ATTACKABLE);
	}
}
