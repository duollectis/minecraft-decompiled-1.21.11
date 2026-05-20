package net.minecraft.util.math;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum AxisRotation {
   R0(0, DirectionTransformation.IDENTITY, DirectionTransformation.IDENTITY, DirectionTransformation.IDENTITY),
   R90(1, DirectionTransformation.field_64508, DirectionTransformation.field_64511, DirectionTransformation.field_64514),
   R180(2, DirectionTransformation.field_64507, DirectionTransformation.field_64510, DirectionTransformation.field_64513),
   R270(3, DirectionTransformation.field_64506, DirectionTransformation.field_64509, DirectionTransformation.field_64512);

   public static final Codec<AxisRotation> CODEC = Codec.INT.comapFlatMap(degrees -> {
      return switch (MathHelper.floorMod(degrees, 360)) {
         case 0 -> DataResult.success(R0);
         case 90 -> DataResult.success(R90);
         case 180 -> DataResult.success(R180);
         case 270 -> DataResult.success(R270);
         default -> DataResult.error(() -> "Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
      };
   }, rotation -> {
      return switch (rotation) {
         case R0 -> 0;
         case R90 -> 90;
         case R180 -> 180;
         case R270 -> 270;
      };
   });
   public final int index;
   public final DirectionTransformation field_64521;
   public final DirectionTransformation field_64522;
   public final DirectionTransformation field_64523;

   private AxisRotation(
      final int index,
      final DirectionTransformation directionTransformation,
      final DirectionTransformation directionTransformation2,
      final DirectionTransformation directionTransformation3
   ) {
      this.index = index;
      this.field_64521 = directionTransformation;
      this.field_64522 = directionTransformation2;
      this.field_64523 = directionTransformation3;
   }

   @Deprecated
   public static AxisRotation fromDegrees(int degrees) {
      return switch (MathHelper.floorMod(degrees, 360)) {
         case 0 -> R0;
         case 90 -> R90;
         case 180 -> R180;
         case 270 -> R270;
         default -> throw new JsonParseException("Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
      };
   }

   public static DirectionTransformation method_76599(AxisRotation axisRotation, AxisRotation axisRotation2) {
      return axisRotation2.field_64522.prepend(axisRotation.field_64521);
   }

   public static DirectionTransformation method_76600(AxisRotation axisRotation, AxisRotation axisRotation2, AxisRotation axisRotation3) {
      return axisRotation3.field_64523.prepend(axisRotation2.field_64522.prepend(axisRotation.field_64521));
   }

   public int rotate(int index) {
      return (index + this.index) % 4;
   }
}
