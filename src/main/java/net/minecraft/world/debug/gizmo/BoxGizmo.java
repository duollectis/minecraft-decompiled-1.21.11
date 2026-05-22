package net.minecraft.world.debug.gizmo;

import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Отладочный примитив — параллелепипед (AABB).
 * <p>
 * Поддерживает заливку граней и/или обводку рёбер согласно {@link DrawStyle}.
 * При включённом {@code coloredCornerStroke} три ребра из начальной вершины
 * окрашиваются в цвета осей координат (X — красный, Y — зелёный, Z — синий),
 * смешанные с основным цветом обводки.
 */
public record BoxGizmo(Box aabb, DrawStyle style, boolean coloredCornerStroke) implements Gizmo {

	/**
	 * Цвет подмешивания для ребра вдоль оси X (красный канал).
	 * Значение {@code -34953} = {@code 0xFFFF7777} в ARGB.
	 */
	private static final int AXIS_X_TINT = -34953;

	/**
	 * Цвет подмешивания для ребра вдоль оси Y (зелёный канал).
	 * Значение {@code -8913033} = {@code 0xFF779977} в ARGB.
	 */
	private static final int AXIS_Y_TINT = -8913033;

	/**
	 * Цвет подмешивания для ребра вдоль оси Z (синий канал).
	 * Значение {@code -8947713} = {@code 0xFF7777FF} в ARGB.
	 */
	private static final int AXIS_Z_TINT = -8947713;

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		double minX = aabb.minX;
		double minY = aabb.minY;
		double minZ = aabb.minZ;
		double maxX = aabb.maxX;
		double maxY = aabb.maxY;
		double maxZ = aabb.maxZ;

		if (style.hasFill()) {
			int fillColor = style.fill(opacity);
			consumer.addQuad(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, maxY, maxZ), new Vec3d(maxX, minY, maxZ), fillColor);
			consumer.addQuad(new Vec3d(minX, minY, minZ), new Vec3d(minX, minY, maxZ), new Vec3d(minX, maxY, maxZ), new Vec3d(minX, maxY, minZ), fillColor);
			consumer.addQuad(new Vec3d(minX, minY, minZ), new Vec3d(minX, maxY, minZ), new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, minY, minZ), fillColor);
			consumer.addQuad(new Vec3d(minX, minY, maxZ), new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, maxY, maxZ), new Vec3d(minX, maxY, maxZ), fillColor);
			consumer.addQuad(new Vec3d(minX, maxY, minZ), new Vec3d(minX, maxY, maxZ), new Vec3d(maxX, maxY, maxZ), new Vec3d(maxX, maxY, minZ), fillColor);
			consumer.addQuad(new Vec3d(minX, minY, minZ), new Vec3d(maxX, minY, minZ), new Vec3d(maxX, minY, maxZ), new Vec3d(minX, minY, maxZ), fillColor);
		}

		if (style.hasStroke()) {
			int strokeColor = style.stroke(opacity);
			float strokeWidth = style.strokeWidth();

			// Три ребра из начальной вершины (minX, minY, minZ) — опционально окрашены по осям
			consumer.addLine(
					new Vec3d(minX, minY, minZ),
					new Vec3d(maxX, minY, minZ),
					coloredCornerStroke ? ColorHelper.mix(strokeColor, AXIS_X_TINT) : strokeColor,
					strokeWidth
			);
			consumer.addLine(
					new Vec3d(minX, minY, minZ),
					new Vec3d(minX, maxY, minZ),
					coloredCornerStroke ? ColorHelper.mix(strokeColor, AXIS_Y_TINT) : strokeColor,
					strokeWidth
			);
			consumer.addLine(
					new Vec3d(minX, minY, minZ),
					new Vec3d(minX, minY, maxZ),
					coloredCornerStroke ? ColorHelper.mix(strokeColor, AXIS_Z_TINT) : strokeColor,
					strokeWidth
			);

			// Остальные рёбра без цветового выделения
			consumer.addLine(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, maxY, minZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(maxX, maxY, minZ), new Vec3d(minX, maxY, minZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(minX, maxY, minZ), new Vec3d(minX, maxY, maxZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(minX, maxY, maxZ), new Vec3d(minX, minY, maxZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(minX, minY, maxZ), new Vec3d(maxX, minY, maxZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, minY, minZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(minX, maxY, maxZ), new Vec3d(maxX, maxY, maxZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, maxY, maxZ), strokeColor, strokeWidth);
			consumer.addLine(new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, maxY, maxZ), strokeColor, strokeWidth);
		}
	}
}
