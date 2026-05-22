package net.minecraft.util.math.random;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Утилитарный класс для генерации и преобразования сидов генераторов случайных чисел.
 * Содержит константы золотого и серебряного соотношений для 64-битного пространства,
 * а также методы для создания сидов алгоритма Xoroshiro128++.
 */
public final class RandomSeed {

	public static final long GOLDEN_RATIO_64 = -7046029254386353131L;
	public static final long SILVER_RATIO_64 = 7640891576956012809L;

	private static final HashFunction MD5_HASH = Hashing.md5();
	private static final AtomicLong SEED_UNIQUIFIER = new AtomicLong(8682522807148012L);

	/**
	 * Применяет финализатор Стаффорда-13 для улучшения статистических свойств сида.
	 * Используется для устранения корреляций в близких значениях сидов.
	 */
	@VisibleForTesting
	public static long mixStafford13(long seed) {
		seed = (seed ^ seed >>> 30) * -4658895280553007687L;
		seed = (seed ^ seed >>> 27) * -7723592293110705685L;
		return seed ^ seed >>> 31;
	}

	public static XoroshiroSeed createUnmixedXoroshiroSeed(long seed) {
		long lo = seed ^ SILVER_RATIO_64;
		long hi = lo + GOLDEN_RATIO_64;
		return new XoroshiroSeed(lo, hi);
	}

	public static XoroshiroSeed createXoroshiroSeed(long seed) {
		return createUnmixedXoroshiroSeed(seed).mix();
	}

	public static XoroshiroSeed createXoroshiroSeed(String seed) {
		byte[] bytes = MD5_HASH.hashString(seed, StandardCharsets.UTF_8).asBytes();
		long lo = Longs.fromBytes(bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);
		long hi = Longs.fromBytes(bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]);
		return new XoroshiroSeed(lo, hi);
	}

	/**
	 * Генерирует уникальный сид на основе системного времени и атомарного счётчика.
	 * Комбинирование двух источников энтропии снижает вероятность коллизий.
	 */
	public static long getSeed() {
		return SEED_UNIQUIFIER.updateAndGet(uniquifier -> uniquifier * 1181783497276652981L)
				^ System.nanoTime();
	}

	/**
	 * Пара 64-битных значений, образующих 128-битный сид для алгоритма Xoroshiro128++.
	 */
	public record XoroshiroSeed(long seedLo, long seedHi) {

		public XoroshiroSeed split(long lo, long hi) {
			return new XoroshiroSeed(seedLo ^ lo, seedHi ^ hi);
		}

		public XoroshiroSeed split(XoroshiroSeed seed) {
			return split(seed.seedLo, seed.seedHi);
		}

		public XoroshiroSeed mix() {
			return new XoroshiroSeed(
					mixStafford13(seedLo),
					mixStafford13(seedHi)
			);
		}
	}
}
