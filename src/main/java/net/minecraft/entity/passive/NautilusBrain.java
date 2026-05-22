package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.rule.GameRules;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Мозг наутилуса: регистрирует сенсоры и задачи поведения.
 */
public class NautilusBrain {

	private static final float IDLE_MOVE_SPEED = 1.0F;
	private static final float TEMPT_MOVE_SPEED = 1.3F;
	private static final float BREED_MOVE_SPEED = 0.4F;
	private static final float FLEE_MOVE_SPEED = 1.6F;
	private static final UniformIntProvider ATTACK_TARGET_COOLDOWN = UniformIntProvider.create(2400, 3600);
	private static final float DASH_ATTACK_SPEED = 0.6F;
	private static final float DASH_ATTACK_MAX_SPEED = 2.0F;
	private static final int ANGRY_AT_MEMORY_DURATION = 400;
	private static final int DASH_ATTACK_COOLDOWN = 80;
	private static final double DASH_ATTACK_RANGE = 12.0;
	private static final double DASH_ATTACK_MIN_RANGE = 11.0;
	public static final TargetPredicate FIGHT_TARGET_PREDICATE = TargetPredicate.createAttackable()
	                                                                            .setPredicate(
			                                                                            (entity, world) -> (world
					                                                                            .getGameRules()
					                                                                            .getValue(GameRules.DO_MOB_GRIEFING)
					                                                                            || !entity
					                                                                            .getType()
					                                                                            .equals(EntityType.ARMOR_STAND)
			                                                                            )
					                                                                            && world
					                                                                            .getWorldBorder()
					                                                                            .contains(entity.getBoundingBox())
	                                                                            );
	protected static final ImmutableList<SensorType<? extends Sensor<? super NautilusEntity>>>
			SENSORS =
			ImmutableList.of(
					SensorType.NEAREST_LIVING_ENTITIES,
					SensorType.NEAREST_ADULT,
					SensorType.NEAREST_PLAYERS,
					SensorType.HURT_BY,
					SensorType.NAUTILUS_TEMPTATIONS
			);
	protected static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES = ImmutableList.of(
			MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.VISIBLE_MOBS,
			MemoryModuleType.WALK_TARGET,
			MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			MemoryModuleType.PATH,
			MemoryModuleType.NEAREST_VISIBLE_ADULT,
			MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
			MemoryModuleType.IS_TEMPTED,
			MemoryModuleType.TEMPTING_PLAYER,
			MemoryModuleType.BREED_TARGET,
			MemoryModuleType.IS_PANICKING,
			MemoryModuleType.ATTACK_TARGET,
			new MemoryModuleType[]{
					MemoryModuleType.CHARGE_COOLDOWN_TICKS,
					MemoryModuleType.HURT_BY,
					MemoryModuleType.ANGRY_AT,
					MemoryModuleType.ATTACK_TARGET_COOLDOWN
			}
	);

	/**
	 * Инициализирует ialize.
	 *
	 * @param nautilus nautilus
	 * @param random random
	 */
	protected static void initialize(AbstractNautilusEntity nautilus, Random random) {
		nautilus.getBrain().remember(MemoryModuleType.ATTACK_TARGET_COOLDOWN, ATTACK_TARGET_COOLDOWN.get(random));
	}

	protected static Brain.Profile<NautilusEntity> createProfile() {
		return Brain.createProfile(MEMORY_MODULES, SENSORS);
	}

