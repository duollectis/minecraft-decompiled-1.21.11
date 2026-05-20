package net.minecraft.client.realms.task;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.Backup;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.exception.RetryCallException;
import net.minecraft.client.realms.gui.screen.RealmsConfigureWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsGenericErrorScreen;
import net.minecraft.text.Text;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class RestoreTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Text TITLE = Text.translatable("mco.backup.restoring");
   private final Backup backup;
   private final long field_61055;
   private final RealmsConfigureWorldScreen lastScreen;

   public RestoreTask(Backup backup, long l, RealmsConfigureWorldScreen realmsConfigureWorldScreen) {
      this.backup = backup;
      this.field_61055 = l;
      this.lastScreen = realmsConfigureWorldScreen;
   }

   @Override
   public void run() {
      RealmsClient realmsClient = RealmsClient.create();
      int i = 0;

      while (i < 25) {
         try {
            if (this.aborted()) {
               return;
            }

            realmsClient.restoreWorld(this.field_61055, this.backup.backupId);
            pause(1L);
            if (this.aborted()) {
               return;
            }

            setScreen(this.lastScreen);
            return;
         } catch (RetryCallException var4) {
            if (this.aborted()) {
               return;
            }

            pause(var4.delaySeconds);
            i++;
         } catch (RealmsServiceException var5) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't restore backup", var5);
            setScreen(new RealmsGenericErrorScreen(var5, this.lastScreen));
            return;
         } catch (Exception var6) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't restore backup", var6);
            this.error(var6);
            return;
         }
      }
   }

   @Override
   public Text getTitle() {
      return TITLE;
   }
}
