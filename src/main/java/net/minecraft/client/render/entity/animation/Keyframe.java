package net.minecraft.client.render.entity.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
/**
 * {@code Keyframe}.
 */
public record Keyframe(
		float timestamp,
		Vector3fc preTarget,
		Vector3fc postTarget,
		Transformation.Interpolation interpolation
) {

	public Keyframe(float f, Vector3fc vector3fc, Transformation.Interpolation interpolation) {
		this(f, vector3fc, vector3fc, interpolation);
	}
}
