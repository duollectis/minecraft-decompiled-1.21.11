package net.minecraft.client.realms.gui.screen.tab;

import com.google.common.collect.Lists;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tab.GridScreenTab;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Positioner;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.WorldTemplate;
import net.minecraft.client.realms.gui.RealmsPopups;
import net.minecraft.client.realms.gui.RealmsWorldSlotButton;
import net.minecraft.client.realms.gui.screen.RealmsBackupScreen;
import net.minecraft.client.realms.gui.screen.RealmsConfigureWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsCreateWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsLongRunningMcoTaskScreen;
import net.minecraft.client.realms.gui.screen.RealmsSelectWorldTemplateScreen;
import net.minecraft.client.realms.gui.screen.RealmsSlotOptionsScreen;
import net.minecraft.client.realms.task.SwitchMinigameTask;
import net.minecraft.client.realms.task.SwitchSlotTask;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RealmsWorldsTab extends GridScreenTab implements RealmsUpdatableTab {
   public static final Text TITLE_TEXT = Text.translatable("mco.configure.worlds.title");
   private final RealmsConfigureWorldScreen screen;
   private final MinecraftClient client;
   private RealmsServer server;
   private final ButtonWidget optionsButton;
   private final ButtonWidget backupButton;
   private final ButtonWidget resetButton;
   private final List<RealmsWorldSlotButton> slotButtons = Lists.newArrayList();

   public RealmsWorldsTab(RealmsConfigureWorldScreen screen, MinecraftClient client, RealmsServer server) {
      super(TITLE_TEXT);
      this.screen = screen;
      this.client = client;
      this.server = server;
      GridWidget.Adder adder = this.grid.setSpacing(20).createAdder(1);
      GridWidget.Adder adder2 = new GridWidget().setSpacing(16).createAdder(4);
      this.slotButtons.clear();

      for (int i = 1; i < 5; i++) {
         this.slotButtons.add(adder2.add(this.createSlotButton(i), Positioner.create().alignBottom()));
      }

      adder.add(adder2.getGridWidget());
      GridWidget.Adder adder3 = new GridWidget().setSpacing(8).createAdder(1);
      this.optionsButton = adder3.add(
         ButtonWidget.builder(
               Text.translatable("mco.configure.world.buttons.options"),
               button -> client.setScreen(
                  new RealmsSlotOptionsScreen(screen, server.slots.get(server.activeSlot).method_71181(), server.worldType, server.activeSlot)
               )
            )
            .dimensions(0, 0, 150, 20)
            .build()
      );
      this.backupButton = adder3.add(
         ButtonWidget.builder(
               Text.translatable("mco.configure.world.backup"),
               button -> client.setScreen(new RealmsBackupScreen(screen, server.method_25067(), server.activeSlot))
            )
            .dimensions(0, 0, 150, 20)
            .build()
      );
      this.resetButton = adder3.add(ButtonWidget.builder(Text.empty(), button -> this.reset()).dimensions(0, 0, 150, 20).build());
      adder.add(adder3.getGridWidget(), Positioner.create().alignHorizontalCenter());
      this.backupButton.active = true;
      this.update(server);
   }

   private void reset() {
      if (this.isMinigame()) {
         this.client
            .setScreen(
               new RealmsSelectWorldTemplateScreen(
                  Text.translatable("mco.template.title.minigame"), this::switchMinigame, RealmsServer.WorldType.MINIGAME, null
               )
            );
      } else {
         this.client
            .setScreen(
               RealmsCreateWorldScreen.resetWorld(
                  this.screen, this.server.method_25067(), () -> this.client.execute(() -> this.client.setScreen(this.screen.getNewScreen()))
               )
            );
      }
   }

   private void switchMinigame(@Nullable WorldTemplate template) {
      if (template != null && WorldTemplate.WorldTemplateType.MINIGAME == template.type()) {
         this.screen.stateChanged();
         RealmsConfigureWorldScreen realmsConfigureWorldScreen = this.screen.getNewScreen();
         this.client
            .setScreen(
               new RealmsLongRunningMcoTaskScreen(realmsConfigureWorldScreen, new SwitchMinigameTask(this.server.id, template, realmsConfigureWorldScreen))
            );
      } else {
         this.client.setScreen(this.screen);
      }
   }

   private boolean isMinigame() {
      return this.server.isMinigame();
   }

   @Override
   public void onLoaded(RealmsServer server) {
      this.update(server);
   }

   @Override
   public void update(RealmsServer server) {
      this.server = server;
      this.optionsButton.active = !server.expired && !this.isMinigame();
      this.resetButton.active = !server.expired;
      if (this.isMinigame()) {
         this.resetButton.setMessage(Text.translatable("mco.configure.world.buttons.switchminigame"));
      } else {
         boolean bl = server.slots.containsKey(server.activeSlot) && server.slots.get(server.activeSlot).options.empty;
         if (bl) {
            this.resetButton.setMessage(Text.translatable("mco.configure.world.buttons.newworld"));
         } else {
            this.resetButton.setMessage(Text.translatable("mco.configure.world.buttons.resetworld"));
         }
      }

      this.backupButton.active = !this.isMinigame();

      for (RealmsWorldSlotButton realmsWorldSlotButton : this.slotButtons) {
         RealmsWorldSlotButton.State state = realmsWorldSlotButton.setServer(server);
         if (state.active) {
            realmsWorldSlotButton.setDimensions(80, 80);
         } else {
            realmsWorldSlotButton.setDimensions(50, 50);
         }
      }
   }

   private RealmsWorldSlotButton createSlotButton(int slotIndex) {
      return new RealmsWorldSlotButton(0, 0, 80, 80, slotIndex, this.server, button -> {
         RealmsWorldSlotButton.State state = ((RealmsWorldSlotButton)button).getState();
         switch (state.action) {
            case SWITCH_SLOT:
               if (state.minigame) {
                  this.showSwitchMinigameScreen();
               } else if (state.empty) {
                  this.createWorld(slotIndex, this.server);
               } else {
                  this.switchWorld(slotIndex, this.server);
               }
            case NOTHING:
               return;
            default:
               throw new IllegalStateException("Unknown action " + state.action);
         }
      });
   }

   private void showSwitchMinigameScreen() {
      RealmsSelectWorldTemplateScreen realmsSelectWorldTemplateScreen = new RealmsSelectWorldTemplateScreen(
         Text.translatable("mco.template.title.minigame"),
         this::switchMinigame,
         RealmsServer.WorldType.MINIGAME,
         null,
         List.of(Text.translatable("mco.minigame.world.info.line1").withColor(-4539718), Text.translatable("mco.minigame.world.info.line2").withColor(-4539718))
      );
      this.client.setScreen(realmsSelectWorldTemplateScreen);
   }

   private void switchWorld(int slotId, RealmsServer server) {
      this.client
         .setScreen(
            RealmsPopups.createInfoPopup(
               this.screen,
               Text.translatable("mco.configure.world.slot.switch.question.line1"),
               popup -> {
                  RealmsConfigureWorldScreen realmsConfigureWorldScreen = this.screen.getNewScreen();
                  this.screen.stateChanged();
                  this.client
                     .setScreen(
                        new RealmsLongRunningMcoTaskScreen(
                           realmsConfigureWorldScreen,
                           new SwitchSlotTask(server.id, slotId, () -> this.client.execute(() -> this.client.setScreen(realmsConfigureWorldScreen)))
                        )
                     );
               }
            )
         );
   }

   private void createWorld(int slotId, RealmsServer server) {
      this.client
         .setScreen(
            RealmsPopups.createInfoPopup(
               this.screen,
               Text.translatable("mco.configure.world.slot.switch.question.line1"),
               popup -> {
                  this.screen.stateChanged();
                  RealmsCreateWorldScreen realmsCreateWorldScreen = RealmsCreateWorldScreen.newWorld(
                     this.screen, slotId, server, () -> this.client.execute(() -> this.client.setScreen(this.screen.getNewScreen()))
                  );
                  this.client.setScreen(realmsCreateWorldScreen);
               }
            )
         );
   }
}
