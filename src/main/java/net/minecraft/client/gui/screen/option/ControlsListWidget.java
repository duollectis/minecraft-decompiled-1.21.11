package net.minecraft.client.gui.screen.option;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.NarratedMultilineTextWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.ArrayUtils;

@Environment(EnvType.CLIENT)
public class ControlsListWidget extends ElementListWidget<ControlsListWidget.Entry> {
   private static final int field_49533 = 20;
   final KeybindsScreen parent;
   private int maxKeyNameLength;

   public ControlsListWidget(KeybindsScreen parent, MinecraftClient client) {
      super(client, parent.width, parent.layout.getContentHeight(), parent.layout.getHeaderHeight(), 20);
      this.parent = parent;
      KeyBinding[] keyBindings = (KeyBinding[])ArrayUtils.clone(client.options.allKeys);
      Arrays.sort((Object[])keyBindings);
      KeyBinding.Category category = null;

      for (KeyBinding keyBinding : keyBindings) {
         KeyBinding.Category category2 = keyBinding.getCategory();
         if (category2 != category) {
            category = category2;
            this.addEntry(new ControlsListWidget.CategoryEntry(category2));
         }

         Text text = Text.translatable(keyBinding.getId());
         int i = client.textRenderer.getWidth(text);
         if (i > this.maxKeyNameLength) {
            this.maxKeyNameLength = i;
         }

         this.addEntry(new ControlsListWidget.KeyBindingEntry(keyBinding, text));
      }
   }

   public void update() {
      KeyBinding.updateKeysByCode();
      this.updateChildren();
   }

   public void updateChildren() {
      this.children().forEach(ControlsListWidget.Entry::update);
   }

   @Override
   public int getRowWidth() {
      return 340;
   }

   @Environment(EnvType.CLIENT)
   public class CategoryEntry extends ControlsListWidget.Entry {
      private final NarratedMultilineTextWidget field_62179;

      public CategoryEntry(final KeyBinding.Category category) {
         this.field_62179 = NarratedMultilineTextWidget.builder(category.getLabel(), ControlsListWidget.this.client.textRenderer)
            .alwaysShowBorders(false)
            .backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering.ON_FOCUS)
            .build();
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
         this.field_62179
            .setPosition(ControlsListWidget.this.width / 2 - this.field_62179.getWidth() / 2, this.getContentBottomEnd() - this.field_62179.getHeight());
         this.field_62179.render(context, mouseX, mouseY, deltaTicks);
      }

      @Override
      public List<? extends Element> children() {
         return List.of(this.field_62179);
      }

      @Override
      public List<? extends Selectable> selectableChildren() {
         return List.of(this.field_62179);
      }

      @Override
      protected void update() {
      }
   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry extends ElementListWidget.Entry<ControlsListWidget.Entry> {
      abstract void update();
   }

   @Environment(EnvType.CLIENT)
   public class KeyBindingEntry extends ControlsListWidget.Entry {
      private static final Text RESET_TEXT = Text.translatable("controls.reset");
      private static final int field_49535 = 10;
      private final KeyBinding binding;
      private final Text bindingName;
      private final ButtonWidget editButton;
      private final ButtonWidget resetButton;
      private boolean duplicate = false;

      KeyBindingEntry(final KeyBinding binding, final Text bindingName) {
         this.binding = binding;
         this.bindingName = bindingName;
         this.editButton = ButtonWidget.builder(bindingName, button -> {
               ControlsListWidget.this.parent.selectedKeyBinding = binding;
               ControlsListWidget.this.update();
            })
            .dimensions(0, 0, 75, 20)
            .narrationSupplier(
               textSupplier -> binding.isUnbound()
                  ? Text.translatable("narrator.controls.unbound", bindingName)
                  : Text.translatable("narrator.controls.bound", bindingName, textSupplier.get())
            )
            .build();
         this.resetButton = ButtonWidget.builder(RESET_TEXT, button -> {
            binding.setBoundKey(binding.getDefaultKey());
            ControlsListWidget.this.update();
         }).dimensions(0, 0, 50, 20).narrationSupplier(textSupplier -> Text.translatable("narrator.controls.reset", bindingName)).build();
         this.update();
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
         int i = ControlsListWidget.this.getScrollbarX() - this.resetButton.getWidth() - 10;
         int j = this.getContentY() - 2;
         this.resetButton.setPosition(i, j);
         this.resetButton.render(context, mouseX, mouseY, deltaTicks);
         int k = i - 5 - this.editButton.getWidth();
         this.editButton.setPosition(k, j);
         this.editButton.render(context, mouseX, mouseY, deltaTicks);
         context.drawTextWithShadow(ControlsListWidget.this.client.textRenderer, this.bindingName, this.getContentX(), this.getContentMiddleY() - 9 / 2, -1);
         if (this.duplicate) {
            int l = 3;
            int m = this.editButton.getX() - 6;
            context.fill(m, this.getContentY() - 1, m + 3, this.getContentBottomEnd(), -256);
         }
      }

      @Override
      public List<? extends Element> children() {
         return ImmutableList.of(this.editButton, this.resetButton);
      }

      @Override
      public List<? extends Selectable> selectableChildren() {
         return ImmutableList.of(this.editButton, this.resetButton);
      }

      @Override
      protected void update() {
         this.editButton.setMessage(this.binding.getBoundKeyLocalizedText());
         this.resetButton.active = !this.binding.isDefault();
         this.duplicate = false;
         MutableText mutableText = Text.empty();
         if (!this.binding.isUnbound()) {
            for (KeyBinding keyBinding : ControlsListWidget.this.client.options.allKeys) {
               if (keyBinding != this.binding && this.binding.equals(keyBinding) && (!keyBinding.isDefault() || !this.binding.isDefault())) {
                  if (this.duplicate) {
                     mutableText.append(", ");
                  }

                  this.duplicate = true;
                  mutableText.append(Text.translatable(keyBinding.getId()));
               }
            }
         }

         if (this.duplicate) {
            this.editButton
               .setMessage(Text.literal("[ ").append(this.editButton.getMessage().copy().formatted(Formatting.WHITE)).append(" ]").formatted(Formatting.YELLOW));
            this.editButton.setTooltip(Tooltip.of(Text.translatable("controls.keybinds.duplicateKeybinds", mutableText)));
         } else {
            this.editButton.setTooltip(null);
         }

         if (ControlsListWidget.this.parent.selectedKeyBinding == this.binding) {
            this.editButton
               .setMessage(
                  Text.literal("> ")
                     .append(this.editButton.getMessage().copy().formatted(Formatting.WHITE, Formatting.UNDERLINE))
                     .append(" <")
                     .formatted(Formatting.YELLOW)
               );
         }
      }
   }
}
