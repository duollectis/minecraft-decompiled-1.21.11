package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

@Environment(EnvType.CLIENT)
public record PingResult(@SerializedName("pingResults") List<RegionPingResult> pingResults, @SerializedName("worldIds") List<Long> worldIds)
   implements RealmsSerializable {
}
