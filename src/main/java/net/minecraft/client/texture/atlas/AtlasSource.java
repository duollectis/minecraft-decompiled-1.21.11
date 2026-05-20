package net.minecraft.client.texture.atlas;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteOpener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface AtlasSource {
   ResourceFinder RESOURCE_FINDER = new ResourceFinder("textures", ".png");

   void load(ResourceManager resourceManager, AtlasSource.SpriteRegions regions);

   MapCodec<? extends AtlasSource> getCodec();

   @Environment(EnvType.CLIENT)
   public interface SpriteRegion extends AtlasSource.SpriteSource {
      default void close() {
      }
   }

   @Environment(EnvType.CLIENT)
   public interface SpriteRegions {
      default void add(Identifier id, Resource resource) {
         this.add(id, opener -> opener.loadSprite(id, resource));
      }

      void add(Identifier arg, AtlasSource.SpriteRegion region);

      void removeIf(Predicate<Identifier> predicate);
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface SpriteSource {
      @Nullable SpriteContents load(SpriteOpener spriteOpener);
   }
}
