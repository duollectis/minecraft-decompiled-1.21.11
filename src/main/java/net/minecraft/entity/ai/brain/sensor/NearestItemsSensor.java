package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сенсор поиска ближайшего видимого предмета, который моб хочет подобрать.
 * Ищет {@link ItemEntity} в радиусе {@code MAX_RANGE} блоков и фильтрует по {@code canGather}.
 */
public class NearestItemsSensor extends Sensor<MobEntity> {

	private static final double HORIZONTAL_RANGE = 32.0;
	private static final double VERTICAL_RANGE = 16.0;
	public static final int MAX_RANGE = 32;

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
	}

	@Override
	protected void sense(ServerWorld world, MobEntity entity) {
		Brain<?> brain = entity.getBrain();

		List<ItemEntity> items = world.getEntitiesByClass(
				ItemEntity.class,
				entity.getBoundingBox().expand(HORIZONTAL_RANGE, VERTICAL_RANGE, HORIZONTAL_RANGE),
				itemEntity -> true
		);
		items.sort(Comparator.comparingDouble(entity::squaredDistanceTo));

		Optional<ItemEntity> nearest = items.stream()
				.filter(item -> entity.canGather(world, item.getStack()))
				.filter(item -> item.isInRange(entity, MAX_RANGE))
				.filter(entity::canSee)
				.findFirst();

		brain.remember(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, nearest);
	}
}
