package net.minecraft.world.gen.placementmodifier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;

import java.util.stream.Stream;

/**
 * Модификатор размещения, добавляющий случайное смещение по XZ и Y
 * к входной позиции.
 */
public class RandomOffsetPlacementModifier extends PlacementModifier {

	public static final MapCodec<RandomOffsetPlacementModifier> MODIFIER_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			IntProvider.createValidatingCodec(-16, 16)
				.fieldOf("xz_spread")
				.forGetter(modifier -> modifier.spreadXz),
			IntProvider.createValidatingCodec(-16, 16)
				.fieldOf("y_spread")
				.forGetter(modifier -> modifier.spreadY)
		)
		.apply(instance, RandomOffsetPlacementModifier::new)
	);
	private final IntProvider spreadXz;
	private final IntProvider spreadY;

	public static RandomOffsetPlacementModifier of(IntProvider spreadXz, IntProvider spreadY) {
		return new RandomOffsetPlacementModifier(spreadXz, spreadY);
	}

	public static RandomOffsetPlacementModifier vertically(IntProvider spreadY) {
		return new RandomOffsetPlacementModifier(ConstantIntProvider.create(0), spreadY);
	}

	public static RandomOffsetPlacementModifier horizontally(IntProvider spreadXz) {
		return new RandomOffsetPlacementModifier(spreadXz, ConstantIntProvider.create(0));
	}

	private RandomOffsetPlacementModifier(IntProvider spreadXz, IntProvider spreadY) {
		this.spreadXz = spreadXz;
		this.spreadY = spreadY;
	}

	@Override
	public Stream<BlockPos> getPositions(FeaturePlacementContext context, Random random, BlockPos pos) {
		int x = pos.getX() + spreadXz.get(random);
		int y = pos.getY() + spreadY.get(random);
		int z = pos.getZ() + spreadXz.get(random);
		return Stream.of(new BlockPos(x, y, z));
	}

	@Override
	public PlacementModifierType<?> getType() {
		return PlacementModifierType.RANDOM_OFFSET;
	}
}
