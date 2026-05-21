package net.minecraft.world.debug.gizmo;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;

/**
 * {@code LineGizmo}.
 */
public record LineGizmo(Vec3d start, Vec3d end, int color, float width) implements Gizmo {

	public static final float LINE_WIDTH = 3.0F;

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		consumer.addLine(this.start, this.end, ColorHelper.scaleAlpha(this.color, opacity), this.width);
	}
}
