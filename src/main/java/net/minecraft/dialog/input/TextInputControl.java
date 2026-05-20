package net.minecraft.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;

public record TextInputControl(int width, Text label, boolean labelVisible, String initial, int maxLength, Optional<TextInputControl.Multiline> multiline)
   implements InputControl {
   public static final MapCodec<TextInputControl> CODEC = RecordCodecBuilder.<TextInputControl>mapCodec(
         instance -> instance.group(
               Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(TextInputControl::width),
               TextCodecs.CODEC.fieldOf("label").forGetter(TextInputControl::label),
               Codec.BOOL.optionalFieldOf("label_visible", true).forGetter(TextInputControl::labelVisible),
               Codec.STRING.optionalFieldOf("initial", "").forGetter(TextInputControl::initial),
               Codecs.POSITIVE_INT.optionalFieldOf("max_length", 32).forGetter(TextInputControl::maxLength),
               TextInputControl.Multiline.CODEC.optionalFieldOf("multiline").forGetter(TextInputControl::multiline)
            )
            .apply(instance, TextInputControl::new)
      )
      .validate(
         inputControl -> inputControl.initial.length() > inputControl.maxLength()
            ? DataResult.error(() -> "Default text length exceeds allowed size")
            : DataResult.success(inputControl)
      );

   @Override
   public MapCodec<TextInputControl> getCodec() {
      return CODEC;
   }

   public record Multiline(Optional<Integer> maxLines, Optional<Integer> height) {
      public static final int MAX_HEIGHT = 512;
      public static final Codec<TextInputControl.Multiline> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codecs.POSITIVE_INT.optionalFieldOf("max_lines").forGetter(TextInputControl.Multiline::maxLines),
               Codecs.rangedInt(1, 512).optionalFieldOf("height").forGetter(TextInputControl.Multiline::height)
            )
            .apply(instance, TextInputControl.Multiline::new)
      );
   }
}
