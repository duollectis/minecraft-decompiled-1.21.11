package net.minecraft.world.biome.source;

/**
 * {@code SeedMixer}.
 */
public class SeedMixer {

	private static final long LCG_MULTIPLIER = 6364136223846793005L;
	private static final long LCG_INCREMENT = 1442695040888963407L;

	public static long mixSeed(long seed, long salt) {
		seed *= seed * 6364136223846793005L + 1442695040888963407L;
		return seed + salt;
	}
}
