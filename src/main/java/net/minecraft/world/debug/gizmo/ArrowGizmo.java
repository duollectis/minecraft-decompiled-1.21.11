package net.minecraft.world.debug.gizmo;

import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Отладочный примитив — стрелка от точки {@code start} до точки {@code end}.
 * <p>
 * Стрелка состоит из основного отрезка и четырёх «перьев» наконечника,
 * ориентированных по направлению вектора. Размер наконечника масштабируется
 * пропорционально длине стрелки, но ограничен диапазоном [{@code 0.1}, {@code 1.0}].
 */
public record ArrowGizmo(Vec3d start, Vec3d end, int color, float width) implements Gizmo {

	/** Размер наконечника стрелки по умолчанию. */
	public static final float ARROW_SIZE = 2.5F;

	/** Коэффициент масштабирования наконечника относительно длины стрелки. */
	private static final float HEAD_SCALE_FACTOR = 0.1F;

	/** Минимальный размер наконечника. */
	private static final float HEAD_MIN_SIZE = 0.1F;

	/** Максимальный размер наконечника. */
	private static final float HEAD_MAX_SIZE = 1.0F;

	@Override
	public void draw(GizmoDrawer consumer, float opacity) {
		int scaledColor = ColorHelper.scaleAlpha(color, opacity);
		consumer.addLine(start, end, scaledColor, width);

		Quaternionf rotation = new Quaternionf().rotationTo(
				new Vector3f(1.0F, 0.0F, 0.0F),
				end.subtract(start).toVector3f().normalize()
		);

		float headSize = (float) MathHelper.clamp(end.distanceTo(start) * HEAD_SCALE_FACTOR, HEAD_MIN_SIZE, HEAD_MAX_SIZE);

		Vector3f[] feathers = {
				rotation.transform(-headSize, headSize, 0.0F, new Vector3f()),
				rotation.transform(-headSize, 0.0F, headSize, new Vector3f()),
				rotation.transform(-headSize, -headSize, 0.0F, new Vector3f()),
				rotation.transform(-headSize, 0.0F, -headSize, new Vector3f())
		};

		for (Vector3f feather : feathers) {
			consumer.addLine(end.add(feather.x, feather.y, feather.z), end, scaledColor, width);
		}
	}
}
