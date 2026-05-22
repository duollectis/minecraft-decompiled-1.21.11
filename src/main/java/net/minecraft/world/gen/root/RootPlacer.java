package net.minecraft.world.gen.root;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Базовый класс размещателя корней дерева — генерирует корневую систему
 * между основанием и стволом, опционально размещая блоки над корнями.
 */
public abstract class RootPlacer {

	public static final Codec<RootPlacer> TYPE_CODEC =
		Registries.ROOT_PLACER_TYPE.getCodec().dispatch(RootPlacer::getType, RootPlacerType::getCodec);
	protected final IntProvider trunkOffsetY;
	protected final BlockStateProvider rootProvider;
	protected final Optional<AboveRootPlacement> aboveRootPlacement;

	protected static <P extends RootPlacer> P3<Mu<P>, IntProvider, BlockStateProvider, Optional<AboveRootPlacement>> createCodecParts(
			Instance<P> instance
	) {
		return instance.group(
				IntProvider.VALUE_CODEC.fieldOf("trunk_offset_y").forGetter(rootPlacer -> rootPlacer.trunkOffsetY),
				BlockStateProvider.TYPE_CODEC.fieldOf("root_provider").forGetter(rootPlacer -> rootPlacer.rootProvider),
				AboveRootPlacement.CODEC
						.optionalFieldOf("above_root_placement")
						.forGetter(rootPlacer -> rootPlacer.aboveRootPlacement)
		);
	}

	public RootPlacer(
			IntProvider trunkOffsetY,
			BlockStateProvider rootProvider,
			Optional<AboveRootPlacement> aboveRootPlacement
	) {
		this.trunkOffsetY = trunkOffsetY;
		this.rootProvider = rootProvider;
		this.aboveRootPlacement = aboveRootPlacement;
	}

	protected abstract RootPlacerType<?> getType();

	public abstract boolean generate(
			TestableWorld world,
			BiConsumer<BlockPos, BlockState> replacer,
			Random random,
			BlockPos pos,
			BlockPos trunkPos,
			TreeFeatureConfig config
	);

	protected boolean canGrowThrough(TestableWorld world, BlockPos pos) {
		return TreeFeature.canReplace(world, pos);
	}

	protected void placeRoots(
		TestableWorld world,
		BiConsumer<BlockPos, BlockState> replacer,
		Random random,
		BlockPos pos,
		TreeFeatureConfig config
	) {
		if (!canGrowThrough(world, pos)) {
			return;
		}

		replacer.accept(pos, applyWaterlogging(world, pos, rootProvider.get(random, pos)));

		aboveRootPlacement.ifPresent(above -> {
			BlockPos abovePos = pos.up();
			boolean isAir = world.testBlockState(abovePos, AbstractBlock.AbstractBlockState::isAir);

			if (random.nextFloat() < above.aboveRootPlacementChance() && isAir) {
				replacer.accept(
					abovePos,
					applyWaterlogging(world, abovePos, above.aboveRootProvider().get(random, abovePos))
				);
			}
		});
	}

	/**
	 * Применяет состояние waterlogged к блоку, если он поддерживает это свойство
	 * и в данной позиции находится вода.
	 */
	protected BlockState applyWaterlogging(TestableWorld world, BlockPos pos, BlockState state) {
		if (!state.contains(Properties.WATERLOGGED)) {
			return state;
		}

		boolean isWater = world.testFluidState(pos, fluidState -> fluidState.isIn(FluidTags.WATER));
		return state.with(Properties.WATERLOGGED, isWater);
	}

	public BlockPos trunkOffset(BlockPos pos, Random random) {
		return pos.up(trunkOffsetY.get(random));
	}
}
