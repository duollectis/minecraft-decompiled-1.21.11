package net.minecraft.client.render.model;

import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public enum CubeFace {
   DOWN(
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MAX_Z)
   ),
   UP(
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MIN_Z)
   ),
   NORTH(
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MIN_Z)
   ),
   SOUTH(
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MAX_Z)
   ),
   WEST(
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MIN_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MAX_Z)
   ),
   EAST(
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MAX_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MIN_Y, CubeFace.class_12350.MIN_Z),
      new CubeFace.Corner(CubeFace.class_12350.MAX_X, CubeFace.class_12350.MAX_Y, CubeFace.class_12350.MIN_Z)
   );

   private static final Map<Direction, CubeFace> DIRECTION_LOOKUP = Util.make(new EnumMap<>(Direction.class), enumMap -> {
      enumMap.put(Direction.DOWN, DOWN);
      enumMap.put(Direction.UP, UP);
      enumMap.put(Direction.NORTH, NORTH);
      enumMap.put(Direction.SOUTH, SOUTH);
      enumMap.put(Direction.WEST, WEST);
      enumMap.put(Direction.EAST, EAST);
   });
   private final CubeFace.Corner[] corners;

   public static CubeFace getFace(Direction direction) {
      return DIRECTION_LOOKUP.get(direction);
   }

   private CubeFace(final CubeFace.Corner... corners) {
      this.corners = corners;
   }

   public CubeFace.Corner getCorner(int corner) {
      return this.corners[corner];
   }

   @Environment(EnvType.CLIENT)
   public record Corner(CubeFace.class_12350 xSide, CubeFace.class_12350 ySide, CubeFace.class_12350 zSide) {
      public Vector3f method_76646(Vector3fc vector3fc, Vector3fc vector3fc2) {
         return new Vector3f(
            this.xSide.method_76645(vector3fc, vector3fc2), this.ySide.method_76645(vector3fc, vector3fc2), this.zSide.method_76645(vector3fc, vector3fc2)
         );
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum class_12350 {
      MIN_X,
      MIN_Y,
      MIN_Z,
      MAX_X,
      MAX_Y,
      MAX_Z;

      public float method_76645(Vector3fc vector3fc, Vector3fc vector3fc2) {
         return switch (this) {
            case MIN_X -> vector3fc.x();
            case MIN_Y -> vector3fc.y();
            case MIN_Z -> vector3fc.z();
            case MAX_X -> vector3fc2.x();
            case MAX_Y -> vector3fc2.y();
            case MAX_Z -> vector3fc2.z();
         };
      }

      public float method_76644(float f, float g, float h, float i, float j, float k) {
         return switch (this) {
            case MIN_X -> f;
            case MIN_Y -> g;
            case MIN_Z -> h;
            case MAX_X -> i;
            case MAX_Y -> j;
            case MAX_Z -> k;
         };
      }
   }
}