	/**
	 * Create.
	 *
	 * @param brain brain
	 *
	 * @return Brain — результат операции
	 */
	protected static Brain<?> create(Brain<NautilusEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		addFightActivities(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreActivities(Brain<NautilusEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.<Task<? super NautilusEntity>>of(
						new FleeTask(FLEE_MOVE_SPEED),
						new UpdateLookControlTask(45, 90),
						new MoveToTargetTask(),
						new TickCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
						new TickCooldownTask(MemoryModuleType.CHARGE_COOLDOWN_TICKS),
						new TickCooldownTask(MemoryModuleType.ATTACK_TARGET_COOLDOWN)
				)
		);
	}

	private static void addIdleActivities(Brain<NautilusEntity> brain) {
		brain.setTaskList(
				Activity.IDLE,
				ImmutableList.of(
						Pair.of(1, new BreedTask(EntityType.NAUTILUS, BREED_MOVE_SPEED, 2)),
						Pair.of(2, new TemptTask(entity -> TEMPT_MOVE_SPEED, entity -> entity.isBaby() ? 2.5 : 3.5)),
						Pair.of(3, UpdateAttackTargetTask.create(NautilusBrain::findAttackTarget)),
						Pair.of(
								4,
								new CompositeTask(
										ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleState.VALUE_ABSENT),
										ImmutableSet.of(),
										CompositeTask.Order.ORDERED,
										CompositeTask.RunMode.TRY_ALL,
										ImmutableList.of(
												Pair.of(StrollTask.createDynamicRadius(1.0F), 2),
												Pair.of(GoToLookTargetTask.create(1.0F, 3), 3)
										)
								)
						)
				)
		);
	}

	private static void addFightActivities(Brain<NautilusEntity> brain) {
		brain.setTaskList(
				Activity.FIGHT,
				ImmutableList.of(Pair.of(
						0,
						new DashAttackTask(
								DASH_ATTACK_COOLDOWN,
								FIGHT_TARGET_PREDICATE,
								DASH_ATTACK_SPEED,
								2.0F,
								DASH_ATTACK_RANGE,
								DASH_ATTACK_MIN_RANGE,
								SoundEvents.ENTITY_NAUTILUS_DASH
						)
				)),
				ImmutableSet.of(
						Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleState.VALUE_PRESENT),
						Pair.of(MemoryModuleType.TEMPTING_PLAYER, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.BREED_TARGET, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.CHARGE_COOLDOWN_TICKS, MemoryModuleState.VALUE_ABSENT)
				)
		);
	}

	public static Optional<? extends LivingEntity> findAttackTarget(
			ServerWorld world,
			AbstractNautilusEntity nautilus
	) {
		if (!TargetUtil.hasBreedTarget(nautilus) && nautilus.isTouchingWater() && !nautilus.isBaby()
				&& !nautilus.isTamed()) {
			Optional<LivingEntity> optional = TargetUtil.getEntity(nautilus, MemoryModuleType.ANGRY_AT)
			                                            .filter(target -> target.isTouchingWater()
					                                            && Sensor.testAttackableTargetPredicateIgnoreVisibility(
					                                            world,
					                                            nautilus,
					                                            target
			                                            ));
			if (optional.isPresent()) {
				return optional;
			}
			else if (nautilus.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET_COOLDOWN)) {
				return Optional.empty();
			}
			else {
				nautilus
						.getBrain()
						.remember(MemoryModuleType.ATTACK_TARGET_COOLDOWN, ATTACK_TARGET_COOLDOWN.get(world.random));
				return world.random.nextFloat() < 0.5F
				       ? Optional.empty()
				       : nautilus.getBrain()
				                 .getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				                 .orElse(LivingTargetCache.empty())
				                 .findFirst(NautilusBrain::isTarget);
			}
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Обрабатывает событие damage.
	 *
	 * @param world world
	 * @param nautilus nautilus
	 * @param attacker attacker
	 */
	protected static void onDamage(ServerWorld world, AbstractNautilusEntity nautilus, LivingEntity attacker) {
		if (Sensor.testAttackableTargetPredicateIgnoreVisibility(world, nautilus, attacker)) {
			nautilus.getBrain().forget(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
			nautilus.getBrain().remember(MemoryModuleType.ANGRY_AT, attacker.getUuid(), ANGRY_AT_MEMORY_DURATION);
		}
	}

	private static boolean isTarget(LivingEntity entity) {
		return entity.isTouchingWater() && entity.getType().isIn(EntityTypeTags.NAUTILUS_HOSTILES);
	}

	/**
	 * Обновляет activities.
	 *
	 * @param nautilus nautilus
	 */
	public static void updateActivities(NautilusEntity nautilus) {
		nautilus.getBrain().resetPossibleActivities(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
	}

	public static Predicate<ItemStack> getNautilusFoodPredicate() {
		return stack -> stack.isIn(ItemTags.NAUTILUS_FOOD);
	}
}
