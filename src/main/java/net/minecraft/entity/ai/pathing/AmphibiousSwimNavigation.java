package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Навигация земноводного существа: одинаково хорошо работает на суше и в воде.
 * В воде проверяет прямую видимость, на суше всегда использует A*.
 */
public class AmphibiousSwimNavigation extends EntityNavigation {

	public AmphibiousSwimNavigation(MobEntity mobEntity, World world) {
		super(mobEntity, world);
	}

	@Override
	protected PathNodeNavigator createPathNodeNavigator(int range) {
		nodeMaker = new AmphibiousPathNodeMaker(false);
		return new PathNodeNavigator(nodeMaker, range);
	}

	@Override
	protected boolean isAtValidPosition() {
		return true;
	}

	@Override
	protected Vec3d getPos() {
		return new Vec3d(entity.getX(), entity.getBodyY(0.5), entity.getZ());
	}

	@Override
	protected double adjustTargetY(Vec3d pos) {
		return pos.y;
	}

	@Override
	protected boolean canPathDirectlyThrough(Vec3d origin, Vec3d target) {
		return entity.isInFluid() && doesNotCollide(entity, origin, target, false);
	}

	@Override
	public boolean isValidPosition(BlockPos pos) {
		return !world.getBlockState(pos.down()).isAir();
	}

	/** Земноводное всегда может плавать — флаг не используется. */
	@Override
	public void setCanSwim(boolean canSwim) {
	}

	@Override
	public boolean canControlOpeningDoors() {
		return true;
	}
}
