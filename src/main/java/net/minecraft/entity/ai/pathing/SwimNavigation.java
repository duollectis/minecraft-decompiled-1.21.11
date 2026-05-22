package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Навигация водного существа: движется в жидкости, не корректирует Y-координату цели.
 * Дельфины дополнительно могут выпрыгивать из воды.
 */
public class SwimNavigation extends EntityNavigation {

	private boolean canJumpOutOfWater;

	public SwimNavigation(MobEntity mobEntity, World world) {
		super(mobEntity, world);
	}

	@Override
	protected PathNodeNavigator createPathNodeNavigator(int range) {
		canJumpOutOfWater = entity.getType() == EntityType.DOLPHIN;
		nodeMaker = new WaterPathNodeMaker(canJumpOutOfWater);
		nodeMaker.setCanEnterOpenDoors(false);
		return new PathNodeNavigator(nodeMaker, range);
	}

	@Override
	protected boolean isAtValidPosition() {
		return canJumpOutOfWater || entity.isInFluid();
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
		return doesNotCollide(entity, origin, target, false);
	}

	@Override
	public boolean isValidPosition(BlockPos pos) {
		return !world.getBlockState(pos).isOpaqueFullCube();
	}

	/** Плавание не управляется флагом canSwim — существо всегда плавает. */
	@Override
	public void setCanSwim(boolean canSwim) {
	}

	@Override
	public boolean canControlOpeningDoors() {
		return false;
	}
}
