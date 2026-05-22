package net.minecraft.world.border;

/**
 * Стадия изменения границы мира, определяющая направление движения и цвет отображения.
 * <p>
 * Цвета хранятся в формате RGB как целое число и используются клиентом
 * для визуальной индикации состояния границы.
 */
public enum WorldBorderStage {

	GROWING(0x40FF80),
	SHRINKING(0xFF3030),
	STATIONARY(0x20A0FF);

	private final int color;

	WorldBorderStage(int color) {
		this.color = color;
	}

	public int getColor() {
		return color;
	}
}
