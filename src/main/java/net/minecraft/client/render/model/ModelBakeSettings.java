package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@Environment(EnvType.CLIENT)
public interface ModelBakeSettings {
   Matrix4fc TRANSFORM_NONE = new Matrix4f();

   default AffineTransformation getRotation() {
      return AffineTransformation.identity();
   }

   default Matrix4fc forward(Direction facing) {
      return TRANSFORM_NONE;
   }

   default Matrix4fc reverse(Direction facing) {
      return TRANSFORM_NONE;
   }
}
