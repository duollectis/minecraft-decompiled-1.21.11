package net.minecraft.dialog.type;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.dialog.DialogActionButtonData;

public interface SimpleDialog extends Dialog {
   @Override
   MapCodec<? extends SimpleDialog> getCodec();

   List<DialogActionButtonData> getButtons();
}
