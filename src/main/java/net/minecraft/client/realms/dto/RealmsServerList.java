package net.minecraft.client.realms.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.CheckedGson;
import net.minecraft.client.realms.RealmsSerializable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record RealmsServerList(@SerializedName("servers") List<RealmsServer> servers) implements RealmsSerializable {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsServerList parse(CheckedGson gson, String json) {
      try {
         RealmsServerList realmsServerList = gson.fromJson(json, RealmsServerList.class);
         if (realmsServerList != null) {
            realmsServerList.servers.forEach(RealmsServer::replaceNullsWithDefaults);
            return realmsServerList;
         }

         LOGGER.error("Could not parse McoServerList: {}", json);
      } catch (Exception var3) {
         LOGGER.error("Could not parse McoServerList", var3);
      }

      return new RealmsServerList(List.of());
   }
}
