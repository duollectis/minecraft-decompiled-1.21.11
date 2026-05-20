package net.minecraft.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;

@Environment(EnvType.CLIENT)
public enum BlockRenderLayer {
   SOLID(RenderPipelines.SOLID_TERRAIN, 4194304, false),
   CUTOUT(RenderPipelines.CUTOUT_TERRAIN, 4194304, false),
   TRANSLUCENT(RenderPipelines.TRANSLUCENT, 786432, true),
   TRIPWIRE(RenderPipelines.TRIPWIRE_TERRAIN, 1536, true);

   private final RenderPipeline pipeline;
   private final int size;
   private final boolean translucent;
   private final String name;

   private BlockRenderLayer(final RenderPipeline pipeline, final int size, final boolean mipmap) {
      this.pipeline = pipeline;
      this.size = size;
      this.translucent = mipmap;
      this.name = this.toString().toLowerCase(Locale.ROOT);
   }

   public RenderPipeline getPipeline() {
      return this.pipeline;
   }

   public int getBufferSize() {
      return this.size;
   }

   public String getName() {
      return this.name;
   }

   public boolean isTranslucent() {
      return this.translucent;
   }
}
