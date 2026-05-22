package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Фактор назначения (destination factor) для операции смешивания цветов (blending).
 * Определяет, на что умножается цвет уже существующего пикселя в буфере кадра
 * перед сложением с цветом нового фрагмента.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum DestFactor {
	CONSTANT_ALPHA,
	CONSTANT_COLOR,
	DST_ALPHA,
	DST_COLOR,
	ONE,
	ONE_MINUS_CONSTANT_ALPHA,
	ONE_MINUS_CONSTANT_COLOR,
	ONE_MINUS_DST_ALPHA,
	ONE_MINUS_DST_COLOR,
	ONE_MINUS_SRC_ALPHA,
	ONE_MINUS_SRC_COLOR,
	SRC_ALPHA,
	SRC_COLOR,
	ZERO;
}
