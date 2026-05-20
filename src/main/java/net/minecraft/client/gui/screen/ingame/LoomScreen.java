package net.minecraft.client.gui.screen.ingame;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.Sprite;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BannerItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LoomScreen extends HandledScreen<LoomScreenHandler> {
   private static final Identifier BANNER_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/banner");
   private static final Identifier DYE_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/dye");
   private static final Identifier PATTERN_SLOT_TEXTURE = Identifier.ofVanilla("container/slot/banner_pattern");
   private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/loom/scroller");
   private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/loom/scroller_disabled");
   private static final Identifier PATTERN_SELECTED_TEXTURE = Identifier.ofVanilla("container/loom/pattern_selected");
   private static final Identifier PATTERN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("container/loom/pattern_highlighted");
   private static final Identifier PATTERN_TEXTURE = Identifier.ofVanilla("container/loom/pattern");
   private static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("container/loom/error");
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/loom.png");
   private static final int PATTERN_LIST_COLUMNS = 4;
   private static final int PATTERN_LIST_ROWS = 4;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int SCROLLBAR_HEIGHT = 15;
   private static final int PATTERN_ENTRY_SIZE = 14;
   private static final int SCROLLBAR_AREA_HEIGHT = 56;
   private static final int PATTERN_LIST_OFFSET_X = 60;
   private static final int PATTERN_LIST_OFFSET_Y = 13;
   private static final float field_59943 = 64.0F;
   private static final float field_59944 = 21.0F;
   private static final float field_59945 = 40.0F;
   private BannerFlagBlockModel bannerField;
   private @Nullable BannerPatternsComponent bannerPatterns;
   private ItemStack banner = ItemStack.EMPTY;
   private ItemStack dye = ItemStack.EMPTY;
   private ItemStack pattern = ItemStack.EMPTY;
   private boolean canApplyDyePattern;
   private boolean hasTooManyPatterns;
   private float scrollPosition;
   private boolean scrollbarClicked;
   private int visibleTopRow;

   public LoomScreen(LoomScreenHandler screenHandler, PlayerInventory inventory, Text title) {
      super(screenHandler, inventory, title);
      screenHandler.setInventoryChangeListener(this::onInventoryChanged);
      this.titleY -= 2;
   }

   @Override
   protected void init() {
      super.init();
      ModelPart modelPart = this.client.getLoadedEntityModels().getModelPart(EntityModelLayers.STANDING_BANNER_FLAG);
      this.bannerField = new BannerFlagBlockModel(modelPart);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      this.drawMouseoverTooltip(context, mouseX, mouseY);
   }

   private int getRows() {
      return MathHelper.ceilDiv(this.handler.getBannerPatterns().size(), 4);
   }

   @Override
   protected void drawBackground(DrawContext context, float deltaTicks, int mouseX, int mouseY) {
      int i = this.x;
      int j = this.y;
      context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0.0F, 0.0F, this.backgroundWidth, this.backgroundHeight, 256, 256);
      Slot slot = this.handler.getBannerSlot();
      Slot slot2 = this.handler.getDyeSlot();
      Slot slot3 = this.handler.getPatternSlot();
      Slot slot4 = this.handler.getOutputSlot();
      if (!slot.hasStack()) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BANNER_SLOT_TEXTURE, i + slot.x, j + slot.y, 16, 16);
      }

      if (!slot2.hasStack()) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, DYE_SLOT_TEXTURE, i + slot2.x, j + slot2.y, 16, 16);
      }

      if (!slot3.hasStack()) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, PATTERN_SLOT_TEXTURE, i + slot3.x, j + slot3.y, 16, 16);
      }

      int k = (int)(41.0F * this.scrollPosition);
      Identifier identifier = this.canApplyDyePattern ? SCROLLER_TEXTURE : SCROLLER_DISABLED_TEXTURE;
      int l = i + 119;
      int m = j + 13 + k;
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, l, m, 12, 15);
      if (mouseX >= l && mouseX < l + 12 && mouseY >= m && mouseY < m + 15) {
         context.setCursor(this.scrollbarClicked ? StandardCursors.RESIZE_NS : StandardCursors.POINTING_HAND);
      }

      if (this.bannerPatterns != null && !this.hasTooManyPatterns) {
         DyeColor dyeColor = ((BannerItem)slot4.getStack().getItem()).getColor();
         int n = i + 141;
         int o = j + 8;
         context.addBannerResult(this.bannerField, dyeColor, this.bannerPatterns, n, o, n + 20, o + 40);
      } else if (this.hasTooManyPatterns) {
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ERROR_TEXTURE, i + slot4.x - 5, j + slot4.y - 5, 26, 26);
      }

      if (this.canApplyDyePattern) {
         int p = i + 60;
         int n = j + 13;
         List<RegistryEntry<BannerPattern>> list = this.handler.getBannerPatterns();

         label79:
         for (int q = 0; q < 4; q++) {
            for (int r = 0; r < 4; r++) {
               int s = q + this.visibleTopRow;
               int t = s * 4 + r;
               if (t >= list.size()) {
                  break label79;
               }

               int u = p + r * 14;
               int v = n + q * 14;
               RegistryEntry<BannerPattern> registryEntry = list.get(t);
               boolean bl = mouseX >= u && mouseY >= v && mouseX < u + 14 && mouseY < v + 14;
               Identifier identifier2;
               if (t == this.handler.getSelectedPattern()) {
                  identifier2 = PATTERN_SELECTED_TEXTURE;
               } else if (bl) {
                  identifier2 = PATTERN_HIGHLIGHTED_TEXTURE;
                  DyeColor dyeColor2 = ((DyeItem)this.dye.getItem()).getColor();
                  context.drawTooltip(Text.translatable(registryEntry.value().translationKey() + "." + dyeColor2.getId()), mouseX, mouseY);
                  context.setCursor(StandardCursors.POINTING_HAND);
               } else {
                  identifier2 = PATTERN_TEXTURE;
               }

               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier2, u, v, 14, 14);
               Sprite sprite = context.getSprite(TexturedRenderLayers.getBannerPatternTextureId(registryEntry));
               this.drawBanner(context, u, v, sprite);
            }
         }
      }

      MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ITEMS_3D);
   }

   private void drawBanner(DrawContext context, int x, int y, Sprite sprite) {
      context.getMatrices().pushMatrix();
      context.getMatrices().translate(x + 4, y + 2);
      float f = sprite.getMinU();
      float g = f + (sprite.getMaxU() - sprite.getMinU()) * 21.0F / 64.0F;
      float h = sprite.getMaxV() - sprite.getMinV();
      float i = sprite.getMinV() + h / 64.0F;
      float j = i + h * 40.0F / 64.0F;
      int k = 5;
      int l = 10;
      context.fill(0, 0, 5, 10, DyeColor.GRAY.getEntityColor());
      context.drawTexturedQuad(sprite.getAtlasId(), 0, 0, 5, 10, f, g, i, j);
      context.getMatrices().popMatrix();
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      if (this.canApplyDyePattern) {
         int i = this.x + 60;
         int j = this.y + 13;

         for (int k = 0; k < 4; k++) {
            for (int l = 0; l < 4; l++) {
               double d = click.x() - (i + l * 14);
               double e = click.y() - (j + k * 14);
               int m = k + this.visibleTopRow;
               int n = m * 4 + l;
               if (d >= 0.0 && e >= 0.0 && d < 14.0 && e < 14.0 && this.handler.onButtonClick(this.client.player, n)) {
                  MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                  this.client.interactionManager.clickButton(this.handler.syncId, n);
                  return true;
               }
            }
         }

         i = this.x + 119;
         j = this.y + 9;
         if (click.x() >= i && click.x() < i + 12 && click.y() >= j && click.y() < j + 56) {
            this.scrollbarClicked = true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      int i = this.getRows() - 4;
      if (this.scrollbarClicked && this.canApplyDyePattern && i > 0) {
         int j = this.y + 13;
         int k = j + 56;
         this.scrollPosition = ((float)click.y() - j - 7.5F) / (k - j - 15.0F);
         this.scrollPosition = MathHelper.clamp(this.scrollPosition, 0.0F, 1.0F);
         this.visibleTopRow = Math.max((int)(this.scrollPosition * i + 0.5), 0);
         return true;
      } else {
         return super.mouseDragged(click, offsetX, offsetY);
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.scrollbarClicked = false;
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
         return true;
      } else {
         int i = this.getRows() - 4;
         if (this.canApplyDyePattern && i > 0) {
            float f = (float)verticalAmount / i;
            this.scrollPosition = MathHelper.clamp(this.scrollPosition - f, 0.0F, 1.0F);
            this.visibleTopRow = Math.max((int)(this.scrollPosition * i + 0.5F), 0);
         }

         return true;
      }
   }

   @Override
   protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top) {
      return mouseX < left || mouseY < top || mouseX >= left + this.backgroundWidth || mouseY >= top + this.backgroundHeight;
   }

   private void onInventoryChanged() {
      ItemStack itemStack = this.handler.getOutputSlot().getStack();
      if (itemStack.isEmpty()) {
         this.bannerPatterns = null;
      } else {
         this.bannerPatterns = itemStack.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT);
      }

      ItemStack itemStack2 = this.handler.getBannerSlot().getStack();
      ItemStack itemStack3 = this.handler.getDyeSlot().getStack();
      ItemStack itemStack4 = this.handler.getPatternSlot().getStack();
      BannerPatternsComponent bannerPatternsComponent = itemStack2.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT);
      this.hasTooManyPatterns = bannerPatternsComponent.layers().size() >= 6;
      if (this.hasTooManyPatterns) {
         this.bannerPatterns = null;
      }

      if (!ItemStack.areEqual(itemStack2, this.banner) || !ItemStack.areEqual(itemStack3, this.dye) || !ItemStack.areEqual(itemStack4, this.pattern)) {
         this.canApplyDyePattern = !itemStack2.isEmpty() && !itemStack3.isEmpty() && !this.hasTooManyPatterns && !this.handler.getBannerPatterns().isEmpty();
      }

      if (this.visibleTopRow >= this.getRows()) {
         this.visibleTopRow = 0;
         this.scrollPosition = 0.0F;
      }

      this.banner = itemStack2.copy();
      this.dye = itemStack3.copy();
      this.pattern = itemStack4.copy();
   }
}
