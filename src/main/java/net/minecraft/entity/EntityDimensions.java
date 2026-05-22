package net.minecraft.entity;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Описывает физические размеры сущности: ширину, высоту, высоту глаз и точки прикрепления.
 * Флаг {@code fixed} запрещает масштабирование — используется для сущностей с фиксированным хитбоксом
 * (например, предметы, снаряды), которые не должны изменять размер при эффектах.
 * Высота глаз по умолчанию составляет 85% от высоты сущности.
 */
public record EntityDimensions(
	float width,
	float height,
	float eyeHeight,
	EntityAttachments attachments,
	boolean fixed
) {

	private static final float DEFAULT_EYE_HEIGHT_RATIO = 0.85F;

	private EntityDimensions(float width, float height, boolean fixed) {
		this(width, height, getDefaultEyeHeight(height), EntityAttachments.of(width, height), fixed);
	}

	private static float getDefaultEyeHeight(float height) {
		return height * DEFAULT_EYE_HEIGHT_RATIO;
	}

	public Box getBoxAt(Vec3d pos) {
		return getBoxAt(pos.x, pos.y, pos.z);
	}

	/**
	 * Строит AABB-хитбокс с центром по X/Z и основанием в точке Y.
	 *
	 * @param x центр по X
	 * @param y нижняя грань по Y
	 * @param z центр по Z
	 * @return ограничивающий прямоугольник сущности
	 */
	public Box getBoxAt(double x, double y, double z) {
		float halfWidth = width / 2.0F;
		return new Box(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth);
	}

	/**
	 * Масштабирует размеры равномерно по всем осям.
	 * Если {@code fixed == true} или коэффициент равен 1.0, возвращает {@code this}.
	 *
	 * @param ratio коэффициент масштабирования
	 * @return масштабированные размеры или {@code this}
	 */
	public EntityDimensions scaled(float ratio) {
		return scaled(ratio, ratio);
	}

	/**
	 * Масштабирует размеры раздельно по ширине и высоте.
	 * Если {@code fixed == true} или оба коэффициента равны 1.0, возвращает {@code this}.
	 *
	 * @param widthRatio  коэффициент масштабирования ширины
	 * @param heightRatio коэффициент масштабирования высоты
	 * @return масштабированные размеры или {@code this}
	 */
	public EntityDimensions scaled(float widthRatio, float heightRatio) {
		return !fixed && (widthRatio != 1.0F || heightRatio != 1.0F)
			? new EntityDimensions(
				width * widthRatio,
				height * heightRatio,
				eyeHeight * heightRatio,
				attachments.scale(widthRatio, heightRatio, widthRatio),
				false
			)
			: this;
	}

	/**
	 * Создаёт изменяемые размеры (масштабирование разрешено).
	 *
	 * @param width  ширина хитбокса
	 * @param height высота хитбокса
	 * @return новый экземпляр с {@code fixed = false}
	 */
	public static EntityDimensions changing(float width, float height) {
		return new EntityDimensions(width, height, false);
	}

	/**
	 * Создаёт фиксированные размеры (масштабирование запрещено).
	 *
	 * @param width  ширина хитбокса
	 * @param height высота хитбокса
	 * @return новый экземпляр с {@code fixed = true}
	 */
	public static EntityDimensions fixed(float width, float height) {
		return new EntityDimensions(width, height, true);
	}

	public EntityDimensions withEyeHeight(float eyeHeight) {
		return new EntityDimensions(width, height, eyeHeight, attachments, fixed);
	}

	public EntityDimensions withAttachments(EntityAttachments.Builder attachments) {
		return new EntityDimensions(width, height, eyeHeight, attachments.build(width, height), fixed);
	}
}
