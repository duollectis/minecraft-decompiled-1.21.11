package net.minecraft.client.render.model;

import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelRotation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.AxisRotation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MatrixUtil;
import org.joml.GeometryUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BakedQuadFactory {
   private static final Vector3fc field_60150 = new Vector3f(0.5F, 0.5F, 0.5F);

   @VisibleForTesting
   static ModelElementFace.UV setDefaultUV(Vector3fc from, Vector3fc to, Direction facing) {
      return switch (facing) {
         case DOWN -> new ModelElementFace.UV(from.x(), 16.0F - to.z(), to.x(), 16.0F - from.z());
         case UP -> new ModelElementFace.UV(from.x(), from.z(), to.x(), to.z());
         case NORTH -> new ModelElementFace.UV(16.0F - to.x(), 16.0F - to.y(), 16.0F - from.x(), 16.0F - from.y());
         case SOUTH -> new ModelElementFace.UV(from.x(), 16.0F - to.y(), to.x(), 16.0F - from.y());
         case WEST -> new ModelElementFace.UV(from.z(), 16.0F - to.y(), to.z(), 16.0F - from.y());
         case EAST -> new ModelElementFace.UV(16.0F - to.z(), 16.0F - to.y(), 16.0F - from.z(), 16.0F - from.y());
      };
   }

   public static BakedQuad bake(
      Baker.class_12356 arg,
      Vector3fc vector3fc,
      Vector3fc vector3fc2,
      ModelElementFace modelElementFace,
      Sprite sprite,
      Direction direction,
      ModelBakeSettings modelBakeSettings,
      @Nullable ModelRotation modelRotation,
      boolean bl,
      int i
   ) {
      ModelElementFace.UV uV = modelElementFace.uvs();
      if (uV == null) {
         uV = setDefaultUV(vector3fc, vector3fc2, direction);
      }

      Matrix4fc matrix4fc = modelBakeSettings.reverse(direction);
      Vector3fc[] vector3fcs = new Vector3fc[4];
      long[] ls = new long[4];
      CubeFace cubeFace = CubeFace.getFace(direction);

      for (int j = 0; j < 4; j++) {
         packVertexData(
            j,
            cubeFace,
            uV,
            modelElementFace.rotation(),
            matrix4fc,
            vector3fc,
            vector3fc2,
            sprite,
            modelBakeSettings.getRotation(),
            modelRotation,
            vector3fcs,
            ls,
            arg
         );
      }

      Direction direction2 = decodeDirection(vector3fcs);
      if (modelRotation == null && direction2 != null) {
         encodeDirection(vector3fcs, ls, direction2);
      }

      return new BakedQuad(
         vector3fcs[0],
         vector3fcs[1],
         vector3fcs[2],
         vector3fcs[3],
         ls[0],
         ls[1],
         ls[2],
         ls[3],
         modelElementFace.tintIndex(),
         Objects.requireNonNullElse(direction2, Direction.UP),
         sprite,
         bl,
         i
      );
   }

   private static void packVertexData(
      int i,
      CubeFace cubeFace,
      ModelElementFace.UV uV,
      AxisRotation axisRotation,
      Matrix4fc matrix4fc,
      Vector3fc vector3fc,
      Vector3fc vector3fc2,
      Sprite sprite,
      AffineTransformation affineTransformation,
      @Nullable ModelRotation modelRotation,
      Vector3fc[] vector3fcs,
      long[] ls,
      Baker.class_12356 arg
   ) {
      CubeFace.Corner corner = cubeFace.getCorner(i);
      Vector3f vector3f = corner.method_76646(vector3fc, vector3fc2).div(16.0F);
      if (modelRotation != null) {
         transformVertex(vector3f, modelRotation.origin(), modelRotation.transform());
      }

      if (affineTransformation != AffineTransformation.identity()) {
         transformVertex(vector3f, field_60150, affineTransformation.getMatrix());
      }

      float f = ModelElementFace.getUValue(uV, axisRotation, i);
      float g = ModelElementFace.getVValue(uV, axisRotation, i);
      float j;
      float h;
      if (MatrixUtil.isIdentity(matrix4fc)) {
         h = f;
         j = g;
      } else {
         Vector3f vector3f2 = matrix4fc.transformPosition(new Vector3f(setCenterBack(f), setCenterBack(g), 0.0F));
         h = setCenterForward(vector3f2.x);
         j = setCenterForward(vector3f2.y);
      }

      vector3fcs[i] = arg.method_76676(vector3f);
      ls[i] = Vector2f.toLong(sprite.getFrameU(h), sprite.getFrameV(j));
   }

   private static float setCenterBack(float f) {
      return f - 0.5F;
   }

   private static float setCenterForward(float f) {
      return f + 0.5F;
   }

   private static void transformVertex(Vector3f vertex, Vector3fc vector3fc, Matrix4fc matrix4fc) {
      vertex.sub(vector3fc);
      matrix4fc.transformPosition(vertex);
      vertex.add(vector3fc);
   }

   private static @Nullable Direction decodeDirection(Vector3fc[] vector3fcs) {
      Vector3f vector3f = new Vector3f();
      GeometryUtils.normal(vector3fcs[0], vector3fcs[1], vector3fcs[2], vector3f);
      return method_76653(vector3f);
   }

   private static @Nullable Direction method_76653(Vector3f vector3f) {
      if (!vector3f.isFinite()) {
         return null;
      } else {
         Direction direction = null;
         float f = 0.0F;

         for (Direction direction2 : Direction.values()) {
            float g = vector3f.dot(direction2.getFloatVector());
            if (g >= 0.0F && g > f) {
               f = g;
               direction = direction2;
            }
         }

         return direction;
      }
   }

   private static void encodeDirection(Vector3fc[] vector3fcs, long[] ls, Direction direction) {
      float f = 999.0F;
      float g = 999.0F;
      float h = 999.0F;
      float i = -999.0F;
      float j = -999.0F;
      float k = -999.0F;

      for (int l = 0; l < 4; l++) {
         Vector3fc vector3fc = vector3fcs[l];
         float m = vector3fc.x();
         float n = vector3fc.y();
         float o = vector3fc.z();
         if (m < f) {
            f = m;
         }

         if (n < g) {
            g = n;
         }

         if (o < h) {
            h = o;
         }

         if (m > i) {
            i = m;
         }

         if (n > j) {
            j = n;
         }

         if (o > k) {
            k = o;
         }
      }

      CubeFace cubeFace = CubeFace.getFace(direction);

      for (int p = 0; p < 4; p++) {
         CubeFace.Corner corner = cubeFace.getCorner(p);
         float nx = corner.xSide().method_76644(f, g, h, i, j, k);
         float ox = corner.ySide().method_76644(f, g, h, i, j, k);
         float q = corner.zSide().method_76644(f, g, h, i, j, k);
         int r = method_76655(vector3fcs, p, nx, ox, q);
         if (r == -1) {
            throw new IllegalStateException("Can't find vertex to swap");
         }

         if (r != p) {
            method_76656(vector3fcs, r, p);
            method_76654(ls, r, p);
         }
      }
   }

   private static int method_76655(Vector3fc[] vector3fcs, int i, float f, float g, float h) {
      for (int j = i; j < 4; j++) {
         Vector3fc vector3fc = vector3fcs[j];
         if (f == vector3fc.x() && g == vector3fc.y() && h == vector3fc.z()) {
            return j;
         }
      }

      return -1;
   }

   private static void method_76656(Vector3fc[] vector3fcs, int i, int j) {
      Vector3fc vector3fc = vector3fcs[i];
      vector3fcs[i] = vector3fcs[j];
      vector3fcs[j] = vector3fc;
   }

   private static void method_76654(long[] ls, int i, int j) {
      long l = ls[i];
      ls[i] = ls[j];
      ls[j] = l;
   }
}
