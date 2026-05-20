package net.minecraft.text.object;

import com.mojang.serialization.MapCodec;
import net.minecraft.text.StyleSpriteSource;

public interface TextObjectContents {
   StyleSpriteSource spriteSource();

   String asText();

   MapCodec<? extends TextObjectContents> getCodec();
}
