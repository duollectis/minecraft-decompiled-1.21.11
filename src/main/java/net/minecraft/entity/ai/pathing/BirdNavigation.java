package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Навигация летающего существа: движется напрямую через воздух,
 * проверяя отсутствие коллизий по прямой линии.
 */
public class BirdNavigation extends EntityNavigation {

	public BirdNavigation(MobEntity mobEntity, World world) {
		super(mobEntity, world);
	}

	@Override
	protected PathNodeNavigator createPathNodeNavigator(int range) {
		nodeMaker = new BirdPathNodeMaker();
		return new PathNodeNavigator(nodeMaker, range);
	}

	@Override
	protected boolean canPathDirectlyThrough(Vec3d origin, Vec3d target) {
		return doesNotCollide(entity, origin, target, true);
	}

	@Override
	protected boolean isAtValidPosition() {
		return canSwim() && entity.isInFluid() || !entity.hasVehicle();
	}

	@Override
	protected Vec3d getPos() {
		return entity.getEntityPos();
	}

	@Override
	public Path findPathTo(Entity entity, int distance) {
		return findPathTo(entity.getBlockPos(), distance);
	}

	/**
	 * Переопределяет tick: для летающих существ переход к следующему узлу происходит
	 * при совпадении блочных координат, а не по расстоянию.
	 */
	@Override
	public void tick() {
		tickCount++;

		if (inRecalculationCooldown) {
			recalculatePath();
		}

		if (isIdle()) {
			return;
		}

		if (isAtValidPosition()) {
			continueFollowingPath();
		} else if (currentPath != null && !currentPath.isFinished()) {
			Vec3d nodePos = currentPath.getNodePosition(entity);

			if (entity.getBlockX() == MathHelper.floor(nodePos.x)
					&& entity.getBlockY() == MathHelper.floor(nodePos.y)
					&& entity.getBlockZ() == MathHelper.floor(nodePos.z)) {
				currentPath.next();
			}
		}

		if (!isIdle()) {
			Vec3d targetPos = currentPath.getNodePosition(entity);
			entity.getMoveControl().moveTo(targetPos.x, targetPos.y, targetPos.z, speed);
		}
	}

	@Override
	public boolean isValidPosition(BlockPos pos) {
		return world.getBlockState(pos).hasSolidTopSurface(world, pos, entity);
	}

	@Override
	public boolean canControlOpeningDoors() {
		return false;
	}
}
