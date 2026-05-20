package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CrafterScreenHandler;
import net.minecraft.screen.slot.CrafterInputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class CrafterScreen extends HandledScreen<CrafterScreenHandler> {
   private static final Identifier DISABLED_SLOT_TEXTURE = Identifier.ofVanilla("container/crafter/disabled_slot");
   private static final Identifier POWERED_REDSTONE_TEXTURE = Identifier.ofVanilla("container/crafter/powered_redstone");
   private static final Identifier UNPOWERED_REDSTONE_TEXTURE = Identifier.ofVanilla("container/crafter/unpowered_redstone");
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/crafter.png");
   private static final Text TOGGLEABLE_SLOT_TEXT = Text.translatable("gui.togglable_slot");
   private final PlayerEntity player;

   public CrafterScreen(CrafterScreenHandler handler, PlayerInventory playerInventory, Text title) {
      super(handler, playerInventory, title);
      this.player = playerInventory.player;
   }

   @Override
   protected void init() {
      super.init();
      this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
   }

   @Override
   protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
      if (slot instanceof CrafterInputSlot && !slot.hasStack() && !this.player.isSpectator()) {
         switch (actionType) {
            case PICKUP:
               if (this.handler.isSlotDisabled(slotId)) {
                  this.enableSlot(slotId);
               } else if (this.handler.getCursorStack().isEmpty()) {
                  this.disableSlot(slotId);
               }
               break;
            case SWAP:
               ItemStack itemStack = this.player.getInventory().getStack(button);
               if (this.handler.isSlotDisabled(slotId) && !itemStack.isEmpty()) {
                  this.enableSlot(slotId);
               }
         }
      }

      super.onMouseClick(slot, slotId, button, actionType);
   }

   private void enableSlot(int slotId) {
      this.setSlotEnabled(slotId, true);
   }

   private void disableSlot(int slotId) {
      this.setSlotEnabled(slotId, false);
   }

   private void setSlotEnabled(int slotId, boolean enabled) {
      this.handler.setSlotEnabled(slotId, enabled);
      super.onSlotChangedState(slotId, this.handler.syncId, enabled);
      float f = enabled ? 1.0F : 0.75F;
      this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, f);
   }

   @Override
   public void drawSlot(DrawContext context, Slot slot, int mouseX, int mouseY) {
      if (slot instanceof CrafterInputSlot crafterInputSlot) {
         if (this.handler.isSlotDisabled(slot.id)) {
            this.drawDisabledSlot(context, crafterInputSlot);
         } else {
            super.drawSlot(context, slot, mouseX, mouseY);
         }

         int i = this.x + crafterInputSlot.x - 2;
         int j = this.y + crafterInputSlot.y - 2;
         if (mouseX > i && mouseY > j && mouseX < i + 19 && mouseY < j + 19) {
            context.setCursor(StandardCursors.POINTING_HAND);
         }
      } else {
         super.drawSlot(context, slot, mouseX, mouseY);
      }
   }

   private void drawDisabledSlot(DrawContext context, CrafterInputSlot slot) {
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT_TEXTURE, slot.x - 1, slot.y - 1, 18, 18);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      this.drawArrowTexture(context);
      this.drawMouseoverTooltip(context, mouseX, mouseY);
      if (this.focusedSlot instanceof CrafterInputSlot
         && !this.handler.isSlotDisabled(this.focusedSlot.id)
         && this.handler.getCursorStack().isEmpty()
         && !this.focusedSlot.hasStack()
         && !this.player.isSpectator()) {
         context.drawTooltip(this.textRenderer, TOGGLEABLE_SLOT_TEXT, mouseX, mouseY);
      }
   }

   private void drawArrowTexture(DrawContext context) {
      int i = this.width / 2 + 9;
      int j = this.height / 2 - 48;
      Identifier identifier;
      if (this.handler.isTriggered()) {
         identifier = POWERED_REDSTONE_TEXTURE;
      } else {
         identifier = UNPOWERED_REDSTONE_TEXTURE;
      }

      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, i, j, 16, 16);
   }

   @Override
   protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
      int i = (this.width - this.backgroundWidth) / 2;
      int j = (this.height - this.backgroundHeight) / 2;
      context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
   }
}
