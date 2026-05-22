package net.minecraft.entity;

import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Хранит точки прикрепления сущности (пассажиры, имя, специальные точки).
 * Каждый тип прикрепления ({@link EntityAttachmentType}) имеет список точек
 * в локальном пространстве сущности. При запросе точки она поворачивается
 * на угол рыскания сущности для перевода в мировые координаты.
 */
public class EntityAttachments {

	private final Map<EntityAttachmentType, List<Vec3d>> points;

	EntityAttachments(Map<EntityAttachmentType, List<Vec3d>> points) {
		this.points = points;
	}

	public static EntityAttachments of(float width, float height) {
		return builder().build(width, height);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Создаёт масштабированную копию всех точек прикрепления.
	 *
	 * @param xScale масштаб по оси X
	 * @param yScale масштаб по оси Y
	 * @param zScale масштаб по оси Z
	 * @return новый экземпляр с масштабированными точками
	 */
	public EntityAttachments scale(float xScale, float yScale, float zScale) {
		return new EntityAttachments(Util.mapEnum(
			EntityAttachmentType.class, type -> {
				List<Vec3d> scaled = new ArrayList<>();
				for (Vec3d point : points.get(type)) {
					scaled.add(point.multiply(xScale, yScale, zScale));
				}
				return scaled;
			}
		));
	}

	public @Nullable Vec3d getPointNullable(EntityAttachmentType type, int index, float yaw) {
		List<Vec3d> list = points.get(type);
		return index >= 0 && index < list.size()
			? rotatePoint(list.get(index), yaw)
			: null;
	}

	/**
	 * Возвращает точку прикрепления по типу и индексу.
	 *
	 * @param type  тип прикрепления
	 * @param index индекс точки
	 * @param yaw   угол рыскания сущности в градусах
	 * @return повёрнутая точка в локальном пространстве
	 * @throws IllegalStateException если точка с данным индексом не существует
	 */
	public Vec3d getPoint(EntityAttachmentType type, int index, float yaw) {
		Vec3d point = getPointNullable(type, index, yaw);
		if (point == null) {
			throw new IllegalStateException("Had no attachment point of type: " + type + " for index: " + index);
		}

		return point;
	}

	/**
	 * Возвращает среднюю точку всех прикреплений данного типа.
	 * Используется для типов, где нет конкретного индекса (например, центр груди Вардена).
	 *
	 * @param type тип прикрепления
	 * @return средняя точка всех прикреплений данного типа
	 * @throws IllegalStateException если список точек пуст
	 */
	public Vec3d getPointOrDefault(EntityAttachmentType type) {
		List<Vec3d> list = points.get(type);
		if (list == null || list.isEmpty()) {
			throw new IllegalStateException("No attachment points of type: " + type);
		}

		Vec3d sum = Vec3d.ZERO;
		for (Vec3d point : list) {
			sum = sum.add(point);
		}

		return sum.multiply(1.0F / list.size());
	}

	public Vec3d getPointOrDefault(EntityAttachmentType type, int index, float yaw) {
		List<Vec3d> list = points.get(type);
		if (list.isEmpty()) {
			throw new IllegalStateException("Had no attachment points of type: " + type);
		}

		Vec3d point = list.get(MathHelper.clamp(index, 0, list.size() - 1));
		return rotatePoint(point, yaw);
	}

	private static Vec3d rotatePoint(Vec3d point, float yaw) {
		return point.rotateY(-yaw * (float) (Math.PI / 180.0));
	}

	/**
	 * Строитель для создания {@link EntityAttachments} с кастомными точками прикрепления.
	 * Незаданные типы заполняются значениями по умолчанию из {@link EntityAttachmentType}.
	 */
	public static class Builder {

		private final Map<EntityAttachmentType, List<Vec3d>> points = new EnumMap<>(EntityAttachmentType.class);

		Builder() {
		}

		public Builder add(EntityAttachmentType type, float x, float y, float z) {
			return add(type, new Vec3d(x, y, z));
		}

		public Builder add(EntityAttachmentType type, Vec3d point) {
			points.computeIfAbsent(type, list -> new ArrayList<>(1)).add(point);
			return this;
		}

		public EntityAttachments build(float width, float height) {
			Map<EntityAttachmentType, List<Vec3d>> map = Util.mapEnum(
				EntityAttachmentType.class, type -> {
					List<Vec3d> custom = points.get(type);
					return custom == null ? type.createPoint(width, height) : List.copyOf(custom);
				}
			);
			return new EntityAttachments(map);
		}
	}
}
