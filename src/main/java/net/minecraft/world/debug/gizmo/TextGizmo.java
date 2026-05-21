package net.minecraft.world.debug.gizmo;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;

import java.util.OptionalDouble;

/**
 * {@code TextGizmo}.
 */
public record TextGizmo(Vec3d pos, String text, TextGizmo.Style style) implements Gizmo {

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		TextGizmo.Style style;
		if (opacity < 1.0F) {
			style =
					new TextGizmo.Style(
							ColorHelper.scaleAlpha(this.style.color, opacity),
							this.style.scale,
							this.style.adjustLeft
					);
		}
		else {
			style = this.style;
		}

		consumer.addText(this.pos, this.text, style);
	}

	/**
	 * {@code Style}.
	 */
	public record Style(int color, float scale, OptionalDouble adjustLeft) {

		public static final float DEFAULT_SCALE = 0.32F;

		public static TextGizmo.Style left() {
			return new TextGizmo.Style(-1, 0.32F, OptionalDouble.empty());
		}

		public static TextGizmo.Style left(int color) {
			return new TextGizmo.Style(color, 0.32F, OptionalDouble.empty());
		}

		public static TextGizmo.Style centered(int color) {
			return new TextGizmo.Style(color, 0.32F, OptionalDouble.of(0.0));
		}

		public TextGizmo.Style scaled(float scale) {
			return new TextGizmo.Style(this.color, scale, this.adjustLeft);
		}

		public TextGizmo.Style adjusted(float adjustLeft) {
			return new TextGizmo.Style(this.color, this.scale, OptionalDouble.of(adjustLeft));
		}
	}
}
