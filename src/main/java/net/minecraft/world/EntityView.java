package net.minecraft.world;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Представление мира для запросов к сущностям.
 * Предоставляет методы поиска сущностей по типу, классу, расстоянию и UUID.
 */
public interface EntityView {

	List<Entity> getOtherEntities(@Nullable Entity except, Box box, Predicate<? super Entity> predicate);

	<T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate);

	default <T extends Entity> List<T> getEntitiesByClass(
		Class<T> entityClass,
		Box box,
		Predicate<? super T> predicate
	) {
		return getEntitiesByType(TypeFilter.instanceOf(entityClass), box, predicate);
	}

	List<? extends PlayerEntity> getPlayers();

	default List<Entity> getOtherEntities(@Nullable Entity except, Box box) {
		return getOtherEntities(except, box, EntityPredicates.EXCEPT_SPECTATOR);
	}

	default boolean doesNotIntersectEntities(@Nullable Entity except, VoxelShape shape) {
		if (shape.isEmpty()) {
			return true;
		}

		for (Entity entity : getOtherEntities(except, shape.getBoundingBox())) {
			if (!entity.isRemoved()
				&& entity.intersectionChecked
				&& (except == null || !entity.isConnectedThroughVehicle(except))
				&& VoxelShapes.matchesAnywhere(shape, VoxelShapes.cuboid(entity.getBoundingBox()), BooleanBiFunction.AND)
			) {
				return false;
			}
		}

		return true;
	}

	default <T extends Entity> List<T> getNonSpectatingEntities(Class<T> entityClass, Box box) {
		return getEntitiesByClass(entityClass, box, EntityPredicates.EXCEPT_SPECTATOR);
	}

	default List<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box) {
		if (box.getAverageSideLength() < 1.0E-7) {
			return List.of();
		}

		Predicate<Entity> predicate = entity == null
			? EntityPredicates.CAN_COLLIDE
			: EntityPredicates.EXCEPT_SPECTATOR.and(entity::collidesWith);

		List<Entity> candidates = getOtherEntities(entity, box.expand(1.0E-7), predicate);

		if (candidates.isEmpty()) {
			return List.of();
		}

		ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(candidates.size());

		for (Entity candidate : candidates) {
			builder.add(VoxelShapes.cuboid(candidate.getBoundingBox()));
		}

		return builder.build();
	}

	/**
	 * Находит ближайшего игрока в заданном радиусе, удовлетворяющего предикату.
	 *
	 * @param x             координата X центра поиска
	 * @param y             координата Y центра поиска
	 * @param z             координата Z центра поиска
	 * @param maxDistance   максимальное расстояние (отрицательное — без ограничения)
	 * @param targetPredicate дополнительный фильтр игроков, или {@code null}
	 * @return ближайший подходящий игрок, или {@code null}
	 */
	default @Nullable PlayerEntity getClosestPlayer(
		double x,
		double y,
		double z,
		double maxDistance,
		@Nullable Predicate<Entity> targetPredicate
	) {
		double closestDistSq = -1.0;
		PlayerEntity closest = null;

		for (PlayerEntity player : getPlayers()) {
			if (targetPredicate != null && !targetPredicate.test(player)) {
				continue;
			}

			double distSq = player.squaredDistanceTo(x, y, z);

			if ((maxDistance < 0.0 || distSq < maxDistance * maxDistance) && (closestDistSq == -1.0 || distSq < closestDistSq)) {
				closestDistSq = distSq;
				closest = player;
			}
		}

		return closest;
	}

	default @Nullable PlayerEntity getClosestPlayer(Entity entity, double maxDistance) {
		return getClosestPlayer(entity.getX(), entity.getY(), entity.getZ(), maxDistance, false);
	}

	default @Nullable PlayerEntity getClosestPlayer(
		double x,
		double y,
		double z,
		double maxDistance,
		boolean ignoreCreative
	) {
		Predicate<Entity> predicate = ignoreCreative
			? EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR
			: EntityPredicates.EXCEPT_SPECTATOR;
		return getClosestPlayer(x, y, z, maxDistance, predicate);
	}

	default boolean isPlayerInRange(double x, double y, double z, double range) {
		for (PlayerEntity player : getPlayers()) {
			if (!EntityPredicates.EXCEPT_SPECTATOR.test(player) || !EntityPredicates.VALID_LIVING_ENTITY.test(player)) {
				continue;
			}

			double distSq = player.squaredDistanceTo(x, y, z);

			if (range < 0.0 || distSq < range * range) {
				return true;
			}
		}

		return false;
	}

	default @Nullable PlayerEntity getPlayerByUuid(UUID uuid) {
		List<? extends PlayerEntity> players = getPlayers();

		for (int i = 0; i < players.size(); i++) {
			PlayerEntity player = players.get(i);

			if (uuid.equals(player.getUuid())) {
				return player;
			}
		}

		return null;
	}
}
