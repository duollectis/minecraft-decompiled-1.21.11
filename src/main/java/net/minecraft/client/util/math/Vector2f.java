package net.minecraft.client.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record Vector2f(float x, float y) {

	private static final long UINT_MASK = 4294967295L;

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

	/**
	 * Упаковывает два float-значения в одно long-значение для эффективного хранения.
	 * Старшие 32 бита — X, младшие 32 бита — Y.
	 *
	 * @param x компонента X
	 * @param y компонента Y
	 * @return упакованное long-значение
	 */
	public static long toLong(float x, float y) {
		long packedX = Float.floatToIntBits(x) & UINT_MASK;
		long packedY = Float.floatToIntBits(y) & UINT_MASK;
		return packedX << 32 | packedY;
	}

	public static float getX(long packed) {
		return Float.intBitsToFloat((int) (packed >> 32));
	}

	public static float getY(long packed) {
		return Float.intBitsToFloat((int) packed);
	}
}
