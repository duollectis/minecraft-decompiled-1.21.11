package net.minecraft.entity.ai.control;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

/**
 * Базовый контроллер движения наземных мобов.
 * Обрабатывает состояния STRAFE, MOVE_TO и JUMPING,
 * включая автоматический прыжок через препятствия.
 */
public class MoveControl implements Control {

	public static final float MIN_SPEED_THRESHOLD = 5.0E-4F;
	public static final float REACHED_DESTINATION_DISTANCE_SQUARED = 2.5000003E-7F;
	private static final double STRAFE_SPEED = 0.25;

	protected static final int MAX_TURN_DEGREES = 90;
	protected final MobEntity entity;
	protected double targetX;
	protected double targetY;
	protected double targetZ;
	protected double speed;
	protected float forwardMovement;
	protected float sidewaysMovement;
	protected MoveControl.State state = MoveControl.State.WAIT;

	public MoveControl(MobEntity entity) {
		this.entity = entity;
	}

	public boolean isMoving() {
		return state == MoveControl.State.MOVE_TO;
	}

	public double getSpeed() {
		return speed;
	}

	public void moveTo(double x, double y, double z, double speed) {
		targetX = x;
		targetY = y;
		targetZ = z;
		this.speed = speed;

		if (state != MoveControl.State.JUMPING) {
			state = MoveControl.State.MOVE_TO;
		}
	}

	public void strafeTo(float forward, float sideways) {
		state = MoveControl.State.STRAFE;
		forwardMovement = forward;
		sidewaysMovement = sideways;
		speed = STRAFE_SPEED;
	}

	public void tick() {
		if (state == MoveControl.State.STRAFE) {
			tickStrafe();
		}
		else if (state == MoveControl.State.MOVE_TO) {
			tickMoveTo();
		}
		else if (state == MoveControl.State.JUMPING) {
			tickJumping();
		}
		else {
			entity.setForwardSpeed(0.0F);
		}
	}

	private void tickStrafe() {
		float baseSpeed = (float) entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
		float scaledSpeed = (float) speed * baseSpeed;
		float fwd = forwardMovement;
		float side = sidewaysMovement;

		float magnitude = MathHelper.sqrt(fwd * fwd + side * side);

		if (magnitude < 1.0F) {
			magnitude = 1.0F;
		}

		magnitude = scaledSpeed / magnitude;
		fwd *= magnitude;
		side *= magnitude;

		float sinYaw = MathHelper.sin(entity.getYaw() * (float) (Math.PI / 180.0));
		float cosYaw = MathHelper.cos(entity.getYaw() * (float) (Math.PI / 180.0));
		float worldX = fwd * cosYaw - side * sinYaw;
		float worldZ = side * cosYaw + fwd * sinYaw;

		if (!isPosWalkable(worldX, worldZ)) {
			forwardMovement = 1.0F;
			sidewaysMovement = 0.0F;
		}

		entity.setMovementSpeed(scaledSpeed);
		entity.setForwardSpeed(forwardMovement);
		entity.setSidewaysSpeed(sidewaysMovement);
		state = MoveControl.State.WAIT;
	}

	private void tickMoveTo() {
		state = MoveControl.State.WAIT;

		double dx = targetX - entity.getX();
		double dz = targetZ - entity.getZ();
		double dy = targetY - entity.getY();
		double distSq = dx * dx + dy * dy + dz * dz;

		if (distSq < REACHED_DESTINATION_DISTANCE_SQUARED) {
			entity.setForwardSpeed(0.0F);
			return;
		}

		float targetYaw = (float) (MathHelper.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
		entity.setYaw(wrapDegrees(entity.getYaw(), targetYaw, MAX_TURN_DEGREES));
		entity.setMovementSpeed((float) (speed * entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED)));

		BlockPos blockPos = entity.getBlockPos();
		BlockState blockState = entity.getEntityWorld().getBlockState(blockPos);
		VoxelShape collisionShape = blockState.getCollisionShape(entity.getEntityWorld(), blockPos);

		boolean shouldJump = (dy > entity.getStepHeight() && dx * dx + dz * dz < Math.max(1.0F, entity.getWidth()))
				|| (collisionShape.isEmpty() == false
				&& entity.getY() < collisionShape.getMax(Direction.Axis.Y) + blockPos.getY()
				&& blockState.isIn(BlockTags.DOORS) == false
				&& blockState.isIn(BlockTags.FENCES) == false);

		if (shouldJump) {
			entity.getJumpControl().setActive();
			state = MoveControl.State.JUMPING;
		}
	}

	private void tickJumping() {
		entity.setMovementSpeed((float) (speed * entity.getAttributeValue(EntityAttributes.MOVEMENT_SPEED)));

		if (entity.isOnGround() || (entity.isInFluid() && entity.shouldSwimInFluids())) {
			state = MoveControl.State.WAIT;
		}
	}

	private boolean isPosWalkable(float x, float z) {
		EntityNavigation navigation = entity.getNavigation();

		if (navigation == null) {
			return true;
		}

		PathNodeMaker nodeMaker = navigation.getNodeMaker();

		if (nodeMaker == null) {
			return true;
		}

		return nodeMaker.getDefaultNodeType(
				entity,
				BlockPos.ofFloored(entity.getX() + x, entity.getBlockY(), entity.getZ() + z)
		) == PathNodeType.WALKABLE;
	}

	/**
	 * Поворачивает угол {@code from} в сторону {@code to} не более чем на {@code max} градусов,
	 * нормализуя результат в диапазон [0, 360].
	 */
	protected float wrapDegrees(float from, float to, float max) {
		float delta = MathHelper.wrapDegrees(to - from);
		delta = MathHelper.clamp(delta, -max, max);

		float result = from + delta;

		if (result < 0.0F) {
			result += 360.0F;
		}
		else if (result > 360.0F) {
			result -= 360.0F;
		}

		return result;
	}

	public double getTargetX() {
		return targetX;
	}

	public double getTargetY() {
		return targetY;
	}

	public double getTargetZ() {
		return targetZ;
	}

	public void setWaiting() {
		state = MoveControl.State.WAIT;
	}

	protected enum State {
		WAIT,
		MOVE_TO,
		STRAFE,
		JUMPING;
	}
}
