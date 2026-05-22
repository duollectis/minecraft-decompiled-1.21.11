package com.mojang.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Режим фильтрации текстуры при масштабировании.
 * Определяет алгоритм интерполяции пикселей при увеличении или уменьшении текстуры.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum FilterMode {
	/** Ближайший сосед — пиксельная фильтрация без сглаживания. */
	NEAREST,
	/** Билинейная фильтрация — плавное сглаживание между пикселями. */
	LINEAR;
}
