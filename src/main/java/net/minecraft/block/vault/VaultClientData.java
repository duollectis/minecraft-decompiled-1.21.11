package net.minecraft.block.vault;

import net.minecraft.util.math.MathHelper;

/**
 * {@code VaultClientData}.
 */
public class VaultClientData {

	public static final float DISPLAY_ROTATION_SPEED = 10.0F;
	private float displayRotation;
	private float lastDisplayRotation;

	public VaultClientData() {
	}

	public float getDisplayRotation() {
		return this.displayRotation;
	}

	public float getLastDisplayRotation() {
		return this.lastDisplayRotation;
	}

	/**
	 * Rotate display.
	 */
	public void rotateDisplay() {
		this.lastDisplayRotation = this.displayRotation;
		this.displayRotation = MathHelper.wrapDegrees(this.displayRotation + 10.0F);
	}
}
