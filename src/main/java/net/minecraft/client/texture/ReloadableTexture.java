package net.minecraft.client.texture;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public abstract class ReloadableTexture extends AbstractTexture {
   private final Identifier textureId;

   public ReloadableTexture(Identifier textureId) {
      this.textureId = textureId;
   }

   public Identifier getId() {
      return this.textureId;
   }

   public void reload(TextureContents textureContents) {
      boolean bl = textureContents.clamp();
      boolean bl2 = textureContents.blur();
      AddressMode addressMode = bl ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
      FilterMode filterMode = bl2 ? FilterMode.LINEAR : FilterMode.NEAREST;
      this.sampler = RenderSystem.getSamplerCache().get(addressMode, addressMode, filterMode, filterMode, false);

      try (NativeImage nativeImage = textureContents.image()) {
         this.load(nativeImage);
      }
   }

   protected void load(NativeImage image) {
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.close();
      this.glTexture = gpuDevice.createTexture(this.textureId::toString, 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
      this.glTextureView = gpuDevice.createTextureView(this.glTexture);
      gpuDevice.createCommandEncoder().writeToTexture(this.glTexture, image);
   }

   public abstract TextureContents loadContents(ResourceManager resourceManager) throws IOException;
}
