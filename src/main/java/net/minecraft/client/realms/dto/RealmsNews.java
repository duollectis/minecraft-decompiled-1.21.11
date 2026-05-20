package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record RealmsNews(@Nullable String newsLink) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsNews parse(String json) {
      String string = null;

      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         string = JsonUtils.getNullableStringOr("newsLink", jsonObject, null);
      } catch (Exception var3) {
         LOGGER.error("Could not parse RealmsNews", var3);
      }

      return new RealmsNews(string);
   }
}
