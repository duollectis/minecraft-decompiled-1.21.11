package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record RealmsDescriptionDto(@SerializedName("name") @Nullable String name, @SerializedName("description") String description)
   implements RealmsSerializable {
}
