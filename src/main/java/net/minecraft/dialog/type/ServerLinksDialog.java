package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.dialog.DialogActionButtonData;
import net.minecraft.dialog.DialogCommonData;
import net.minecraft.util.dynamic.Codecs;

public record ServerLinksDialog(DialogCommonData common, Optional<DialogActionButtonData> exitAction, int columns, int buttonWidth) implements ColumnsDialog {
   public static final MapCodec<ServerLinksDialog> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            DialogCommonData.CODEC.forGetter(ServerLinksDialog::common),
            DialogActionButtonData.CODEC.optionalFieldOf("exit_action").forGetter(ServerLinksDialog::exitAction),
            Codecs.POSITIVE_INT.optionalFieldOf("columns", 2).forGetter(ServerLinksDialog::columns),
            WIDTH_CODEC.optionalFieldOf("button_width", 150).forGetter(ServerLinksDialog::buttonWidth)
         )
         .apply(instance, ServerLinksDialog::new)
   );

   @Override
   public MapCodec<ServerLinksDialog> getCodec() {
      return CODEC;
   }
}
