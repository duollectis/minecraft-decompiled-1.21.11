package net.minecraft.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.List;
import java.util.Map;

/**
 * Торт со свечой. Объединяет механику торта (поедание) и свечи (поджигание/тушение).
 * Реестр соответствий {@code CandleBlock → CandleCakeBlock} используется при установке свечи на торт.
 */
public class CandleCakeBlock extends AbstractCandleBlock {

	public static final MapCodec<CandleCakeBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							Registries.BLOCK.getCodec().fieldOf("candle").forGetter(block -> block.candle),
							createSettingsCodec()
					)
					.apply(instance, CandleCakeBlock::new)
	);
	public static final BooleanProperty LIT = AbstractCandleBlock.LIT;
	private static final float PIXEL = 0.0625F;
	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createColumnShape(2.0, 8.0, 14.0),
			Block.createColumnShape(14.0, 0.0, 8.0)
	);
	private static final Map<CandleBlock, CandleCakeBlock> CANDLES_TO_CANDLE_CAKES = Maps.newHashMap();
	private static final Iterable<Vec3d> PARTICLE_OFFSETS = List.of(new Vec3d(8.0, 16.0, 8.0).multiply(PIXEL));
	private final CandleBlock candle;

	@Override
	public MapCodec<CandleCakeBlock> getCodec() {
		return CODEC;
	}

	public CandleCakeBlock(Block candle, AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(LIT, false));
		if (candle instanceof CandleBlock candleBlock) {
			CANDLES_TO_CANDLE_CAKES.put(candleBlock, this);
			this.candle = candleBlock;
		} else {
			throw new IllegalArgumentException(
					"Expected block to be of " + CandleBlock.class + " was " + candle.getClass());
		}
	}

	@Override
	protected Iterable<Vec3d> getParticleOffsets(BlockState state) {
		return PARTICLE_OFFSETS;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUseWithItem(
			ItemStack stack,
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit
	) {
		if (stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE)) {
			return ActionResult.PASS;
		}

		if (isHittingCandle(hit) && stack.isEmpty() && state.get(LIT)) {
			extinguish(player, state, world, pos);
			return ActionResult.SUCCESS;
		}

		return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		ActionResult actionResult = CakeBlock.tryEat(world, pos, Blocks.CAKE.getDefaultState(), player);
		if (actionResult.isAccepted()) {
			dropStacks(state, world, pos);
		}

		return actionResult;
	}

	private static boolean isHittingCandle(BlockHitResult hitResult) {
		return hitResult.getPos().y - hitResult.getBlockPos().getY() > 0.5;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(Blocks.CAKE);
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
		return direction == Direction.DOWN && !state.canPlaceAt(world, pos)
		       ? Blocks.AIR.getDefaultState()
		       : super.getStateForNeighborUpdate(
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

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return world.getBlockState(pos.down()).isSolid();
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		return CakeBlock.DEFAULT_COMPARATOR_OUTPUT;
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	public static BlockState getCandleCakeFromCandle(CandleBlock candle) {
		return CANDLES_TO_CANDLE_CAKES.get(candle).getDefaultState();
	}

	/**
	 * Возвращает {@code true}, если торт со свечой можно поджечь: он принадлежит тегу
	 * {@code CANDLE_CAKES}, содержит свойство {@code LIT} и при этом свеча не горит.
	 */
	public static boolean canBeLit(BlockState state) {
		return state.isIn(BlockTags.CANDLE_CAKES, s -> s.contains(LIT)) && state.get(LIT) == false;
	}
}
