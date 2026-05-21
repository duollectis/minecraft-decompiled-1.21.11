package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.task.TargetUtil;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * {@code WanderAroundPointOfInterestGoal}.
 */
public class WanderAroundPointOfInterestGoal extends WanderAroundGoal {

	private static final int HORIZONTAL_RANGE = 10;
	private static final int VERTICAL_RANGE = 7;

	public WanderAroundPointOfInterestGoal(PathAwareEntity entity, double speed, boolean canDespawn) {
		super(entity, speed, 10, canDespawn);
	}

	@Override
	public boolean canStart() {
		ServerWorld serverWorld = (ServerWorld) this.mob.getEntityWorld();
		BlockPos blockPos = this.mob.getBlockPos();
		return serverWorld.isNearOccupiedPointOfInterest(blockPos) ? false : super.canStart();
	}

	@Override
	protected @Nullable Vec3d getWanderTarget() {
		ServerWorld serverWorld = (ServerWorld) this.mob.getEntityWorld();
		BlockPos blockPos = this.mob.getBlockPos();
		ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(blockPos);
		ChunkSectionPos
				chunkSectionPos2 =
				TargetUtil.getPosClosestToOccupiedPointOfInterest(serverWorld, chunkSectionPos, 2);
		return chunkSectionPos2 != chunkSectionPos
		       ? NoPenaltyTargeting.findTo(
				this.mob,
				10,
				7,
				Vec3d.ofBottomCenter(chunkSectionPos2.getCenterPos()),
				(float) (Math.PI / 2)
		)
		       : null;
	}
}
