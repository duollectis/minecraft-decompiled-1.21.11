package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelTransform}.
 */
public record ModelTransform(
		float x,
		float y,
		float z,
		float pitch,
		float yaw,
		float roll,
		float xScale,
		float yScale,
		float zScale
) {

	public static final ModelTransform NONE = of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

	/**
	 * Origin.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return ModelTransform — результат операции
	 */
	public static ModelTransform origin(float x, float y, float z) {
		return of(x, y, z, 0.0F, 0.0F, 0.0F);
	}

	/**
	 * Rotation.
	 *
	 * @param pitch pitch
	 * @param yaw yaw
	 * @param roll roll
	 *
	 * @return ModelTransform — результат операции
	 */
	public static ModelTransform rotation(float pitch, float yaw, float roll) {
		return of(0.0F, 0.0F, 0.0F, pitch, yaw, roll);
	}

	/**
	 * Of.
	 *
	 * @param originX origin x
	 * @param originY origin y
	 * @param originZ origin z
	 * @param pitch pitch
	 * @param yaw yaw
	 * @param roll roll
	 *
	 * @return ModelTransform — результат операции
	 */
	public static ModelTransform of(float originX, float originY, float originZ, float pitch, float yaw, float roll) {
		return new ModelTransform(originX, originY, originZ, pitch, yaw, roll, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * Перемещает origin.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return ModelTransform — результат операции
	 */
	public ModelTransform moveOrigin(float x, float y, float z) {
		return new ModelTransform(
				this.x + x,
				this.y + y,
				this.z + z,
				this.pitch,
				this.yaw,
				this.roll,
				this.xScale,
				this.yScale,
				this.zScale
		);
	}

	/**
	 * With scale.
	 *
	 * @param scale scale
	 *
	 * @return ModelTransform — результат операции
	 */
	public ModelTransform withScale(float scale) {
		return new ModelTransform(this.x, this.y, this.z, this.pitch, this.yaw, this.roll, scale, scale, scale);
	}

	/**
	 * Scaled.
	 *
	 * @param scale scale
	 *
	 * @return ModelTransform — результат операции
	 */
	public ModelTransform scaled(float scale) {
		return scale == 1.0F ? this : this.scaled(scale, scale, scale);
	}

	/**
	 * Scaled.
	 *
	 * @param xScale x scale
	 * @param yScale y scale
	 * @param zScale z scale
	 *
	 * @return ModelTransform — результат операции
	 */
	public ModelTransform scaled(float xScale, float yScale, float zScale) {
		return new ModelTransform(
				this.x * xScale,
				this.y * yScale,
				this.z * zScale,
				this.pitch,
				this.yaw,
				this.roll,
				this.xScale * xScale,
				this.yScale * yScale,
				this.zScale * zScale
		);
	}
}
