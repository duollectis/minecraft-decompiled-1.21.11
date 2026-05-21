package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * {@code PitcherCropBlock}.
 */
public class PitcherCropBlock extends TallPlantBlock implements Fertilizable {

	public static final MapCodec<PitcherCropBlock> CODEC = createCodec(PitcherCropBlock::new);
	public static final int MAX_AGE = 4;
	public static final IntProperty AGE = Properties.AGE_4;
	public static final EnumProperty<DoubleBlockHalf> HALF = TallPlantBlock.HALF;
	private static final int FULL_GROWN_AGE = 3;
	private static final int LOWER_HALF_MIN_AGE = 1;
	private static final VoxelShape AGE_0_SHAPE = Block.createColumnShape(6.0, -1.0, 3.0);
	private static final VoxelShape LOWER_COLLISION_SHAPE = Block.createColumnShape(10.0, -1.0, 5.0);
	private final Function<BlockState, VoxelShape> shapeFunction = this.createShapeFunction();

	@Override
	public MapCodec<PitcherCropBlock> getCodec() {
		return CODEC;
	}

	public PitcherCropBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		int[] is = new int[]{0, 9, 11, 22, 26};
		return this.createShapeFunction(state -> {
			int i = (state.get(AGE) == 0 ? 4 : 6) + is[state.get(AGE)];
			int j = state.get(AGE) == 0 ? 6 : 10;

			return switch ((DoubleBlockHalf) state.get(HALF)) {
				case LOWER -> Block.createColumnShape(j, -1.0, Math.min(16, -1 + i));
				case UPPER -> Block.createColumnShape(j, 0.0, Math.max(0, -1 + i - 16));
			};
		});
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState();
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shapeFunction.apply(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if (state.get(HALF) == DoubleBlockHalf.LOWER) {
			return state.get(AGE) == 0 ? AGE_0_SHAPE : LOWER_COLLISION_SHAPE;
		}
		else {
			return VoxelShapes.empty();
		}
	}

	@Override
	public BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		if (isDoubleTallAtAge(state.get(AGE))) {
			return super.getStateForNeighborUpdate(
					state,
					world,
					tickView,
					pos,
					direction,
					neighborPos,
					neighborState,
					random
			);
		}
		else {
			return state.canPlaceAt(world, pos) ? state : Blocks.AIR.getDefaultState();
		}
	}

	@Override
	public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return isLowerHalf(state) && !canPlaceAt(world, pos) ? false : super.canPlaceAt(state, world, pos);
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOf(Blocks.FARMLAND);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
		super.appendProperties(builder);
	}

	@Override
	public void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean bl
	) {
		if (world instanceof ServerWorld serverWorld && entity instanceof RavagerEntity && serverWorld
				.getGameRules()
				.getValue(GameRules.DO_MOB_GRIEFING)) {
			serverWorld.breakBlock(pos, true, entity);
		}
	}

	@Override
	public boolean canReplace(BlockState state, ItemPlacementContext context) {
		return false;
	}

	@Override
	public void onPlaced(
			World world,
			BlockPos pos,
			BlockState state,
			@Nullable LivingEntity placer,
			ItemStack itemStack
	) {
	}

	@Override
	public boolean hasRandomTicks(BlockState state) {
		return state.get(HALF) == DoubleBlockHalf.LOWER && !this.isFullyGrown(state);
	}

	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		float f = CropBlock.getAvailableMoisture(this, world, pos);
		boolean bl = random.nextInt((int) (25.0F / f) + 1) == 0;
		if (bl) {
			this.tryGrow(world, state, pos, 1);
		}
	}

	private void tryGrow(ServerWorld world, BlockState state, BlockPos pos, int amount) {
		int i = Math.min(state.get(AGE) + amount, 4);
		if (this.canGrow(world, pos, state, i)) {
			BlockState blockState = state.with(AGE, i);
			world.setBlockState(pos, blockState, 2);
			if (isDoubleTallAtAge(i)) {
				world.setBlockState(pos.up(), blockState.with(HALF, DoubleBlockHalf.UPPER), 3);
			}
		}
	}

	private static boolean canGrowAt(WorldView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return blockState.isAir() || blockState.isOf(Blocks.PITCHER_CROP);
	}

	private static boolean canPlaceAt(WorldView world, BlockPos pos) {
		return CropBlock.hasEnoughLightAt(world, pos);
	}

	private static boolean isLowerHalf(BlockState state) {
		return state.isOf(Blocks.PITCHER_CROP) && state.get(HALF) == DoubleBlockHalf.LOWER;
	}

	private static boolean isDoubleTallAtAge(int age) {
		return age >= 3;
	}

	private boolean canGrow(WorldView world, BlockPos pos, BlockState state, int age) {
		return !this.isFullyGrown(state) && canPlaceAt(world, pos) && (!isDoubleTallAtAge(age) || canGrowAt(
				world,
				pos.up()
		)
		);
	}

	private boolean isFullyGrown(BlockState state) {
		return state.get(AGE) >= 4;
	}

	private PitcherCropBlock.@Nullable LowerHalfContext getLowerHalfContext(
			WorldView world,
			BlockPos pos,
			BlockState state
	) {
		if (isLowerHalf(state)) {
			return new PitcherCropBlock.LowerHalfContext(pos, state);
		}
		else {
			BlockPos blockPos = pos.down();
			BlockState blockState = world.getBlockState(blockPos);
			return isLowerHalf(blockState) ? new PitcherCropBlock.LowerHalfContext(blockPos, blockState) : null;
		}
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		PitcherCropBlock.LowerHalfContext lowerHalfContext = this.getLowerHalfContext(world, pos, state);
		return lowerHalfContext == null ? false : this.canGrow(
				world,
				lowerHalfContext.pos,
				lowerHalfContext.state,
				lowerHalfContext.state.get(AGE) + 1
		);
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		PitcherCropBlock.LowerHalfContext lowerHalfContext = this.getLowerHalfContext(world, pos, state);
		if (lowerHalfContext != null) {
			this.tryGrow(world, lowerHalfContext.state, lowerHalfContext.pos, 1);
		}
	}

	/**
	 * {@code LowerHalfContext}.
	 */
	record LowerHalfContext(BlockPos pos, BlockState state) {
	}
}
