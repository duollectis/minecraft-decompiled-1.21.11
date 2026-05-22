package net.minecraft.entity.vehicle;

import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Абстрактный контроллер движения вагонетки.
 * Инкапсулирует всю физику рельсового движения, позволяя переключаться между
 * классической ({@link DefaultMinecartController}) и экспериментальной
 * ({@link ExperimentalMinecartController}) реализациями через флаг {@code minecart_improvements}.
 */
public abstract class MinecartController {

	protected final AbstractMinecartEntity minecart;

	protected MinecartController(AbstractMinecartEntity minecart) {
		this.minecart = minecart;
	}

	public PositionInterpolator getInterpolator() {
		return null;
	}

	public void setLerpTargetVelocity(Vec3d velocity) {
		setVelocity(velocity);
	}

	/** Выполняет один тик физики вагонетки. */
	public abstract void tick();

	public World getWorld() {
		return minecart.getEntityWorld();
	}

	/**
	 * Выполняет движение вагонетки по рельсу за один тик.
	 *
	 * @param world серверный мир для доступа к блокам и правилам
	 */
	public abstract void moveOnRail(ServerWorld world);

	/**
	 * Перемещает вагонетку вдоль рельса на заданное расстояние.
	 *
	 * @param blockPos позиция блока рельса
	 * @param railShape форма рельса
	 * @param remainingMovement оставшееся расстояние для перемещения
	 * @return остаток расстояния после перемещения
	 */
	public abstract double moveAlongTrack(BlockPos blockPos, RailShape railShape, double remainingMovement);

	/**
	 * Обрабатывает столкновения вагонетки с другими сущностями.
	 *
	 * @return {@code true} если столкновение было обработано и требуется коррекция позиции
	 */
	public abstract boolean handleCollision();

	public Vec3d getVelocity() {
		return minecart.getVelocity();
	}

	public void setVelocity(Vec3d velocity) {
		minecart.setVelocity(velocity);
	}

	public void setVelocity(double x, double y, double z) {
		minecart.setVelocity(x, y, z);
	}

	public Vec3d getPos() {
		return minecart.getEntityPos();
	}

	public double getX() {
		return minecart.getX();
	}

	public double getY() {
		return minecart.getY();
	}

	public double getZ() {
		return minecart.getZ();
	}

	public void setPos(Vec3d pos) {
		minecart.setPosition(pos);
	}

	public void setPos(double x, double y, double z) {
		minecart.setPosition(x, y, z);
	}

	public float getPitch() {
		return minecart.getPitch();
	}

	public void setPitch(float pitch) {
		minecart.setPitch(pitch);
	}

	public float getYaw() {
		return minecart.getYaw();
	}

	public void setYaw(float yaw) {
		minecart.setYaw(yaw);
	}

	public Direction getHorizontalFacing() {
		return minecart.getHorizontalFacing();
	}

	/**
	 * Ограничивает скорость вагонетки до допустимого максимума.
	 * По умолчанию не изменяет скорость; переопределяется в конкретных контроллерах.
	 */
	public Vec3d limitSpeed(Vec3d velocity) {
		return velocity;
	}

	/**
	 * Возвращает максимальную скорость вагонетки в блоках/тик.
	 *
	 * @param world серверный мир (может влиять на скорость через правила игры)
	 */
	public abstract double getMaxSpeed(ServerWorld world);

	/**
	 * Возвращает коэффициент сохранения скорости за тик (0.0–1.0).
	 * Значение меньше 1.0 означает постепенное замедление.
	 */
	public abstract double getSpeedRetention();
}
