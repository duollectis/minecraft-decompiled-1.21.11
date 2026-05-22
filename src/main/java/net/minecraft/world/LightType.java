package net.minecraft.world;

/**
 * Тип источника освещения в мире.
 * {@link #SKY} — солнечный свет, зависит от времени суток и погоды.
 * {@link #BLOCK} — свет от блоков (факелы, лава и т.д.), не зависит от времени.
 */
public enum LightType {
	SKY,
	BLOCK;
}
