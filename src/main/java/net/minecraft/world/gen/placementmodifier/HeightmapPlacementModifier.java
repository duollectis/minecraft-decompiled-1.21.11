package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.Stream;

/**
 * Модификатор размещения, смещающий Y-координату позиции на высоту поверхности
 * согласно указанному типу карты высот.
 */
public class HeightmapPlacementModifier extends PlacementModifier {

	public static final MapCodec<HeightmapPlacementModifier> MODIFIER_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(Heightmap.Type.CODEC
				.fieldOf("heightmap")
				.forGetter(modifier -> modifier.heightmap))
			.apply(instance, HeightmapPlacementModifier::new)
	);
	private final Heightmap.Type heightmap;

	private HeightmapPlacementModifier(Heightmap.Type heightmap) {
		this.heightmap = heightmap;
	}

	public static HeightmapPlacementModifier of(Heightmap.Type heightmap) {
		return new HeightmapPlacementModifier(heightmap);
	}

	@Override
	public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		int x = pos.getX();
		int z = pos.getZ();
		int topY = context.getTopY(heightmap, x, z);
		return topY > context.getBottomY() ? Stream.of(new BlockPos(x, topY, z)) : Stream.of();
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.HEIGHTMAP;
	}
}
