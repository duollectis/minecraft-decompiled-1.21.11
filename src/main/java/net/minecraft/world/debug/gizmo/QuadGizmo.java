package net.minecraft.world.debug.gizmo;

import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Отладочный примитив — четырёхугольник (quad) из четырёх вершин.
 * <p>
 * Поддерживает заливку и/или обводку через {@link DrawStyle}.
 * Фабричный метод {@link #ofFace} позволяет создать quad для конкретной грани AABB.
 */
public record QuadGizmo(Vec3d a, Vec3d b, Vec3d c, Vec3d d, DrawStyle style) implements Gizmo {

	/**
	 * Создаёт quad для заданной грани AABB, ограниченного точками {@code nwd} и {@code seu}.
	 * <p>
	 * Параметры {@code nwd} (north-west-down) и {@code seu} (south-east-up) задают
	 * минимальный и максимальный углы параллелепипеда соответственно.
	 *
	 * @param nwd       минимальный угол AABB (north-west-down)
	 * @param seu       максимальный угол AABB (south-east-up)
	 * @param direction грань, для которой создаётся quad
	 * @param style     стиль отрисовки
	 * @return quad, соответствующий указанной грани
	 */
	public static QuadGizmo ofFace(Vec3d nwd, Vec3d seu, Direction direction, DrawStyle style) {
		return switch (direction) {
			case DOWN -> new QuadGizmo(
					new Vec3d(nwd.x, nwd.y, nwd.z),
					new Vec3d(seu.x, nwd.y, nwd.z),
					new Vec3d(seu.x, nwd.y, seu.z),
					new Vec3d(nwd.x, nwd.y, seu.z),
					style
			);
			case UP -> new QuadGizmo(
					new Vec3d(nwd.x, seu.y, nwd.z),
					new Vec3d(nwd.x, seu.y, seu.z),
					new Vec3d(seu.x, seu.y, seu.z),
					new Vec3d(seu.x, seu.y, nwd.z),
					style
			);
			case NORTH -> new QuadGizmo(
					new Vec3d(nwd.x, nwd.y, nwd.z),
					new Vec3d(nwd.x, seu.y, nwd.z),
					new Vec3d(seu.x, seu.y, nwd.z),
					new Vec3d(seu.x, nwd.y, nwd.z),
					style
			);
			case SOUTH -> new QuadGizmo(
					new Vec3d(nwd.x, nwd.y, seu.z),
					new Vec3d(seu.x, nwd.y, seu.z),
					new Vec3d(seu.x, seu.y, seu.z),
					new Vec3d(nwd.x, seu.y, seu.z),
					style
			);
			case WEST -> new QuadGizmo(
					new Vec3d(nwd.x, nwd.y, nwd.z),
					new Vec3d(nwd.x, nwd.y, seu.z),
					new Vec3d(nwd.x, seu.y, seu.z),
					new Vec3d(nwd.x, seu.y, nwd.z),
					style
			);
			case EAST -> new QuadGizmo(
					new Vec3d(seu.x, nwd.y, nwd.z),
					new Vec3d(seu.x, seu.y, nwd.z),
					new Vec3d(seu.x, seu.y, seu.z),
					new Vec3d(seu.x, nwd.y, seu.z),
					style
			);
		};
	}

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		if (style.hasFill()) {
			consumer.addQuad(a, b, c, d, style.fill(opacity));
		}

		if (style.hasStroke()) {
			int strokeColor = style.stroke(opacity);
			float strokeWidth = style.strokeWidth();
			consumer.addLine(a, b, strokeColor, strokeWidth);
			consumer.addLine(b, c, strokeColor, strokeWidth);
			consumer.addLine(c, d, strokeColor, strokeWidth);
			consumer.addLine(d, a, strokeColor, strokeWidth);
		}
	}
}
