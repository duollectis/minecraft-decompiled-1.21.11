package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * {@code WallTorchBlock}.
 */
public class WallTorchBlock extends TorchBlock {

	public static final MapCodec<WallTorchBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(PARTICLE_TYPE_CODEC.forGetter(block -> block.particle), createSettingsCodec())
					.apply(instance, WallTorchBlock::new)
	);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	private static final Map<Direction, VoxelShape> SHAPES_BY_DIRECTION = VoxelShapes.createHorizontalFacingShapeMap(
			Block.createCuboidZShape(5.0, 3.0, 13.0, 11.0, 16.0)
	);

	@Override
	public MapCodec<WallTorchBlock> getCodec() {
		return CODEC;
	}

	public WallTorchBlock(SimpleParticleType simpleParticleType, AbstractBlock.Settings settings) {
		super(simpleParticleType, settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getBoundingShape(state);
	}

	public static VoxelShape getBoundingShape(BlockState state) {
		return SHAPES_BY_DIRECTION.get(state.get(FACING));
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return canPlaceAt(world, pos, state.get(FACING));
	}

	/**
	 * Проверяет возможность place at.
	 *
	 * @param world world
	 * @param pos pos
	 * @param facing facing
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public static boolean canPlaceAt(WorldView world, BlockPos pos, Direction facing) {
		BlockPos blockPos = pos.offset(facing.getOpposite());
		BlockState blockState = world.getBlockState(blockPos);
		return blockState.isSideSolidFullSquare(world, blockPos, facing);
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState blockState = this.getDefaultState();
		WorldView worldView = ctx.getWorld();
		BlockPos blockPos = ctx.getBlockPos();
		Direction[] directions = ctx.getPlacementDirections();

		for (Direction direction : directions) {
			if (direction.getAxis().isHorizontal()) {
				Direction direction2 = direction.getOpposite();
				blockState = blockState.with(FACING, direction2);
				if (blockState.canPlaceAt(worldView, blockPos)) {
					return blockState;
				}
			}
		}

		return null;
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
		return direction.getOpposite() == state.get(FACING) && !state.canPlaceAt(world, pos)
		       ? Blocks.AIR.getDefaultState() : state;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		Direction direction = state.get(FACING);
		double d = pos.getX() + 0.5;
		double e = pos.getY() + 0.7;
		double f = pos.getZ() + 0.5;
		double g = 0.22;
		double h = 0.27;
		Direction direction2 = direction.getOpposite();
		world.addParticleClient(
				ParticleTypes.SMOKE,
				d + 0.27 * direction2.getOffsetX(),
				e + 0.22,
				f + 0.27 * direction2.getOffsetZ(),
				0.0,
				0.0,
				0.0
		);
		world.addParticleClient(
				this.particle,
				d + 0.27 * direction2.getOffsetX(),
				e + 0.22,
				f + 0.27 * direction2.getOffsetZ(),
				0.0,
				0.0,
				0.0
		);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
