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
 * {@code IronGolemWanderAroundGoal}.
 */
public class IronGolemWanderAroundGoal extends WanderAroundGoal {

	private static final int CHUNK_RANGE = 2;
	private static final int ENTITY_COLLISION_RANGE = 32;
	private static final int HORIZONTAL_RANGE = 10;
	private static final int VERTICAL_RANGE = 7;

	public IronGolemWanderAroundGoal(PathAwareEntity pathAwareEntity, double d) {
		super(pathAwareEntity, d, 240, false);
	}

	@Override
	protected @Nullable Vec3d getWanderTarget() {
		float f = this.mob.getEntityWorld().random.nextFloat();
		if (this.mob.getEntityWorld().random.nextFloat() < 0.3F) {
			return this.findRandomInRange();
		}
		else {
			Vec3d vec3d;
			if (f < 0.7F) {
				vec3d = this.findVillagerPos();
				if (vec3d == null) {
					vec3d = this.findRandomBlockPos();
				}
			}
			else {
				vec3d = this.findRandomBlockPos();
				if (vec3d == null) {
					vec3d = this.findVillagerPos();
				}
			}

			return vec3d == null ? this.findRandomInRange() : vec3d;
		}
	}

	private @Nullable Vec3d findRandomInRange() {
		return FuzzyTargeting.find(this.mob, 10, 7);
	}

	private @Nullable Vec3d findVillagerPos() {
		ServerWorld serverWorld = (ServerWorld) this.mob.getEntityWorld();
		List<VillagerEntity>
				list =
				serverWorld.getEntitiesByType(
						EntityType.VILLAGER,
						this.mob.getBoundingBox().expand(32.0),
						this::canVillagerSummonGolem
				);
		if (list.isEmpty()) {
			return null;
		}
		else {
			VillagerEntity villagerEntity = list.get(this.mob.getEntityWorld().random.nextInt(list.size()));
			Vec3d vec3d = villagerEntity.getEntityPos();
			return FuzzyTargeting.findTo(this.mob, 10, 7, vec3d);
		}
	}

	private @Nullable Vec3d findRandomBlockPos() {
		ChunkSectionPos chunkSectionPos = this.findRandomChunkPos();
		if (chunkSectionPos == null) {
			return null;
		}
		else {
			BlockPos blockPos = this.findRandomPosInChunk(chunkSectionPos);
			return blockPos == null ? null : FuzzyTargeting.findTo(this.mob, 10, 7, Vec3d.ofBottomCenter(blockPos));
		}
	}

	private @Nullable ChunkSectionPos findRandomChunkPos() {
		ServerWorld serverWorld = (ServerWorld) this.mob.getEntityWorld();
		List<ChunkSectionPos> list = ChunkSectionPos.stream(ChunkSectionPos.from(this.mob), 2)
		                                            .filter(sectionPos ->
				                                            serverWorld.getOccupiedPointOfInterestDistance(sectionPos)
						                                            == 0)
		                                            .collect(Collectors.toList());
		return list.isEmpty() ? null : list.get(serverWorld.random.nextInt(list.size()));
	}

	private @Nullable BlockPos findRandomPosInChunk(ChunkSectionPos pos) {
		ServerWorld serverWorld = (ServerWorld) this.mob.getEntityWorld();
		PointOfInterestStorage pointOfInterestStorage = serverWorld.getPointOfInterestStorage();
		List<BlockPos> list = pointOfInterestStorage.getInCircle(
				                                            registryEntry -> true, pos.getCenterPos(), 8, PointOfInterestStorage.OccupationStatus.IS_OCCUPIED
		                                            )
		                                            .map(PointOfInterest::getPos)
		                                            .collect(Collectors.toList());
		return list.isEmpty() ? null : list.get(serverWorld.random.nextInt(list.size()));
	}

	private boolean canVillagerSummonGolem(VillagerEntity villager) {
		return villager.canSummonGolem(this.mob.getEntityWorld().getTime());
	}
}
