package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Трансформация одной кости модели: набор ключевых кадров и цель применения.
 * <p>
 * Связывает массив {@link Keyframe} с конкретным {@link Target} (перемещение,
 * вращение или масштаб). Используется в {@link AnimationDefinition} для описания
 * анимации отдельной кости по имени.
 */
@Environment(EnvType.CLIENT)
public record Transformation(Transformation.Target target, Keyframe... keyframes) {

	/**
	 * Стратегия интерполяции между двумя ключевыми кадрами.
	 * <p>
	 * Принимает массив кадров и индексы начального/конечного кадра,
	 * вычисляет промежуточное значение и записывает его в {@code dest}.
	 */
	@Environment(EnvType.CLIENT)
	public interface Interpolation {

		Vector3f apply(Vector3f dest, float delta, Keyframe[] keyframes, int start, int end, float scale);
	}

	/** Стандартные реализации интерполяции. */
	@Environment(EnvType.CLIENT)
	public static class Interpolations {

		/** Линейная интерполяция между {@code postTarget} начального и {@code preTarget} конечного кадра. */
		public static final Transformation.Interpolation LINEAR = (dest, delta, keyframes, start, end, scale) -> {
			Vector3fc from = keyframes[start].postTarget();
			Vector3fc to = keyframes[end].preTarget();
			return from.lerp(to, delta, dest).mul(scale);
		};

		/**
		 * Кубическая интерполяция Катмулла–Рома по четырём соседним кадрам.
		 * Обеспечивает плавные переходы без резких изломов на стыках кадров.
		 */
		public static final Transformation.Interpolation CUBIC = (dest, delta, keyframes, start, end, scale) -> {
			Vector3fc p0 = keyframes[Math.max(0, start - 1)].postTarget();
			Vector3fc p1 = keyframes[start].postTarget();
			Vector3fc p2 = keyframes[end].postTarget();
			Vector3fc p3 = keyframes[Math.min(keyframes.length - 1, end + 1)].postTarget();
			dest.set(
					MathHelper.catmullRom(delta, p0.x(), p1.x(), p2.x(), p3.x()) * scale,
					MathHelper.catmullRom(delta, p0.y(), p1.y(), p2.y(), p3.y()) * scale,
					MathHelper.catmullRom(delta, p0.z(), p1.z(), p2.z(), p3.z()) * scale
			);
			return dest;
		};
	}

	/** Цель применения вычисленного вектора трансформации к части модели. */
	@Environment(EnvType.CLIENT)
	public interface Target {

		void apply(ModelPart modelPart, Vector3f vec);
	}

	/** Стандартные цели трансформации, делегирующие к методам {@link ModelPart}. */
	@Environment(EnvType.CLIENT)
	public static class Targets {

		public static final Transformation.Target MOVE_ORIGIN = ModelPart::moveOrigin;
		public static final Transformation.Target ROTATE = ModelPart::rotate;
		public static final Transformation.Target SCALE = ModelPart::scale;
	}
}
