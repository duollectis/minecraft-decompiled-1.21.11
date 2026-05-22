package net.minecraft.entity.ai.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Базовая цель взаимодействия с дверью: определяет позицию двери по текущему пути
 * навигации и отслеживает момент, когда моб прошёл сквозь неё (для остановки цели).
 */
public abstract class DoorInteractGoal extends Goal {

	private static final double DOOR_REACH_DISTANCE_SQ = 2.25;
	private static final int PATH_LOOKAHEAD = 2;

	protected MobEntity mob;
	protected BlockPos doorPos = BlockPos.ORIGIN;
	protected boolean doorValid;
	private boolean shouldStop;
	private float offsetX;
	private float offsetZ;

	public DoorInteractGoal(MobEntity mob) {
		this.mob = mob;

		if (!NavigationConditions.hasMobNavigation(mob)) {
			throw new IllegalArgumentException("Unsupported mob type for DoorInteractGoal");
		}
	}

	protected boolean isDoorOpen() {
		if (!doorValid) {
			return false;
		}

		BlockState blockState = mob.getEntityWorld().getBlockState(doorPos);

		if (!(blockState.getBlock() instanceof DoorBlock)) {
			doorValid = false;
			return false;
		}

		return blockState.get(DoorBlock.OPEN);
	}

	protected void setDoorOpen(boolean open) {
		if (!doorValid) {
			return;
		}

		BlockState blockState = mob.getEntityWorld().getBlockState(doorPos);

		if (blockState.getBlock() instanceof DoorBlock doorBlock) {
			doorBlock.setOpen(mob, mob.getEntityWorld(), blockState, doorPos, open);
		}
	}

	@Override
	public boolean canStart() {
		if (!NavigationConditions.hasMobNavigation(mob)) {
			return false;
		}

		if (!mob.horizontalCollision) {
			return false;
		}

		Path path = mob.getNavigation().getCurrentPath();

		if (path == null || path.isFinished()) {
			return false;
		}

		for (int i = 0; i < Math.min(path.getCurrentNodeIndex() + PATH_LOOKAHEAD, path.getLength()); i++) {
			PathNode pathNode = path.getNode(i);
			doorPos = new BlockPos(pathNode.x, pathNode.y + 1, pathNode.z);

			if (mob.squaredDistanceTo(doorPos.getX(), mob.getY(), doorPos.getZ()) <= DOOR_REACH_DISTANCE_SQ) {
				doorValid = DoorBlock.canOpenByHand(mob.getEntityWorld(), doorPos);

				if (doorValid) {
					return true;
				}
			}
		}

		doorPos = mob.getBlockPos().up();
		doorValid = DoorBlock.canOpenByHand(mob.getEntityWorld(), doorPos);
		return doorValid;
	}

	@Override
	public boolean shouldContinue() {
		return !shouldStop;
	}

	@Override
	public void start() {
		shouldStop = false;
		offsetX = (float) (doorPos.getX() + 0.5 - mob.getX());
		offsetZ = (float) (doorPos.getZ() + 0.5 - mob.getZ());
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		float currentOffsetX = (float) (doorPos.getX() + 0.5 - mob.getX());
		float currentOffsetZ = (float) (doorPos.getZ() + 0.5 - mob.getZ());
		float dotProduct = offsetX * currentOffsetX + offsetZ * currentOffsetZ;

		if (dotProduct < 0.0F) {
			shouldStop = true;
		}
	}
}
