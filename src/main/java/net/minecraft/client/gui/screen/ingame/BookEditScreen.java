package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public class BookEditScreen extends Screen {
   public static final int MAX_TEXT_WIDTH = 114;
   public static final int MAX_TEXT_HEIGHT = 126;
   public static final int WIDTH = 192;
   public static final int HEIGHT = 192;
   public static final int field_52805 = 256;
   public static final int field_52806 = 256;
   private static final int field_63897 = 4;
   private static final int field_63898 = 98;
   private static final int field_63899 = 157;
   private static final int field_63900 = 43;
   private static final int field_63901 = 116;
   private static final int field_63902 = 16;
   private static final int field_63903 = 148;
   private static final Text TITLE_TEXT = Text.translatable("book.edit.title");
   private static final Text field_63896 = Text.translatable("book.signButton");
   private final PlayerEntity player;
   private final ItemStack stack;
   private final BookSigningScreen signingScreen;
   private int currentPage;
   private final List<String> pages = Lists.newArrayList();
   private PageTurnWidget nextPageButton;
   private PageTurnWidget previousPageButton;
   private final Hand hand;
   private Text pageIndicatorText = ScreenTexts.EMPTY;
   private EditBoxWidget editBox;

   public BookEditScreen(PlayerEntity player, ItemStack stack, Hand hand, WritableBookContentComponent writableBookContent) {
      super(TITLE_TEXT);
      this.player = player;
      this.stack = stack;
      this.hand = hand;
      writableBookContent.stream(MinecraftClient.getInstance().shouldFilterText()).forEach(this.pages::add);
      if (this.pages.isEmpty()) {
         this.pages.add("");
      }

      this.signingScreen = new BookSigningScreen(this, player, hand, this.pages);
   }

   private int countPages() {
      return this.pages.size();
   }

   @Override
   protected void init() {
      int i = this.method_75831();
      int j = this.method_75828();
      int k = 8;
      this.editBox = EditBoxWidget.builder()
         .hasOverlay(false)
         .textColor(-16777216)
         .cursorColor(-16777216)
         .hasBackground(false)
         .textShadow(false)
         .x((this.width - 114) / 2 - 8)
         .y(28)
         .build(this.textRenderer, 122, 134, ScreenTexts.EMPTY);
      this.editBox.setMaxLength(1024);
      this.editBox.setMaxLines(126 / 9);
      this.editBox.setChangeListener(page -> this.pages.set(this.currentPage, page));
      this.addDrawableChild(this.editBox);
      this.updatePage();
      this.pageIndicatorText = this.getPageIndicatorText();
      this.previousPageButton = this.addDrawableChild(new PageTurnWidget(i + 43, j + 157, false, button -> this.openPreviousPage(), true));
      this.nextPageButton = this.addDrawableChild(new PageTurnWidget(i + 116, j + 157, true, button -> this.openNextPage(), true));
      this.addDrawableChild(
         ButtonWidget.builder(field_63896, button -> this.client.setScreen(this.signingScreen))
            .position(this.width / 2 - 98 - 2, this.method_75829())
            .width(98)
            .build()
      );
      this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
         this.client.setScreen(null);
         this.finalizeBook();
      }).position(this.width / 2 + 2, this.method_75829()).width(98).build());
      this.updatePreviousPageButtonVisibility();
   }

   private int method_75831() {
      return (this.width - 192) / 2;
   }

   private int method_75828() {
      return 2;
   }

   private int method_75829() {
      return this.method_75828() + 192 + 2;
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.editBox);
   }

   @Override
   public Text getNarratedTitle() {
      return ScreenTexts.joinSentences(super.getNarratedTitle(), this.getPageIndicatorText());
   }

   private Text getPageIndicatorText() {
      return Text.translatable("book.pageIndicator", this.currentPage + 1, this.countPages()).withColor(-16777216).withoutShadow();
   }

   private void openPreviousPage() {
      if (this.currentPage > 0) {
         this.currentPage--;
         this.updatePage();
      }

      this.updatePreviousPageButtonVisibility();
   }

   private void openNextPage() {
      if (this.currentPage < this.countPages() - 1) {
         this.currentPage++;
      } else {
         this.appendNewPage();
         if (this.currentPage < this.countPages() - 1) {
            this.currentPage++;
         }
      }

      this.updatePage();
      this.updatePreviousPageButtonVisibility();
   }

   private void updatePage() {
      this.editBox.setText(this.pages.get(this.currentPage), true);
      this.pageIndicatorText = this.getPageIndicatorText();
   }

   private void updatePreviousPageButtonVisibility() {
      this.previousPageButton.visible = this.currentPage > 0;
   }

   private void removeEmptyPages() {
      ListIterator<String> listIterator = this.pages.listIterator(this.pages.size());

      while (listIterator.hasPrevious() && listIterator.previous().isEmpty()) {
         listIterator.remove();
      }
   }

   private void finalizeBook() {
      this.removeEmptyPages();
      this.writeNbtData();
      int i = this.hand == Hand.MAIN_HAND ? this.player.getInventory().getSelectedSlot() : 40;
      this.client.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(i, this.pages, Optional.empty()));
   }

   private void writeNbtData() {
      this.stack.set(DataComponentTypes.WRITABLE_BOOK_CONTENT, new WritableBookContentComponent(this.pages.stream().map(RawFilteredPair::of).toList()));
   }

   private void appendNewPage() {
      if (this.countPages() < 100) {
         this.pages.add("");
      }
   }

   @Override
   public boolean deferSubtitles() {
      return true;
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      switch (input.key()) {
         case 266:
            this.previousPageButton.onPress(input);
            return true;
         case 267:
            this.nextPageButton.onPress(input);
            return true;
         default:
            return super.keyPressed(input);
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      this.method_75830(context.getTextConsumer());
   }

   private void method_75830(DrawnTextConsumer drawnTextConsumer) {
      int i = this.method_75831();
      int j = this.method_75828();
      drawnTextConsumer.text(Alignment.RIGHT, i + 148, j + 16, this.pageIndicatorText);
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.renderBackground(context, mouseX, mouseY, deltaTicks);
      context.drawTexture(RenderPipelines.GUI_TEXTURED, BookScreen.BOOK_TEXTURE, this.method_75831(), this.method_75828(), 0.0F, 0.0F, 192, 192, 256, 256);
   }
}
