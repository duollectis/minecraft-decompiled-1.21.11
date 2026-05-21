package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code GlyphProvider}.
 */
public interface GlyphProvider {

	BakedGlyph get(int codePoint);

	BakedGlyph getObfuscated(Random random, int width);
}
