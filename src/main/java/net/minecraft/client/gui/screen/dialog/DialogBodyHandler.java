package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.dialog.body.DialogBody;

@Environment(EnvType.CLIENT)
public interface DialogBodyHandler<T extends DialogBody> {
   Widget createWidget(DialogScreen<?> dialogScreen, T body);
}
