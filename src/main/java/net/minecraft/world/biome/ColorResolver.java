package net.minecraft.world.biome;

@FunctionalInterface
/**
 * {@code ColorResolver}.
 */
public interface ColorResolver {

	int getColor(Biome biome, double x, double z);
}
