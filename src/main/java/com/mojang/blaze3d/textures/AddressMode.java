package com.mojang.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Режим адресации текстуры при выходе UV-координат за пределы диапазона [0, 1].
 * Определяет, как сэмплер обрабатывает координаты вне границ текстуры.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum AddressMode {
	/** Текстура повторяется тайлингом. */
	REPEAT,
	/** Координата фиксируется на ближайшем краю текстуры. */
	CLAMP_TO_EDGE;
}
