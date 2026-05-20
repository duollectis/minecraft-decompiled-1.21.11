package net.minecraft.client.gui.screen.ingame;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MountScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class MountScreen<T extends MountScreenHandler> extends HandledScreen<T> {
   protected final int slotColumnCount;
   protected float mouseX;
   protected float mouseY;
   protected LivingEntity mount;

   public MountScreen(T handler, PlayerInventory inventory, Text title, int slotColumnCount, LivingEntity mount) {
      super(handler, inventory, title);
      this.slotColumnCount = slotColumnCount;
      this.mount = mount;
   }

   @Override
   protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
      int i = (this.width - this.backgroundWidth) / 2;
      int j = (this.height - this.backgroundHeight) / 2;
      context.drawTexture(RenderPipelines.GUI_TEXTURED, this.getTexture(), i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
      if (this.slotColumnCount > 0 && this.getChestSlotsTexture() != null) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, this.getChestSlotsTexture(), 90, 54, 0, 0, i + 79, j + 17, this.slotColumnCount * 18, 54);
      }

      if (this.canEquipSaddle()) {
         this.drawSlot(context, i + 7, j + 35 - 18);
      }

      if (this.canEquipArmor()) {
         this.drawSlot(context, i + 7, j + 35);
      }

      InventoryScreen.drawEntity(context, i + 26, j + 18, i + 78, j + 70, 17, 0.25F, this.mouseX, this.mouseY, this.mount);
   }

   protected void drawSlot(DrawContext context, int x, int y) {
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, this.getSlotTexture(), x, y, 18, 18);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      super.render(context, mouseX, mouseY, deltaTicks);
      this.drawMouseoverTooltip(context, mouseX, mouseY);
   }

   protected abstract Identifier getTexture();

   protected abstract Identifier getSlotTexture();

   protected abstract @Nullable Identifier getChestSlotsTexture();

   protected abstract boolean canEquipSaddle();

   protected abstract boolean canEquipArmor();
}
