package net.minecraft.world.debug.gizmo;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Отладочный примитив — отрезок между двумя точками.
 */
public record LineGizmo(Vec3d start, Vec3d end, int color, float width) implements Gizmo {

	/** Толщина линии по умолчанию в пикселях. */
	public static final float LINE_WIDTH = 3.0F;

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		consumer.addLine(start, end, ColorHelper.scaleAlpha(color, opacity), width);
	}
}
