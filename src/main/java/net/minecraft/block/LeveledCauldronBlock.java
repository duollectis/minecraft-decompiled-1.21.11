package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.CollisionEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;

/**
 * {@code LeveledCauldronBlock}.
 */
public class LeveledCauldronBlock extends AbstractCauldronBlock {

	public static final MapCodec<LeveledCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    Biome.Precipitation.CODEC.fieldOf("precipitation").forGetter(block -> block.precipitation),
					                    CauldronBehavior.CODEC.fieldOf("interactions").forGetter(block -> block.behaviorMap),
					                    createSettingsCodec()
			                    )
			                    .apply(instance, LeveledCauldronBlock::new)
	);
	public static final int MIN_LEVEL = 1;
	public static final int MAX_LEVEL = 3;
	public static final IntProperty LEVEL = Properties.LEVEL_3;
	private static final int BASE_FLUID_HEIGHT = 6;
	private static final double FLUID_HEIGHT_PER_LEVEL = 3.0;
	private static final VoxelShape[] INSIDE_COLLISION_SHAPE_BY_LEVEL = Util.make(
			() -> Block.createShapeArray(
					2,
					level -> VoxelShapes.union(
							AbstractCauldronBlock.OUTLINE_SHAPE,
							Block.createColumnShape(12.0, 4.0, getFluidHeight(level + 1))
					)
			)
	);
	private final Biome.Precipitation precipitation;

	@Override
	public MapCodec<LeveledCauldronBlock> getCodec() {
		return CODEC;
	}

	public LeveledCauldronBlock(
			Biome.Precipitation precipitation,
			CauldronBehavior.CauldronBehaviorMap behaviorMap,
			AbstractBlock.Settings settings
	) {
		super(settings, behaviorMap);
		this.precipitation = precipitation;
		this.setDefaultState(this.stateManager.getDefaultState().with(LEVEL, 1));
	}

	@Override
	public boolean isFull(BlockState state) {
		return state.get(LEVEL) == 3;
	}

	@Override
	protected boolean canBeFilledByDripstone(Fluid fluid) {
		return fluid == Fluids.WATER && this.precipitation == Biome.Precipitation.RAIN;
	}

	@Override
	protected double getFluidHeight(BlockState state) {
		return getFluidHeight(state.get(LEVEL)) / 16.0;
	}

	private static double getFluidHeight(int level) {
		return 6.0 + level * 3.0;
	}

	@Override
	protected VoxelShape getInsideCollisionShape(BlockState state, BlockView world, BlockPos pos, Entity entity) {
		return INSIDE_COLLISION_SHAPE_BY_LEVEL[state.get(LEVEL) - 1];
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean bl
	) {
		if (world instanceof ServerWorld serverWorld) {
			BlockPos blockPos = pos.toImmutable();
			handler.addPreCallback(
					CollisionEvent.EXTINGUISH, collidedEntity -> {
						if (collidedEntity.isOnFire() && collidedEntity.canModifyAt(serverWorld, blockPos)) {
							this.onFireCollision(state, world, blockPos);
						}
					}
			);
		}

		handler.addEvent(CollisionEvent.EXTINGUISH);
	}

	private void onFireCollision(BlockState state, World world, BlockPos pos) {
		if (this.precipitation == Biome.Precipitation.SNOW) {
			decrementFluidLevel(Blocks.WATER_CAULDRON.getDefaultState().with(LEVEL, state.get(LEVEL)), world, pos);
		}
		else {
			decrementFluidLevel(state, world, pos);
		}
	}

	/**
	 * Decrement fluid level.
	 *
	 * @param state state
	 * @param world world
	 * @param pos pos
	 */
	public static void decrementFluidLevel(BlockState state, World world, BlockPos pos) {
		int i = state.get(LEVEL) - 1;
		BlockState blockState = i == 0 ? Blocks.CAULDRON.getDefaultState() : state.with(LEVEL, i);
		world.setBlockState(pos, blockState);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
	}

	@Override
	public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
		if (CauldronBlock.canFillWithPrecipitation(world, precipitation) && state.get(LEVEL) != 3
				&& precipitation == this.precipitation) {
			BlockState blockState = state.cycle(LEVEL);
			world.setBlockState(pos, blockState);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
		}
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return state.get(LEVEL);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LEVEL);
	}

	@Override
	protected void fillFromDripstone(BlockState state, World world, BlockPos pos, Fluid fluid) {
		if (!this.isFull(state)) {
			BlockState blockState = state.with(LEVEL, state.get(LEVEL) + 1);
			world.setBlockState(pos, blockState);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(blockState));
			world.syncWorldEvent(1047, pos, 0);
		}
	}
}
