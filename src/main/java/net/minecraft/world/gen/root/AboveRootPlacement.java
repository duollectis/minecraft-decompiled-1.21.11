package net.minecraft.world.gen.root;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

/**
 * Конфигурация размещения блоков над корнями дерева — задаёт провайдер блока
 * и вероятность его появления над каждым корневым блоком.
 */
public record AboveRootPlacement(BlockStateProvider aboveRootProvider, float aboveRootPlacementChance) {

	public static final Codec<AboveRootPlacement> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			BlockStateProvider.TYPE_CODEC
				.fieldOf("above_root_provider")
				.forGetter(AboveRootPlacement::aboveRootProvider),
			Codec.floatRange(0.0F, 1.0F)
				.fieldOf("above_root_placement_chance")
				.forGetter(AboveRootPlacement::aboveRootPlacementChance)
		)
		.apply(instance, AboveRootPlacement::new)
	);
}
