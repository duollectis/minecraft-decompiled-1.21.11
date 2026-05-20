package net.minecraft.client.font;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;

@Environment(EnvType.CLIENT)
public interface GlyphRect {
   Style style();

   float getLeft();

   float getTop();

   float getRight();

   float getBottom();
}
