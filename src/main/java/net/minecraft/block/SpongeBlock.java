package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jspecify.annotations.Nullable;

/**
 * Блок губки — при контакте с водой поглощает её в радиусе {@link #ABSORB_RADIUS} блоков
 * (не более {@link #ABSORB_LIMIT} блоков за раз) и превращается в мокрую губку.
 */
public class SpongeBlock extends Block {

	public static final MapCodec<SpongeBlock> CODEC = createCodec(SpongeBlock::new);
	public static final int ABSORB_RADIUS = 6;
	public static final int ABSORB_LIMIT = 64;
	private static final Direction[] DIRECTIONS = Direction.values();

	@Override
	public MapCodec<SpongeBlock> getCodec() {
		return CODEC;
	}

	public SpongeBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!oldState.isOf(state.getBlock())) {
			this.update(world, pos);
		}
	}

	@Override
	protected void neighborUpdate(
			BlockState state,
			World world,
			BlockPos pos,
			Block sourceBlock,
			@Nullable WireOrientation wireOrientation,
			boolean notify
	) {
		this.update(world, pos);
		super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
	}

	protected void update(World world, BlockPos pos) {
		if (this.absorbWater(world, pos)) {
			world.setBlockState(pos, Blocks.WET_SPONGE.getDefaultState(), 2);
			world.playSound(null, pos, SoundEvents.BLOCK_SPONGE_ABSORB, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
	}

	/**
	 * Рекурсивно поглощает воду вокруг губки, удаляя водные блоки и водоросли.
	 * Возвращает {@code true}, если было поглощено хотя бы одно водное пространство.
	 */
	private boolean absorbWater(World world, BlockPos pos) {
		return BlockPos.iterateRecursively(
				pos,
				ABSORB_RADIUS,
				ABSORB_LIMIT + 1,
				(currentPos, queuer) -> {
					for (Direction direction : DIRECTIONS) {
						queuer.accept(currentPos.offset(direction));
					}
				},
				currentPos -> {
					if (currentPos.equals(pos)) {
						return BlockPos.IterationState.ACCEPT;
					}

					BlockState blockState = world.getBlockState(currentPos);
					FluidState fluidState = world.getFluidState(currentPos);

					if (fluidState.isIn(FluidTags.WATER) == false) {
						return BlockPos.IterationState.SKIP;
					}

					if (blockState.getBlock() instanceof FluidDrainable drainable
							&& drainable.tryDrainFluid(null, world, currentPos, blockState).isEmpty() == false) {
						return BlockPos.IterationState.ACCEPT;
					}

					if (blockState.getBlock() instanceof FluidBlock) {
						world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
						return BlockPos.IterationState.ACCEPT;
					}

					if (blockState.isOf(Blocks.KELP)
							|| blockState.isOf(Blocks.KELP_PLANT)
							|| blockState.isOf(Blocks.SEAGRASS)
							|| blockState.isOf(Blocks.TALL_SEAGRASS)
					) {
						BlockEntity blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(currentPos) : null;
						dropStacks(blockState, world, currentPos, blockEntity);
						world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
						return BlockPos.IterationState.ACCEPT;
					}

					return BlockPos.IterationState.SKIP;
				}
		) > 1;
	}
}
