package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record RealmsOptionsDto(
   @SerializedName("slotId") int slotId,
   @SerializedName("spawnProtection") int spawnProtection,
   @SerializedName("forceGameMode") boolean forceGameMode,
   @SerializedName("difficulty") int difficulty,
   @SerializedName("gameMode") int gameMode,
   @SerializedName("slotName") String slotName,
   @SerializedName("version") String version,
   @SerializedName("compatibility") RealmsServer.Compatibility compatibility,
   @SerializedName("worldTemplateId") long worldTemplateId,
   @SerializedName("worldTemplateImage") @Nullable String worldTemplateImage,
   @SerializedName("hardcore") boolean hardcore
) implements RealmsSerializable {
   public RealmsOptionsDto(int slotId, RealmsWorldOptions options, boolean hardcore) {
      this(
         slotId,
         options.spawnProtection,
         options.forceGameMode,
         options.difficulty,
         options.gameMode,
         options.getSlotName(slotId),
         options.version,
         options.compatibility,
         options.templateId,
         options.templateImage,
         hardcore
      );
   }
}
