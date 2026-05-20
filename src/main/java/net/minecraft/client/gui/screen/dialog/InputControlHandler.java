package net.minecraft.client.gui.screen.dialog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.dialog.action.DialogAction;
import net.minecraft.dialog.input.InputControl;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface InputControlHandler<T extends InputControl> {
   void addControl(T inputControl, Screen screen, InputControlHandler.Output output);

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface Output {
      void accept(Widget widget, DialogAction.ValueGetter valueGetter);
   }
}
