package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BakedGlyph}.
 */
public interface BakedGlyph {

	GlyphMetrics getMetrics();

	TextDrawable.@Nullable DrawnGlyphRect create(
			float x,
			float y,
			int color,
			int shadowColor,
			Style style,
			float boldOffset,
			float shadowOffset
	);
}
