package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * {@code WanderNearTargetGoal}.
 */
public class WanderNearTargetGoal extends Goal {

	private final PathAwareEntity mob;
	private @Nullable LivingEntity target;
	private double x;
	private double y;
	private double z;
	private final double speed;
	private final float maxDistance;

	public WanderNearTargetGoal(PathAwareEntity mob, double speed, float maxDistance) {
		this.mob = mob;
		this.speed = speed;
		this.maxDistance = maxDistance;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		this.target = this.mob.getTarget();
		if (this.target == null) {
			return false;
		}
		else if (this.target.squaredDistanceTo(this.mob) > this.maxDistance * this.maxDistance) {
			return false;
		}
		else {
			Vec3d vec3d = NoPenaltyTargeting.findTo(this.mob, 16, 7, this.target.getEntityPos(), (float) (Math.PI / 2));
			if (vec3d == null) {
				return false;
			}
			else {
				this.x = vec3d.x;
				this.y = vec3d.y;
				this.z = vec3d.z;
				return true;
			}
		}
	}

	@Override
	public boolean shouldContinue() {
		return !this.mob.getNavigation().isIdle() && this.target.isAlive()
				&& this.target.squaredDistanceTo(this.mob) < this.maxDistance * this.maxDistance;
	}

	@Override
	public void stop() {
		this.target = null;
	}

	@Override
	public void start() {
		this.mob.getNavigation().startMovingTo(this.x, this.y, this.z, this.speed);
	}
}
