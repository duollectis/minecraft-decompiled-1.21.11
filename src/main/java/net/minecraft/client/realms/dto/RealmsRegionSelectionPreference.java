package net.minecraft.client.realms.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RealmsRegionSelectionPreference implements RealmsSerializable {
   public static final RealmsRegionSelectionPreference DEFAULT = new RealmsRegionSelectionPreference(RegionSelectionMethod.AUTOMATIC_OWNER, null);
   @SerializedName("regionSelectionPreference")
   @JsonAdapter(RegionSelectionMethod.SelectionMethodTypeAdapter.class)
   public final RegionSelectionMethod selectionMethod;
   @SerializedName("preferredRegion")
   @JsonAdapter(RealmsRegion.RegionTypeAdapter.class)
   public @Nullable RealmsRegion preferredRegion;

   public RealmsRegionSelectionPreference(RegionSelectionMethod regionSelectionMethod, @Nullable RealmsRegion realmsRegion) {
      this.selectionMethod = regionSelectionMethod;
      this.preferredRegion = realmsRegion;
   }

   public RealmsRegionSelectionPreference method_71188() {
      return new RealmsRegionSelectionPreference(this.selectionMethod, this.preferredRegion);
   }
}
