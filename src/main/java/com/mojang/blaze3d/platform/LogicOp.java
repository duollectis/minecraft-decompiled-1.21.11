package com.mojang.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

/**
 * Логическая операция над цветом пикселя, применяемая вместо стандартного смешивания.
 * Используется для специальных эффектов, например инверсии цвета.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public enum LogicOp {
	/** Логическая операция отключена. */
	NONE,
	/** Операция OR с инверсией источника: {@code ~src | dst}. */
	OR_REVERSE;
}
