package net.minecraft.network.encoding;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class VelocityEncoding {
   private static final int field_62269 = 15;
   private static final int MAX_15_BIT_INT = 32767;
   private static final double field_62270 = 32766.0;
   private static final int field_62271 = 2;
   private static final int SLOW_BIT_MASK = 3;
   private static final int field_62272 = 4;
   private static final int field_62273 = 3;
   private static final int field_62274 = 18;
   private static final int field_62275 = 33;
   public static final double field_62267 = 1.7179869183E10;
   public static final double field_62268 = 3.051944088384301E-5;

   public static boolean hasFastMarkerBit(int maxDirectionalVelocity) {
      return (maxDirectionalVelocity & 4) == 4;
   }

   public static Vec3d readVelocity(ByteBuf buf) {
      int i = buf.readUnsignedByte();
      if (i == 0) {
         return Vec3d.ZERO;
      } else {
         int j = buf.readUnsignedByte();
         long l = buf.readUnsignedInt();
         long m = l << 16 | j << 8 | i;
         long n = i & 3;
         if (hasFastMarkerBit(i)) {
            n |= (VarInts.read(buf) & 4294967295L) << 2;
         }

         return new Vec3d(fromLong(m >> 3) * n, fromLong(m >> 18) * n, fromLong(m >> 33) * n);
      }
   }

   public static void writeVelocity(ByteBuf buf, Vec3d velocity) {
      double d = clampValue(velocity.x);
      double e = clampValue(velocity.y);
      double f = clampValue(velocity.z);
      double g = MathHelper.absMax(d, MathHelper.absMax(e, f));
      if (g < 3.051944088384301E-5) {
         buf.writeByte(0);
      } else {
         long l = MathHelper.ceilLong(g);
         boolean bl = (l & 3L) != l;
         long m = bl ? l & 3L | 4L : l;
         long n = toLong(d / l) << 3;
         long o = toLong(e / l) << 18;
         long p = toLong(f / l) << 33;
         long q = m | n | o | p;
         buf.writeByte((byte)q);
         buf.writeByte((byte)(q >> 8));
         buf.writeInt((int)(q >> 16));
         if (bl) {
            VarInts.write(buf, (int)(l >> 2));
         }
      }
   }

   private static double clampValue(double value) {
      return Double.isNaN(value) ? 0.0 : Math.clamp(value, -1.7179869183E10, 1.7179869183E10);
   }

   private static long toLong(double value) {
      return Math.round((value * 0.5 + 0.5) * 32766.0);
   }

   private static double fromLong(long value) {
      return Math.min((double)(value & 32767L), 32766.0) * 2.0 / 32766.0 - 1.0;
   }
}
