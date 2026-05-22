package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * Блок свисающего мха (бледный мох). Растёт вниз цепочкой; нижний блок помечается как TIP.
 * Поддерживает удобрение костяной мукой для наращивания цепочки вниз.
 */
public class HangingMossBlock extends Block implements Fertilizable {

	public static final MapCodec<HangingMossBlock> CODEC = createCodec(HangingMossBlock::new);
	private static final VoxelShape SHAPE = Block.createColumnShape(14.0, 0.0, 16.0);
	private static final VoxelShape TIP_SHAPE = Block.createColumnShape(14.0, 2.0, 16.0);
	public static final BooleanProperty TIP = Properties.TIP;

	@Override
	public MapCodec<HangingMossBlock> getCodec() {
		return CODEC;
	}

	public HangingMossBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(TIP, true));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(TIP) ? TIP_SHAPE : SHAPE;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (random.nextInt(500) == 0) {
			BlockState blockState = world.getBlockState(pos.up());
			if (blockState.isIn(BlockTags.PALE_OAK_LOGS) || blockState.isOf(Blocks.PALE_OAK_LEAVES)) {
				world.playSoundClient(
						pos.getX(),
						pos.getY(),
						pos.getZ(),
						SoundEvents.BLOCK_PALE_HANGING_MOSS_IDLE,
						SoundCategory.AMBIENT,
						1.0F,
						1.0F,
						false
				);
			}
		}
	}

	@Override
	protected boolean isTransparent(BlockState state) {
		return true;
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return canPlaceAt(world, pos);
	}

	private boolean canPlaceAt(BlockView world, BlockPos pos) {
		BlockPos above = pos.offset(Direction.UP);
		BlockState aboveState = world.getBlockState(above);
		return MultifaceBlock.canGrowOn(world, Direction.UP, above, aboveState)
				|| aboveState.isOf(Blocks.PALE_HANGING_MOSS);
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		if (canPlaceAt(world, pos) == false) {
			tickView.scheduleBlockTick(pos, this, 1);
		}

		return state.with(TIP, world.getBlockState(pos.down()).isOf(this) == false);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (canPlaceAt(world, pos) == false) {
			world.breakBlock(pos, true);
		}
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(TIP);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return canGrowInto(world.getBlockState(getTipPos(world, pos).down()));
	}

	private boolean canGrowInto(BlockState state) {
		return state.isAir();
	}

	public BlockPos getTipPos(BlockView world, BlockPos pos) {
		BlockPos.Mutable mutable = pos.mutableCopy();
		BlockState current;
		do {
			mutable.move(Direction.DOWN);
			current = world.getBlockState(mutable);
		} while (current.isOf(this));

		return mutable.offset(Direction.UP).toImmutable();
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockPos tipBelow = getTipPos(world, pos).down();
		if (canGrowInto(world.getBlockState(tipBelow))) {
			world.setBlockState(tipBelow, state.with(TIP, true));
		}
	}
}
