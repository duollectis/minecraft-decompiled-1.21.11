package net.minecraft.entity.ai.control;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Контроллер направления взгляда моба.
 * Плавно поворачивает голову к заданной точке в течение {@code lookAtTimer} тиков,
 * после чего возвращает голову к направлению тела.
 */
public class LookControl implements Control {

	private static final float RETURN_TO_BODY_SPEED = 10.0F;
	private static final float LOOK_EPSILON = 1.0E-5F;
	private static final int LOOK_TIMER_RESET = 2;

	protected final MobEntity entity;
	protected float maxYawChange;
	protected float maxPitchChange;
	protected int lookAtTimer;
	protected double x;
	protected double y;
	protected double z;

	public LookControl(MobEntity entity) {
		this.entity = entity;
	}

	public void lookAt(Vec3d direction) {
		lookAt(direction.x, direction.y, direction.z);
	}

	public void lookAt(Entity target) {
		lookAt(target.getX(), target.getEyeY(), target.getZ());
	}

	public void lookAt(Entity target, float maxYawChange, float maxPitchChange) {
		lookAt(target.getX(), target.getEyeY(), target.getZ(), maxYawChange, maxPitchChange);
	}

	public void lookAt(double x, double y, double z) {
		lookAt(x, y, z, entity.getMaxLookYawChange(), entity.getMaxLookPitchChange());
	}

	public void lookAt(double x, double y, double z, float maxYawChange, float maxPitchChange) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.maxYawChange = maxYawChange;
		this.maxPitchChange = maxPitchChange;
		lookAtTimer = LOOK_TIMER_RESET;
	}

	public void tick() {
		if (shouldStayHorizontal()) {
			entity.setPitch(0.0F);
		}

		if (lookAtTimer > 0) {
			lookAtTimer--;
			getTargetYaw().ifPresent(yaw -> entity.headYaw = changeAngle(entity.headYaw, yaw, maxYawChange));
			getTargetPitch().ifPresent(pitch -> entity.setPitch(changeAngle(entity.getPitch(), pitch, maxPitchChange)));
		}
		else {
			entity.headYaw = changeAngle(entity.headYaw, entity.bodyYaw, RETURN_TO_BODY_SPEED);
		}

		clampHeadYaw();
	}

	protected void clampHeadYaw() {
		if (!entity.getNavigation().isIdle()) {
			entity.headYaw = MathHelper.clampAngle(entity.headYaw, entity.bodyYaw, entity.getMaxHeadRotation());
		}
	}

	protected boolean shouldStayHorizontal() {
		return true;
	}

	public boolean isLookingAtSpecificPosition() {
		return lookAtTimer > 0;
	}

	public double getLookX() {
		return x;
	}

	public double getLookY() {
		return y;
	}

	public double getLookZ() {
		return z;
	}

	protected Optional<Float> getTargetPitch() {
		double dx = x - entity.getX();
		double dy = y - entity.getEyeY();
		double dz = z - entity.getZ();
		double horizontalDist = Math.sqrt(dx * dx + dz * dz);

		return Math.abs(dy) <= LOOK_EPSILON && Math.abs(horizontalDist) <= LOOK_EPSILON
				? Optional.empty()
				: Optional.of((float) (-(MathHelper.atan2(dy, horizontalDist) * 180.0F / (float) Math.PI)));
	}

	protected Optional<Float> getTargetYaw() {
		double dx = x - entity.getX();
		double dz = z - entity.getZ();

		return Math.abs(dz) <= LOOK_EPSILON && Math.abs(dx) <= LOOK_EPSILON
				? Optional.empty()
				: Optional.of((float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F);
	}
}
