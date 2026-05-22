package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Режим растеризации полигонов.
 * Определяет, как треугольники отрисовываются на экране — закрашенными или в виде сетки.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum PolygonMode {
	/** Полигоны заполняются цветом (стандартный режим). */
	FILL,
	/** Полигоны отрисовываются только рёбрами (режим каркаса). */
	WIREFRAME;
}
