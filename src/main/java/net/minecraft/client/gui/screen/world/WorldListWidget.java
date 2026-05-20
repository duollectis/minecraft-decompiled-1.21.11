package net.minecraft.client.gui.screen.world;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.SquareWidgetEntry;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.nbt.NbtCrashException;
import net.minecraft.nbt.NbtException;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.path.SymlinkEntry;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class WorldListWidget extends AlwaysSelectedEntryListWidget<WorldListWidget.Entry> {
   public static final DateTimeFormatter DATE_FORMAT = Util.getDefaultLocaleFormatter(FormatStyle.SHORT);
   static final Identifier ERROR_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("world_list/error_highlighted");
   static final Identifier ERROR_TEXTURE = Identifier.ofVanilla("world_list/error");
   static final Identifier MARKED_JOIN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("world_list/marked_join_highlighted");
   static final Identifier MARKED_JOIN_TEXTURE = Identifier.ofVanilla("world_list/marked_join");
   static final Identifier WARNING_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("world_list/warning_highlighted");
   static final Identifier WARNING_TEXTURE = Identifier.ofVanilla("world_list/warning");
   static final Identifier JOIN_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("world_list/join_highlighted");
   static final Identifier JOIN_TEXTURE = Identifier.ofVanilla("world_list/join");
   static final Logger LOGGER = LogUtils.getLogger();
   static final Text FROM_NEWER_VERSION_FIRST_LINE = Text.translatable("selectWorld.tooltip.fromNewerVersion1").formatted(Formatting.RED);
   static final Text FROM_NEWER_VERSION_SECOND_LINE = Text.translatable("selectWorld.tooltip.fromNewerVersion2").formatted(Formatting.RED);
   static final Text SNAPSHOT_FIRST_LINE = Text.translatable("selectWorld.tooltip.snapshot1").formatted(Formatting.GOLD);
   static final Text SNAPSHOT_SECOND_LINE = Text.translatable("selectWorld.tooltip.snapshot2").formatted(Formatting.GOLD);
   static final Text LOCKED_TEXT = Text.translatable("selectWorld.locked").formatted(Formatting.RED);
   static final Text CONVERSION_TOOLTIP = Text.translatable("selectWorld.conversion.tooltip").formatted(Formatting.RED);
   static final Text INCOMPATIBLE_TOOLTIP = Text.translatable("selectWorld.incompatible.tooltip").formatted(Formatting.RED);
   static final Text EXPERIMENTAL_TEXT = Text.translatable("selectWorld.experimental");
   private final Screen parent;
   private CompletableFuture<List<LevelSummary>> levelsFuture;
   private @Nullable List<LevelSummary> levels;
   private final WorldListWidget.LoadingEntry loadingEntry;
   final WorldListWidget.WorldListType worldListType;
   private String search;
   private boolean failedToGetLevels;
   private final @Nullable Consumer<LevelSummary> selectionCallback;
   final @Nullable Consumer<WorldListWidget.WorldEntry> confirmationCallback;

   WorldListWidget(
      Screen parent,
      MinecraftClient client,
      int width,
      int height,
      String search,
      @Nullable WorldListWidget predecessor,
      @Nullable Consumer<LevelSummary> selectionCallback,
      @Nullable Consumer<WorldListWidget.WorldEntry> confirmationCallback,
      WorldListWidget.WorldListType worldListType
   ) {
      super(client, width, height, 0, 36);
      this.parent = parent;
      this.loadingEntry = new WorldListWidget.LoadingEntry(client);
      this.search = search;
      this.selectionCallback = selectionCallback;
      this.confirmationCallback = confirmationCallback;
      this.worldListType = worldListType;
      if (predecessor != null) {
         this.levelsFuture = predecessor.levelsFuture;
      } else {
         this.levelsFuture = this.loadLevels();
      }

      this.addEntry(this.loadingEntry);
      this.show(this.tryGet());
   }

   @Override
   protected void clearEntries() {
      this.children().forEach(WorldListWidget.Entry::close);
      super.clearEntries();
   }

   private @Nullable List<LevelSummary> tryGet() {
      try {
         List<LevelSummary> list = this.levelsFuture.getNow(null);
         if (this.worldListType == WorldListWidget.WorldListType.UPLOAD_WORLD) {
            if (list == null || this.failedToGetLevels) {
               return null;
            }

            this.failedToGetLevels = true;
            list = list.stream().filter(LevelSummary::isImmediatelyLoadable).toList();
         }

         return list;
      } catch (CancellationException | CompletionException var2) {
         return null;
      }
   }

   public void load() {
      this.levelsFuture = this.loadLevels();
   }

   @Override
   public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      List<LevelSummary> list = this.tryGet();
      if (list != this.levels) {
         this.show(list);
      }

      super.renderWidget(context, mouseX, mouseY, deltaTicks);
   }

   private void show(@Nullable List<LevelSummary> summaries) {
      if (summaries != null) {
         if (summaries.isEmpty()) {
            switch (this.worldListType) {
               case SINGLEPLAYER:
                  CreateWorldScreen.show(this.client, () -> this.client.setScreen(null));
                  break;
               case UPLOAD_WORLD:
                  this.clearEntries();
                  this.addEntry(new WorldListWidget.EmptyListEntry(Text.translatable("mco.upload.select.world.none"), this.parent.getTextRenderer()));
            }
         } else {
            this.showSummaries(this.search, summaries);
            this.levels = summaries;
         }
      }
   }

   public void setSearch(String search) {
      if (this.levels != null && !search.equals(this.search)) {
         this.showSummaries(search, this.levels);
      }

      this.search = search;
   }

   private CompletableFuture<List<LevelSummary>> loadLevels() {
      LevelStorage.LevelList levelList;
      try {
         levelList = this.client.getLevelStorage().getLevelList();
      } catch (LevelStorageException var3) {
         LOGGER.error("Couldn't load level list", var3);
         this.showUnableToLoadScreen(var3.getMessageText());
         return CompletableFuture.completedFuture(List.of());
      }

      return this.client.getLevelStorage().loadSummaries(levelList).exceptionally(throwable -> {
         this.client.setCrashReportSupplierAndAddDetails(CrashReport.create(throwable, "Couldn't load level list"));
         return List.of();
      });
   }

   private void showSummaries(String search, List<LevelSummary> summaries) {
      List<WorldListWidget.Entry> list = new ArrayList<>();
      Optional<WorldListWidget.WorldEntry> optional = this.getSelectedAsOptional();
      WorldListWidget.WorldEntry worldEntry = null;

      for (LevelSummary levelSummary : summaries.stream().filter(summary -> this.shouldShow(search.toLowerCase(Locale.ROOT), summary)).toList()) {
         WorldListWidget.WorldEntry worldEntry2 = new WorldListWidget.WorldEntry(this, levelSummary);
         if (optional.isPresent() && optional.get().getLevel().getName().equals(worldEntry2.getLevel().getName())) {
            worldEntry = worldEntry2;
         }

         list.add(worldEntry2);
      }

      this.removeEntries(this.children().stream().filter(child -> !list.contains(child)).toList());
      list.forEach(entry -> {
         if (!this.children().contains(entry)) {
            this.addEntry(entry);
         }
      });
      this.setSelected((WorldListWidget.Entry)worldEntry);
      this.narrateScreenIfNarrationEnabled();
   }

   private boolean shouldShow(String search, LevelSummary summary) {
      return summary.getDisplayName().toLowerCase(Locale.ROOT).contains(search) || summary.getName().toLowerCase(Locale.ROOT).contains(search);
   }

   private void narrateScreenIfNarrationEnabled() {
      this.refreshScroll();
      this.parent.narrateScreenIfNarrationEnabled(true);
   }

   private void showUnableToLoadScreen(Text message) {
      this.client.setScreen(new FatalErrorScreen(Text.translatable("selectWorld.unable_to_load"), message));
   }

   @Override
   public int getRowWidth() {
      return 270;
   }

   public void setSelected(WorldListWidget.@Nullable Entry entry) {
      super.setSelected(entry);
      if (this.selectionCallback != null) {
         this.selectionCallback.accept(entry instanceof WorldListWidget.WorldEntry worldEntry ? worldEntry.level : null);
      }
   }

   public Optional<WorldListWidget.WorldEntry> getSelectedAsOptional() {
      WorldListWidget.Entry entry = this.getSelectedOrNull();
      return entry instanceof WorldListWidget.WorldEntry worldEntry ? Optional.of(worldEntry) : Optional.empty();
   }

   public void refresh() {
      this.load();
      this.client.setScreen(this.parent);
   }

   public Screen getParent() {
      return this.parent;
   }

   @Override
   public void appendClickableNarrations(NarrationMessageBuilder builder) {
      if (this.children().contains(this.loadingEntry)) {
         this.loadingEntry.appendNarrations(builder);
      } else {
         super.appendClickableNarrations(builder);
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Builder {
      private final MinecraftClient client;
      private final Screen parent;
      private int width;
      private int height;
      private String search = "";
      private WorldListWidget.WorldListType worldListType = WorldListWidget.WorldListType.SINGLEPLAYER;
      private @Nullable WorldListWidget predecessor = null;
      private @Nullable Consumer<LevelSummary> selectionCallback = null;
      private @Nullable Consumer<WorldListWidget.WorldEntry> confirmationCallback = null;

      public Builder(MinecraftClient client, Screen parent) {
         this.client = client;
         this.parent = parent;
      }

      public WorldListWidget.Builder width(int width) {
         this.width = width;
         return this;
      }

      public WorldListWidget.Builder height(int height) {
         this.height = height;
         return this;
      }

      public WorldListWidget.Builder search(String search) {
         this.search = search;
         return this;
      }

      public WorldListWidget.Builder predecessor(@Nullable WorldListWidget predecessor) {
         this.predecessor = predecessor;
         return this;
      }

      public WorldListWidget.Builder selectionCallback(Consumer<LevelSummary> selectionCallback) {
         this.selectionCallback = selectionCallback;
         return this;
      }

      public WorldListWidget.Builder confirmationCallback(Consumer<WorldListWidget.WorldEntry> confirmationCallback) {
         this.confirmationCallback = confirmationCallback;
         return this;
      }

      public WorldListWidget.Builder uploadWorld() {
         this.worldListType = WorldListWidget.WorldListType.UPLOAD_WORLD;
         return this;
      }

      public WorldListWidget toWidget() {
         return new WorldListWidget(
            this.parent,
            this.client,
            this.width,
            this.height,
            this.search,
            this.predecessor,
            this.selectionCallback,
            this.confirmationCallback,
            this.worldListType
         );
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class EmptyListEntry extends WorldListWidget.Entry {
      private final TextWidget widget;

      public EmptyListEntry(Text text, TextRenderer textRenderer) {
         this.widget = new TextWidget(text, textRenderer);
      }

      @Override
      public Text getNarration() {
         return this.widget.getMessage();
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
         this.widget.setPosition(this.getContentMiddleX() - this.widget.getWidth() / 2, this.getContentMiddleY() - this.widget.getHeight() / 2);
         this.widget.render(context, mouseX, mouseY, deltaTicks);
      }
   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<WorldListWidget.Entry> implements AutoCloseable {
      @Override
      public void close() {
      }

      public @Nullable LevelSummary getLevel() {
         return null;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class LoadingEntry extends WorldListWidget.Entry {
      private static final Text LOADING_LIST_TEXT = Text.translatable("selectWorld.loading_list");
      private final MinecraftClient client;

      public LoadingEntry(MinecraftClient client) {
         this.client = client;
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
         int i = (this.client.currentScreen.width - this.client.textRenderer.getWidth(LOADING_LIST_TEXT)) / 2;
         int j = this.getContentY() + (this.getContentHeight() - 9) / 2;
         context.drawTextWithShadow(this.client.textRenderer, LOADING_LIST_TEXT, i, j, -1);
         String string = LoadingDisplay.get(Util.getMeasuringTimeMs());
         int k = (this.client.currentScreen.width - this.client.textRenderer.getWidth(string)) / 2;
         int l = j + 9;
         context.drawTextWithShadow(this.client.textRenderer, string, k, l, -8355712);
      }

      @Override
      public Text getNarration() {
         return LOADING_LIST_TEXT;
      }
   }

   @Environment(EnvType.CLIENT)
   public final class WorldEntry extends WorldListWidget.Entry implements SquareWidgetEntry {
      private static final int field_64210 = 32;
      private final WorldListWidget parent;
      private final MinecraftClient client;
      private final Screen screen;
      final LevelSummary level;
      private final WorldIcon icon;
      private final TextWidget displayNameWidget;
      private final TextWidget nameWidget;
      private final TextWidget detailsWidget;
      private @Nullable Path iconPath;

      public WorldEntry(final WorldListWidget parent, final LevelSummary level) {
         this.parent = parent;
         this.client = parent.client;
         this.screen = parent.getParent();
         this.level = level;
         this.icon = WorldIcon.forWorld(this.client.getTextureManager(), level.getName());
         this.iconPath = level.getIconPath();
         int i = parent.getRowWidth() - this.getTextX() - 2;
         Text text = Text.literal(level.getDisplayName());
         this.displayNameWidget = new TextWidget(text, this.client.textRenderer);
         this.displayNameWidget.setMaxWidth(i);
         if (this.client.textRenderer.getWidth(text) > i) {
            this.displayNameWidget.setTooltip(Tooltip.of(text));
         }

         String string = level.getName();
         long l = level.getLastPlayed();
         if (l != -1L) {
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
            string = string + " (" + WorldListWidget.DATE_FORMAT.format(zonedDateTime) + ")";
         }

         Text text2 = Text.literal(string).withColor(-8355712);
         this.nameWidget = new TextWidget(text2, this.client.textRenderer);
         this.nameWidget.setMaxWidth(i);
         if (this.client.textRenderer.getWidth(string) > i) {
            this.nameWidget.setTooltip(Tooltip.of(text2));
         }

         Text text3 = Texts.withStyle(level.getDetails(), Style.EMPTY.withColor(-8355712));
         this.detailsWidget = new TextWidget(text3, this.client.textRenderer);
         this.detailsWidget.setMaxWidth(i);
         if (this.client.textRenderer.getWidth(text3) > i) {
            this.detailsWidget.setTooltip(Tooltip.of(text3));
         }

         this.validateIconPath();
         this.loadIcon();
      }

      private void validateIconPath() {
         if (this.iconPath != null) {
            try {
               BasicFileAttributes basicFileAttributes = Files.readAttributes(this.iconPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
               if (basicFileAttributes.isSymbolicLink()) {
                  List<SymlinkEntry> list = this.client.getSymlinkFinder().validate(this.iconPath);
                  if (!list.isEmpty()) {
                     WorldListWidget.LOGGER.warn("{}", SymlinkValidationException.getMessage(this.iconPath, list));
                     this.iconPath = null;
                  } else {
                     basicFileAttributes = Files.readAttributes(this.iconPath, BasicFileAttributes.class);
                  }
               }

               if (!basicFileAttributes.isRegularFile()) {
                  this.iconPath = null;
               }
            } catch (NoSuchFileException var3) {
               this.iconPath = null;
            } catch (IOException var4) {
               WorldListWidget.LOGGER.error("could not validate symlink", var4);
               this.iconPath = null;
            }
         }
      }

      @Override
      public Text getNarration() {
         Text text = Text.translatable(
            "narrator.select.world_info", this.level.getDisplayName(), Text.of(new Date(this.level.getLastPlayed())), this.level.getDetails()
         );
         if (this.level.isLocked()) {
            text = ScreenTexts.joinSentences(text, WorldListWidget.LOCKED_TEXT);
         }

         if (this.level.isExperimental()) {
            text = ScreenTexts.joinSentences(text, WorldListWidget.EXPERIMENTAL_TEXT);
         }

         return Text.translatable("narrator.select", text);
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
         int i = this.getTextX();
         this.displayNameWidget.setPosition(i, this.getContentY() + 1);
         this.displayNameWidget.render(context, mouseX, mouseY, deltaTicks);
         this.nameWidget.setPosition(i, this.getContentY() + 9 + 3);
         this.nameWidget.render(context, mouseX, mouseY, deltaTicks);
         this.detailsWidget.setPosition(i, this.getContentY() + 9 + 9 + 3);
         this.detailsWidget.render(context, mouseX, mouseY, deltaTicks);
         context.drawTexture(RenderPipelines.GUI_TEXTURED, this.icon.getTextureId(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
         if (this.parent.worldListType == WorldListWidget.WorldListType.SINGLEPLAYER && (this.client.options.getTouchscreen().getValue() || hovered)) {
            context.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
            int j = mouseX - this.getContentX();
            int k = mouseY - this.getContentY();
            boolean bl = this.isInside(j, k, 32);
            Identifier identifier = bl ? WorldListWidget.JOIN_HIGHLIGHTED_TEXTURE : WorldListWidget.JOIN_TEXTURE;
            Identifier identifier2 = bl ? WorldListWidget.WARNING_HIGHLIGHTED_TEXTURE : WorldListWidget.WARNING_TEXTURE;
            Identifier identifier3 = bl ? WorldListWidget.ERROR_HIGHLIGHTED_TEXTURE : WorldListWidget.ERROR_TEXTURE;
            Identifier identifier4 = bl ? WorldListWidget.MARKED_JOIN_HIGHLIGHTED_TEXTURE : WorldListWidget.MARKED_JOIN_TEXTURE;
            if (this.level instanceof LevelSummary.SymlinkLevelSummary || this.level instanceof LevelSummary.RecoveryWarning) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier4, this.getContentX(), this.getContentY(), 32, 32);
               return;
            }

            if (this.level.isLocked()) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  context.drawTooltip(this.client.textRenderer.wrapLines(WorldListWidget.LOCKED_TEXT, 175), mouseX, mouseY);
               }
            } else if (this.level.requiresConversion()) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  context.drawTooltip(this.client.textRenderer.wrapLines(WorldListWidget.CONVERSION_TOOLTIP, 175), mouseX, mouseY);
               }
            } else if (!this.level.isVersionAvailable()) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  context.drawTooltip(this.client.textRenderer.wrapLines(WorldListWidget.INCOMPATIBLE_TOOLTIP, 175), mouseX, mouseY);
               }
            } else if (this.level.shouldPromptBackup()) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier4, this.getContentX(), this.getContentY(), 32, 32);
               if (this.level.wouldBeDowngraded()) {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier3, this.getContentX(), this.getContentY(), 32, 32);
                  if (bl) {
                     context.drawTooltip(
                        ImmutableList.of(
                           WorldListWidget.FROM_NEWER_VERSION_FIRST_LINE.asOrderedText(), WorldListWidget.FROM_NEWER_VERSION_SECOND_LINE.asOrderedText()
                        ),
                        mouseX,
                        mouseY
                     );
                  }
               } else if (!SharedConstants.getGameVersion().stable()) {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier2, this.getContentX(), this.getContentY(), 32, 32);
                  if (bl) {
                     context.drawTooltip(
                        ImmutableList.of(WorldListWidget.SNAPSHOT_FIRST_LINE.asOrderedText(), WorldListWidget.SNAPSHOT_SECOND_LINE.asOrderedText()),
                        mouseX,
                        mouseY
                     );
                  }
               }

               if (bl) {
                  WorldListWidget.this.setCursor(context);
               }
            } else {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, this.getContentX(), this.getContentY(), 32, 32);
               if (bl) {
                  WorldListWidget.this.setCursor(context);
               }
            }
         }
      }

      private int getTextX() {
         return this.getContentX() + 32 + 3;
      }

      @Override
      public boolean mouseClicked(Click click, boolean doubled) {
         if (this.allowConfirmationByKeyboard()) {
            int i = (int)click.x() - this.getContentX();
            int j = (int)click.y() - this.getContentY();
            if (doubled || this.isInside(i, j, 32) && this.parent.worldListType == WorldListWidget.WorldListType.SINGLEPLAYER) {
               this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               Consumer<WorldListWidget.WorldEntry> consumer = this.parent.confirmationCallback;
               if (consumer != null) {
                  consumer.accept(this);
                  return true;
               }
            }
         }

         return super.mouseClicked(click, doubled);
      }

      @Override
      public boolean keyPressed(KeyInput input) {
         if (input.isEnterOrSpace() && this.allowConfirmationByKeyboard()) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Consumer<WorldListWidget.WorldEntry> consumer = this.parent.confirmationCallback;
            if (consumer != null) {
               consumer.accept(this);
               return true;
            }
         }

         return super.keyPressed(input);
      }

      public boolean allowConfirmationByKeyboard() {
         return this.level.isSelectable() || this.parent.worldListType == WorldListWidget.WorldListType.UPLOAD_WORLD;
      }

      public void play() {
         if (this.level.isSelectable()) {
            if (this.level instanceof LevelSummary.SymlinkLevelSummary) {
               this.client.setScreen(SymlinkWarningScreen.world(() -> this.client.setScreen(this.screen)));
            } else {
               this.client.createIntegratedServerLoader().start(this.level.getName(), this.parent::refresh);
            }
         }
      }

      public void deleteIfConfirmed() {
         this.client
            .setScreen(
               new ConfirmScreen(
                  confirmed -> {
                     if (confirmed) {
                        this.client.setScreen(new ProgressScreen(true));
                        this.delete();
                     }

                     this.parent.refresh();
                  },
                  Text.translatable("selectWorld.deleteQuestion"),
                  Text.translatable("selectWorld.deleteWarning", this.level.getDisplayName()),
                  Text.translatable("selectWorld.deleteButton"),
                  ScreenTexts.CANCEL
               )
            );
      }

      public void delete() {
         LevelStorage levelStorage = this.client.getLevelStorage();
         String string = this.level.getName();

         try (LevelStorage.Session session = levelStorage.createSessionWithoutSymlinkCheck(string)) {
            session.deleteSessionLock();
         } catch (IOException var8) {
            SystemToast.addWorldDeleteFailureToast(this.client, string);
            WorldListWidget.LOGGER.error("Failed to delete world {}", string, var8);
         }
      }

      public void edit() {
         this.openReadingWorldScreen();
         String string = this.level.getName();

         LevelStorage.Session session;
         try {
            session = this.client.getLevelStorage().createSession(string);
         } catch (IOException var6) {
            SystemToast.addWorldAccessFailureToast(this.client, string);
            WorldListWidget.LOGGER.error("Failed to access level {}", string, var6);
            this.parent.load();
            return;
         } catch (SymlinkValidationException var7) {
            WorldListWidget.LOGGER.warn("{}", var7.getMessage());
            this.client.setScreen(SymlinkWarningScreen.world(() -> this.client.setScreen(this.screen)));
            return;
         }

         EditWorldScreen editWorldScreen;
         try {
            editWorldScreen = EditWorldScreen.create(this.client, session, edited -> {
               session.tryClose();
               this.parent.refresh();
            });
         } catch (NbtException | NbtCrashException | IOException var5) {
            session.tryClose();
            SystemToast.addWorldAccessFailureToast(this.client, string);
            WorldListWidget.LOGGER.error("Failed to load world data {}", string, var5);
            this.parent.load();
            return;
         }

         this.client.setScreen(editWorldScreen);
      }

      public void recreate() {
         this.openReadingWorldScreen();

         try (LevelStorage.Session session = this.client.getLevelStorage().createSession(this.level.getName())) {
            Pair<LevelInfo, GeneratorOptionsHolder> pair = this.client.createIntegratedServerLoader().loadForRecreation(session);
            LevelInfo levelInfo = (LevelInfo)pair.getFirst();
            GeneratorOptionsHolder generatorOptionsHolder = (GeneratorOptionsHolder)pair.getSecond();
            Path path = CreateWorldScreen.copyDataPack(session.getDirectory(WorldSavePath.DATAPACKS), this.client);
            generatorOptionsHolder.initializeIndexedFeaturesLists();
            if (generatorOptionsHolder.generatorOptions().isLegacyCustomizedType()) {
               this.client
                  .setScreen(
                     new ConfirmScreen(
                        confirmed -> this.client
                           .setScreen(
                              (Screen)(confirmed
                                 ? CreateWorldScreen.create(this.client, this.parent::refresh, levelInfo, generatorOptionsHolder, path)
                                 : this.screen)
                           ),
                        Text.translatable("selectWorld.recreate.customized.title"),
                        Text.translatable("selectWorld.recreate.customized.text"),
                        ScreenTexts.PROCEED,
                        ScreenTexts.CANCEL
                     )
                  );
            } else {
               this.client.setScreen(CreateWorldScreen.create(this.client, this.parent::refresh, levelInfo, generatorOptionsHolder, path));
            }
         } catch (SymlinkValidationException var8) {
            WorldListWidget.LOGGER.warn("{}", var8.getMessage());
            this.client.setScreen(SymlinkWarningScreen.world(() -> this.client.setScreen(this.screen)));
         } catch (Exception var9) {
            WorldListWidget.LOGGER.error("Unable to recreate world", var9);
            this.client
               .setScreen(
                  new NoticeScreen(
                     () -> this.client.setScreen(this.screen),
                     Text.translatable("selectWorld.recreate.error.title"),
                     Text.translatable("selectWorld.recreate.error.text")
                  )
               );
         }
      }

      private void openReadingWorldScreen() {
         this.client.setScreenAndRender(new MessageScreen(Text.translatable("selectWorld.data_read")));
      }

      private void loadIcon() {
         boolean bl = this.iconPath != null && Files.isRegularFile(this.iconPath);
         if (bl) {
            try (InputStream inputStream = Files.newInputStream(this.iconPath)) {
               this.icon.load(NativeImage.read(inputStream));
            } catch (Throwable var7) {
               WorldListWidget.LOGGER.error("Invalid icon for world {}", this.level.getName(), var7);
               this.iconPath = null;
            }
         } else {
            this.icon.destroy();
         }
      }

      @Override
      public void close() {
         if (!this.icon.isClosed()) {
            this.icon.close();
         }
      }

      public String getLevelDisplayName() {
         return this.level.getDisplayName();
      }

      @Override
      public LevelSummary getLevel() {
         return this.level;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum WorldListType {
      SINGLEPLAYER,
      UPLOAD_WORLD;
   }
}
