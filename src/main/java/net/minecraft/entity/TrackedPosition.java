package net.minecraft.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.math.Vec3d;

/**
 * Хранит позицию сущности в упакованном целочисленном формате для точной передачи по сети.
 * Координаты масштабируются с коэффициентом {@code 4096} для сохранения субблочной точности.
 */
public class TrackedPosition {

	private static final double COORDINATE_SCALE = 4096.0;
	private Vec3d pos = Vec3d.ZERO;

	@VisibleForTesting
	static long pack(double value) {
		return Math.round(value * COORDINATE_SCALE);
	}

	@VisibleForTesting
	static double unpack(long value) {
		return value / COORDINATE_SCALE;
	}

	/**
	 * Применяет сетевые дельта-значения к текущей позиции и возвращает новую позицию.
	 * Если дельта по оси равна нулю — координата остаётся без изменений.
	 */
	public Vec3d withDelta(long x, long y, long z) {
		if (x == 0L && y == 0L && z == 0L) {
			return pos;
		}

		double newX = x == 0L ? pos.x : unpack(pack(pos.x) + x);
		double newY = y == 0L ? pos.y : unpack(pack(pos.y) + y);
		double newZ = z == 0L ? pos.z : unpack(pack(pos.z) + z);

		return new Vec3d(newX, newY, newZ);
	}

	public long getDeltaX(Vec3d newPos) {
		return pack(newPos.x) - pack(pos.x);
	}

	public long getDeltaY(Vec3d newPos) {
		return pack(newPos.y) - pack(pos.y);
	}

	public long getDeltaZ(Vec3d newPos) {
		return pack(newPos.z) - pack(pos.z);
	}

	public Vec3d subtract(Vec3d other) {
		return other.subtract(pos);
	}

	public void setPos(Vec3d newPos) {
		pos = newPos;
	}

	public Vec3d getPos() {
		return pos;
	}
}
