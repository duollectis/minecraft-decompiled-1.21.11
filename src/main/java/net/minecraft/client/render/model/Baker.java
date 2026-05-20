package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public interface Baker {
   BakedSimpleModel getModel(Identifier id);

   BlockModelPart method_76673();

   ErrorCollectingSpriteGetter getSpriteGetter();

   Baker.class_12356 method_76674();

   <T> T compute(Baker.ResolvableCacheKey<T> key);

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface ResolvableCacheKey<T> {
      T compute(Baker baker);
   }

   @Environment(EnvType.CLIENT)
   public interface class_12356 {
      default Vector3fc method_76675(float f, float g, float h) {
         return this.method_76676(new Vector3f(f, g, h));
      }

      Vector3fc method_76676(Vector3fc vector3fc);
   }
}
