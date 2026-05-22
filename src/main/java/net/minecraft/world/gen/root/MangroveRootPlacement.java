package net.minecraft.world.gen.root;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

/**
 * Конфигурация корневой системы мангрового дерева — задаёт блоки, сквозь которые
 * могут расти корни, блоки для грязных корней, а также параметры ширины и длины.
 */
public record MangroveRootPlacement(
	RegistryEntryList<Block> canGrowThrough,
	RegistryEntryList<Block> muddyRootsIn,
	BlockStateProvider muddyRootsProvider,
	int maxRootWidth,
	int maxRootLength,
	float randomSkewChance
) {

	public static final Codec<MangroveRootPlacement> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			RegistryCodecs.entryList(RegistryKeys.BLOCK)
				.fieldOf("can_grow_through")
				.forGetter(MangroveRootPlacement::canGrowThrough),
			RegistryCodecs.entryList(RegistryKeys.BLOCK)
				.fieldOf("muddy_roots_in")
				.forGetter(MangroveRootPlacement::muddyRootsIn),
			BlockStateProvider.TYPE_CODEC
				.fieldOf("muddy_roots_provider")
				.forGetter(MangroveRootPlacement::muddyRootsProvider),
			Codec.intRange(1, 12)
				.fieldOf("max_root_width")
				.forGetter(MangroveRootPlacement::maxRootWidth),
			Codec.intRange(1, 64)
				.fieldOf("max_root_length")
				.forGetter(MangroveRootPlacement::maxRootLength),
			Codec.floatRange(0.0F, 1.0F)
				.fieldOf("random_skew_chance")
				.forGetter(MangroveRootPlacement::randomSkewChance)
		)
		.apply(instance, MangroveRootPlacement::new)
	);
}
