package net.minecraft.world.debug.gizmo;

import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Vec3d;

/**
 * Отладочный примитив — горизонтальная окружность, аппроксимированная многоугольником.
 * <p>
 * Окружность лежит в плоскости XZ (Y = константа) и состоит из {@value #SEGMENT_COUNT}
 * равномерно распределённых вершин. Поддерживает заливку и/или обводку через {@link DrawStyle}.
 */
public record CircleGizmo(Vec3d pos, float radius, DrawStyle style) implements Gizmo {

	/** Количество сегментов аппроксимации окружности. */
	private static final int SEGMENT_COUNT = 20;

	/** Угловой шаг между соседними вершинами (2π / SEGMENT_COUNT). */
	private static final float ANGLE_STEP = (float) (Math.PI / 10);

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		if (!style.hasStroke() && !style.hasFill()) {
			return;
		}

		// Массив из SEGMENT_COUNT + 1 вершин: последняя совпадает с первой для замыкания контура
		Vec3d[] vertices = new Vec3d[SEGMENT_COUNT + 1];

		for (int segment = 0; segment < SEGMENT_COUNT; segment++) {
			float angle = segment * ANGLE_STEP;
			vertices[segment] = pos.add(
					(float) (radius * Math.cos(angle)),
					0.0,
					(float) (radius * Math.sin(angle))
			);
		}

		vertices[SEGMENT_COUNT] = vertices[0];

		if (style.hasFill()) {
			consumer.addPolygon(vertices, style.fill(opacity));
		}

		if (style.hasStroke()) {
			int strokeColor = style.stroke(opacity);
			for (int segment = 0; segment < SEGMENT_COUNT; segment++) {
				consumer.addLine(vertices[segment], vertices[segment + 1], strokeColor, style.strokeWidth());
			}
		}
	}
}
