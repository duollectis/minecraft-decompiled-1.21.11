package net.minecraft.entity.ai.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Цель, заставляющая моба войти в ближайшую воду в радиусе {@code WATER_SEARCH_RADIUS} блоков.
 * Активируется, когда моб стоит на земле вне воды.
 */
public class MoveIntoWaterGoal extends Goal {

	private static final int WATER_SEARCH_RADIUS = 2;
	private static final double MOVE_SPEED = 1.0;

	private final PathAwareEntity mob;

	public MoveIntoWaterGoal(PathAwareEntity mob) {
		this.mob = mob;
	}

	@Override
	public boolean canStart() {
		return mob.isOnGround() && !mob.getEntityWorld().getFluidState(mob.getBlockPos()).isIn(FluidTags.WATER);
	}

	@Override
	public void start() {
		int minX = MathHelper.floor(mob.getX() - WATER_SEARCH_RADIUS);
		int minY = MathHelper.floor(mob.getY() - WATER_SEARCH_RADIUS);
		int minZ = MathHelper.floor(mob.getZ() - WATER_SEARCH_RADIUS);
		int maxX = MathHelper.floor(mob.getX() + WATER_SEARCH_RADIUS);
		int maxZ = MathHelper.floor(mob.getZ() + WATER_SEARCH_RADIUS);

		for (BlockPos candidate : BlockPos.iterate(minX, minY, minZ, maxX, mob.getBlockY(), maxZ)) {
			if (mob.getEntityWorld().getFluidState(candidate).isIn(FluidTags.WATER)) {
				mob.getMoveControl().moveTo(candidate.getX(), candidate.getY(), candidate.getZ(), MOVE_SPEED);
				break;
			}
		}
	}
}
