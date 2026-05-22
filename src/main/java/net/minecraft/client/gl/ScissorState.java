package net.minecraft.client.gl;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Состояние scissor-теста: хранит прямоугольник отсечения и флаг активности.
 */
@Environment(EnvType.CLIENT)
public class ScissorState {

	private boolean enabled;
	private int x;
	private int y;
	private int width;
	private int height;

	public void enable(int x, int y, int width, int height) {
		enabled = true;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void disable() {
		enabled = false;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
