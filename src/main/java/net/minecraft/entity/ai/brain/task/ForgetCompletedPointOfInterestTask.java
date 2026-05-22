package net.minecraft.entity.ai.brain.task;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Фабричный класс задачи мозга, сбрасывающей память о точке интереса (POI), если та более не валидна.
 * Освобождает тикет POI, если кровать занята другой сущностью и там нет спящего жителя.
 */
public class ForgetCompletedPointOfInterestTask {

	private static final double CHECK_RANGE = 16.0;

	public static Task<LivingEntity> create(
			Predicate<RegistryEntry<PointOfInterestType>> poiTypePredicate,
			MemoryModuleType<GlobalPos> poiPosModule
	) {
		return TaskTriggerer.task(context -> context.group(context.queryMemoryValue(poiPosModule)).apply(
				context,
				poiPos -> (world, entity, time) -> {
					GlobalPos globalPos = context.getValue(poiPos);
					BlockPos blockPos = globalPos.pos();

					if (world.getRegistryKey() != globalPos.dimension()
							|| !blockPos.isWithinDistance(entity.getEntityPos(), CHECK_RANGE)) {
						return false;
					}

					ServerWorld poiWorld = world.getServer().getWorld(globalPos.dimension());

					if (poiWorld == null || !poiWorld.getPointOfInterestStorage().test(blockPos, poiTypePredicate)) {
						poiPos.forget();
					} else if (isBedOccupiedByOthers(poiWorld, blockPos, entity)) {
						poiPos.forget();

						if (!isSleepingVillagerAt(poiWorld, blockPos)) {
							world.getPointOfInterestStorage().releaseTicket(blockPos);
							world.getSubscriptionTracker().onPoiUpdated(blockPos);
						}
					}

					return true;
				}
		));
	}

	private static boolean isBedOccupiedByOthers(ServerWorld world, BlockPos pos, LivingEntity entity) {
		BlockState blockState = world.getBlockState(pos);
		return blockState.isIn(BlockTags.BEDS) && blockState.get(BedBlock.OCCUPIED) && !entity.isSleeping();
	}

	private static boolean isSleepingVillagerAt(ServerWorld world, BlockPos pos) {
		List<VillagerEntity> sleepers = world.getEntitiesByClass(VillagerEntity.class, new Box(pos), LivingEntity::isSleeping);
		return !sleepers.isEmpty();
	}
}
