package net.minecraft.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.dialog.action.DialogAction;

public record DialogActionButtonData(DialogButtonData data, Optional<DialogAction> action) {
   public static final Codec<DialogActionButtonData> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            DialogButtonData.CODEC.forGetter(DialogActionButtonData::data),
            DialogAction.CODEC.optionalFieldOf("action").forGetter(DialogActionButtonData::action)
         )
         .apply(instance, DialogActionButtonData::new)
   );
}
