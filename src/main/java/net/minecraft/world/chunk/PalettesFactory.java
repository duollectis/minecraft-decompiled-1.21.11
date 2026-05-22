package net.minecraft.world.chunk;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

/**
 * Фабрика палитризованных контейнеров для блок-стейтов и биомов.
 * Создаётся один раз при инициализации мира и переиспользуется для всех секций.
 */
public record PalettesFactory(
	PaletteProvider<BlockState> blockStatesStrategy,
	BlockState defaultBlockState,
	Codec<PalettedContainer<BlockState>> blockStatesContainerCodec,
	PaletteProvider<RegistryEntry<Biome>> biomeStrategy,
	RegistryEntry<Biome> defaultBiome,
	Codec<ReadableContainer<RegistryEntry<Biome>>> biomeContainerCodec
) {

	/**
	 * Создаёт фабрику на основе динамического реестра.
	 * Дефолтный блок — воздух, дефолтный биом — равнины.
	 */
	public static PalettesFactory fromRegistryManager(DynamicRegistryManager registryManager) {
		PaletteProvider<BlockState> blockStatesProvider = PaletteProvider.forBlockStates(Block.STATE_IDS);
		BlockState defaultBlock = Blocks.AIR.getDefaultState();
		Registry<Biome> biomeRegistry = registryManager.getOrThrow(RegistryKeys.BIOME);
		PaletteProvider<RegistryEntry<Biome>> biomeProvider = PaletteProvider.forBiomes(biomeRegistry.getIndexedEntries());
		RegistryEntry.Reference<Biome> plainsEntry = biomeRegistry.getOrThrow(BiomeKeys.PLAINS);
		return new PalettesFactory(
			blockStatesProvider,
			defaultBlock,
			PalettedContainer.createPalettedContainerCodec(BlockState.CODEC, blockStatesProvider, defaultBlock),
			biomeProvider,
			plainsEntry,
			PalettedContainer.createReadableContainerCodec(biomeRegistry.getEntryCodec(), biomeProvider, plainsEntry)
		);
	}

	public PalettedContainer<BlockState> getBlockStateContainer() {
		return new PalettedContainer<>(defaultBlockState, blockStatesStrategy);
	}

	public PalettedContainer<RegistryEntry<Biome>> getBiomeContainer() {
		return new PalettedContainer<>(defaultBiome, biomeStrategy);
	}
}
