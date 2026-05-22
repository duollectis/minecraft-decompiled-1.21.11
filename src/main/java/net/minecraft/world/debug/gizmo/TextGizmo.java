package net.minecraft.world.debug.gizmo;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;

import java.util.OptionalDouble;

/**
 * Отладочный примитив — текстовая метка в мировом пространстве.
 * <p>
 * При opacity < 1.0 создаётся новый стиль с масштабированным альфа-каналом,
 * чтобы обеспечить корректное затухание текста.
 */
public record TextGizmo(Vec3d pos, String text, TextGizmo.Style style) implements Gizmo {

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		Style drawStyle = opacity < 1.0F
				? new Style(ColorHelper.scaleAlpha(style.color, opacity), style.scale, style.adjustLeft)
				: style;

		consumer.addText(pos, text, drawStyle);
	}

	/**
	 * Стиль отображения текстовой метки.
	 * <p>
	 * {@code adjustLeft} задаёт горизонтальное смещение текста:
	 * {@link OptionalDouble#empty()} — выравнивание по левому краю,
	 * {@code 0.0} — центрирование, положительное значение — сдвиг вправо.
	 */
	public record Style(int color, float scale, OptionalDouble adjustLeft) {

		/** Масштаб текста по умолчанию. */
		public static final float DEFAULT_SCALE = 0.32F;

		public static Style left() {
			return new Style(-1, DEFAULT_SCALE, OptionalDouble.empty());
		}

		public static Style left(int color) {
			return new Style(color, DEFAULT_SCALE, OptionalDouble.empty());
		}

		public static Style centered(int color) {
			return new Style(color, DEFAULT_SCALE, OptionalDouble.of(0.0));
		}

		public Style scaled(float scale) {
			return new Style(color, scale, adjustLeft);
		}

		public Style adjusted(float adjustLeftValue) {
			return new Style(color, scale, OptionalDouble.of(adjustLeftValue));
		}
	}
}
