package net.minecraft.network.encoding;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Утилитарный класс для компактного кодирования вектора скорости сущности в сетевом протоколе.
 *
 * <p>Скорость кодируется в 6 байт (или 10 при «быстром» режиме): три 15-битных компонента
 * нормализуются относительно максимальной составляющей, которая хранится отдельно.
 * Бит {@code FAST_MARKER_BIT} сигнализирует, что масштаб не помещается в 2 бита и
 * дополнительно читается как VarInt.</p>
 */
public class VelocityEncoding {

	private static final int VELOCITY_BITS = 15;
	private static final int MAX_15_BIT_INT = 32767;
	private static final double MAX_VELOCITY_VALUE = 32766.0;
	private static final int SLOW_BITS = 2;
	private static final int SLOW_BIT_MASK = 3;
	private static final int FAST_MARKER_BIT = 4;
	private static final int X_SHIFT = 3;
	private static final int Y_SHIFT = 18;
	private static final int Z_SHIFT = 33;
	public static final double MAX_VELOCITY = 1.7179869183E10;
	public static final double MIN_VELOCITY = 3.051944088384301E-5;

	public static boolean hasFastMarkerBit(int maxDirectionalVelocity) {
		return (maxDirectionalVelocity & FAST_MARKER_BIT) == FAST_MARKER_BIT;
	}

	/**
	 * Читает вектор скорости из буфера.
	 * Первый байт {@code 0} означает нулевую скорость ({@link Vec3d#ZERO}).
	 */
	public static Vec3d readVelocity(ByteBuf buf) {
		int firstByte = buf.readUnsignedByte();
		if (firstByte == 0) {
			return Vec3d.ZERO;
		}

		int secondByte = buf.readUnsignedByte();
		long packed = buf.readUnsignedInt();
		long combined = packed << 16 | secondByte << 8 | firstByte;
		long scale = firstByte & SLOW_BIT_MASK;

		if (hasFastMarkerBit(firstByte)) {
			scale |= (VarInts.read(buf) & 4294967295L) << SLOW_BITS;
		}

		return new Vec3d(
				fromLong(combined >> X_SHIFT) * scale,
				fromLong(combined >> Y_SHIFT) * scale,
				fromLong(combined >> Z_SHIFT) * scale
		);
	}

	/**
	 * Записывает вектор скорости в буфер.
	 * Если максимальная составляющая меньше {@link #MIN_VELOCITY}, записывается один нулевой байт.
	 */
	public static void writeVelocity(ByteBuf buf, Vec3d velocity) {
		double vx = clampValue(velocity.x);
		double vy = clampValue(velocity.y);
		double vz = clampValue(velocity.z);
		double maxComponent = MathHelper.absMax(vx, MathHelper.absMax(vy, vz));

		if (maxComponent < MIN_VELOCITY) {
			buf.writeByte(0);
			return;
		}

		long scale = MathHelper.ceilLong(maxComponent);
		boolean needsExtraScale = (scale & SLOW_BIT_MASK) != scale;
		long scaleBits = needsExtraScale ? scale & SLOW_BIT_MASK | FAST_MARKER_BIT : scale;
		long xBits = toLong(vx / scale) << X_SHIFT;
		long yBits = toLong(vy / scale) << Y_SHIFT;
		long zBits = toLong(vz / scale) << Z_SHIFT;
		long encoded = scaleBits | xBits | yBits | zBits;

		buf.writeByte((byte) encoded);
		buf.writeByte((byte) (encoded >> 8));
		buf.writeInt((int) (encoded >> 16));

		if (needsExtraScale) {
			VarInts.write(buf, (int) (scale >> SLOW_BITS));
		}
	}

	private static double clampValue(double value) {
		return Double.isNaN(value) ? 0.0 : Math.clamp(value, -MAX_VELOCITY, MAX_VELOCITY);
	}

	private static long toLong(double value) {
		return Math.round((value * 0.5 + 0.5) * MAX_VELOCITY_VALUE);
	}

	private static double fromLong(long value) {
		return Math.min((double) (value & MAX_15_BIT_INT), MAX_VELOCITY_VALUE) * 2.0 / MAX_VELOCITY_VALUE - 1.0;
	}
}
