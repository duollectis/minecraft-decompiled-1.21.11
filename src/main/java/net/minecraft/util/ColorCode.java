package net.minecraft.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.dynamic.Codecs;

import java.util.HexFormat;

/**
 * Цветовой код в формате RGBA, упакованный в 32-битное целое число.
 * Используется для передачи цветов через кодеки и сетевые пакеты.
 *
 * @param rgba цвет в формате RGBA (8 бит на канал)
 */
public record ColorCode(int rgba) {

	public static final Codec<ColorCode> CODEC = Codecs.HEX_ARGB.xmap(ColorCode::new, ColorCode::rgba);

	@Override
	public String toString() {
		return HexFormat.of().toHexDigits(rgba, 8);
	}
}
