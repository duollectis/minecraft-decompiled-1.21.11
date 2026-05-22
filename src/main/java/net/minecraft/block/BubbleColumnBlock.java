package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Блок столба пузырей, создаваемый над душевым песком (восходящий поток)
 * или магмовым блоком (нисходящий поток). Свойство {@link #DRAG} определяет
 * направление: {@code true} — тянет вниз (магма), {@code false} — толкает вверх (душевой песок).
 */
public class BubbleColumnBlock extends Block implements FluidDrainable {

	public static final MapCodec<BubbleColumnBlock> CODEC = createCodec(BubbleColumnBlock::new);
	public static final BooleanProperty DRAG = Properties.DRAG;
	private static final int SCHEDULED_TICK_DELAY = 5;

	@Override
	public MapCodec<BubbleColumnBlock> getCodec() {
		return CODEC;
	}

	public BubbleColumnBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(DRAG, true));
	}

	@Override
	protected void onEntityCollision(
		BlockState state,
		World world,
		BlockPos pos,
		Entity entity,
		EntityCollisionHandler handler,
		boolean isAboveSurface
	) {
		if (isAboveSurface == false) {
			return;
		}

		BlockState above = world.getBlockState(pos.up());
		boolean isOpenAbove = above.getCollisionShape(world, pos).isEmpty() && above.getFluidState().isEmpty();

		if (isOpenAbove) {
			entity.onBubbleColumnSurfaceCollision(state.get(DRAG), pos);
		} else {
			entity.onBubbleColumnCollision(state.get(DRAG));
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		update(world, pos, state, world.getBlockState(pos.down()));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return Fluids.WATER.getStill(false);
	}

	/**
	 * Обновляет столб пузырей снизу вверх, начиная с позиции {@code pos}.
	 * Вызывается при изменении блока-источника (душевой песок / магма).
	 *
	 * @param world мир
	 * @param pos позиция нижнего блока столба
	 * @param state состояние нового источника пузырей
	 */
	public static void update(WorldAccess world, BlockPos pos, BlockState state) {
		update(world, pos, world.getBlockState(pos), state);
	}

	/**
	 * Распространяет состояние столба пузырей вверх по колонне воды.
	 * Останавливается, если блок не является стоячей водой или столбом пузырей.
	 *
	 * @param world мир
	 * @param pos позиция начала обновления
	 * @param water текущее состояние блока воды на позиции
	 * @param bubbleSource блок-источник, определяющий тип столба (магма/душевой песок)
	 */
	public static void update(WorldAccess world, BlockPos pos, BlockState water, BlockState bubbleSource) {
		if (isStillWater(water)) {
			BlockState blockState = getBubbleState(bubbleSource);
			world.setBlockState(pos, blockState, 2);
			BlockPos.Mutable mutable = pos.mutableCopy().move(Direction.UP);

			while (isStillWater(world.getBlockState(mutable))) {
				if (!world.setBlockState(mutable, blockState, 2)) {
					return;
				}

				mutable.move(Direction.UP);
			}
		}
	}

	private static boolean isStillWater(BlockState state) {
		return state.isOf(Blocks.BUBBLE_COLUMN)
				|| state.isOf(Blocks.WATER) && state.getFluidState().getLevel() >= 8 && state.getFluidState().isStill();
	}

	private static BlockState getBubbleState(BlockState state) {
		if (state.isOf(Blocks.BUBBLE_COLUMN)) {
			return state;
		}

		if (state.isOf(Blocks.SOUL_SAND)) {
			return Blocks.BUBBLE_COLUMN.getDefaultState().with(DRAG, false);
		}

		return state.isOf(Blocks.MAGMA_BLOCK)
			? Blocks.BUBBLE_COLUMN.getDefaultState().with(DRAG, true)
			: Blocks.WATER.getDefaultState();
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		double x = pos.getX();
		double y = pos.getY();
		double z = pos.getZ();

		if (state.get(DRAG)) {
			world.addImportantParticleClient(ParticleTypes.CURRENT_DOWN, x + 0.5, y + 0.8, z, 0.0, 0.0, 0.0);

			if (random.nextInt(200) == 0) {
				world.playSoundClient(
					x,
					y,
					z,
					SoundEvents.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
					SoundCategory.BLOCKS,
					0.2F + random.nextFloat() * 0.2F,
					0.9F + random.nextFloat() * 0.15F,
					false
				);
			}
		} else {
			world.addImportantParticleClient(ParticleTypes.BUBBLE_COLUMN_UP, x + 0.5, y, z + 0.5, 0.0, 0.04, 0.0);
			world.addImportantParticleClient(
				ParticleTypes.BUBBLE_COLUMN_UP,
				x + random.nextFloat(),
				y + random.nextFloat(),
				z + random.nextFloat(),
				0.0,
				0.04,
				0.0
			);

			if (random.nextInt(200) == 0) {
				world.playSoundClient(
					x,
					y,
					z,
					SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT,
					SoundCategory.BLOCKS,
					0.2F + random.nextFloat() * 0.2F,
					0.9F + random.nextFloat() * 0.15F,
					false
				);
			}
		}
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
		tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));

		if (!state.canPlaceAt(world, pos)
			|| direction == Direction.DOWN
			|| direction == Direction.UP && neighborState.isOf(Blocks.BUBBLE_COLUMN) == false
			&& isStillWater(neighborState)
		) {
			tickView.scheduleBlockTick(pos, this, SCHEDULED_TICK_DELAY);
		}

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

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos.down());
		return blockState.isOf(Blocks.BUBBLE_COLUMN) || blockState.isOf(Blocks.MAGMA_BLOCK)
				|| blockState.isOf(Blocks.SOUL_SAND);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(DRAG);
	}

	@Override
	public ItemStack tryDrainFluid(@Nullable LivingEntity drainer, WorldAccess world, BlockPos pos, BlockState state) {
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
		return new ItemStack(Items.WATER_BUCKET);
	}

	@Override
	public Optional<SoundEvent> getBucketFillSound() {
		return Fluids.WATER.getBucketFillSound();
	}
}
