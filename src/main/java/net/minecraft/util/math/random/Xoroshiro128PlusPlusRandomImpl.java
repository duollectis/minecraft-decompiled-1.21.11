package net.minecraft.util.math.random;

import com.mojang.serialization.Codec;
import net.minecraft.util.Util;

import java.util.stream.LongStream;

/**
 * Низкоуровневая реализация алгоритма Xoroshiro128++ — современного генератора
 * псевдослучайных чисел с периодом 2^128 - 1 и отличными статистическими свойствами.
 * Хранит 128-битное состояние в двух полях {@code seedLo} и {@code seedHi}.
 * При нулевом состоянии автоматически инициализируется константами золотого и серебряного соотношений.
 */
public class Xoroshiro128PlusPlusRandomImpl {

	public static final Codec<Xoroshiro128PlusPlusRandomImpl> CODEC = Codec.LONG_STREAM
			.comapFlatMap(
					stream -> Util
							.decodeFixedLengthArray(stream, 2)
							.map(seeds -> new Xoroshiro128PlusPlusRandomImpl(seeds[0], seeds[1])),
					impl -> LongStream.of(impl.seedLo, impl.seedHi)
			);

	private long seedLo;
	private long seedHi;

	public Xoroshiro128PlusPlusRandomImpl(RandomSeed.XoroshiroSeed seed) {
		this(seed.seedLo(), seed.seedHi());
	}

	public Xoroshiro128PlusPlusRandomImpl(long seedLo, long seedHi) {
		this.seedLo = seedLo;
		this.seedHi = seedHi;

		// Нулевое состояние недопустимо для xoroshiro — инициализируем константами φ и ψ
		if ((this.seedLo | this.seedHi) == 0L) {
			this.seedLo = RandomSeed.GOLDEN_RATIO_64;
			this.seedHi = RandomSeed.SILVER_RATIO_64;
		}
	}

	/**
	 * Вычисляет следующее 64-битное значение и обновляет внутреннее состояние.
	 * Формула: result = rotl(lo + hi, 17) + lo; затем hi ^= lo; lo = rotl(lo, 49) ^ hi ^ (hi << 21); hi = rotl(hi, 28).
	 */
	public long next() {
		long lo = seedLo;
		long hi = seedHi;
		long result = Long.rotateLeft(lo + hi, 17) + lo;

		hi ^= lo;
		seedLo = Long.rotateLeft(lo, 49) ^ hi ^ hi << 21;
		seedHi = Long.rotateLeft(hi, 28);

		return result;
	}
}
