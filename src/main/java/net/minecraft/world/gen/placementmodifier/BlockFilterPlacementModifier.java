package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.blockpredicate.BlockPredicate;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

/**
 * Модификатор размещения, фильтрующий позиции по предикату блока —
 * пропускает позицию только если {@link BlockPredicate} возвращает {@code true}.
 */
public class BlockFilterPlacementModifier extends AbstractConditionalPlacementModifier {

	public static final MapCodec<BlockFilterPlacementModifier> MODIFIER_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(BlockPredicate.BASE_CODEC
				.fieldOf("predicate")
				.forGetter(modifier -> modifier.predicate))
			.apply(instance, BlockFilterPlacementModifier::new)
	);
	private final BlockPredicate predicate;

	private BlockFilterPlacementModifier(BlockPredicate predicate) {
		this.predicate = predicate;
	}

	public static BlockFilterPlacementModifier of(BlockPredicate predicate) {
		return new BlockFilterPlacementModifier(predicate);
	}

	@Override
	protected boolean shouldPlace(FeaturePlacementContext context, Random random, BlockPos pos) {
		return predicate.test(context.getWorld(), pos);
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.BLOCK_PREDICATE_FILTER;
	}
}
