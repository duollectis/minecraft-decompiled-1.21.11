package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

@Environment(EnvType.CLIENT)
public record RealmsRegionDataList(@SerializedName("regionDataList") List<RegionData> regionData) implements RealmsSerializable {
   public static RealmsRegionDataList empty() {
      return new RealmsRegionDataList(List.of());
   }
}
