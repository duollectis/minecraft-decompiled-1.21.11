package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Фактор источника (source factor) для операции смешивания цветов (blending).
 * Определяет, на что умножается цвет нового фрагмента перед сложением
 * с цветом уже существующего пикселя в буфере кадра.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum SourceFactor {
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
	/** Насыщение альфа-канала источника: {@code min(src.a, 1 - dst.a)}. */
	SRC_ALPHA_SATURATE,
	SRC_COLOR,
	ZERO;
}
