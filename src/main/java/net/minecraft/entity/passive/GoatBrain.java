package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.rule.GameRules;

/**
 * Мозг козы: регистрирует сенсоры и задачи поведения.
 */
public class GoatBrain {

	public static final int PREPARE_RAM_DURATION = 20;
	public static final int MAX_RAM_TARGET_DISTANCE = 7;
	private static final UniformIntProvider WALKING_SPEED = UniformIntProvider.create(5, 16);
	private static final float FOLLOWING_TARGET_WALK_SPEED = 1.0F;
	private static final float TEMPTED_WALK_SPEED = 1.25F;
	private static final float FOLLOW_ADULT_WALK_SPEED = 1.25F;
	private static final float NORMAL_WALK_SPEED = 2.0F;
	private static final float PREPARING_RAM_WALK_SPEED = 1.25F;
	private static final UniformIntProvider LONG_JUMP_COOLDOWN_RANGE = UniformIntProvider.create(600, 1200);
	public static final int LONG_JUMP_VERTICAL_RANGE = 5;
	public static final int LONG_JUMP_HORIZONTAL_RANGE = 5;
	public static final float LONG_JUMP_SPEED = 0.7F;
	private static final UniformIntProvider RAM_COOLDOWN_RANGE = UniformIntProvider.create(600, 6000);
	private static final UniformIntProvider SCREAMING_RAM_COOLDOWN_RANGE = UniformIntProvider.create(100, 300);
	private static final TargetPredicate RAM_TARGET_PREDICATE = TargetPredicate.createAttackable()
	                                                                           .setPredicate(
			                                                                           (target, world) -> !target
					                                                                           .getType()
					                                                                           .equals(EntityType.GOAT)
					                                                                           && (world
					                                                                           .getGameRules()
					                                                                           .getValue(GameRules.DO_MOB_GRIEFING)
					                                                                           || !target
					                                                                           .getType()
					                                                                           .equals(EntityType.ARMOR_STAND)
			                                                                           )
					                                                                           && world
					                                                                           .getWorldBorder()
					                                                                           .contains(target.getBoundingBox())
	                                                                           );
	private static final float RAM_SPEED = 3.0F;
	public static final int MIN_RAM_TARGET_DISTANCE = 4;
	public static final float ADULT_RAM_STRENGTH_MULTIPLIER = 2.5F;
	public static final float BABY_RAM_STRENGTH_MULTIPLIER = 1.0F;

	/**
	 * Сбрасывает long jump cooldown.
	 *
	 * @param goat goat
	 * @param random random
	 */
	protected static void resetLongJumpCooldown(GoatEntity goat, Random random) {
		goat.getBrain().remember(MemoryModuleType.LONG_JUMP_COOLING_DOWN, LONG_JUMP_COOLDOWN_RANGE.get(random));
		goat.getBrain().remember(MemoryModuleType.RAM_COOLDOWN_TICKS, RAM_COOLDOWN_RANGE.get(random));
	}

	/**
	 * Create.
	 *
	 * @param brain brain
	 *
	 * @return Brain — результат операции
	 */
	protected static Brain<?> create(Brain<GoatEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		addLongJumpActivities(brain);
		addRamActivities(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreActivities(Brain<GoatEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.<Task<? super GoatEntity>>of(
						new StayAboveWaterTask(0.8F),
						new FleeTask(2.0F),
						new UpdateLookControlTask(45, 90),
						new MoveToTargetTask(),
						new TickCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
						new TickCooldownTask(MemoryModuleType.LONG_JUMP_COOLING_DOWN),
						new TickCooldownTask(MemoryModuleType.RAM_COOLDOWN_TICKS)
				)
		);
	}

	private static void addIdleActivities(Brain<GoatEntity> brain) {
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
						Pair.of(0, new BreedTask(EntityType.GOAT)),
						Pair.of(1, new TemptTask(goat -> TEMPTED_WALK_SPEED)),
						Pair.of(2, WalkTowardsEntityTask.createNearestVisibleAdult(WALKING_SPEED, FOLLOW_ADULT_WALK_SPEED)),
						Pair.of(
								3,
								new RandomTask(
										ImmutableList.of(
												Pair.of(StrollTask.create(1.0F), 2),
												Pair.of(GoToLookTargetTask.create(1.0F, 3), 2),
												Pair.of(new WaitTask(30, 60), 1)
										)
								)
						)
				),
				ImmutableSet.of(
						Pair.of(MemoryModuleType.RAM_TARGET, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryModuleState.VALUE_ABSENT)
				)
		);
	}

	private static void addLongJumpActivities(Brain<GoatEntity> brain) {
		brain.setTaskList(
				Activity.LONG_JUMP,
				ImmutableList.of(
						Pair.of(0, new LeapingChargeTask(LONG_JUMP_COOLDOWN_RANGE, SoundEvents.ENTITY_GOAT_STEP)),
						Pair.of(
								1,
								new LongJumpTask<>(
										LONG_JUMP_COOLDOWN_RANGE,
										5,
										5,
										LONG_JUMP_SPEED,
										goat -> goat.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_LONG_JUMP
										                           : SoundEvents.ENTITY_GOAT_LONG_JUMP
								)
						)
				),
				ImmutableSet.of(
						Pair.of(MemoryModuleType.TEMPTING_PLAYER, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.BREED_TARGET, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.LONG_JUMP_COOLING_DOWN, MemoryModuleState.VALUE_ABSENT)
				)
		);
	}

	private static void addRamActivities(Brain<GoatEntity> brain) {
		brain.setTaskList(
				Activity.RAM,
				ImmutableList.of(
						Pair.of(
								0,
								new RamImpactTask(
										goat -> goat.isScreaming() ? SCREAMING_RAM_COOLDOWN_RANGE : RAM_COOLDOWN_RANGE,
										RAM_TARGET_PREDICATE,
										3.0F,
										goat -> goat.isBaby() ? 1.0 : ADULT_RAM_STRENGTH_MULTIPLIER,
										goat -> goat.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_RAM_IMPACT
										                           : SoundEvents.ENTITY_GOAT_RAM_IMPACT,
										goat -> SoundEvents.ENTITY_GOAT_HORN_BREAK
								)
						),
						Pair.of(
								1,
								new PrepareRamTask<>(
										goat -> goat.isScreaming() ? SCREAMING_RAM_COOLDOWN_RANGE.getMin()
										                           : RAM_COOLDOWN_RANGE.getMin(),
										4,
										7,
										TEMPTED_WALK_SPEED,
										RAM_TARGET_PREDICATE,
										PREPARE_RAM_DURATION,
										goat -> goat.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_PREPARE_RAM
										                           : SoundEvents.ENTITY_GOAT_PREPARE_RAM
								)
						)
				),
				ImmutableSet.of(
						Pair.of(MemoryModuleType.TEMPTING_PLAYER, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.BREED_TARGET, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryModuleState.VALUE_ABSENT)
				)
		);
	}

	/**
	 * Обновляет activities.
	 *
	 * @param goat goat
	 */
	public static void updateActivities(GoatEntity goat) {
		goat.getBrain().resetPossibleActivities(ImmutableList.of(Activity.RAM, Activity.LONG_JUMP, Activity.IDLE));
	}
}
