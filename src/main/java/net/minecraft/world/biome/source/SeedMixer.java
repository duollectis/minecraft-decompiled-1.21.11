package net.minecraft.world.biome.source;

/**
 * Утилитарный класс для смешивания сидов на основе линейного конгруэнтного генератора (LCG).
 * Используется в {@link BiomeAccess} для создания псевдослучайных смещений угловых точек Вороного.
 */
public class SeedMixer {

	/** Множитель LCG (константа Кнута для 64-битного LCG). */
	private static final long LCG_MULTIPLIER = 6364136223846793005L;

	/** Инкремент LCG (константа Кнута для 64-битного LCG). */
	private static final long LCG_INCREMENT = 1442695040888963407L;

	/**
	 * Смешивает сид с солью через один шаг LCG.
	 * Результат детерминирован и зависит от обоих входных значений.
	 */
	public static long mixSeed(long seed, long salt) {
		seed *= seed * LCG_MULTIPLIER + LCG_INCREMENT;
		return seed + salt;
	}
}
