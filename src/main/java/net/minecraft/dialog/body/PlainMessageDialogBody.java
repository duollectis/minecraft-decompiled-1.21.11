package net.minecraft.dialog.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public record PlainMessageDialogBody(Text contents, int width) implements DialogBody {
   public static final int DEFAULT_WIDTH = 200;
   public static final MapCodec<PlainMessageDialogBody> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            TextCodecs.CODEC.fieldOf("contents").forGetter(PlainMessageDialogBody::contents),
            Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(PlainMessageDialogBody::width)
         )
         .apply(instance, PlainMessageDialogBody::new)
   );
   public static final Codec<PlainMessageDialogBody> ALTERNATIVE_CODEC = Codec.withAlternative(
      CODEC.codec(), TextCodecs.CODEC, contents -> new PlainMessageDialogBody(contents, 200)
   );

   @Override
   public MapCodec<PlainMessageDialogBody> getTypeCodec() {
      return CODEC;
   }
}
