package net.minecraft.client.realms.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record WorldTemplatePaginatedList(List<WorldTemplate> templates, int page, int size, int total) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public WorldTemplatePaginatedList(int size) {
      this(List.of(), 0, size, -1);
   }

   public boolean isLastPage() {
      return this.page * this.size >= this.total && this.page > 0 && this.total > 0 && this.size > 0;
   }

   public static WorldTemplatePaginatedList parse(String json) {
      List<WorldTemplate> list = new ArrayList<>();
      int i = 0;
      int j = 0;
      int k = 0;

      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         if (jsonObject.get("templates").isJsonArray()) {
            for (JsonElement jsonElement : jsonObject.get("templates").getAsJsonArray()) {
               WorldTemplate worldTemplate = WorldTemplate.parse(jsonElement.getAsJsonObject());
               if (worldTemplate != null) {
                  list.add(worldTemplate);
               }
            }
         }

         i = JsonUtils.getIntOr("page", jsonObject, 0);
         j = JsonUtils.getIntOr("size", jsonObject, 0);
         k = JsonUtils.getIntOr("total", jsonObject, 0);
      } catch (Exception var9) {
         LOGGER.error("Could not parse WorldTemplatePaginatedList", var9);
      }

      return new WorldTemplatePaginatedList(list, i, j, k);
   }
}
