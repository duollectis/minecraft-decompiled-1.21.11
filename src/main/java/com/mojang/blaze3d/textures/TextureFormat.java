package com.mojang.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Формат пикселей GPU-текстуры.
 * Определяет количество каналов, их тип и размер одного пикселя в байтах.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum TextureFormat {
	/** 4 байта на пиксель: красный, зелёный, синий, альфа (по 8 бит каждый). */
	RGBA8(4),
	/** 1 байт на пиксель: один красный канал (8 бит без знака). */
	RED8(1),
	/** 1 байт на пиксель: один красный канал (8 бит целое). */
	RED8I(1),
	/** 4 байта на пиксель: буфер глубины (32-битный float). */
	DEPTH32(4);

	private final int pixelSize;

	TextureFormat(int pixelSize) {
		this.pixelSize = pixelSize;
	}

	/** Возвращает размер одного пикселя в байтах. */
	public int pixelSize() {
		return pixelSize;
	}

	/** Возвращает {@code true}, если формат содержит цветовые каналы (не глубина). */
	public boolean hasColorAspect() {
		return this == RGBA8 || this == RED8;
	}

	/** Возвращает {@code true}, если формат является буфером глубины. */
	public boolean hasDepthAspect() {
		return this == DEPTH32;
	}
}
