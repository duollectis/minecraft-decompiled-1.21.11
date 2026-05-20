package net.minecraft.client.realms.gui.screen;

import com.mojang.logging.LogUtils;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.Backup;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.RealmsPopups;
import net.minecraft.client.realms.task.DownloadTask;
import net.minecraft.client.realms.task.RestoreTask;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsBackupScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Text BACKUPS_TEXT = Text.translatable("mco.configure.world.backup");
   static final Text RESTORE_TEXT = Text.translatable("mco.backup.button.restore");
   static final Text CHANGES_TOOLTIP = Text.translatable("mco.backup.changes.tooltip");
   private static final Text NO_BACKUPS_TEXT = Text.translatable("mco.backup.nobackups");
   private static final Text DOWNLOAD_TEXT = Text.translatable("mco.backup.button.download");
   private static final String UPLOADED = "uploaded";
   private static final int field_49447 = 8;
   public static final DateTimeFormatter FORMATTER = Util.getDefaultLocaleFormatter(FormatStyle.SHORT);
   final RealmsConfigureWorldScreen parent;
   List<Backup> backups = Collections.emptyList();
   RealmsBackupScreen.@Nullable BackupObjectSelectionList selectionList;
   final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);
   private final int slotId;
   @Nullable ButtonWidget downloadButton;
   final RealmsServer serverData;
   boolean noBackups = false;

   public RealmsBackupScreen(RealmsConfigureWorldScreen parent, RealmsServer serverData, int slotId) {
      super(BACKUPS_TEXT);
      this.parent = parent;
      this.serverData = serverData;
      this.slotId = slotId;
   }

   @Override
   public void init() {
      this.layout.addHeader(BACKUPS_TEXT, this.textRenderer);
      this.selectionList = this.layout.addBody(new RealmsBackupScreen.BackupObjectSelectionList());
      DirectionalLayoutWidget directionalLayoutWidget = this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
      this.downloadButton = directionalLayoutWidget.add(ButtonWidget.builder(DOWNLOAD_TEXT, button -> this.downloadClicked()).build());
      this.downloadButton.active = false;
      directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.BACK, button -> this.close()).build());
      this.layout.forEachChild(element -> {
         ClickableWidget var10000 = this.addDrawableChild(element);
      });
      this.refreshWidgetPositions();
      this.startBackupFetcher();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      if (this.noBackups && this.selectionList != null) {
         context.drawTextWithShadow(
            this.textRenderer,
            NO_BACKUPS_TEXT,
            this.width / 2 - this.textRenderer.getWidth(NO_BACKUPS_TEXT) / 2,
            this.selectionList.getY() + this.selectionList.getHeight() / 2 - 9 / 2,
            -1
         );
      }
   }

   @Override
   protected void refreshWidgetPositions() {
      this.layout.refreshPositions();
      if (this.selectionList != null) {
         this.selectionList.position(this.width, this.layout);
      }
   }

   private void startBackupFetcher() {
      (new Thread("Realms-fetch-backups") {
            @Override
            public void run() {
               RealmsClient realmsClient = RealmsClient.create();

               try {
                  List<Backup> list = realmsClient.backupsFor(RealmsBackupScreen.this.serverData.id).backups();
                  RealmsBackupScreen.this.client
                     .execute(
                        () -> {
                           RealmsBackupScreen.this.backups = list;
                           RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                           if (!RealmsBackupScreen.this.noBackups && RealmsBackupScreen.this.downloadButton != null) {
                              RealmsBackupScreen.this.downloadButton.active = true;
                           }

                           if (RealmsBackupScreen.this.selectionList != null) {
                              RealmsBackupScreen.this.selectionList
                                 .replaceEntries(
                                    RealmsBackupScreen.this.backups
                                       .stream()
                                       .map(backup -> RealmsBackupScreen.this.new BackupObjectSelectionListEntry(backup))
                                       .toList()
                                 );
                           }
                        }
                     );
               } catch (RealmsServiceException var3) {
                  RealmsBackupScreen.LOGGER.error("Couldn't request backups", var3);
               }
            }
         })
         .start();
   }

   @Override
   public void close() {
      this.client.setScreen(this.parent);
   }

   private void downloadClicked() {
      this.client
         .setScreen(
            RealmsPopups.createInfoPopup(
               this,
               Text.translatable("mco.configure.world.restore.download.question.line1"),
               button -> this.client
                  .setScreen(
                     new RealmsLongRunningMcoTaskScreen(
                        this.parent.getNewScreen(),
                        new DownloadTask(
                           this.serverData.id,
                           this.slotId,
                           Objects.requireNonNullElse(this.serverData.name, "")
                              + " ("
                              + this.serverData.slots.get(this.serverData.activeSlot).options.getSlotName(this.serverData.activeSlot)
                              + ")",
                           this
                        )
                     )
                  )
            )
         );
   }

   @Environment(EnvType.CLIENT)
   class BackupObjectSelectionList extends ElementListWidget<RealmsBackupScreen.BackupObjectSelectionListEntry> {
      private static final int field_49450 = 36;

      public BackupObjectSelectionList() {
         super(
            MinecraftClient.getInstance(),
            RealmsBackupScreen.this.width,
            RealmsBackupScreen.this.layout.getContentHeight(),
            RealmsBackupScreen.this.layout.getHeaderHeight(),
            36
         );
      }

      @Override
      public int getRowWidth() {
         return 300;
      }
   }

   @Environment(EnvType.CLIENT)
   class BackupObjectSelectionListEntry extends ElementListWidget.Entry<RealmsBackupScreen.BackupObjectSelectionListEntry> {
      private static final int field_44525 = 2;
      private final Backup mBackup;
      private @Nullable ButtonWidget restoreButton;
      private @Nullable ButtonWidget changesButton;
      private final List<ClickableWidget> buttons = new ArrayList<>();

      public BackupObjectSelectionListEntry(final Backup backup) {
         this.mBackup = backup;
         this.updateChangeList(backup);
         if (!backup.changeList.isEmpty()) {
            this.changesButton = ButtonWidget.builder(
                  RealmsBackupScreen.CHANGES_TOOLTIP,
                  button -> RealmsBackupScreen.this.client.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, this.mBackup))
               )
               .width(8 + RealmsBackupScreen.this.textRenderer.getWidth(RealmsBackupScreen.CHANGES_TOOLTIP))
               .narrationSupplier(this::getNarration)
               .build();
            this.buttons.add(this.changesButton);
         }

         if (!RealmsBackupScreen.this.serverData.expired) {
            this.restoreButton = ButtonWidget.builder(RealmsBackupScreen.RESTORE_TEXT, button -> this.restore())
               .width(8 + RealmsBackupScreen.this.textRenderer.getWidth(RealmsBackupScreen.CHANGES_TOOLTIP))
               .narrationSupplier(this::getNarration)
               .build();
            this.buttons.add(this.restoreButton);
         }
      }

      private MutableText getNarration(Supplier<MutableText> messageSupplier) {
         return ScreenTexts.joinSentences(
            Text.translatable("mco.backup.narration", RealmsBackupScreen.FORMATTER.format(this.mBackup.getLastModifiedTime())), messageSupplier.get()
         );
      }

      private void updateChangeList(Backup backup) {
         int i = RealmsBackupScreen.this.backups.indexOf(backup);
         if (i != RealmsBackupScreen.this.backups.size() - 1) {
            Backup backup2 = RealmsBackupScreen.this.backups.get(i + 1);

            for (String string : backup.metadata.keySet()) {
               if (!string.contains("uploaded") && backup2.metadata.containsKey(string)) {
                  if (!backup.metadata.get(string).equals(backup2.metadata.get(string))) {
                     this.addChange(string);
                  }
               } else {
                  this.addChange(string);
               }
            }
         }
      }

      private void addChange(String metadataKey) {
         if (metadataKey.contains("uploaded")) {
            String string = RealmsBackupScreen.FORMATTER.format(this.mBackup.getLastModifiedTime());
            this.mBackup.changeList.put(metadataKey, string);
            this.mBackup.uploadedVersion = true;
         } else {
            this.mBackup.changeList.put(metadataKey, this.mBackup.metadata.get(metadataKey));
         }
      }

      private void restore() {
         Text text = RealmsUtil.convertToAgePresentation(this.mBackup.lastModifiedDate);
         String string = RealmsBackupScreen.FORMATTER.format(this.mBackup.getLastModifiedTime());
         Text text2 = Text.translatable("mco.configure.world.restore.question.line1", string, text);
         RealmsBackupScreen.this.client
            .setScreen(
               RealmsPopups.createContinuableWarningPopup(
                  RealmsBackupScreen.this,
                  text2,
                  popup -> {
                     RealmsConfigureWorldScreen realmsConfigureWorldScreen = RealmsBackupScreen.this.parent.getNewScreen();
                     RealmsBackupScreen.this.client
                        .setScreen(
                           new RealmsLongRunningMcoTaskScreen(
                              realmsConfigureWorldScreen, new RestoreTask(this.mBackup, RealmsBackupScreen.this.serverData.id, realmsConfigureWorldScreen)
                           )
                        );
                  }
               )
            );
      }

      @Override
      public List<? extends Element> children() {
         return this.buttons;
      }

      @Override
      public List<? extends Selectable> selectableChildren() {
         return this.buttons;
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
         int i = this.getContentMiddleY();
         int j = i - 9 - 2;
         int k = i + 2;
         int l = this.mBackup.uploadedVersion ? -8388737 : -1;
         context.drawTextWithShadow(
            RealmsBackupScreen.this.textRenderer,
            Text.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentation(this.mBackup.lastModifiedDate)),
            this.getContentX(),
            j,
            l
         );
         context.drawTextWithShadow(
            RealmsBackupScreen.this.textRenderer, RealmsBackupScreen.FORMATTER.format(this.mBackup.getLastModifiedTime()), this.getContentX(), k, -11776948
         );
         int m = 0;
         int n = this.getContentMiddleY() - 10;
         if (this.restoreButton != null) {
            m += this.restoreButton.getWidth() + 8;
            this.restoreButton.setX(this.getContentRightEnd() - m);
            this.restoreButton.setY(n);
            this.restoreButton.render(context, mouseX, mouseY, deltaTicks);
         }

         if (this.changesButton != null) {
            m += this.changesButton.getWidth() + 8;
            this.changesButton.setX(this.getContentRightEnd() - m);
            this.changesButton.setY(n);
            this.changesButton.render(context, mouseX, mouseY, deltaTicks);
         }
      }
   }
}
