package net.minecraft.client.render.model.json;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MatrixUtil;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public record ModelRotation(Vector3fc origin, ModelRotation.class_12353 value, boolean rescale, Matrix4fc transform) {
   public ModelRotation(Vector3fc vector3fc, ModelRotation.class_12353 arg, boolean bl) {
      this(vector3fc, arg, bl, method_75421(arg, bl));
   }

   private static Matrix4f method_75421(ModelRotation.class_12353 arg, boolean bl) {
      Matrix4f matrix4f = arg.method_76652();
      if (bl && !MatrixUtil.isIdentity(matrix4f)) {
         Vector3fc vector3fc = method_75422(matrix4f);
         matrix4f.scale(vector3fc);
      }

      return matrix4f;
   }

   private static Vector3fc method_75422(Matrix4fc matrix4fc) {
      Vector3f vector3f = new Vector3f();
      float f = method_76651(matrix4fc, Direction.Axis.X, vector3f);
      float g = method_76651(matrix4fc, Direction.Axis.Y, vector3f);
      float h = method_76651(matrix4fc, Direction.Axis.Z, vector3f);
      return vector3f.set(f, g, h);
   }

   private static float method_76651(Matrix4fc matrix4fc, Direction.Axis axis, Vector3f vector3f) {
      Vector3f vector3f2 = vector3f.set(axis.getPositiveDirection().getFloatVector());
      Vector3f vector3f3 = matrix4fc.transformDirection(vector3f2);
      float f = Math.abs(vector3f3.x);
      float g = Math.abs(vector3f3.y);
      float h = Math.abs(vector3f3.z);
      float i = Math.max(Math.max(f, g), h);
      return 1.0F / i;
   }

   @Environment(EnvType.CLIENT)
   public record class_12352(float x, float y, float z) implements ModelRotation.class_12353 {
      @Override
      public Matrix4f method_76652() {
         return new Matrix4f()
            .rotationZYX(
               this.z * (float) (java.lang.Math.PI / 180.0), this.y * (float) (java.lang.Math.PI / 180.0), this.x * (float) (java.lang.Math.PI / 180.0)
            );
      }
   }

   @Environment(EnvType.CLIENT)
   public interface class_12353 {
      Matrix4f method_76652();
   }

   @Environment(EnvType.CLIENT)
   public record class_12354(Direction.Axis axis, float angle) implements ModelRotation.class_12353 {
      @Override
      public Matrix4f method_76652() {
         Matrix4f matrix4f = new Matrix4f();
         if (this.angle == 0.0F) {
            return matrix4f;
         } else {
            Vector3fc vector3fc = this.axis.getPositiveDirection().getFloatVector();
            matrix4f.rotation(this.angle * (float) (java.lang.Math.PI / 180.0), vector3fc);
            return matrix4f;
         }
      }
   }
}
