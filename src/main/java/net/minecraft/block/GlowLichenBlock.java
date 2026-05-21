package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.function.ToIntFunction;

/**
 * {@code GlowLichenBlock}.
 */
public class GlowLichenBlock extends MultifaceGrowthBlock implements Fertilizable {

	public static final MapCodec<GlowLichenBlock> CODEC = createCodec(GlowLichenBlock::new);
	private final MultifaceGrower grower = new MultifaceGrower(this);

	@Override
	public MapCodec<GlowLichenBlock> getCodec() {
		return CODEC;
	}

	public GlowLichenBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	public static ToIntFunction<BlockState> getLuminanceSupplier(int luminance) {
		return state -> MultifaceBlock.hasAnyDirection(state) ? luminance : 0;
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return Direction
				.stream()
				.anyMatch(direction -> this.grower.canGrow(state, world, pos, direction.getOpposite()));
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		this.grower.grow(state, world, pos, random);
	}

	@Override
	protected boolean isTransparent(BlockState state) {
		return state.getFluidState().isEmpty();
	}

	@Override
	public MultifaceGrower getGrower() {
		return this.grower;
	}
}
