package net.minecraft.entity.vehicle;

import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * {@code MinecartController}.
 */
public abstract class MinecartController {

	protected final AbstractMinecartEntity minecart;

	protected MinecartController(AbstractMinecartEntity minecart) {
		this.minecart = minecart;
	}

	public PositionInterpolator getInterpolator() {
		return null;
	}

	public void setLerpTargetVelocity(Vec3d vec3d) {
		this.setVelocity(vec3d);
	}

	/**
	 * Tick.
	 */
	public abstract void tick();

	public World getWorld() {
		return this.minecart.getEntityWorld();
	}

	/**
	 * Перемещает on rail.
	 *
	 * @param world world
	 */
	public abstract void moveOnRail(ServerWorld world);

	/**
	 * Перемещает along track.
	 *
	 * @param blockPos block pos
	 * @param railShape rail shape
	 * @param remainingMovement remaining movement
	 *
	 * @return double — результат операции
	 */
	public abstract double moveAlongTrack(BlockPos blockPos, RailShape railShape, double remainingMovement);

	/**
	 * Обрабатывает collision.
	 *
	 * @return boolean — результат операции
	 */
	public abstract boolean handleCollision();

	public Vec3d getVelocity() {
		return this.minecart.getVelocity();
	}

	public void setVelocity(Vec3d velocity) {
		this.minecart.setVelocity(velocity);
	}

	public void setVelocity(double x, double y, double z) {
		this.minecart.setVelocity(x, y, z);
	}

	public Vec3d getPos() {
		return this.minecart.getEntityPos();
	}

	public double getX() {
		return this.minecart.getX();
	}

	public double getY() {
		return this.minecart.getY();
	}

	public double getZ() {
		return this.minecart.getZ();
	}

	public void setPos(Vec3d pos) {
		this.minecart.setPosition(pos);
	}

	public void setPos(double x, double y, double z) {
		this.minecart.setPosition(x, y, z);
	}

	public float getPitch() {
		return this.minecart.getPitch();
	}

	public void setPitch(float pitch) {
		this.minecart.setPitch(pitch);
	}

	public float getYaw() {
		return this.minecart.getYaw();
	}

	public void setYaw(float yaw) {
		this.minecart.setYaw(yaw);
	}

	public Direction getHorizontalFacing() {
		return this.minecart.getHorizontalFacing();
	}

	/**
	 * Limit speed.
	 *
	 * @param velocity velocity
	 *
	 * @return Vec3d — результат операции
	 */
	public Vec3d limitSpeed(Vec3d velocity) {
		return velocity;
	}

	public abstract double getMaxSpeed(ServerWorld world);

	public abstract double getSpeedRetention();
}
