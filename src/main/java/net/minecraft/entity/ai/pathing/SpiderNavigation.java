package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Навигация паука: умеет карабкаться по стенам.
 * При отсутствии пути продолжает двигаться напрямую к цели.
 */
public class SpiderNavigation extends MobNavigation {

	private @Nullable BlockPos targetPos;

	public SpiderNavigation(MobEntity mobEntity, World world) {
		super(mobEntity, world);
	}

	@Override
	public Path findPathTo(BlockPos target, int distance) {
		targetPos = target;
		return super.findPathTo(target, distance);
	}

	@Override
	public Path findPathTo(Entity entity, int distance) {
		targetPos = entity.getBlockPos();
		return super.findPathTo(entity, distance);
	}

	@Override
	public boolean startMovingTo(Entity entity, double speed) {
		Path path = findPathTo(entity, 0);

		if (path != null) {
			return startMovingAlong(path, speed);
		}

		targetPos = entity.getBlockPos();
		this.speed = speed;
		return true;
	}

	/**
	 * При отсутствии пути (паук карабкается по стене) двигается напрямую к цели,
	 * пока не окажется в пределах ширины существа или не поднимется выше цели.
	 */
	@Override
	public void tick() {
		if (!isIdle()) {
			super.tick();
			return;
		}

		if (targetPos == null) {
			return;
		}

		boolean withinWidth = targetPos.isWithinDistance(entity.getEntityPos(), entity.getWidth());
		boolean aboveTargetAndAligned = entity.getY() > targetPos.getY()
				&& BlockPos.ofFloored(targetPos.getX(), entity.getY(), targetPos.getZ())
				.isWithinDistance(entity.getEntityPos(), entity.getWidth());

		if (withinWidth || aboveTargetAndAligned) {
			targetPos = null;
		} else {
			entity.getMoveControl().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), speed);
		}
	}
}
