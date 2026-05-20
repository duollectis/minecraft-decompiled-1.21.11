package net.minecraft.client.realms.gui;

import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.PopupScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class RealmsPopups {
   private static final int INFO_TEXT_COLOR = 8226750;
   private static final Text INFO_TEXT = Text.translatable("mco.info").withColor(8226750);
   private static final Text WARNING_TEXT = Text.translatable("mco.warning").withColor(-65536);

   public static PopupScreen createCustomPopup(Screen parent, Text title, Text message, Consumer<PopupScreen> onContinuePressed) {
      return new PopupScreen.Builder(parent, title)
         .message(message)
         .button(ScreenTexts.CONTINUE, onContinuePressed)
         .button(ScreenTexts.CANCEL, PopupScreen::close)
         .build();
   }

   public static PopupScreen createInfoPopup(Screen parent, Text message, Consumer<PopupScreen> onContinuePressed) {
      return new PopupScreen.Builder(parent, INFO_TEXT)
         .message(message)
         .button(ScreenTexts.CONTINUE, onContinuePressed)
         .button(ScreenTexts.CANCEL, PopupScreen::close)
         .build();
   }

   public static PopupScreen createContinuableWarningPopup(Screen parent, Text message, Consumer<PopupScreen> onContinuePressed) {
      return new PopupScreen.Builder(parent, WARNING_TEXT)
         .message(message)
         .button(ScreenTexts.CONTINUE, onContinuePressed)
         .button(ScreenTexts.CANCEL, PopupScreen::close)
         .build();
   }

   public static PopupScreen createNonContinuableWarningPopup(Screen parent, Text message, Consumer<PopupScreen> onOkPressed) {
      return new PopupScreen.Builder(parent, WARNING_TEXT).message(message).button(ScreenTexts.OK, onOkPressed).build();
   }
}
