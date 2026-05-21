package net.minecraft.entity.mob;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.ai.brain.Activity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.brain.task.*;
import net.minecraft.entity.passive.NautilusBrain;
import net.minecraft.sound.SoundEvents;

/**
 * {@code ZombieNautilusBrain}.
 */
public class ZombieNautilusBrain {

	private static final float WALK_SPEED = 1.0F;
	private static final float SWIM_SPEED = 0.9F;
	private static final float FLEE_SPEED = 0.5F;
	private static final float ATTACK_SPEED = 2.0F;
	private static final int ATTACK_COOLDOWN_TICKS = 80;
	private static final double ATTACK_RANGE = 12.0;
	private static final double CHASE_RANGE = 11.0;
	protected static final ImmutableList<SensorType<? extends Sensor<? super ZombieNautilusEntity>>>
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

	protected static Brain.Profile<ZombieNautilusEntity> createProfile() {
		return Brain.createProfile(MEMORY_MODULES, SENSORS);
	}

	protected static Brain<?> create(Brain<ZombieNautilusEntity> brain) {
		addCoreActivities(brain);
		addIdleActivities(brain);
		addFightActivities(brain);
		brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
		brain.setDefaultActivity(Activity.IDLE);
		brain.resetPossibleActivities();
		return brain;
	}

	private static void addCoreActivities(Brain<ZombieNautilusEntity> brain) {
		brain.setTaskList(
				Activity.CORE,
				0,
				ImmutableList.<Task<? super ZombieNautilusEntity>>of(
						new UpdateLookControlTask(45, 90),
						new MoveToTargetTask(),
						new TickCooldownTask(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS),
						new TickCooldownTask(MemoryModuleType.CHARGE_COOLDOWN_TICKS),
						new TickCooldownTask(MemoryModuleType.ATTACK_TARGET_COOLDOWN)
				)
		);
	}

	private static void addIdleActivities(Brain<ZombieNautilusEntity> brain) {
		brain.setTaskList(
				Activity.IDLE,
				ImmutableList.of(
						Pair.of(1, new TemptTask(entity -> 0.9F, entity -> entity.isBaby() ? 2.5 : 3.5)),
						Pair.of(
								2,
								UpdateAttackTargetTask.<ZombieNautilusEntity>create((world, nautilus) -> NautilusBrain.findAttackTarget(
										world,
										nautilus
								))
						),
						Pair.of(
								3,
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

	private static void addFightActivities(Brain<ZombieNautilusEntity> brain) {
		brain.setTaskList(
				Activity.FIGHT,
				ImmutableList.of(
						Pair.of(
								0,
								new DashAttackTask(
										80,
										NautilusBrain.FIGHT_TARGET_PREDICATE,
										0.5F,
										2.0F,
										12.0,
										11.0,
										SoundEvents.ENTITY_ZOMBIE_NAUTILUS_DASH
								)
						)
				),
				ImmutableSet.of(
						Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleState.VALUE_PRESENT),
						Pair.of(MemoryModuleType.TEMPTING_PLAYER, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.BREED_TARGET, MemoryModuleState.VALUE_ABSENT),
						Pair.of(MemoryModuleType.CHARGE_COOLDOWN_TICKS, MemoryModuleState.VALUE_ABSENT)
				)
		);
	}

	public static void updateActivities(ZombieNautilusEntity nautilus) {
		nautilus.getBrain().resetPossibleActivities(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
	}
}
