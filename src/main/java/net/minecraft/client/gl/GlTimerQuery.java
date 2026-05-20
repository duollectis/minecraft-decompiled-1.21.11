package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL32C;

@Environment(EnvType.CLIENT)
public class GlTimerQuery implements GpuQuery {
   private final int id;
   private boolean closed;
   private OptionalLong value = OptionalLong.empty();

   GlTimerQuery(int id) {
      this.id = id;
   }

   @Override
   public OptionalLong getValue() {
      RenderSystem.assertOnRenderThread();
      if (this.closed) {
         throw new IllegalStateException("GlTimerQuery is closed");
      } else if (this.value.isPresent()) {
         return this.value;
      } else if (GL32C.glGetQueryObjecti(this.id, 34919) == 1) {
         this.value = OptionalLong.of(ARBTimerQuery.glGetQueryObjecti64(this.id, 34918));
         return this.value;
      } else {
         return OptionalLong.empty();
      }
   }

   @Override
   public void close() {
      RenderSystem.assertOnRenderThread();
      if (!this.closed) {
         this.closed = true;
         GL32C.glDeleteQueries(this.id);
      }
   }
}
