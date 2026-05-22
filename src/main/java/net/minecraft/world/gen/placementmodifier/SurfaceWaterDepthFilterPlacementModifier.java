package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

/**
 * Модификатор размещения, фильтрующий позиции по глубине воды над поверхностью —
 * пропускает только те, где глубина воды не превышает {@code maxWaterDepth} блоков.
 */
public class SurfaceWaterDepthFilterPlacementModifier extends AbstractConditionalPlacementModifier {

	public static final MapCodec<SurfaceWaterDepthFilterPlacementModifier> MODIFIER_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(Codec.INT
				.fieldOf("max_water_depth")
				.forGetter(modifier -> modifier.maxWaterDepth))
			.apply(instance, SurfaceWaterDepthFilterPlacementModifier::new)
	);
	private final int maxWaterDepth;

	private SurfaceWaterDepthFilterPlacementModifier(int maxWaterDepth) {
		this.maxWaterDepth = maxWaterDepth;
	}

	public static SurfaceWaterDepthFilterPlacementModifier of(int maxWaterDepth) {
		return new SurfaceWaterDepthFilterPlacementModifier(maxWaterDepth);
	}

	@Override
	protected boolean shouldPlace(FeaturePlacementContext context, Random random, BlockPos pos) {
		int oceanFloorY = context.getTopY(Heightmap.Type.OCEAN_FLOOR, pos.getX(), pos.getZ());
		int worldSurfaceY = context.getTopY(Heightmap.Type.WORLD_SURFACE, pos.getX(), pos.getZ());
		return worldSurfaceY - oceanFloorY <= maxWaterDepth;
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.SURFACE_WATER_DEPTH_FILTER;
	}
}
