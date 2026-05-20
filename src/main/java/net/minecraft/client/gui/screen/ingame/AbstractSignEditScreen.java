package net.minecraft.client.gui.screen.ingame;

import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.block.entity.AbstractSignBlockEntityRenderer;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractSignEditScreen extends Screen {
   protected final SignBlockEntity blockEntity;
   private SignText text;
   private final String[] messages;
   private final boolean front;
   protected final WoodType signType;
   private int ticksSinceOpened;
   private int currentRow;
   private @Nullable SelectionManager selectionManager;

   public AbstractSignEditScreen(SignBlockEntity blockEntity, boolean front, boolean filtered) {
      this(blockEntity, front, filtered, Text.translatable("sign.edit"));
   }

   public AbstractSignEditScreen(SignBlockEntity blockEntity, boolean front, boolean filtered, Text title) {
      super(title);
      this.blockEntity = blockEntity;
      this.text = blockEntity.getText(front);
      this.front = front;
      this.signType = AbstractSignBlock.getWoodType(blockEntity.getCachedState().getBlock());
      this.messages = IntStream.range(0, 4).mapToObj(line -> this.text.getMessage(line, filtered)).map(Text::getString).toArray(String[]::new);
   }

   @Override
   protected void init() {
      this.addDrawableChild(
         ButtonWidget.builder(ScreenTexts.DONE, button -> this.finishEditing()).dimensions(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()
      );
      this.selectionManager = new SelectionManager(
         () -> this.messages[this.currentRow],
         this::setCurrentRowMessage,
         SelectionManager.makeClipboardGetter(this.client),
         SelectionManager.makeClipboardSetter(this.client),
         textLine -> this.client.textRenderer.getWidth(textLine) <= this.blockEntity.getMaxTextWidth()
      );
   }

   @Override
   public void tick() {
      this.ticksSinceOpened++;
      if (!this.canEdit()) {
         this.finishEditing();
      }
   }

   private boolean canEdit() {
      return this.client.player != null && !this.blockEntity.isRemoved() && !this.blockEntity.isPlayerTooFarToEdit(this.client.player.getUuid());
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (input.isUp()) {
         this.currentRow = this.currentRow - 1 & 3;
         this.selectionManager.putCursorAtEnd();
         return true;
      } else if (input.isDown() || input.isEnter()) {
         this.currentRow = this.currentRow + 1 & 3;
         this.selectionManager.putCursorAtEnd();
         return true;
      } else {
         return this.selectionManager.handleSpecialKey(input) ? true : super.keyPressed(input);
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      this.selectionManager.insert(input);
      return true;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 40, -1);
      this.renderSign(context);
   }

   @Override
   public void close() {
      this.finishEditing();
   }

   @Override
   public void removed() {
      ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
      if (clientPlayNetworkHandler != null) {
         clientPlayNetworkHandler.sendPacket(
            new UpdateSignC2SPacket(this.blockEntity.getPos(), this.front, this.messages[0], this.messages[1], this.messages[2], this.messages[3])
         );
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean deferSubtitles() {
      return true;
   }

   protected abstract void renderSignBackground(DrawContext context);

   protected abstract Vector3f getTextScale();

   protected abstract float getYOffset();

   private void renderSign(DrawContext context) {
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(this.width / 2.0F, this.getYOffset());
      context.getMatrices().pushMatrix();
      this.renderSignBackground(context);
      context.getMatrices().popMatrix();
      this.renderSignText(context);
      context.getMatrices().popMatrix();
   }

   private void renderSignText(DrawContext context) {
      Vector3f vector3f = this.getTextScale();
      context.getMatrices().scale(vector3f.x(), vector3f.y());
      int i = this.text.isGlowing() ? this.text.getColor().getSignColor() : AbstractSignBlockEntityRenderer.getTextColor(this.text);
      boolean bl = this.ticksSinceOpened / 6 % 2 == 0;
      int j = this.selectionManager.getSelectionStart();
      int k = this.selectionManager.getSelectionEnd();
      int l = 4 * this.blockEntity.getTextLineHeight() / 2;
      int m = this.currentRow * this.blockEntity.getTextLineHeight() - l;

      for (int n = 0; n < this.messages.length; n++) {
         String string = this.messages[n];
         if (string != null) {
            if (this.textRenderer.isRightToLeft()) {
               string = this.textRenderer.mirror(string);
            }

            int o = -this.textRenderer.getWidth(string) / 2;
            context.drawText(this.textRenderer, string, o, n * this.blockEntity.getTextLineHeight() - l, i, false);
            if (n == this.currentRow && j >= 0 && bl) {
               int p = this.textRenderer.getWidth(string.substring(0, Math.max(Math.min(j, string.length()), 0)));
               int q = p - this.textRenderer.getWidth(string) / 2;
               if (j >= string.length()) {
                  context.drawText(this.textRenderer, "_", q, m, i, false);
               }
            }
         }
      }

      for (int nx = 0; nx < this.messages.length; nx++) {
         String string = this.messages[nx];
         if (string != null && nx == this.currentRow && j >= 0) {
            int o = this.textRenderer.getWidth(string.substring(0, Math.max(Math.min(j, string.length()), 0)));
            int p = o - this.textRenderer.getWidth(string) / 2;
            if (bl && j < string.length()) {
               context.fill(p, m - 1, p + 1, m + this.blockEntity.getTextLineHeight(), ColorHelper.fullAlpha(i));
            }

            if (k != j) {
               int q = Math.min(j, k);
               int r = Math.max(j, k);
               int s = this.textRenderer.getWidth(string.substring(0, q)) - this.textRenderer.getWidth(string) / 2;
               int t = this.textRenderer.getWidth(string.substring(0, r)) - this.textRenderer.getWidth(string) / 2;
               int u = Math.min(s, t);
               int v = Math.max(s, t);
               context.drawSelection(u, m, v, m + this.blockEntity.getTextLineHeight(), true);
            }
         }
      }
   }

   private void setCurrentRowMessage(String message) {
      this.messages[this.currentRow] = message;
      this.text = this.text.withMessage(this.currentRow, Text.literal(message));
      this.blockEntity.setText(this.text, this.front);
   }

   private void finishEditing() {
      this.client.setScreen(null);
   }
}
