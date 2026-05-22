package net.minecraft.world.biome.source;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

/**
 * Функциональный интерфейс для получения биома по координатам шума.
 * Реализуется {@link BiomeSource} и его подклассами.
 */
@FunctionalInterface
public interface BiomeSupplier {

	RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise);
}
