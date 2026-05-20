package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

@Environment(EnvType.CLIENT)
public record RealmsWorldResetDto(
   @SerializedName("seed") String seed,
   @SerializedName("worldTemplateId") long worldTemplateId,
   @SerializedName("levelType") int levelType,
   @SerializedName("generateStructures") boolean generateStructures,
   @SerializedName("experiments") Set<String> experiments
) implements RealmsSerializable {
}
