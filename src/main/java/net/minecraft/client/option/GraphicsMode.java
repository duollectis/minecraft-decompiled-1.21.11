package net.minecraft.client.option;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuDeviceInfo;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.render.ChunkBuilderMode;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public enum GraphicsMode implements StringIdentifiable {
   FAST("fast", "options.graphics.fast"),
   FANCY("fancy", "options.graphics.fancy"),
   FABULOUS("fabulous", "options.graphics.fabulous"),
   CUSTOM("custom", "options.graphics.custom");

   private final String name;
   private final String translationKey;
   public static final Codec<GraphicsMode> CODEC = StringIdentifiable.createCodec(GraphicsMode::values);

   private GraphicsMode(final String name, final String translationKey) {
      this.name = name;
      this.translationKey = translationKey;
   }

   @Override
   public String asString() {
      return this.name;
   }

   public String getTranslationKey() {
      return this.translationKey;
   }

   public void apply(MinecraftClient client) {
      GameOptionsScreen gameOptionsScreen = client.currentScreen instanceof GameOptionsScreen ? (GameOptionsScreen)client.currentScreen : null;
      GpuDevice gpuDevice = RenderSystem.getDevice();
      switch (this) {
         case FAST: {
            int i = 8;
            this.applyOption(gameOptionsScreen, client.options.getBiomeBlendRadius(), 1);
            this.applyOption(gameOptionsScreen, client.options.getViewDistance(), 8);
            this.applyOption(gameOptionsScreen, client.options.getChunkBuilderMode(), ChunkBuilderMode.NONE);
            this.applyOption(gameOptionsScreen, client.options.getSimulationDistance(), 6);
            this.applyOption(gameOptionsScreen, client.options.getAo(), false);
            this.applyOption(gameOptionsScreen, client.options.getCloudRenderMode(), CloudRenderMode.FAST);
            this.applyOption(gameOptionsScreen, client.options.getParticles(), ParticlesMode.DECREASED);
            this.applyOption(gameOptionsScreen, client.options.getMipmapLevels(), 2);
            this.applyOption(gameOptionsScreen, client.options.getEntityShadows(), false);
            this.applyOption(gameOptionsScreen, client.options.getEntityDistanceScaling(), 0.75);
            this.applyOption(gameOptionsScreen, client.options.getMenuBackgroundBlurriness(), 2);
            this.applyOption(gameOptionsScreen, client.options.getCloudRenderDistance(), 32);
            this.applyOption(gameOptionsScreen, client.options.getCutoutLeaves(), false);
            this.applyOption(gameOptionsScreen, client.options.getImprovedTransparency(), false);
            this.applyOption(gameOptionsScreen, client.options.getWeatherRadius(), 5);
            this.applyOption(gameOptionsScreen, client.options.getMaxAnisotropy(), 1);
            this.applyOption(gameOptionsScreen, client.options.getTextureFiltering(), TextureFilteringMode.NONE);
            break;
         }
         case FANCY: {
            int i = 16;
            this.applyOption(gameOptionsScreen, client.options.getBiomeBlendRadius(), 2);
            this.applyOption(gameOptionsScreen, client.options.getViewDistance(), 16);
            this.applyOption(gameOptionsScreen, client.options.getChunkBuilderMode(), ChunkBuilderMode.PLAYER_AFFECTED);
            this.applyOption(gameOptionsScreen, client.options.getSimulationDistance(), 12);
            this.applyOption(gameOptionsScreen, client.options.getAo(), true);
            this.applyOption(gameOptionsScreen, client.options.getCloudRenderMode(), CloudRenderMode.FANCY);
            this.applyOption(gameOptionsScreen, client.options.getParticles(), ParticlesMode.ALL);
            this.applyOption(gameOptionsScreen, client.options.getMipmapLevels(), 4);
            this.applyOption(gameOptionsScreen, client.options.getEntityShadows(), true);
            this.applyOption(gameOptionsScreen, client.options.getEntityDistanceScaling(), 1.0);
            this.applyOption(gameOptionsScreen, client.options.getMenuBackgroundBlurriness(), 5);
            this.applyOption(gameOptionsScreen, client.options.getCloudRenderDistance(), 64);
            this.applyOption(gameOptionsScreen, client.options.getCutoutLeaves(), true);
            this.applyOption(gameOptionsScreen, client.options.getImprovedTransparency(), false);
            this.applyOption(gameOptionsScreen, client.options.getWeatherRadius(), 10);
            this.applyOption(gameOptionsScreen, client.options.getMaxAnisotropy(), 1);
            this.applyOption(gameOptionsScreen, client.options.getTextureFiltering(), TextureFilteringMode.RGSS);
            break;
         }
         case FABULOUS: {
            int i = 32;
            this.applyOption(gameOptionsScreen, client.options.getBiomeBlendRadius(), 2);
            this.applyOption(gameOptionsScreen, client.options.getViewDistance(), 32);
            this.applyOption(gameOptionsScreen, client.options.getChunkBuilderMode(), ChunkBuilderMode.PLAYER_AFFECTED);
            this.applyOption(gameOptionsScreen, client.options.getSimulationDistance(), 12);
            this.applyOption(gameOptionsScreen, client.options.getAo(), true);
            this.applyOption(gameOptionsScreen, client.options.getCloudRenderMode(), CloudRenderMode.FANCY);
            this.applyOption(gameOptionsScreen, client.options.getParticles(), ParticlesMode.ALL);
            this.applyOption(gameOptionsScreen, client.options.getMipmapLevels(), 4);
            this.applyOption(gameOptionsScreen, client.options.getEntityShadows(), true);
            this.applyOption(gameOptionsScreen, client.options.getEntityDistanceScaling(), 1.25);
            this.applyOption(gameOptionsScreen, client.options.getMenuBackgroundBlurriness(), 5);
            this.applyOption(gameOptionsScreen, client.options.getCloudRenderDistance(), 128);
            this.applyOption(gameOptionsScreen, client.options.getCutoutLeaves(), true);
            this.applyOption(gameOptionsScreen, client.options.getImprovedTransparency(), Util.getOperatingSystem() != Util.OperatingSystem.OSX);
            this.applyOption(gameOptionsScreen, client.options.getWeatherRadius(), 10);
            this.applyOption(gameOptionsScreen, client.options.getMaxAnisotropy(), 2);
            if (GpuDeviceInfo.get(gpuDevice).method_76745()) {
               this.applyOption(gameOptionsScreen, client.options.getTextureFiltering(), TextureFilteringMode.RGSS);
            } else {
               this.applyOption(gameOptionsScreen, client.options.getTextureFiltering(), TextureFilteringMode.ANISOTROPIC);
            }
         }
      }
   }

   <T> void applyOption(@Nullable GameOptionsScreen screen, SimpleOption<T> option, T value) {
      if (option.getValue() != value) {
         option.setValue(value);
         if (screen != null) {
            screen.update(option);
         }
      }
   }
}
