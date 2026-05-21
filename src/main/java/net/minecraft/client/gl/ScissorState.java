package net.minecraft.client.gl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code ScissorState}.
 */
public class ScissorState {

	private boolean enabled;
	private int x;
	private int y;
	private int width;
	private int height;

	/**
	 * Enable.
	 *
	 * @param x x
	 * @param y y
	 * @param width width
	 * @param height height
	 */
	public void enable(int x, int y, int width, int height) {
		this.enabled = true;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	/**
	 * Disable.
	 */
	public void disable() {
		this.enabled = false;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}
}
