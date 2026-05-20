package net.minecraft.client.texture.atlas;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public record SingleAtlasSource(Identifier resourceId, Optional<Identifier> spriteId) implements AtlasSource {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<SingleAtlasSource> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            Identifier.CODEC.fieldOf("resource").forGetter(SingleAtlasSource::resourceId),
            Identifier.CODEC.optionalFieldOf("sprite").forGetter(SingleAtlasSource::spriteId)
         )
         .apply(instance, SingleAtlasSource::new)
   );

   public SingleAtlasSource(Identifier resourceId) {
      this(resourceId, Optional.empty());
   }

   @Override
   public void load(ResourceManager resourceManager, AtlasSource.SpriteRegions regions) {
      Identifier identifier = RESOURCE_FINDER.toResourcePath(this.resourceId);
      Optional<Resource> optional = resourceManager.getResource(identifier);
      if (optional.isPresent()) {
         regions.add(this.spriteId.orElse(this.resourceId), optional.get());
      } else {
         LOGGER.warn("Missing sprite: {}", identifier);
      }
   }

   @Override
   public MapCodec<SingleAtlasSource> getCodec() {
      return CODEC;
   }
}
