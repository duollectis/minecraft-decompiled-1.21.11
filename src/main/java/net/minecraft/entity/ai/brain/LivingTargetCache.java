package net.minecraft.entity.ai.brain;

import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Кэш видимых живых сущностей для мозга.
 * Хранит список ближайших сущностей и лениво вычисляет их видимость
 * через {@link Sensor#testTargetPredicate}, кэшируя результат в {@link Object2BooleanOpenHashMap}.
 * Это позволяет избежать повторных дорогостоящих raycast-проверок за один тик.
 */
public class LivingTargetCache {

	private static final LivingTargetCache EMPTY = new LivingTargetCache();
	private final List<LivingEntity> entities;
	private final Predicate<LivingEntity> targetPredicate;

	private LivingTargetCache() {
		entities = List.of();
		targetPredicate = entity -> false;
	}

	public LivingTargetCache(ServerWorld world, LivingEntity owner, List<LivingEntity> entities) {
		this.entities = entities;
		Object2BooleanOpenHashMap<LivingEntity> visibilityCache = new Object2BooleanOpenHashMap<>(entities.size());
		Predicate<LivingEntity> visibilityTest = target -> Sensor.testTargetPredicate(world, owner, target);
		targetPredicate = entity -> visibilityCache.computeIfAbsent(entity, visibilityTest);
	}

	public static LivingTargetCache empty() {
		return EMPTY;
	}

	public Optional<LivingEntity> findFirst(Predicate<LivingEntity> predicate) {
		for (LivingEntity entity : entities) {
			if (predicate.test(entity) && targetPredicate.test(entity)) {
				return Optional.of(entity);
			}
		}

		return Optional.empty();
	}

	public Iterable<LivingEntity> iterate(Predicate<LivingEntity> predicate) {
		return Iterables.filter(entities, entity -> predicate.test(entity) && targetPredicate.test(entity));
	}

	public Stream<LivingEntity> stream(Predicate<LivingEntity> predicate) {
		return entities.stream().filter(entity -> predicate.test(entity) && targetPredicate.test(entity));
	}

	public boolean contains(LivingEntity entity) {
		return entities.contains(entity) && targetPredicate.test(entity);
	}

	public boolean anyMatch(Predicate<LivingEntity> predicate) {
		for (LivingEntity entity : entities) {
			if (predicate.test(entity) && targetPredicate.test(entity)) {
				return true;
			}
		}

		return false;
	}
}
