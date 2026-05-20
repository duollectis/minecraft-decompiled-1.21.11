package net.minecraft.resource;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.JsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AbstractFileResourcePack implements ResourcePack {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourcePackInfo info;

   protected AbstractFileResourcePack(ResourcePackInfo info) {
      this.info = info;
   }

   @Override
   public <T> @Nullable T parseMetadata(ResourceMetadataSerializer<T> metadataSerializer) throws IOException {
      InputSupplier<InputStream> inputSupplier = this.openRoot("pack.mcmeta");
      if (inputSupplier == null) {
         return null;
      } else {
         Object var4;
         try (InputStream inputStream = inputSupplier.get()) {
            var4 = parseMetadata(metadataSerializer, inputStream, this.info);
         }

         return (T)var4;
      }
   }

   public static <T> @Nullable T parseMetadata(
      ResourceMetadataSerializer<T> resourceMetadataSerializer, InputStream inputStream, ResourcePackInfo resourcePackInfo
   ) {
      JsonObject jsonObject;
      try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
         jsonObject = JsonHelper.deserialize(bufferedReader);
      } catch (Exception var9) {
         LOGGER.error("Couldn't load {} {} metadata: {}", new Object[]{resourcePackInfo.id(), resourceMetadataSerializer.name(), var9.getMessage()});
         return null;
      }

      return (T)(!jsonObject.has(resourceMetadataSerializer.name())
         ? null
         : resourceMetadataSerializer.codec()
            .parse(JsonOps.INSTANCE, jsonObject.get(resourceMetadataSerializer.name()))
            .ifError(
               error -> LOGGER.error(
                  "Couldn't load {} {} metadata: {}", new Object[]{resourcePackInfo.id(), resourceMetadataSerializer.name(), error.message()}
               )
            )
            .result()
            .orElse(null));
   }

   @Override
   public ResourcePackInfo getInfo() {
      return this.info;
   }
}
