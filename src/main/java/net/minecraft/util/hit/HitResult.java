package net.minecraft.util.hit;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/**
 * Базовый класс результата трассировки луча (raycasting).
 * Хранит точку попадания и предоставляет метод вычисления расстояния до сущности.
 */
public abstract class HitResult {

	protected final Vec3d pos;

	protected HitResult(Vec3d pos) {
		this.pos = pos;
	}

	public abstract Type getType();

	public Vec3d getPos() {
		return pos;
	}

	public double squaredDistanceTo(Entity entity) {
		double dx = pos.x - entity.getX();
		double dy = pos.y - entity.getY();
		double dz = pos.z - entity.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	/** Тип результата трассировки луча. */
	public enum Type {
		MISS,
		BLOCK,
		ENTITY
	}
}
