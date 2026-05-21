package net.minecraft.entity;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * {@code EntityDimensions}.
 */
public record EntityDimensions(
		float width,
		float height,
		float eyeHeight,
		EntityAttachments attachments,
		boolean fixed
) {

	private EntityDimensions(float width, float height, boolean fixed) {
		this(width, height, getDefaultEyeHeight(height), EntityAttachments.of(width, height), fixed);
	}

	private static float getDefaultEyeHeight(float height) {
		return height * 0.85F;
	}

	public Box getBoxAt(Vec3d pos) {
		return this.getBoxAt(pos.x, pos.y, pos.z);
	}

	public Box getBoxAt(double x, double y, double z) {
		float f = this.width / 2.0F;
		float g = this.height;
		return new Box(x - f, y, z - f, x + f, y + g, z + f);
	}

	/**
	 * Scaled.
	 *
	 * @param ratio ratio
	 *
	 * @return EntityDimensions — результат операции
	 */
	public EntityDimensions scaled(float ratio) {
		return this.scaled(ratio, ratio);
	}

	/**
	 * Scaled.
	 *
	 * @param widthRatio width ratio
	 * @param heightRatio height ratio
	 *
	 * @return EntityDimensions — результат операции
	 */
	public EntityDimensions scaled(float widthRatio, float heightRatio) {
		return !this.fixed && (widthRatio != 1.0F || heightRatio != 1.0F)
		       ? new EntityDimensions(
				this.width * widthRatio,
				this.height * heightRatio,
				this.eyeHeight * heightRatio,
				this.attachments.scale(widthRatio, heightRatio, widthRatio),
				false
		)
		       : this;
	}

	/**
	 * Changing.
	 *
	 * @param width width
	 * @param height height
	 *
	 * @return EntityDimensions — результат операции
	 */
	public static EntityDimensions changing(float width, float height) {
		return new EntityDimensions(width, height, false);
	}

	/**
	 * Fixed.
	 *
	 * @param width width
	 * @param height height
	 *
	 * @return EntityDimensions — результат операции
	 */
	public static EntityDimensions fixed(float width, float height) {
		return new EntityDimensions(width, height, true);
	}

	/**
	 * With eye height.
	 *
	 * @param eyeHeight eye height
	 *
	 * @return EntityDimensions — результат операции
	 */
	public EntityDimensions withEyeHeight(float eyeHeight) {
		return new EntityDimensions(this.width, this.height, eyeHeight, this.attachments, this.fixed);
	}

	/**
	 * With attachments.
	 *
	 * @param attachments attachments
	 *
	 * @return EntityDimensions — результат операции
	 */
	public EntityDimensions withAttachments(EntityAttachments.Builder attachments) {
		return new EntityDimensions(
				this.width,
				this.height,
				this.eyeHeight,
				attachments.build(this.width, this.height),
				this.fixed
		);
	}
}
