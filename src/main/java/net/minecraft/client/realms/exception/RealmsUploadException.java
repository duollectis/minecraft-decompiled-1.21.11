package net.minecraft.client.realms.exception;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class RealmsUploadException extends RuntimeException {
   public @Nullable Text getStatus() {
      return null;
   }

   public Text @Nullable [] getStatusTexts() {
      return null;
   }
}
