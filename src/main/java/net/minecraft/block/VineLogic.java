package net.minecraft.block;

import net.minecraft.util.math.random.Random;

/**
 * {@code VineLogic}.
 */
public class VineLogic {

	private static final double GROWTH_DECAY_FACTOR = 0.826;
	public static final double MIN_GROWTH_CHANCE = 0.1;

	public static boolean isValidForWeepingStem(BlockState state) {
		return state.isAir();
	}

	public static int getGrowthLength(Random random) {
		double d = 1.0;

		int i;
		for (i = 0; random.nextDouble() < d; i++) {
			d *= 0.826;
		}

		return i;
	}
}
