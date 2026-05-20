package net.minecraft.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.dialog.body.DialogBody;
import net.minecraft.dialog.type.DialogInput;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public record DialogCommonData(
   Text title,
   Optional<Text> externalTitle,
   boolean canCloseWithEscape,
   boolean pause,
   AfterAction afterAction,
   List<DialogBody> body,
   List<DialogInput> inputs
) {
   public static final MapCodec<DialogCommonData> CODEC = RecordCodecBuilder.<DialogCommonData>mapCodec(
         instance -> instance.group(
               TextCodecs.CODEC.fieldOf("title").forGetter(DialogCommonData::title),
               TextCodecs.CODEC.optionalFieldOf("external_title").forGetter(DialogCommonData::externalTitle),
               Codec.BOOL.optionalFieldOf("can_close_with_escape", true).forGetter(DialogCommonData::canCloseWithEscape),
               Codec.BOOL.optionalFieldOf("pause", true).forGetter(DialogCommonData::pause),
               AfterAction.CODEC.optionalFieldOf("after_action", AfterAction.CLOSE).forGetter(DialogCommonData::afterAction),
               DialogBody.LIST_CODEC.optionalFieldOf("body", List.of()).forGetter(DialogCommonData::body),
               DialogInput.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(DialogCommonData::inputs)
            )
            .apply(instance, DialogCommonData::new)
      )
      .validate(
         data -> data.pause && !data.afterAction.canUnpause()
            ? DataResult.error(() -> "Dialogs that pause the game must use after_action values that unpause it after user action!")
            : DataResult.success(data)
      );

   public Text getExternalTitle() {
      return this.externalTitle.orElse(this.title);
   }
}
