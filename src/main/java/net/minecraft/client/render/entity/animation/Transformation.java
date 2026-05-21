package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
/**
 * {@code Transformation}.
 */
public record Transformation(Transformation.Target target, Keyframe... keyframes) {

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Interpolation}.
	 */
	public interface Interpolation {

		Vector3f apply(Vector3f dest, float delta, Keyframe[] keyframes, int start, int end, float scale);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Interpolations}.
	 */
	public static class Interpolations {

		public static final Transformation.Interpolation LINEAR = (dest, delta, keyframes, start, end, scale) -> {
			Vector3fc vector3fc = keyframes[start].postTarget();
			Vector3fc vector3fc2 = keyframes[end].preTarget();
			return vector3fc.lerp(vector3fc2, delta, dest).mul(scale);
		};
		public static final Transformation.Interpolation CUBIC = (dest, delta, keyframes, start, end, scale) -> {
			Vector3fc vector3fc = keyframes[Math.max(0, start - 1)].postTarget();
			Vector3fc vector3fc2 = keyframes[start].postTarget();
			Vector3fc vector3fc3 = keyframes[end].postTarget();
			Vector3fc vector3fc4 = keyframes[Math.min(keyframes.length - 1, end + 1)].postTarget();
			dest.set(
					MathHelper.catmullRom(delta, vector3fc.x(), vector3fc2.x(), vector3fc3.x(), vector3fc4.x()) * scale,
					MathHelper.catmullRom(delta, vector3fc.y(), vector3fc2.y(), vector3fc3.y(), vector3fc4.y()) * scale,
					MathHelper.catmullRom(delta, vector3fc.z(), vector3fc2.z(), vector3fc3.z(), vector3fc4.z()) * scale
			);
			return dest;
		};
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Target}.
	 */
	public interface Target {

		void apply(ModelPart modelPart, Vector3f vec);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Targets}.
	 */
	public static class Targets {

		public static final Transformation.Target MOVE_ORIGIN = ModelPart::moveOrigin;
		public static final Transformation.Target ROTATE = ModelPart::rotate;
		public static final Transformation.Target SCALE = ModelPart::scale;
	}
}
