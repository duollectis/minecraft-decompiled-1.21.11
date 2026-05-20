package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface EffectGlyph {
   TextDrawable create(float minX, float minY, float maxX, float maxY, float depth, int color, int shadowColor, float shadowOffset);
}
