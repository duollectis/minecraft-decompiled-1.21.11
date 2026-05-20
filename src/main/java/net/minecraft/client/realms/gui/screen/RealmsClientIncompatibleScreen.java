package net.minecraft.client.realms.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class RealmsClientIncompatibleScreen extends RealmsScreen {
   private static final Text INCOMPATIBLE_TITLE = Text.translatable("mco.client.incompatible.title").withColor(-65536);
   private static final Text GAME_VERSION = Text.literal(SharedConstants.getGameVersion().name()).withColor(-65536);
   private static final Text UNSUPPORTED_SNAPSHOT_VERSION = Text.translatable("mco.client.unsupported.snapshot.version", GAME_VERSION);
   private static final Text OUTDATED_STABLE_VERSION = Text.translatable("mco.client.outdated.stable.version", GAME_VERSION);
   private final Screen parent;
   private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

   public RealmsClientIncompatibleScreen(Screen parent) {
      super(INCOMPATIBLE_TITLE);
      this.parent = parent;
   }

   @Override
   public void init() {
      this.layout.addHeader(INCOMPATIBLE_TITLE, this.textRenderer);
      this.layout.addBody(new MultilineTextWidget(this.getErrorText(), this.textRenderer).setCentered(true));
      this.layout.addFooter(ButtonWidget.builder(ScreenTexts.BACK, buttonWidget -> this.close()).width(200).build());
      this.layout.forEachChild(element -> {
         ClickableWidget var10000 = this.addDrawableChild(element);
      });
      this.refreshWidgetPositions();
   }

   @Override
   protected void refreshWidgetPositions() {
      this.layout.refreshPositions();
   }

   @Override
   public void close() {
      this.client.setScreen(this.parent);
   }

   private Text getErrorText() {
      return SharedConstants.getGameVersion().stable() ? OUTDATED_STABLE_VERSION : UNSUPPORTED_SNAPSHOT_VERSION;
   }
}
