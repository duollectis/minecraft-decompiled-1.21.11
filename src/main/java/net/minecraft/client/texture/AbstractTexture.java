package net.minecraft.client.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.GpuSampler;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public abstract class AbstractTexture implements AutoCloseable {
   protected @Nullable GpuTexture glTexture;
   protected @Nullable GpuTextureView glTextureView;
   protected GpuSampler sampler = RenderSystem.getSamplerCache().get(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, false);

   @Override
   public void close() {
      if (this.glTexture != null) {
         this.glTexture.close();
         this.glTexture = null;
      }

      if (this.glTextureView != null) {
         this.glTextureView.close();
         this.glTextureView = null;
      }
   }

   public GpuTexture getGlTexture() {
      if (this.glTexture == null) {
         throw new IllegalStateException("Texture does not exist, can't get it before something initializes it");
      } else {
         return this.glTexture;
      }
   }

   public GpuTextureView getGlTextureView() {
      if (this.glTextureView == null) {
         throw new IllegalStateException("Texture view does not exist, can't get it before something initializes it");
      } else {
         return this.glTextureView;
      }
   }

   public GpuSampler getSampler() {
      return this.sampler;
   }
}
