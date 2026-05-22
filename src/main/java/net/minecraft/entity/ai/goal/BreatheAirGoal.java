package net.minecraft.entity.ai.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldView;

import java.util.EnumSet;

/**
 * Цель, заставляющая водного моба всплывать к поверхности для пополнения запаса воздуха.
 * Активируется, когда уровень воздуха опускается ниже {@link #MIN_AIR_LEVEL}.
 */
public class BreatheAirGoal extends Goal {

	private static final int MIN_AIR_LEVEL = 140;

	private final PathAwareEntity mob;

	public BreatheAirGoal(PathAwareEntity mob) {
		this.mob = mob;
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
	}

	@Override
	public boolean canStart() {
		return mob.getAir() < MIN_AIR_LEVEL;
	}

	@Override
	public boolean shouldContinue() {
		return canStart();
	}

	@Override
	public boolean canStop() {
		return false;
	}

	@Override
	public void start() {
		moveToAir();
	}

	private void moveToAir() {
		Iterable<BlockPos> searchArea = BlockPos.iterate(
			MathHelper.floor(mob.getX() - 1.0),
			mob.getBlockY(),
			MathHelper.floor(mob.getZ() - 1.0),
			MathHelper.floor(mob.getX() + 1.0),
			MathHelper.floor(mob.getY() + 8.0),
			MathHelper.floor(mob.getZ() + 1.0)
		);

		BlockPos airPos = null;

		for (BlockPos candidate : searchArea) {
			if (isAirPos(mob.getEntityWorld(), candidate)) {
				airPos = candidate;
				break;
			}
		}

		if (airPos == null) {
			airPos = BlockPos.ofFloored(mob.getX(), mob.getY() + 8.0, mob.getZ());
		}

		mob.getNavigation().startMovingTo(airPos.getX(), airPos.getY() + 1, airPos.getZ(), 1.0);
	}

	@Override
	public void tick() {
		moveToAir();
		mob.updateVelocity(0.02F, new Vec3d(mob.sidewaysSpeed, mob.upwardSpeed, mob.forwardSpeed));
		mob.move(MovementType.SELF, mob.getVelocity());
	}

	private boolean isAirPos(WorldView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return (world.getFluidState(pos).isEmpty() || blockState.isOf(Blocks.BUBBLE_COLUMN))
			&& blockState.canPathfindThrough(NavigationType.LAND);
	}
}
