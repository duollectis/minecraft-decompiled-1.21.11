package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.WorldTemplate;
import net.minecraft.client.realms.dto.WorldTemplatePaginatedList;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.util.RealmsTextureManager;
import net.minecraft.client.realms.util.TextRenderingUtils;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Urls;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsSelectWorldTemplateScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   static final Identifier SLOT_FRAME_TEXTURE = Identifier.ofVanilla("widget/slot_frame");
   private static final Text SELECT_TEXT = Text.translatable("mco.template.button.select");
   private static final Text TRAILER_TEXT = Text.translatable("mco.template.button.trailer");
   private static final Text PUBLISHER_TEXT = Text.translatable("mco.template.button.publisher");
   private static final int field_45974 = 100;
   final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
   final Consumer<WorldTemplate> callback;
   RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList templateList;
   private final RealmsServer.WorldType worldType;
   private final List<Text> field_62460;
   private ButtonWidget selectButton;
   private ButtonWidget trailerButton;
   private ButtonWidget publisherButton;
   @Nullable WorldTemplate selectedTemplate = null;
   @Nullable String currentLink;
   @Nullable List<TextRenderingUtils.Line> noTemplatesMessage;

   public RealmsSelectWorldTemplateScreen(
      Text title, Consumer<WorldTemplate> callback, RealmsServer.WorldType worldType, @Nullable WorldTemplatePaginatedList worldTemplatePaginatedList
   ) {
      this(title, callback, worldType, worldTemplatePaginatedList, List.of());
   }

   public RealmsSelectWorldTemplateScreen(
      Text title, Consumer<WorldTemplate> callback, RealmsServer.WorldType worldType, @Nullable WorldTemplatePaginatedList templateList, List<Text> list
   ) {
      super(title);
      this.callback = callback;
      this.worldType = worldType;
      if (templateList == null) {
         this.templateList = new RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList();
         this.setPagination(new WorldTemplatePaginatedList(10));
      } else {
         this.templateList = new RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList(Lists.newArrayList(templateList.templates()));
         this.setPagination(templateList);
      }

      this.field_62460 = list;
   }

   @Override
   public void init() {
      this.layout.setHeaderHeight(33 + this.field_62460.size() * (9 + 4));
      DirectionalLayoutWidget directionalLayoutWidget = this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(4));
      directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
      directionalLayoutWidget.add(new TextWidget(this.title, this.textRenderer));
      this.field_62460.forEach(text -> directionalLayoutWidget.add(new TextWidget(text, this.textRenderer)));
      this.templateList = this.layout.addBody(new RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList(this.templateList.getValues()));
      DirectionalLayoutWidget directionalLayoutWidget2 = this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
      directionalLayoutWidget2.getMainPositioner().alignHorizontalCenter();
      this.trailerButton = directionalLayoutWidget2.add(ButtonWidget.builder(TRAILER_TEXT, button -> this.onTrailer()).width(100).build());
      this.selectButton = directionalLayoutWidget2.add(ButtonWidget.builder(SELECT_TEXT, button -> this.selectTemplate()).width(100).build());
      directionalLayoutWidget2.add(ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close()).width(100).build());
      this.publisherButton = directionalLayoutWidget2.add(ButtonWidget.builder(PUBLISHER_TEXT, button -> this.onPublish()).width(100).build());
      this.updateButtonStates();
      this.layout.forEachChild(child -> {
         ClickableWidget var10000 = this.addDrawableChild(child);
      });
      this.refreshWidgetPositions();
   }

   @Override
   protected void refreshWidgetPositions() {
      this.templateList.position(this.width, this.layout);
      this.layout.refreshPositions();
   }

   @Override
   public Text getNarratedTitle() {
      List<Text> list = Lists.newArrayListWithCapacity(2);
      list.add(this.title);
      list.addAll(this.field_62460);
      return ScreenTexts.joinLines(list);
   }

   void updateButtonStates() {
      this.publisherButton.visible = this.selectedTemplate != null && !this.selectedTemplate.link().isEmpty();
      this.trailerButton.visible = this.selectedTemplate != null && !this.selectedTemplate.trailer().isEmpty();
      this.selectButton.active = this.selectedTemplate != null;
   }

   @Override
   public void close() {
      this.callback.accept(null);
   }

   private void selectTemplate() {
      if (this.selectedTemplate != null) {
         this.callback.accept(this.selectedTemplate);
      }
   }

   private void onTrailer() {
      if (this.selectedTemplate != null && !this.selectedTemplate.trailer().isBlank()) {
         ConfirmLinkScreen.open(this, this.selectedTemplate.trailer());
      }
   }

   private void onPublish() {
      if (this.selectedTemplate != null && !this.selectedTemplate.link().isBlank()) {
         ConfirmLinkScreen.open(this, this.selectedTemplate.link());
      }
   }

   private void setPagination(WorldTemplatePaginatedList templateList) {
      (new Thread("realms-template-fetcher") {
            @Override
            public void run() {
               WorldTemplatePaginatedList worldTemplatePaginatedList = templateList;
               RealmsClient realmsClient = RealmsClient.create();

               while (worldTemplatePaginatedList != null) {
                  Either<WorldTemplatePaginatedList, Exception> either = RealmsSelectWorldTemplateScreen.this.fetchWorldTemplates(
                     worldTemplatePaginatedList, realmsClient
                  );
                  worldTemplatePaginatedList = RealmsSelectWorldTemplateScreen.this.client
                     .submit(
                        () -> {
                           if (either.right().isPresent()) {
                              RealmsSelectWorldTemplateScreen.LOGGER.error("Couldn't fetch templates", (Throwable)either.right().get());
                              if (RealmsSelectWorldTemplateScreen.this.templateList.isEmpty()) {
                                 RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(
                                    I18n.translate("mco.template.select.failure")
                                 );
                              }

                              return null;
                           } else {
                              WorldTemplatePaginatedList worldTemplatePaginatedListx = (WorldTemplatePaginatedList)either.left().get();

                              for (WorldTemplate worldTemplate : worldTemplatePaginatedListx.templates()) {
                                 RealmsSelectWorldTemplateScreen.this.templateList.addEntry(worldTemplate);
                              }

                              if (worldTemplatePaginatedListx.templates().isEmpty()) {
                                 if (RealmsSelectWorldTemplateScreen.this.templateList.isEmpty()) {
                                    String string = I18n.translate("mco.template.select.none", "%link");
                                    TextRenderingUtils.LineSegment lineSegment = TextRenderingUtils.LineSegment.link(
                                       I18n.translate("mco.template.select.none.linkTitle"), Urls.REALMS_CONTENT_CREATOR.toString()
                                    );
                                    RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(string, lineSegment);
                                 }

                                 return null;
                              } else {
                                 return worldTemplatePaginatedListx;
                              }
                           }
                        }
                     )
                     .join();
               }
            }
         })
         .start();
   }

   Either<WorldTemplatePaginatedList, Exception> fetchWorldTemplates(WorldTemplatePaginatedList templateList, RealmsClient realms) {
      try {
         return Either.left(realms.fetchWorldTemplates(templateList.page() + 1, templateList.size(), this.worldType));
      } catch (RealmsServiceException var4) {
         return Either.right(var4);
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      this.currentLink = null;
      if (this.noTemplatesMessage != null) {
         this.renderMessages(context, mouseX, mouseY, this.noTemplatesMessage);
      }
   }

   private void renderMessages(DrawContext context, int x, int y, List<TextRenderingUtils.Line> messages) {
      for (int i = 0; i < messages.size(); i++) {
         TextRenderingUtils.Line line = messages.get(i);
         int j = row(4 + i);
         int k = line.segments.stream().mapToInt(segment -> this.textRenderer.getWidth(segment.renderedText())).sum();
         int l = this.width / 2 - k / 2;

         for (TextRenderingUtils.LineSegment lineSegment : line.segments) {
            int m = lineSegment.isLink() ? -13408581 : -1;
            String string = lineSegment.renderedText();
            context.drawTextWithShadow(this.textRenderer, string, l, j, m);
            int n = l + this.textRenderer.getWidth(string);
            if (lineSegment.isLink() && x > l && x < n && y > j - 3 && y < j + 8) {
               context.drawTooltip(Text.literal(lineSegment.getLinkUrl()), x, y);
               this.currentLink = lineSegment.getLinkUrl();
            }

            l = n;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   class WorldTemplateObjectSelectionList extends AlwaysSelectedEntryListWidget<RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionListEntry> {
      public WorldTemplateObjectSelectionList() {
         this(Collections.emptyList());
      }

      public WorldTemplateObjectSelectionList(final Iterable<WorldTemplate> templates) {
         super(
            MinecraftClient.getInstance(),
            RealmsSelectWorldTemplateScreen.this.width,
            RealmsSelectWorldTemplateScreen.this.layout.getContentHeight(),
            RealmsSelectWorldTemplateScreen.this.layout.getHeaderHeight(),
            46
         );
         templates.forEach(this::addEntry);
      }

      public void addEntry(WorldTemplate template) {
         this.addEntry(RealmsSelectWorldTemplateScreen.this.new WorldTemplateObjectSelectionListEntry(template));
      }

      @Override
      public boolean mouseClicked(Click click, boolean doubled) {
         if (RealmsSelectWorldTemplateScreen.this.currentLink != null) {
            ConfirmLinkScreen.open(RealmsSelectWorldTemplateScreen.this, RealmsSelectWorldTemplateScreen.this.currentLink);
            return true;
         } else {
            return super.mouseClicked(click, doubled);
         }
      }

      public void setSelected(RealmsSelectWorldTemplateScreen.@Nullable WorldTemplateObjectSelectionListEntry worldTemplateObjectSelectionListEntry) {
         super.setSelected(worldTemplateObjectSelectionListEntry);
         RealmsSelectWorldTemplateScreen.this.selectedTemplate = worldTemplateObjectSelectionListEntry == null
            ? null
            : worldTemplateObjectSelectionListEntry.mTemplate;
         RealmsSelectWorldTemplateScreen.this.updateButtonStates();
      }

      @Override
      public int getRowWidth() {
         return 300;
      }

      public boolean isEmpty() {
         return this.getEntryCount() == 0;
      }

      public List<WorldTemplate> getValues() {
         return this.children().stream().map(child -> child.mTemplate).collect(Collectors.toList());
      }
   }

   @Environment(EnvType.CLIENT)
   class WorldTemplateObjectSelectionListEntry
      extends AlwaysSelectedEntryListWidget.Entry<RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionListEntry> {
      private static final ButtonTextures LINK_TEXTURES = new ButtonTextures(Identifier.ofVanilla("icon/link"), Identifier.ofVanilla("icon/link_highlighted"));
      private static final ButtonTextures VIDEO_LINK_TEXTURES = new ButtonTextures(
         Identifier.ofVanilla("icon/video_link"), Identifier.ofVanilla("icon/video_link_highlighted")
      );
      private static final Text INFO_TOOLTIP_TEXT = Text.translatable("mco.template.info.tooltip");
      private static final Text TRAILER_TOOLTIP_TEXT = Text.translatable("mco.template.trailer.tooltip");
      public final WorldTemplate mTemplate;
      private @Nullable TexturedButtonWidget infoButton;
      private @Nullable TexturedButtonWidget trailerButton;

      public WorldTemplateObjectSelectionListEntry(final WorldTemplate template) {
         this.mTemplate = template;
         if (!template.link().isBlank()) {
            this.infoButton = new TexturedButtonWidget(
               15, 15, LINK_TEXTURES, ConfirmLinkScreen.opening(RealmsSelectWorldTemplateScreen.this, template.link()), INFO_TOOLTIP_TEXT
            );
            this.infoButton.setTooltip(Tooltip.of(INFO_TOOLTIP_TEXT));
         }

         if (!template.trailer().isBlank()) {
            this.trailerButton = new TexturedButtonWidget(
               15, 15, VIDEO_LINK_TEXTURES, ConfirmLinkScreen.opening(RealmsSelectWorldTemplateScreen.this, template.trailer()), TRAILER_TOOLTIP_TEXT
            );
            this.trailerButton.setTooltip(Tooltip.of(TRAILER_TOOLTIP_TEXT));
         }
      }

      @Override
      public boolean mouseClicked(Click click, boolean doubled) {
         RealmsSelectWorldTemplateScreen.this.selectedTemplate = this.mTemplate;
         RealmsSelectWorldTemplateScreen.this.updateButtonStates();
         if (doubled && this.isFocused()) {
            RealmsSelectWorldTemplateScreen.this.callback.accept(this.mTemplate);
         }

         if (this.infoButton != null) {
            this.infoButton.mouseClicked(click, doubled);
         }

         if (this.trailerButton != null) {
            this.trailerButton.mouseClicked(click, doubled);
         }

         return super.mouseClicked(click, doubled);
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
         context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            RealmsTextureManager.getTextureId(this.mTemplate.id(), this.mTemplate.image()),
            this.getContentX() + 1,
            this.getContentY() + 1 + 1,
            0.0F,
            0.0F,
            38,
            38,
            38,
            38
         );
         context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED, RealmsSelectWorldTemplateScreen.SLOT_FRAME_TEXTURE, this.getContentX(), this.getContentY() + 1, 40, 40
         );
         int i = 5;
         int j = RealmsSelectWorldTemplateScreen.this.textRenderer.getWidth(this.mTemplate.version());
         if (this.infoButton != null) {
            this.infoButton.setPosition(this.getContentRightEnd() - j - this.infoButton.getWidth() - 10, this.getContentY());
            this.infoButton.render(context, mouseX, mouseY, deltaTicks);
         }

         if (this.trailerButton != null) {
            this.trailerButton.setPosition(this.getContentRightEnd() - j - this.trailerButton.getWidth() * 2 - 15, this.getContentY());
            this.trailerButton.render(context, mouseX, mouseY, deltaTicks);
         }

         int k = this.getContentX() + 45 + 20;
         int l = this.getContentY() + 5;
         context.drawTextWithShadow(RealmsSelectWorldTemplateScreen.this.textRenderer, this.mTemplate.name(), k, l, -1);
         context.drawTextWithShadow(RealmsSelectWorldTemplateScreen.this.textRenderer, this.mTemplate.version(), this.getContentRightEnd() - j - 5, l, -6250336);
         context.drawTextWithShadow(RealmsSelectWorldTemplateScreen.this.textRenderer, this.mTemplate.author(), k, l + 9 + 5, -6250336);
         if (!this.mTemplate.recommendedPlayers().isBlank()) {
            context.drawTextWithShadow(
               RealmsSelectWorldTemplateScreen.this.textRenderer, this.mTemplate.recommendedPlayers(), k, this.getContentBottomEnd() - 9 / 2 - 5, -8355712
            );
         }
      }

      @Override
      public Text getNarration() {
         Text text = ScreenTexts.joinLines(
            Text.literal(this.mTemplate.name()),
            Text.translatable("mco.template.select.narrate.authors", this.mTemplate.author()),
            Text.literal(this.mTemplate.recommendedPlayers()),
            Text.translatable("mco.template.select.narrate.version", this.mTemplate.version())
         );
         return Text.translatable("narrator.select", text);
      }
   }
}
