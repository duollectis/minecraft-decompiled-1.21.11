package net.minecraft.util.math;

import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.math.NumberUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * Утилитарный класс математических операций для игровой логики Minecraft.
 * Содержит быстрые тригонометрические функции, интерполяцию, работу с углами,
 * упаковку координат и другие вспомогательные вычисления.
 */
public class MathHelper {

	private static final long FLOAT_EXPONENT_MASK = 61440L;
	private static final long HALF_PI_RADIANS_SINE_TABLE_INDEX = 16384L;
	private static final long DOUBLE_SIGN_MASK = -4611686018427387904L;
	private static final long LONG_SIGN_BIT = Long.MIN_VALUE;
	public static final float PI = (float) Math.PI;
	public static final float HALF_PI = (float) (Math.PI / 2);
	public static final float TAU = (float) (Math.PI * 2);
	public static final float RADIANS_PER_DEGREE = (float) (Math.PI / 180.0);
	public static final float DEGREES_PER_RADIAN = 180.0F / (float) Math.PI;
	public static final float EPSILON = 1.0E-5F;
	public static final float SQUARE_ROOT_OF_TWO = sqrt(2.0F);
	public static final Vector3f Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
	public static final Vector3f X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
	public static final Vector3f Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
	private static final int SINE_TABLE_SIZE = 65536;
	private static final int SINE_TABLE_MASK = 65535;
	private static final int COSINE_TABLE_OFFSET = 16384;
	private static final double DEGREES_TO_SINE_TABLE_INDEX = 10430.378350470453;
	private static final float[] SINE_TABLE = Util.make(
			new float[SINE_TABLE_SIZE], sineTable -> {
				for (int ix = 0; ix < sineTable.length; ix++) {
					sineTable[ix] = (float) Math.sin(ix / DEGREES_TO_SINE_TABLE_INDEX);
				}
			}
	);
	private static final Random RANDOM = Random.createThreadSafe();
	private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
			0,
			1,
			28,
			2,
			29,
			14,
			24,
			3,
			30,
			22,
			20,
			15,
			25,
			17,
			4,
			8,
			31,
			27,
			13,
			23,
			21,
			19,
			16,
			7,
			26,
			12,
			18,
			6,
			11,
			5,
			10,
			9
	};
	private static final double ARCSINE_MACLAURIN_3 = 0.16666666666666666;
	private static final int ARCSINE_TABLE_BITS = 8;
	private static final int ARCSINE_TABLE_LENGTH = 257;
	private static final double ROUNDER_256THS = Double.longBitsToDouble(4805340802404319232L);
	private static final double[] ARCSINE_TABLE = new double[ARCSINE_TABLE_LENGTH];
	private static final double[] COSINE_OF_ARCSINE_TABLE = new double[ARCSINE_TABLE_LENGTH];

	/**
	 * Sin.
	 *
	 * @param value value
	 *
	 * @return float — результат операции
	 */
	public static float sin(double value) {
		return SINE_TABLE[(int) ((long) (value * DEGREES_TO_SINE_TABLE_INDEX) & SINE_TABLE_MASK)];
	}

	/**
	 * Cos.
	 *
	 * @param value value
	 *
	 * @return float — результат операции
	 */
	public static float cos(double value) {
		return SINE_TABLE[(int) ((long) (value * DEGREES_TO_SINE_TABLE_INDEX + 16384.0) & SINE_TABLE_MASK)];
	}

	/**
	 * Sqrt.
	 *
	 * @param value value
	 *
	 * @return float — результат операции
	 */
	public static float sqrt(float value) {
		return (float) Math.sqrt(value);
	}

	/**
	 * Floor.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int floor(float value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	/**
	 * Floor.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	/**
	 * Lfloor.
	 *
	 * @param value value
	 *
	 * @return long — результат операции
	 */
	public static long lfloor(double value) {
		long l = (long) value;
		return value < l ? l - 1L : l;
	}

	/**
	 * Abs.
	 *
	 * @param value value
	 *
	 * @return float — результат операции
	 */
	public static float abs(float value) {
		return Math.abs(value);
	}

	/**
	 * Abs.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int abs(int value) {
		return Math.abs(value);
	}

	/**
	 * Ceil.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int ceil(float value) {
		int i = (int) value;
		return value > i ? i + 1 : i;
	}

	/**
	 * Ceil.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int ceil(double value) {
		int i = (int) value;
		return value > i ? i + 1 : i;
	}

	/**
	 * Ceil long.
	 *
	 * @param value value
	 *
	 * @return long — результат операции
	 */
	public static long ceilLong(double value) {
		long l = (long) value;
		return value > l ? l + 1L : l;
	}

	/**
	 * Clamp.
	 *
	 * @param value value
	 * @param min min
	 * @param max max
	 *
	 * @return int — результат операции
	 */
	public static int clamp(int value, int min, int max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * Clamp.
	 *
	 * @param value value
	 * @param min min
	 * @param max max
	 *
	 * @return long — результат операции
	 */
	public static long clamp(long value, long min, long max) {
		return Math.min(Math.max(value, min), max);
	}

	/**
	 * Clamp.
	 *
	 * @param value value
	 * @param min min
	 * @param max max
	 *
	 * @return float — результат операции
	 */
	public static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}

	/**
	 * Clamp.
	 *
	 * @param value value
	 * @param min min
	 * @param max max
	 *
	 * @return double — результат операции
	 */
	public static double clamp(double value, double min, double max) {
		return value < min ? min : Math.min(value, max);
	}

	/**
	 * Clamped lerp.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return double — результат операции
	 */
	public static double clampedLerp(double delta, double start, double end) {
		if (delta < 0.0) {
			return start;
		}
		else {
			return delta > 1.0 ? end : lerp(delta, start, end);
		}
	}

	/**
	 * Clamped lerp.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return float — результат операции
	 */
	public static float clampedLerp(float delta, float start, float end) {
		if (delta < 0.0F) {
			return start;
		}
		else {
			return delta > 1.0F ? end : lerp(delta, start, end);
		}
	}

	/**
	 * Chebyshev distance.
	 *
	 * @param i i
	 * @param j j
	 *
	 * @return int — результат операции
	 */
	public static int chebyshevDistance(int i, int j) {
		return Math.max(Math.abs(i), Math.abs(j));
	}

	/**
	 * Chebyshev distance f.
	 *
	 * @param f f
	 * @param g g
	 *
	 * @return float — результат операции
	 */
	public static float chebyshevDistanceF(float f, float g) {
		return Math.max(Math.abs(f), Math.abs(g));
	}

	/**
	 * Abs max.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return double — результат операции
	 */
	public static double absMax(double a, double b) {
		return Math.max(Math.abs(a), Math.abs(b));
	}

	/**
	 * Chebyshev distance between.
	 *
	 * @param i i
	 * @param j j
	 * @param k k
	 * @param l l
	 *
	 * @return int — результат операции
	 */
	public static int chebyshevDistanceBetween(int i, int j, int k, int l) {
		return chebyshevDistance(k - i, l - j);
	}

	/**
	 * Floor div.
	 *
	 * @param dividend dividend
	 * @param divisor divisor
	 *
	 * @return int — результат операции
	 */
	public static int floorDiv(int dividend, int divisor) {
		return Math.floorDiv(dividend, divisor);
	}

	/**
	 * Next int.
	 *
	 * @param random random
	 * @param min min
	 * @param max max
	 *
	 * @return int — результат операции
	 */
	public static int nextInt(Random random, int min, int max) {
		return min >= max ? min : random.nextInt(max - min + 1) + min;
	}

	/**
	 * Next float.
	 *
	 * @param random random
	 * @param min min
	 * @param max max
	 *
	 * @return float — результат операции
	 */
	public static float nextFloat(Random random, float min, float max) {
		return min >= max ? min : random.nextFloat() * (max - min) + min;
	}

	/**
	 * Next double.
	 *
	 * @param random random
	 * @param min min
	 * @param max max
	 *
	 * @return double — результат операции
	 */
	public static double nextDouble(Random random, double min, double max) {
		return min >= max ? min : random.nextDouble() * (max - min) + min;
	}

	/**
	 * Approximately equals.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return boolean — результат операции
	 */
	public static boolean approximatelyEquals(float a, float b) {
		return Math.abs(b - a) < EPSILON;
	}

	/**
	 * Approximately equals.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return boolean — результат операции
	 */
	public static boolean approximatelyEquals(double a, double b) {
		return Math.abs(b - a) < EPSILON;
	}

	/**
	 * Floor mod.
	 *
	 * @param dividend dividend
	 * @param divisor divisor
	 *
	 * @return int — результат операции
	 */
	public static int floorMod(int dividend, int divisor) {
		return Math.floorMod(dividend, divisor);
	}

	/**
	 * Floor mod.
	 *
	 * @param dividend dividend
	 * @param divisor divisor
	 *
	 * @return float — результат операции
	 */
	public static float floorMod(float dividend, float divisor) {
		return (dividend % divisor + divisor) % divisor;
	}

	/**
	 * Floor mod.
	 *
	 * @param dividend dividend
	 * @param divisor divisor
	 *
	 * @return double — результат операции
	 */
	public static double floorMod(double dividend, double divisor) {
		return (dividend % divisor + divisor) % divisor;
	}

	public static boolean isMultipleOf(int a, int b) {
		return a % b == 0;
	}

	/**
	 * Pack degrees.
	 *
	 * @param degrees degrees
	 *
	 * @return byte — результат операции
	 */
	public static byte packDegrees(float degrees) {
		return (byte) floor(degrees * 256.0F / 360.0F);
	}

	/**
	 * Unpack degrees.
	 *
	 * @param packedDegrees packed degrees
	 *
	 * @return float — результат операции
	 */
	public static float unpackDegrees(byte packedDegrees) {
		return packedDegrees * 360 / 256.0F;
	}

	/**
	 * Wrap degrees.
	 *
	 * @param degrees degrees
	 *
	 * @return int — результат операции
	 */
	public static int wrapDegrees(int degrees) {
		int i = degrees % 360;
		if (i >= 180) {
			i -= 360;
		}

		if (i < -180) {
			i += 360;
		}

		return i;
	}

	/**
	 * Wrap degrees.
	 *
	 * @param degrees degrees
	 *
	 * @return float — результат операции
	 */
	public static float wrapDegrees(long degrees) {
		float f = (float) (degrees % 360L);
		if (f >= 180.0F) {
			f -= 360.0F;
		}

		if (f < -180.0F) {
			f += 360.0F;
		}

		return f;
	}

	/**
	 * Wrap degrees.
	 *
	 * @param degrees degrees
	 *
	 * @return float — результат операции
	 */
	public static float wrapDegrees(float degrees) {
		float f = degrees % 360.0F;
		if (f >= 180.0F) {
			f -= 360.0F;
		}

		if (f < -180.0F) {
			f += 360.0F;
		}

		return f;
	}

	/**
	 * Wrap degrees.
	 *
	 * @param degrees degrees
	 *
	 * @return double — результат операции
	 */
	public static double wrapDegrees(double degrees) {
		double d = degrees % 360.0;
		if (d >= 180.0) {
			d -= 360.0;
		}

		if (d < -180.0) {
			d += 360.0;
		}

		return d;
	}

	/**
	 * Subtract angles.
	 *
	 * @param start start
	 * @param end end
	 *
	 * @return float — результат операции
	 */
	public static float subtractAngles(float start, float end) {
		return wrapDegrees(end - start);
	}

	/**
	 * Angle between.
	 *
	 * @param first first
	 * @param second second
	 *
	 * @return float — результат операции
	 */
	public static float angleBetween(float first, float second) {
		return abs(subtractAngles(first, second));
	}

	/**
	 * Clamp angle.
	 *
	 * @param value value
	 * @param mean mean
	 * @param delta delta
	 *
	 * @return float — результат операции
	 */
	public static float clampAngle(float value, float mean, float delta) {
		float f = subtractAngles(value, mean);
		float g = clamp(f, -delta, delta);
		return mean - g;
	}

	/**
	 * Step towards.
	 *
	 * @param from from
	 * @param to to
	 * @param step step
	 *
	 * @return float — результат операции
	 */
	public static float stepTowards(float from, float to, float step) {
		step = abs(step);
		return from < to ? clamp(from + step, from, to) : clamp(from - step, to, from);
	}

	/**
	 * Step unwrapped angle towards.
	 *
	 * @param from from
	 * @param to to
	 * @param step step
	 *
	 * @return float — результат операции
	 */
	public static float stepUnwrappedAngleTowards(float from, float to, float step) {
		float f = subtractAngles(from, to);
		return stepTowards(from, from + f, step);
	}

	/**
	 * Разбирает int.
	 *
	 * @param string string
	 * @param fallback fallback
	 *
	 * @return int — результат операции
	 */
	public static int parseInt(String string, int fallback) {
		return NumberUtils.toInt(string, fallback);
	}

	/**
	 * Smallest encompassing power of two.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int smallestEncompassingPowerOfTwo(int value) {
		int i = value - 1;
		i |= i >> 1;
		i |= i >> 2;
		i |= i >> 4;
		i |= i >> 8;
		i |= i >> 16;
		return i + 1;
	}

	/**
	 * Smallest encompassing square side length.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int smallestEncompassingSquareSideLength(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("itemCount must be greater than or equal to zero");
		}
		else {
			return ceil(Math.sqrt(value));
		}
	}

	public static boolean isPowerOfTwo(int value) {
		return value != 0 && (value & value - 1) == 0;
	}

	/**
	 * Ceil log2.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int ceilLog2(int value) {
		value = isPowerOfTwo(value) ? value : smallestEncompassingPowerOfTwo(value);
		return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int) (value * 125613361L >> 27) & 31];
	}

	/**
	 * Floor log2.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int floorLog2(int value) {
		return ceilLog2(value) - (isPowerOfTwo(value) ? 0 : 1);
	}

	/**
	 * Fractional part.
	 *
	 * @param value value
	 *
	 * @return float — результат операции
	 */
	public static float fractionalPart(float value) {
		return value - floor(value);
	}

	/**
	 * Fractional part.
	 *
	 * @param value value
	 *
	 * @return double — результат операции
	 */
	public static double fractionalPart(double value) {
		return value - lfloor(value);
	}

	@Deprecated
	/**
	 * Проверяет наличие h code.
	 *
	 * @param vec vec
	 *
	 * @return long — {@code true} если условие выполнено
	 */
	public static long hashCode(Vec3i vec) {
		return hashCode(vec.getX(), vec.getY(), vec.getZ());
	}

	@Deprecated
	/**
	 * Проверяет наличие h code.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return long — {@code true} если условие выполнено
	 */
	public static long hashCode(int x, int y, int z) {
		long l = x * 3129871 ^ z * 116129781L ^ y;
		l = l * l * 42317861L + l * 11L;
		return l >> 16;
	}

	/**
	 * Random uuid.
	 *
	 * @param random random
	 *
	 * @return UUID — результат операции
	 */
	public static UUID randomUuid(Random random) {
		long l = random.nextLong() & -61441L | HALF_PI_RADIANS_SINE_TABLE_INDEX;
		long m = random.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
		return new UUID(l, m);
	}

	/**
	 * Random uuid.
	 *
	 * @return UUID — результат операции
	 */
	public static UUID randomUuid() {
		return randomUuid(RANDOM);
	}

	public static double getLerpProgress(double value, double start, double end) {
		return (value - start) / (end - start);
	}

	public static float getLerpProgress(float value, float start, float end) {
		return (value - start) / (end - start);
	}

	/**
	 * Intersects ray.
	 *
	 * @param origin origin
	 * @param direction direction
	 * @param box box
	 *
	 * @return boolean — результат операции
	 */
	public static boolean intersectsRay(Vec3d origin, Vec3d direction, Box box) {
		double d = (box.minX + box.maxX) * 0.5;
		double e = (box.maxX - box.minX) * 0.5;
		double f = origin.x - d;
		if (Math.abs(f) > e && f * direction.x >= 0.0) {
			return false;
		}
		else {
			double g = (box.minY + box.maxY) * 0.5;
			double h = (box.maxY - box.minY) * 0.5;
			double i = origin.y - g;
			if (Math.abs(i) > h && i * direction.y >= 0.0) {
				return false;
			}
			else {
				double j = (box.minZ + box.maxZ) * 0.5;
				double k = (box.maxZ - box.minZ) * 0.5;
				double l = origin.z - j;
				if (Math.abs(l) > k && l * direction.z >= 0.0) {
					return false;
				}
				else {
					double m = Math.abs(direction.x);
					double n = Math.abs(direction.y);
					double o = Math.abs(direction.z);
					double p = direction.y * l - direction.z * i;
					if (Math.abs(p) > h * o + k * n) {
						return false;
					}
					else {
						p = direction.z * f - direction.x * l;
						if (Math.abs(p) > e * o + k * m) {
							return false;
						}
						else {
							p = direction.x * i - direction.y * f;
							return Math.abs(p) < e * n + h * m;
						}
					}
				}
			}
		}
	}

	/**
	 * Atan2.
	 *
	 * @param y y
	 * @param x x
	 *
	 * @return double — результат операции
	 */
	public static double atan2(double y, double x) {
		double d = x * x + y * y;
		if (Double.isNaN(d)) {
			return Double.NaN;
		}
		else {
			boolean bl = y < 0.0;
			if (bl) {
				y = -y;
			}

			boolean bl2 = x < 0.0;
			if (bl2) {
				x = -x;
			}

			boolean bl3 = y > x;
			if (bl3) {
				double e = x;
				x = y;
				y = e;
			}

			double e = fastInverseSqrt(d);
			x *= e;
			y *= e;
			double f = ROUNDER_256THS + y;
			int i = (int) Double.doubleToRawLongBits(f);
			double g = ARCSINE_TABLE[i];
			double h = COSINE_OF_ARCSINE_TABLE[i];
			double j = f - ROUNDER_256THS;
			double k = y * h - x * j;
			double l = (6.0 + k * k) * k * ARCSINE_MACLAURIN_3;
			double m = g + l;
			if (bl3) {
				m = (Math.PI / 2) - m;
			}

			if (bl2) {
				m = Math.PI - m;
			}

			if (bl) {
				m = -m;
			}

			return m;
		}
	}

	/**
	 * Inverse sqrt.
	 *
	 * @param x x
	 *
	 * @return float — результат операции
	 */
	public static float inverseSqrt(float x) {
		return org.joml.Math.invsqrt(x);
	}

	/**
	 * Inverse sqrt.
	 *
	 * @param x x
	 *
	 * @return double — результат операции
	 */
	public static double inverseSqrt(double x) {
		return org.joml.Math.invsqrt(x);
	}

	@Deprecated
	/**
	 * Fast inverse sqrt.
	 *
	 * @param x x
	 *
	 * @return double — результат операции
	 */
	public static double fastInverseSqrt(double x) {
		double d = 0.5 * x;
		long l = Double.doubleToRawLongBits(x);
		l = 6910469410427058090L - (l >> 1);
		x = Double.longBitsToDouble(l);
		return x * (1.5 - d * x * x);
	}

	/**
	 * Fast inverse cbrt.
	 *
	 * @param x x
	 *
	 * @return float — результат операции
	 */
	public static float fastInverseCbrt(float x) {
		int i = Float.floatToIntBits(x);
		i = 1419967116 - i / 3;
		float f = Float.intBitsToFloat(i);
		f = 0.6666667F * f + 1.0F / (3.0F * f * f * x);
		return 0.6666667F * f + 1.0F / (3.0F * f * f * x);
	}

	/**
	 * Hsv to rgb.
	 *
	 * @param hue hue
	 * @param saturation saturation
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int hsvToRgb(float hue, float saturation, float value) {
		return hsvToArgb(hue, saturation, value, 0);
	}

	/**
	 * Hsv to argb.
	 *
	 * @param hue hue
	 * @param saturation saturation
	 * @param value value
	 * @param alpha alpha
	 *
	 * @return int — результат операции
	 */
	public static int hsvToArgb(float hue, float saturation, float value, int alpha) {
		int i = (int) (hue * 6.0F) % 6;
		float f = hue * 6.0F - i;
		float g = value * (1.0F - saturation);
		float h = value * (1.0F - f * saturation);
		float j = value * (1.0F - (1.0F - f) * saturation);
		float k;
		float l;
		float m;
		switch (i) {
			case 0:
				k = value;
				l = j;
				m = g;
				break;
			case 1:
				k = h;
				l = value;
				m = g;
				break;
			case 2:
				k = g;
				l = value;
				m = j;
				break;
			case 3:
				k = g;
				l = h;
				m = value;
				break;
			case 4:
				k = j;
				l = g;
				m = value;
				break;
			case 5:
				k = value;
				l = g;
				m = h;
				break;
			default:
				throw new RuntimeException(
						"Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation
								+ ", " + value);
		}

		return ColorHelper.getArgb(
				alpha,
				clamp((int) (k * 255.0F), 0, 255),
				clamp((int) (l * 255.0F), 0, 255),
				clamp((int) (m * 255.0F), 0, 255)
		);
	}

	/**
	 * Ideal hash.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int idealHash(int value) {
		value ^= value >>> 16;
		value *= -2048144789;
		value ^= value >>> 13;
		value *= -1028477387;
		return value ^ value >>> 16;
	}

	/**
	 * Binary search.
	 *
	 * @param min min
	 * @param max max
	 * @param predicate predicate
	 *
	 * @return int — результат операции
	 */
	public static int binarySearch(int min, int max, IntPredicate predicate) {
		int i = max - min;

		while (i > 0) {
			int j = i / 2;
			int k = min + j;
			if (predicate.test(k)) {
				i = j;
			}
			else {
				min = k + 1;
				i -= j + 1;
			}
		}

		return min;
	}

	/**
	 * Lerp.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return int — результат операции
	 */
	public static int lerp(float delta, int start, int end) {
		return start + floor(delta * (end - start));
	}

	/**
	 * Lerp positive.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return int — результат операции
	 */
	public static int lerpPositive(float delta, int start, int end) {
		int i = end - start;
		return start + floor(delta * (i - 1)) + (delta > 0.0F ? 1 : 0);
	}

	/**
	 * Lerp.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return float — результат операции
	 */
	public static float lerp(float delta, float start, float end) {
		return start + delta * (end - start);
	}

	/**
	 * Lerp.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return Vec3d — результат операции
	 */
	public static Vec3d lerp(double delta, Vec3d start, Vec3d end) {
		return new Vec3d(lerp(delta, start.x, end.x), lerp(delta, start.y, end.y), lerp(delta, start.z, end.z));
	}

	/**
	 * Lerp.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return double — результат операции
	 */
	public static double lerp(double delta, double start, double end) {
		return start + delta * (end - start);
	}

	/**
	 * Lerp2.
	 *
	 * @param deltaX delta x
	 * @param deltaY delta y
	 * @param x0y0 x0y0
	 * @param x1y0 x1y0
	 * @param x0y1 x0y1
	 * @param x1y1 x1y1
	 *
	 * @return double — результат операции
	 */
	public static double lerp2(double deltaX, double deltaY, double x0y0, double x1y0, double x0y1, double x1y1) {
		return lerp(deltaY, lerp(deltaX, x0y0, x1y0), lerp(deltaX, x0y1, x1y1));
	}

	public static double lerp3(
			double deltaX,
			double deltaY,
			double deltaZ,
			double x0y0z0,
			double x1y0z0,
			double x0y1z0,
			double x1y1z0,
			double x0y0z1,
			double x1y0z1,
			double x0y1z1,
			double x1y1z1
	) {
		return lerp(
				deltaZ,
				lerp2(deltaX, deltaY, x0y0z0, x1y0z0, x0y1z0, x1y1z0),
				lerp2(deltaX, deltaY, x0y0z1, x1y0z1, x0y1z1, x1y1z1)
		);
	}

	/**
	 * Catmull rom.
	 *
	 * @param delta delta
	 * @param p0 p0
	 * @param p1 p1
	 * @param p2 p2
	 * @param p3 p3
	 *
	 * @return float — результат операции
	 */
	public static float catmullRom(float delta, float p0, float p1, float p2, float p3) {
		return 0.5F
				* (
				2.0F * p1
						+ (p2 - p0) * delta
						+ (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * delta * delta
						+ (3.0F * p1 - p0 - 3.0F * p2 + p3) * delta * delta * delta
		);
	}

	/**
	 * Perlin fade.
	 *
	 * @param value value
	 *
	 * @return double — результат операции
	 */
	public static double perlinFade(double value) {
		return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
	}

	/**
	 * Perlin fade derivative.
	 *
	 * @param value value
	 *
	 * @return double — результат операции
	 */
	public static double perlinFadeDerivative(double value) {
		return 30.0 * value * value * (value - 1.0) * (value - 1.0);
	}

	/**
	 * Sign.
	 *
	 * @param value value
	 *
	 * @return int — результат операции
	 */
	public static int sign(double value) {
		if (value == 0.0) {
			return 0;
		}
		else {
			return value > 0.0 ? 1 : -1;
		}
	}

	/**
	 * Lerp angle degrees.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return float — результат операции
	 */
	public static float lerpAngleDegrees(float delta, float start, float end) {
		return start + delta * wrapDegrees(end - start);
	}

	/**
	 * Lerp angle degrees.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return double — результат операции
	 */
	public static double lerpAngleDegrees(double delta, double start, double end) {
		return start + delta * wrapDegrees(end - start);
	}

	/**
	 * Lerp angle radians.
	 *
	 * @param delta delta
	 * @param start start
	 * @param end end
	 *
	 * @return float — результат операции
	 */
	public static float lerpAngleRadians(float delta, float start, float end) {
		float f = end - start;

		while (f < (float) -Math.PI) {
			f += (float) (Math.PI * 2);
		}

		while (f >= (float) Math.PI) {
			f -= (float) (Math.PI * 2);
		}

		return start + delta * f;
	}

	/**
	 * Wrap.
	 *
	 * @param value value
	 * @param maxDeviation max deviation
	 *
	 * @return float — результат операции
	 */
	public static float wrap(float value, float maxDeviation) {
		return (Math.abs(value % maxDeviation - maxDeviation * 0.5F) - maxDeviation * 0.25F) / (maxDeviation * 0.25F);
	}

	/**
	 * Square.
	 *
	 * @param n n
	 *
	 * @return float — результат операции
	 */
	public static float square(float n) {
		return n * n;
	}

	/**
	 * Cube.
	 *
	 * @param n n
	 *
	 * @return float — результат операции
	 */
	public static float cube(float n) {
		return n * n * n;
	}

	/**
	 * Square.
	 *
	 * @param n n
	 *
	 * @return double — результат операции
	 */
	public static double square(double n) {
		return n * n;
	}

	/**
	 * Square.
	 *
	 * @param n n
	 *
	 * @return int — результат операции
	 */
	public static int square(int n) {
		return n * n;
	}

	/**
	 * Square.
	 *
	 * @param n n
	 *
	 * @return long — результат операции
	 */
	public static long square(long n) {
		return n * n;
	}

	/**
	 * Clamped map.
	 *
	 * @param value value
	 * @param oldStart old start
	 * @param oldEnd old end
	 * @param newStart new start
	 * @param newEnd new end
	 *
	 * @return double — результат операции
	 */
	public static double clampedMap(double value, double oldStart, double oldEnd, double newStart, double newEnd) {
		return clampedLerp(getLerpProgress(value, oldStart, oldEnd), newStart, newEnd);
	}

	/**
	 * Clamped map.
	 *
	 * @param value value
	 * @param oldStart old start
	 * @param oldEnd old end
	 * @param newStart new start
	 * @param newEnd new end
	 *
	 * @return float — результат операции
	 */
	public static float clampedMap(float value, float oldStart, float oldEnd, float newStart, float newEnd) {
		return clampedLerp(getLerpProgress(value, oldStart, oldEnd), newStart, newEnd);
	}

	/**
	 * Map.
	 *
	 * @param value value
	 * @param oldStart old start
	 * @param oldEnd old end
	 * @param newStart new start
	 * @param newEnd new end
	 *
	 * @return double — результат операции
	 */
	public static double map(double value, double oldStart, double oldEnd, double newStart, double newEnd) {
		return lerp(getLerpProgress(value, oldStart, oldEnd), newStart, newEnd);
	}

	/**
	 * Map.
	 *
	 * @param value value
	 * @param oldStart old start
	 * @param oldEnd old end
	 * @param newStart new start
	 * @param newEnd new end
	 *
	 * @return float — результат операции
	 */
	public static float map(float value, float oldStart, float oldEnd, float newStart, float newEnd) {
		return lerp(getLerpProgress(value, oldStart, oldEnd), newStart, newEnd);
	}

	/**
	 * Добавляет noise.
	 *
	 * @param d d
	 *
	 * @return double — результат операции
	 */
	public static double addNoise(double d) {
		return d + (2.0 * Random.create(floor(d * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
	}

	/**
	 * Round up to multiple.
	 *
	 * @param value value
	 * @param divisor divisor
	 *
	 * @return int — результат операции
	 */
	public static int roundUpToMultiple(int value, int divisor) {
		return ceilDiv(value, divisor) * divisor;
	}

	/**
	 * Ceil div.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return int — результат операции
	 */
	public static int ceilDiv(int a, int b) {
		return -Math.floorDiv(-a, b);
	}

	/**
	 * Next between.
	 *
	 * @param random random
	 * @param min min
	 * @param max max
	 *
	 * @return int — результат операции
	 */
	public static int nextBetween(Random random, int min, int max) {
		return random.nextInt(max - min + 1) + min;
	}

	/**
	 * Next between.
	 *
	 * @param random random
	 * @param min min
	 * @param max max
	 *
	 * @return float — результат операции
	 */
	public static float nextBetween(Random random, float min, float max) {
		return random.nextFloat() * (max - min) + min;
	}

	/**
	 * Next gaussian.
	 *
	 * @param random random
	 * @param mean mean
	 * @param deviation deviation
	 *
	 * @return float — результат операции
	 */
	public static float nextGaussian(Random random, float mean, float deviation) {
		return mean + (float) random.nextGaussian() * deviation;
	}

	/**
	 * Squared hypot.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return double — результат операции
	 */
	public static double squaredHypot(double a, double b) {
		return a * a + b * b;
	}

	/**
	 * Hypot.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return double — результат операции
	 */
	public static double hypot(double a, double b) {
		return Math.sqrt(squaredHypot(a, b));
	}

	/**
	 * Hypot.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return float — результат операции
	 */
	public static float hypot(float a, float b) {
		return (float) Math.sqrt(squaredHypot(a, b));
	}

	/**
	 * Squared magnitude.
	 *
	 * @param a a
	 * @param b b
	 * @param c c
	 *
	 * @return double — результат операции
	 */
	public static double squaredMagnitude(double a, double b, double c) {
		return a * a + b * b + c * c;
	}

	/**
	 * Magnitude.
	 *
	 * @param a a
	 * @param b b
	 * @param c c
	 *
	 * @return double — результат операции
	 */
	public static double magnitude(double a, double b, double c) {
		return Math.sqrt(squaredMagnitude(a, b, c));
	}

	/**
	 * Magnitude.
	 *
	 * @param a a
	 * @param b b
	 * @param c c
	 *
	 * @return float — результат операции
	 */
	public static float magnitude(float a, float b, float c) {
		return a * a + b * b + c * c;
	}

	/**
	 * Round down to multiple.
	 *
	 * @param a a
	 * @param b b
	 *
	 * @return int — результат операции
	 */
	public static int roundDownToMultiple(double a, int b) {
		return floor(a / b) * b;
	}

	/**
	 * Stream.
	 *
	 * @param seed seed
	 * @param lowerBound lower bound
	 * @param upperBound upper bound
	 *
	 * @return IntStream — результат операции
	 */
	public static IntStream stream(int seed, int lowerBound, int upperBound) {
		return stream(seed, lowerBound, upperBound, 1);
	}

	/**
	 * Stream.
	 *
	 * @param seed seed
	 * @param lowerBound lower bound
	 * @param upperBound upper bound
	 * @param steps steps
	 *
	 * @return IntStream — результат операции
	 */
	public static IntStream stream(int seed, int lowerBound, int upperBound, int steps) {
		if (lowerBound > upperBound) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"upperBound %d expected to be > lowerBound %d",
					upperBound,
					lowerBound
			));
		}
		else if (steps < 1) {
			throw new IllegalArgumentException(String.format(
					Locale.ROOT,
					"step size expected to be >= 1, was %d",
					steps
			));
		}
		else {
			int i = clamp(seed, lowerBound, upperBound);
			return IntStream.iterate(
					i, ix -> {
						int m = Math.abs(i - ix);
						return i - m >= lowerBound || i + m <= upperBound;
					}, ix -> {
						boolean bl = ix <= i;
						int n = Math.abs(i - ix);
						boolean bl2 = i + n + steps <= upperBound;
						if (!bl || !bl2) {
							int o = i - n - (bl ? steps : 0);
							if (o >= lowerBound) {
								return o;
							}
						}

						return i + n + steps;
					}
			);
		}
	}

	/**
	 * Rotate around.
	 *
	 * @param axis axis
	 * @param rotation rotation
	 * @param result result
	 *
	 * @return Quaternionf — результат операции
	 */
	public static Quaternionf rotateAround(Vector3f axis, Quaternionf rotation, Quaternionf result) {
		float f = axis.dot(rotation.x, rotation.y, rotation.z);
		return result.set(axis.x * f, axis.y * f, axis.z * f, rotation.w).normalize();
	}

	/**
	 * Multiply fraction.
	 *
	 * @param fraction fraction
	 * @param multiplier multiplier
	 *
	 * @return int — результат операции
	 */
	public static int multiplyFraction(Fraction fraction, int multiplier) {
		return fraction.getNumerator() * multiplier / fraction.getDenominator();
	}

	static {
		for (int i = 0; i < ARCSINE_TABLE_LENGTH; i++) {
			double d = i / 256.0;
			double e = Math.asin(d);
			COSINE_OF_ARCSINE_TABLE[i] = Math.cos(e);
			ARCSINE_TABLE[i] = e;
		}
	}
}
