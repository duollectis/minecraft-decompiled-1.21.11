package net.minecraft.util.math;

import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ColorHelper {
   private static final int LINEAR_TO_SRGB_LUT_LENGTH = 1024;
   private static final short[] SRGB_TO_LINEAR = Util.make(new short[256], out -> {
      for (int i = 0; i < out.length; i++) {
         float f = i / 255.0F;
         out[i] = (short)Math.round(computeSrgbToLinear(f) * 1023.0F);
      }
   });
   private static final byte[] LINEAR_TO_SRGB = Util.make(new byte[1024], out -> {
      for (int i = 0; i < out.length; i++) {
         float f = i / 1023.0F;
         out[i] = (byte)Math.round(computeLinearToSrgb(f) * 255.0F);
      }
   });

   private static float computeSrgbToLinear(float srgb) {
      return srgb >= 0.04045F ? (float)Math.pow((srgb + 0.055) / 1.055, 2.4) : srgb / 12.92F;
   }

   private static float computeLinearToSrgb(float linear) {
      return linear >= 0.0031308F ? (float)(1.055 * Math.pow(linear, 0.4166666666666667) - 0.055) : 12.92F * linear;
   }

   public static float srgbToLinear(int srgb) {
      return SRGB_TO_LINEAR[srgb] / 1023.0F;
   }

   public static int linearToSrgb(float linear) {
      return LINEAR_TO_SRGB[MathHelper.floor(linear * 1023.0F)] & 0xFF;
   }

   public static int interpolate(int a, int b, int c, int d) {
      return getArgb(
         (getAlpha(a) + getAlpha(b) + getAlpha(c) + getAlpha(d)) / 4,
         averageSrgbIntensities(getRed(a), getRed(b), getRed(c), getRed(d)),
         averageSrgbIntensities(getGreen(a), getGreen(b), getGreen(c), getGreen(d)),
         averageSrgbIntensities(getBlue(a), getBlue(b), getBlue(c), getBlue(d))
      );
   }

   private static int averageSrgbIntensities(int a, int b, int c, int d) {
      int i = (SRGB_TO_LINEAR[a] + SRGB_TO_LINEAR[b] + SRGB_TO_LINEAR[c] + SRGB_TO_LINEAR[d]) / 4;
      return LINEAR_TO_SRGB[i] & 0xFF;
   }

   public static int getAlpha(int argb) {
      return argb >>> 24;
   }

   public static int getRed(int argb) {
      return argb >> 16 & 0xFF;
   }

   public static int getGreen(int argb) {
      return argb >> 8 & 0xFF;
   }

   public static int getBlue(int argb) {
      return argb & 0xFF;
   }

   public static int getArgb(int alpha, int red, int green, int blue) {
      return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
   }

   public static int getArgb(int red, int green, int blue) {
      return getArgb(255, red, green, blue);
   }

   public static int getArgb(Vec3d rgb) {
      return getArgb(channelFromFloat((float)rgb.getX()), channelFromFloat((float)rgb.getY()), channelFromFloat((float)rgb.getZ()));
   }

   public static int mix(int first, int second) {
      if (first == -1) {
         return second;
      } else {
         return second == -1
            ? first
            : getArgb(
               getAlpha(first) * getAlpha(second) / 255,
               getRed(first) * getRed(second) / 255,
               getGreen(first) * getGreen(second) / 255,
               getBlue(first) * getBlue(second) / 255
            );
      }
   }

   public static int add(int a, int b) {
      return getArgb(getAlpha(a), Math.min(getRed(a) + getRed(b), 255), Math.min(getGreen(a) + getGreen(b), 255), Math.min(getBlue(a) + getBlue(b), 255));
   }

   public static int subtract(int a, int b) {
      return getArgb(getAlpha(a), Math.max(getRed(a) - getRed(b), 0), Math.max(getGreen(a) - getGreen(b), 0), Math.max(getBlue(a) - getBlue(b), 0));
   }

   public static int scaleAlpha(int argb, float scale) {
      if (argb == 0 || scale <= 0.0F) {
         return 0;
      } else {
         return scale >= 1.0F ? argb : withAlpha(getAlphaFloat(argb) * scale, argb);
      }
   }

   public static int scaleRgb(int argb, float scale) {
      return scaleRgb(argb, scale, scale, scale);
   }

   public static int scaleRgb(int argb, float redScale, float greenScale, float blueScale) {
      return getArgb(
         getAlpha(argb),
         Math.clamp((long)((int)(getRed(argb) * redScale)), 0, 255),
         Math.clamp((long)((int)(getGreen(argb) * greenScale)), 0, 255),
         Math.clamp((long)((int)(getBlue(argb) * blueScale)), 0, 255)
      );
   }

   public static int scaleRgb(int argb, int scale) {
      return getArgb(
         getAlpha(argb),
         Math.clamp((long)getRed(argb) * scale / 255L, 0, 255),
         Math.clamp((long)getGreen(argb) * scale / 255L, 0, 255),
         Math.clamp((long)getBlue(argb) * scale / 255L, 0, 255)
      );
   }

   public static int grayscale(int argb) {
      int i = (int)(getRed(argb) * 0.3F + getGreen(argb) * 0.59F + getBlue(argb) * 0.11F);
      return getArgb(getAlpha(argb), i, i, i);
   }

   public static int alphaBlend(int a, int b) {
      int i = getAlpha(a);
      int j = getAlpha(b);
      if (j == 255) {
         return b;
      } else if (j == 0) {
         return a;
      } else {
         int k = j + i * (255 - j) / 255;
         return getArgb(k, blend(k, j, getRed(a), getRed(b)), blend(k, j, getGreen(a), getGreen(b)), blend(k, j, getBlue(a), getBlue(b)));
      }
   }

   private static int blend(int blendedAlpha, int alpha, int a, int b) {
      return (b * alpha + a * (blendedAlpha - alpha)) / blendedAlpha;
   }

   public static int lerp(float delta, int start, int end) {
      int i = MathHelper.lerp(delta, getAlpha(start), getAlpha(end));
      int j = MathHelper.lerp(delta, getRed(start), getRed(end));
      int k = MathHelper.lerp(delta, getGreen(start), getGreen(end));
      int l = MathHelper.lerp(delta, getBlue(start), getBlue(end));
      return getArgb(i, j, k, l);
   }

   public static int lerpLinear(float delta, int start, int end) {
      return getArgb(
         MathHelper.lerp(delta, getAlpha(start), getAlpha(end)),
         LINEAR_TO_SRGB[MathHelper.lerp(delta, SRGB_TO_LINEAR[getRed(start)], SRGB_TO_LINEAR[getRed(end)])] & 0xFF,
         LINEAR_TO_SRGB[MathHelper.lerp(delta, SRGB_TO_LINEAR[getGreen(start)], SRGB_TO_LINEAR[getGreen(end)])] & 0xFF,
         LINEAR_TO_SRGB[MathHelper.lerp(delta, SRGB_TO_LINEAR[getBlue(start)], SRGB_TO_LINEAR[getBlue(end)])] & 0xFF
      );
   }

   public static int fullAlpha(int argb) {
      return argb | 0xFF000000;
   }

   public static int zeroAlpha(int argb) {
      return argb & 16777215;
   }

   public static int withAlpha(int alpha, int rgb) {
      return alpha << 24 | rgb & 16777215;
   }

   public static int withAlpha(float alpha, int color) {
      return channelFromFloat(alpha) << 24 | color & 16777215;
   }

   public static int getWhite(float alpha) {
      return channelFromFloat(alpha) << 24 | 16777215;
   }

   public static int whiteWithAlpha(int alpha) {
      return alpha << 24 | 16777215;
   }

   public static int toAlpha(float alpha) {
      return channelFromFloat(alpha) << 24;
   }

   public static int toAlpha(int alpha) {
      return alpha << 24;
   }

   public static int fromFloats(float alpha, float red, float green, float blue) {
      return getArgb(channelFromFloat(alpha), channelFromFloat(red), channelFromFloat(green), channelFromFloat(blue));
   }

   public static Vector3f toRgbVector(int rgb) {
      return new Vector3f(getRedFloat(rgb), getGreenFloat(rgb), getBlueFloat(rgb));
   }

   public static Vector4f toRgbaVector(int argb) {
      return new Vector4f(getRedFloat(argb), getGreenFloat(argb), getBlueFloat(argb), getAlphaFloat(argb));
   }

   public static int average(int first, int second) {
      return getArgb(
         (getAlpha(first) + getAlpha(second)) / 2,
         (getRed(first) + getRed(second)) / 2,
         (getGreen(first) + getGreen(second)) / 2,
         (getBlue(first) + getBlue(second)) / 2
      );
   }

   public static int channelFromFloat(float value) {
      return MathHelper.floor(value * 255.0F);
   }

   public static float getAlphaFloat(int argb) {
      return floatFromChannel(getAlpha(argb));
   }

   public static float getRedFloat(int argb) {
      return floatFromChannel(getRed(argb));
   }

   public static float getGreenFloat(int argb) {
      return floatFromChannel(getGreen(argb));
   }

   public static float getBlueFloat(int argb) {
      return floatFromChannel(getBlue(argb));
   }

   private static float floatFromChannel(int channel) {
      return channel / 255.0F;
   }

   public static int toAbgr(int argb) {
      return argb & -16711936 | (argb & 0xFF0000) >> 16 | (argb & 0xFF) << 16;
   }

   public static int fromAbgr(int abgr) {
      return toAbgr(abgr);
   }

   public static int withBrightness(int argb, float brightness) {
      int i = getRed(argb);
      int j = getGreen(argb);
      int k = getBlue(argb);
      int l = getAlpha(argb);
      int m = Math.max(Math.max(i, j), k);
      int n = Math.min(Math.min(i, j), k);
      float f = m - n;
      float g;
      if (m != 0) {
         g = f / m;
      } else {
         g = 0.0F;
      }

      float h;
      if (g == 0.0F) {
         h = 0.0F;
      } else {
         float o = (m - i) / f;
         float p = (m - j) / f;
         float q = (m - k) / f;
         if (i == m) {
            h = q - p;
         } else if (j == m) {
            h = 2.0F + o - q;
         } else {
            h = 4.0F + p - o;
         }

         h /= 6.0F;
         if (h < 0.0F) {
            h++;
         }
      }

      if (g == 0.0F) {
         i = j = k = Math.round(brightness * 255.0F);
         return getArgb(l, i, j, k);
      } else {
         float ox = (h - (float)Math.floor(h)) * 6.0F;
         float px = ox - (float)Math.floor(ox);
         float qx = brightness * (1.0F - g);
         float r = brightness * (1.0F - g * px);
         float s = brightness * (1.0F - g * (1.0F - px));
         switch ((int)ox) {
            case 0:
               i = Math.round(brightness * 255.0F);
               j = Math.round(s * 255.0F);
               k = Math.round(qx * 255.0F);
               break;
            case 1:
               i = Math.round(r * 255.0F);
               j = Math.round(brightness * 255.0F);
               k = Math.round(qx * 255.0F);
               break;
            case 2:
               i = Math.round(qx * 255.0F);
               j = Math.round(brightness * 255.0F);
               k = Math.round(s * 255.0F);
               break;
            case 3:
               i = Math.round(qx * 255.0F);
               j = Math.round(r * 255.0F);
               k = Math.round(brightness * 255.0F);
               break;
            case 4:
               i = Math.round(s * 255.0F);
               j = Math.round(qx * 255.0F);
               k = Math.round(brightness * 255.0F);
               break;
            case 5:
               i = Math.round(brightness * 255.0F);
               j = Math.round(qx * 255.0F);
               k = Math.round(r * 255.0F);
         }

         return getArgb(l, i, j, k);
      }
   }
}
