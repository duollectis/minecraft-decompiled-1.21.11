package net.minecraft.entity.ai.brain.sensor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

/**
 * Базовый класс датчика мозга сущности.
 * Датчик периодически опрашивает мир и записывает результаты в память {@link net.minecraft.entity.ai.brain.Brain}.
 * Интервал опроса задаётся в конструкторе; первый запуск смещается случайно для равномерной нагрузки.
 *
 * @param <E> тип сущности, которой принадлежит датчик
 */
public abstract class Sensor<E extends LivingEntity> {

	private static final Random RANDOM = Random.createThreadSafe();
	private static final int DEFAULT_RUN_TIME = 20;
	private static final int BASE_MAX_DISTANCE = 16;

	private static final TargetPredicate TARGET_PREDICATE =
			TargetPredicate.createNonAttackable().setBaseMaxDistance(BASE_MAX_DISTANCE);
	private static final TargetPredicate TARGET_PREDICATE_IGNORE_DISTANCE_SCALING =
			TargetPredicate.createNonAttackable()
					.setBaseMaxDistance(BASE_MAX_DISTANCE)
					.ignoreDistanceScalingFactor();
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE =
			TargetPredicate.createAttackable().setBaseMaxDistance(BASE_MAX_DISTANCE);
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE_IGNORE_DISTANCE_SCALING =
			TargetPredicate.createAttackable()
					.setBaseMaxDistance(BASE_MAX_DISTANCE)
					.ignoreDistanceScalingFactor();
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY =
			TargetPredicate.createAttackable()
					.setBaseMaxDistance(BASE_MAX_DISTANCE)
					.ignoreVisibility();
	private static final TargetPredicate ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY_OR_DISTANCE_SCALING =
			TargetPredicate.createAttackable()
					.setBaseMaxDistance(BASE_MAX_DISTANCE)
					.ignoreVisibility()
					.ignoreDistanceScalingFactor();

	private final int senseInterval;
	private long lastSenseTime;

	public Sensor(int senseInterval) {
		this.senseInterval = senseInterval;
		lastSenseTime = RANDOM.nextInt(senseInterval);
	}

	public Sensor() {
		this(DEFAULT_RUN_TIME);
	}

	public final void tick(ServerWorld world, E entity) {
		if (--lastSenseTime <= 0L) {
			lastSenseTime = senseInterval;
			updateRange(entity);
			sense(world, entity);
		}
	}

	/**
	 * Обновляет дальность обнаружения всех предикатов по атрибуту {@code FOLLOW_RANGE} сущности.
	 */
	private void updateRange(E entity) {
		double followRange = entity.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
		TARGET_PREDICATE.setBaseMaxDistance(followRange);
		TARGET_PREDICATE_IGNORE_DISTANCE_SCALING.setBaseMaxDistance(followRange);
		ATTACKABLE_TARGET_PREDICATE.setBaseMaxDistance(followRange);
		ATTACKABLE_TARGET_PREDICATE_IGNORE_DISTANCE_SCALING.setBaseMaxDistance(followRange);
		ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY.setBaseMaxDistance(followRange);
		ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY_OR_DISTANCE_SCALING.setBaseMaxDistance(followRange);
	}

	protected abstract void sense(ServerWorld world, E entity);

	public abstract Set<MemoryModuleType<?>> getOutputMemoryModules();

	/**
	 * Проверяет видимость цели с учётом того, является ли она текущей целью атаки.
	 * Если цель — текущая цель атаки, масштабирование дистанции игнорируется.
	 */
	public static boolean testTargetPredicate(ServerWorld world, LivingEntity entity, LivingEntity target) {
		return entity.getBrain().hasMemoryModuleWithValue(MemoryModuleType.ATTACK_TARGET, target)
				? TARGET_PREDICATE_IGNORE_DISTANCE_SCALING.test(world, entity, target)
				: TARGET_PREDICATE.test(world, entity, target);
	}

	/**
	 * Проверяет атакуемость цели с учётом того, является ли она текущей целью атаки.
	 */
	public static boolean testAttackableTargetPredicate(ServerWorld world, LivingEntity entity, LivingEntity target) {
		return entity.getBrain().hasMemoryModuleWithValue(MemoryModuleType.ATTACK_TARGET, target)
				? ATTACKABLE_TARGET_PREDICATE_IGNORE_DISTANCE_SCALING.test(world, entity, target)
				: ATTACKABLE_TARGET_PREDICATE.test(world, entity, target);
	}

	public static BiPredicate<ServerWorld, LivingEntity> hasTargetBeenAttackableRecently(
			LivingEntity entity,
			int ticks
	) {
		return hasPredicatePassedRecently(
				ticks,
				(world, target) -> testAttackableTargetPredicate(world, entity, target)
		);
	}

	public static boolean testAttackableTargetPredicateIgnoreVisibility(
			ServerWorld world,
			LivingEntity entity,
			LivingEntity target
	) {
		return entity.getBrain().hasMemoryModuleWithValue(MemoryModuleType.ATTACK_TARGET, target)
				? ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY_OR_DISTANCE_SCALING.test(world, entity, target)
				: ATTACKABLE_TARGET_PREDICATE_IGNORE_VISIBILITY.test(world, entity, target);
	}

	/**
	 * Создаёт предикат, который возвращает {@code true} в течение {@code times} тиков
	 * после последнего успешного прохождения исходного предиката.
	 */
	static <T, U> BiPredicate<T, U> hasPredicatePassedRecently(int times, BiPredicate<T, U> predicate) {
		AtomicInteger counter = new AtomicInteger(0);
		return (world, target) -> {
			if (predicate.test(world, target)) {
				counter.set(times);
				return true;
			}

			return counter.decrementAndGet() >= 0;
		};
	}
}
