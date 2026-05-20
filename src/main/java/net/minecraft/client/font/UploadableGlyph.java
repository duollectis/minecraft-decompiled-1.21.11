package net.minecraft.client.font;

import com.mojang.blaze3d.textures.GpuTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface UploadableGlyph {
   int getWidth();

   int getHeight();

   void upload(int x, int y, GpuTexture texture);

   boolean hasColor();

   float getOversample();

   default float getXMin() {
      return this.getBearingX();
   }

   default float getXMax() {
      return this.getXMin() + this.getWidth() / this.getOversample();
   }

   default float getYMin() {
      return 7.0F - this.getAscent();
   }

   default float getYMax() {
      return this.getYMin() + this.getHeight() / this.getOversample();
   }

   default float getBearingX() {
      return 0.0F;
   }

   default float getAscent() {
      return 7.0F;
   }
}
