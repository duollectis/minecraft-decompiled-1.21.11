package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.OptionalLong;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class GlTimer {
   private @Nullable CommandEncoder encoder;
   private @Nullable GpuQuery query;

   public static GlTimer getInstance() {
      return GlTimer.InstanceHolder.INSTANCE;
   }

   public boolean isRunning() {
      return this.query != null;
   }

   public void beginProfile() {
      RenderSystem.assertOnRenderThread();
      if (this.query != null) {
         throw new IllegalStateException("Current profile not ended");
      } else {
         this.encoder = RenderSystem.getDevice().createCommandEncoder();
         this.query = this.encoder.timerQueryBegin();
      }
   }

   public GlTimer.Query endProfile() {
      RenderSystem.assertOnRenderThread();
      if (this.query != null && this.encoder != null) {
         this.encoder.timerQueryEnd(this.query);
         GlTimer.Query query = new GlTimer.Query(this.query);
         this.query = null;
         this.encoder = null;
         return query;
      } else {
         throw new IllegalStateException("endProfile called before beginProfile");
      }
   }

   @Environment(EnvType.CLIENT)
   static class InstanceHolder {
      static final GlTimer INSTANCE = create();

      private InstanceHolder() {
      }

      private static GlTimer create() {
         return new GlTimer();
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Query {
      private static final long MISSING = 0L;
      private static final long CLOSED = -1L;
      private final GpuQuery query;
      private long result = 0L;

      Query(GpuQuery query) {
         this.query = query;
      }

      public void close() {
         RenderSystem.assertOnRenderThread();
         if (this.result == 0L) {
            this.result = -1L;
            this.query.close();
         }
      }

      public boolean isResultAvailable() {
         RenderSystem.assertOnRenderThread();
         if (this.result != 0L) {
            return true;
         } else {
            OptionalLong optionalLong = this.query.getValue();
            if (optionalLong.isPresent()) {
               this.result = optionalLong.getAsLong();
               this.query.close();
               return true;
            } else {
               return false;
            }
         }
      }

      public long queryResult() {
         RenderSystem.assertOnRenderThread();
         if (this.result == 0L) {
            OptionalLong optionalLong = this.query.getValue();
            if (optionalLong.isPresent()) {
               this.result = optionalLong.getAsLong();
               this.query.close();
            }
         }

         return this.result;
      }
   }
}
