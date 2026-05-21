package net.minecraft.world.gen.stateprovider;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * {@code WeightedBlockStateProvider}.
 */
public class WeightedBlockStateProvider extends BlockStateProvider {

	public static final MapCodec<WeightedBlockStateProvider> CODEC = Pool.createNonEmptyCodec(BlockState.CODEC)
	                                                                     .comapFlatMap(
			                                                                     WeightedBlockStateProvider::wrap,
			                                                                     weightedBlockStateProvider -> weightedBlockStateProvider.states
	                                                                     )
	                                                                     .fieldOf("entries");
	private final Pool<BlockState> states;

	private static DataResult<WeightedBlockStateProvider> wrap(Pool<BlockState> states) {
		return states.isEmpty() ? DataResult.error(() -> "WeightedStateProvider with no states")
		                        : DataResult.success(new WeightedBlockStateProvider(states));
	}

	public WeightedBlockStateProvider(Pool<BlockState> states) {
		this.states = states;
	}

	public WeightedBlockStateProvider(Pool.Builder<BlockState> states) {
		this(states.build());
	}

	@Override
	protected BlockStateProviderType<?> getType() {
		return BlockStateProviderType.WEIGHTED_STATE_PROVIDER;
	}

	@Override
	public BlockState get(Random random, BlockPos pos) {
		return this.states.get(random);
	}
}
