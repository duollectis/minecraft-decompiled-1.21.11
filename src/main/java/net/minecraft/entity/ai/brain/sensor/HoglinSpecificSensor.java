package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сенсор специфической логики хоглина.
 * Ищет ближайший отпугиватель (искажённый гриб), ближайшего взрослого пиглина
 * и считает количество видимых взрослых пиглинов и хоглинов.
 */
public class HoglinSpecificSensor extends Sensor<HoglinEntity> {

	private static final int REPELLENT_SEARCH_RADIUS_XZ = 8;
	private static final int REPELLENT_SEARCH_RADIUS_Y = 4;

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(
				MemoryModuleType.VISIBLE_MOBS,
				MemoryModuleType.NEAREST_REPELLENT,
				MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN,
				MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS,
				MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,
				MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT
		);
	}

	@Override
	protected void sense(ServerWorld world, HoglinEntity entity) {
		Brain<?> brain = entity.getBrain();
		brain.remember(MemoryModuleType.NEAREST_REPELLENT, findNearestWarpedFungus(world, entity));

		Optional<PiglinEntity> nearestPiglin = Optional.empty();
		int piglinCount = 0;
		List<HoglinEntity> adultHoglins = new ArrayList<>();
		LivingTargetCache visibleMobs = brain
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				.orElse(LivingTargetCache.empty());

		for (LivingEntity mob : visibleMobs.iterate(
				m -> !m.isBaby() && (m instanceof PiglinEntity || m instanceof HoglinEntity)
		)) {
			if (mob instanceof PiglinEntity piglin) {
				piglinCount++;

				if (nearestPiglin.isEmpty()) {
					nearestPiglin = Optional.of(piglin);
				}
			}

			if (mob instanceof HoglinEntity hoglin) {
				adultHoglins.add(hoglin);
			}
		}

		brain.remember(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, nearestPiglin);
		brain.remember(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, adultHoglins);
		brain.remember(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, piglinCount);
		brain.remember(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, adultHoglins.size());
	}

	private Optional<BlockPos> findNearestWarpedFungus(ServerWorld world, HoglinEntity hoglin) {
		return BlockPos.findClosest(
				hoglin.getBlockPos(),
				REPELLENT_SEARCH_RADIUS_XZ,
				REPELLENT_SEARCH_RADIUS_Y,
				pos -> world.getBlockState(pos).isIn(BlockTags.HOGLIN_REPELLENTS)
		);
	}
}
