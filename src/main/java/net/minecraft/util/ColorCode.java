package net.minecraft.util;

import com.mojang.serialization.Codec;
import net.minecraft.util.dynamic.Codecs;

import java.util.HexFormat;

/**
 * {@code ColorCode}.
 */
public record ColorCode(int rgba) {

	public static final Codec<ColorCode> CODEC = Codecs.HEX_ARGB.xmap(ColorCode::new, ColorCode::rgba);

	@Override
	public String toString() {
		return HexFormat.of().toHexDigits(this.rgba, 8);
	}
}
