package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.Stream;

/**
 * Базовый класс модификатора размещения фичи — преобразует входную позицию
 * в поток выходных позиций (фильтрация, смещение, умножение и т.д.).
 */
public abstract class PlacementModifier {

	public static final Codec<PlacementModifier> CODEC = Registries.PLACEMENT_MODIFIER_TYPE
		.getCodec()
		.dispatch(PlacementModifier::getType, PlacementModifierType::codec);

	public abstract Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos);

	public abstract PlacementModifierType<?> getType();
}
