package net.minecraft.entity;

import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Типы точек прикрепления сущности.
 * Каждый тип определяет, как вычисляется точка по умолчанию
 * на основе размеров сущности (ширина и высота).
 */
public enum EntityAttachmentType {
	PASSENGER(Point.AT_HEIGHT),
	VEHICLE(Point.ZERO),
	NAME_TAG(Point.AT_HEIGHT),
	WARDEN_CHEST(Point.WARDEN_CHEST);

	private final Point point;

	EntityAttachmentType(Point point) {
		this.point = point;
	}

	public List<Vec3d> createPoint(float width, float height) {
		return point.create(width, height);
	}

	/**
	 * Стратегия вычисления точки прикрепления по умолчанию.
	 * Реализации определяют, где именно располагается точка
	 * относительно центра сущности.
	 */
	public interface Point {

		List<Vec3d> NONE = List.of(Vec3d.ZERO);

		Point ZERO = (width, height) -> NONE;

		Point AT_HEIGHT = (width, height) -> List.of(new Vec3d(0.0, height, 0.0));

		Point WARDEN_CHEST = (width, height) -> List.of(new Vec3d(0.0, height / 2.0, 0.0));

		List<Vec3d> create(float width, float height);
	}
}
