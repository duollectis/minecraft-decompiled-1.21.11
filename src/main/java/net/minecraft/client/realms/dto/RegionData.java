package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;
import net.minecraft.client.realms.ServiceQuality;

@Environment(EnvType.CLIENT)
/**
 * {@code RegionData}.
 */
public record RegionData(
		@SerializedName("regionName") RealmsRegion region,
		@SerializedName("serviceQuality") ServiceQuality serviceQuality
)
		implements RealmsSerializable {
}
