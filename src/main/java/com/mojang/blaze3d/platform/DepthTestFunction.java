package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Функция сравнения глубины, применяемая при тесте глубины фрагмента.
 * Определяет, при каком соотношении глубины фрагмента и значения в буфере глубины
 * фрагмент проходит тест и записывается на экран.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum DepthTestFunction {
	/** Тест глубины отключён — все фрагменты проходят. */
	NO_DEPTH_TEST,
	/** Фрагмент проходит, если его глубина равна значению в буфере. */
	EQUAL_DEPTH_TEST,
	/** Фрагмент проходит, если его глубина меньше или равна значению в буфере (стандартный режим). */
	LEQUAL_DEPTH_TEST,
	/** Фрагмент проходит, если его глубина строго меньше значения в буфере. */
	LESS_DEPTH_TEST,
	/** Фрагмент проходит, если его глубина строго больше значения в буфере. */
	GREATER_DEPTH_TEST;
}
