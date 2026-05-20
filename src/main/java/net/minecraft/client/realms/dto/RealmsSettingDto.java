package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.RealmsSerializable;

@Environment(EnvType.CLIENT)
public record RealmsSettingDto(@SerializedName("name") String name, @SerializedName("value") String value) implements RealmsSerializable {
   public static RealmsSettingDto ofHardcore(boolean hardcore) {
      return new RealmsSettingDto("hardcore", Boolean.toString(hardcore));
   }

   public static boolean isHardcore(List<RealmsSettingDto> settings) {
      for (RealmsSettingDto realmsSettingDto : settings) {
         if (realmsSettingDto.name().equals("hardcore")) {
            return Boolean.parseBoolean(realmsSettingDto.value());
         }
      }

      return false;
   }
}
