package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.resource.Resource;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface SpriteOpener {
   Logger LOGGER = LogUtils.getLogger();

   static SpriteOpener create(Set<ResourceMetadataSerializer<?>> additionalMetadata) {
      return (id, resource) -> {
         Optional<AnimationResourceMetadata> optional;
         Optional<TextureResourceMetadata> optional2;
         List<ResourceMetadataSerializer.Value<?>> list;
         try {
            ResourceMetadata resourceMetadata = resource.getMetadata();
            optional = resourceMetadata.decode(AnimationResourceMetadata.SERIALIZER);
            optional2 = resourceMetadata.decode(TextureResourceMetadata.SERIALIZER);
            list = resourceMetadata.decode(additionalMetadata);
         } catch (Exception var11) {
            LOGGER.error("Unable to parse metadata from {}", id, var11);
            return null;
         }

         NativeImage nativeImage;
         try (InputStream inputStream = resource.getInputStream()) {
            nativeImage = NativeImage.read(inputStream);
         } catch (IOException var13) {
            LOGGER.error("Using missing texture, unable to load {}", id, var13);
            return null;
         }

         SpriteDimensions spriteDimensions;
         if (optional.isPresent()) {
            spriteDimensions = optional.get().getSize(nativeImage.getWidth(), nativeImage.getHeight());
            if (!MathHelper.isMultipleOf(nativeImage.getWidth(), spriteDimensions.width())
               || !MathHelper.isMultipleOf(nativeImage.getHeight(), spriteDimensions.height())) {
               LOGGER.error(
                  "Image {} size {},{} is not multiple of frame size {},{}",
                  new Object[]{id, nativeImage.getWidth(), nativeImage.getHeight(), spriteDimensions.width(), spriteDimensions.height()}
               );
               nativeImage.close();
               return null;
            }
         } else {
            spriteDimensions = new SpriteDimensions(nativeImage.getWidth(), nativeImage.getHeight());
         }

         return new SpriteContents(id, spriteDimensions, nativeImage, optional, list, optional2);
      };
   }

   @Nullable SpriteContents loadSprite(Identifier id, Resource resource);
}
