package net.minecraft.entity.ai.goal;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Цель блуждания железного голема: с вероятностью 30% идёт в случайную точку,
 * иначе — к жителю или к занятой точке интереса (POI) в деревне.
 */
public class IronGolemWanderAroundGoal extends WanderAroundGoal {

	private static final int CHUNK_RANGE = 2;
	private static final int ENTITY_COLLISION_RANGE = 32;
	private static final int HORIZONTAL_RANGE = 10;
	private static final int VERTICAL_RANGE = 7;
	private static final float RANDOM_WANDER_CHANCE = 0.3F;
	private static final float VILLAGER_PRIORITY_THRESHOLD = 0.7F;

	public IronGolemWanderAroundGoal(PathAwareEntity mob, double speed) {
		super(mob, speed, 240, false);
	}

	@Override
	protected @Nullable Vec3d getWanderTarget() {
		if (mob.getEntityWorld().random.nextFloat() < RANDOM_WANDER_CHANCE) {
			return findRandomInRange();
		}

		float roll = mob.getEntityWorld().random.nextFloat();
		Vec3d target;

		if (roll < VILLAGER_PRIORITY_THRESHOLD) {
			target = findVillagerPos();

			if (target == null) {
				target = findRandomBlockPos();
			}
		} else {
			target = findRandomBlockPos();

			if (target == null) {
				target = findVillagerPos();
			}
		}

		return target == null ? findRandomInRange() : target;
	}

	private @Nullable Vec3d findRandomInRange() {
		return FuzzyTargeting.find(mob, HORIZONTAL_RANGE, VERTICAL_RANGE);
	}

	private @Nullable Vec3d findVillagerPos() {
		ServerWorld serverWorld = (ServerWorld) mob.getEntityWorld();
		List<VillagerEntity> villagers = serverWorld.getEntitiesByType(
			EntityType.VILLAGER,
			mob.getBoundingBox().expand(ENTITY_COLLISION_RANGE),
			this::canVillagerSummonGolem
		);

		if (villagers.isEmpty()) {
			return null;
		}

		VillagerEntity villager = villagers.get(mob.getEntityWorld().random.nextInt(villagers.size()));
		return FuzzyTargeting.findTo(mob, HORIZONTAL_RANGE, VERTICAL_RANGE, villager.getEntityPos());
	}

	private @Nullable Vec3d findRandomBlockPos() {
		ChunkSectionPos chunkPos = findRandomChunkPos();

		if (chunkPos == null) {
			return null;
		}

		BlockPos blockPos = findRandomPosInChunk(chunkPos);
		return blockPos == null
			? null
			: FuzzyTargeting.findTo(mob, HORIZONTAL_RANGE, VERTICAL_RANGE, Vec3d.ofBottomCenter(blockPos));
	}

	private @Nullable ChunkSectionPos findRandomChunkPos() {
		ServerWorld serverWorld = (ServerWorld) mob.getEntityWorld();
		List<ChunkSectionPos> candidates = ChunkSectionPos.stream(ChunkSectionPos.from(mob), CHUNK_RANGE)
			.filter(pos -> serverWorld.getOccupiedPointOfInterestDistance(pos) == 0)
			.collect(Collectors.toList());

		return candidates.isEmpty() ? null : candidates.get(serverWorld.random.nextInt(candidates.size()));
	}

	private @Nullable BlockPos findRandomPosInChunk(ChunkSectionPos pos) {
		ServerWorld serverWorld = (ServerWorld) mob.getEntityWorld();
		List<BlockPos> positions = serverWorld.getPointOfInterestStorage()
			.getInCircle(
				registryEntry -> true,
				pos.getCenterPos(),
				8,
				PointOfInterestStorage.OccupationStatus.IS_OCCUPIED
			)
			.map(PointOfInterest::getPos)
			.collect(Collectors.toList());

		return positions.isEmpty() ? null : positions.get(serverWorld.random.nextInt(positions.size()));
	}

	private boolean canVillagerSummonGolem(VillagerEntity villager) {
		return villager.canSummonGolem(mob.getEntityWorld().getTime());
	}
}
