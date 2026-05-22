package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Блок цветка хоруса — верхушка растения хоруса в Крае.
 * <p>
 * Случайно растёт вверх или в стороны, пока не достигнет максимального возраста ({@link #MAX_AGE}).
 * При достижении максимального возраста рост прекращается. Разрушается снарядами.
 */
public class ChorusFlowerBlock extends Block {

	public static final MapCodec<ChorusFlowerBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							Registries.BLOCK.getCodec().fieldOf("plant").forGetter(block -> block.plantBlock),
							createSettingsCodec()
					)
					.apply(instance, ChorusFlowerBlock::new)
	);
	public static final int MAX_AGE = 5;
	public static final IntProperty AGE = Properties.AGE_5;
	private static final VoxelShape SHAPE = Block.createColumnShape(14.0, 0.0, 15.0);
	private final Block plantBlock;

	@Override
	public MapCodec<ChorusFlowerBlock> getCodec() {
		return CODEC;
	}

	public ChorusFlowerBlock(Block plantBlock, AbstractBlock.Settings settings) {
		super(settings);
		this.plantBlock = plantBlock;
		setDefaultState(stateManager.getDefaultState().with(AGE, 0));
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!state.canPlaceAt(world, pos)) {
			world.breakBlock(pos, true);
		}
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return state.get(AGE) < MAX_AGE;
	}

	@Override
	public VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
		return SHAPE;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		BlockPos above = pos.up();
		if (!world.isAir(above) || above.getY() > world.getTopYInclusive()) {
			return;
		}

		int age = state.get(AGE);
		if (age >= MAX_AGE) {
			return;
		}

		boolean canGrowUp = false;
		boolean hasEndStoneBelow = false;
		BlockState belowState = world.getBlockState(pos.down());

		if (belowState.isOf(Blocks.END_STONE)) {
			canGrowUp = true;
		} else if (belowState.isOf(plantBlock)) {
			int stemHeight = 1;

			for (int step = 0; step < 4; step++) {
				BlockState deeper = world.getBlockState(pos.down(stemHeight + 1));
				if (!deeper.isOf(plantBlock)) {
					if (deeper.isOf(Blocks.END_STONE)) {
						hasEndStoneBelow = true;
					}

					break;
				}

				stemHeight++;
			}

			if (stemHeight < 2 || stemHeight <= random.nextInt(hasEndStoneBelow ? 5 : 4)) {
				canGrowUp = true;
			}
		} else if (belowState.isAir()) {
			canGrowUp = true;
		}

		if (canGrowUp && isSurroundedByAir(world, above, null) && world.isAir(pos.up(2))) {
			world.setBlockState(
					pos,
					ChorusPlantBlock.withConnectionProperties(world, pos, plantBlock.getDefaultState()),
					Block.NOTIFY_LISTENERS
			);
			grow(world, above, age);
			return;
		}

		if (age >= 4) {
			die(world, pos);
			return;
		}

		int branchCount = random.nextInt(4);
		if (hasEndStoneBelow) {
			branchCount++;
		}

		boolean didBranch = false;

		for (int attempt = 0; attempt < branchCount; attempt++) {
			Direction direction = Direction.Type.HORIZONTAL.random(random);
			BlockPos branchPos = pos.offset(direction);
			if (world.isAir(branchPos)
					&& world.isAir(branchPos.down())
					&& isSurroundedByAir(world, branchPos, direction.getOpposite())
			) {
				grow(world, branchPos, age + 1);
				didBranch = true;
			}
		}

		if (didBranch) {
			world.setBlockState(
					pos,
					ChorusPlantBlock.withConnectionProperties(world, pos, plantBlock.getDefaultState()),
					Block.NOTIFY_LISTENERS
			);
		} else {
			die(world, pos);
		}
	}

	private void grow(World world, BlockPos pos, int age) {
		world.setBlockState(pos, getDefaultState().with(AGE, age), Block.NOTIFY_LISTENERS);
		world.syncWorldEvent(WorldEvents.CHORUS_FLOWER_GROWS, pos, 0);
	}

	private void die(World world, BlockPos pos) {
		world.setBlockState(pos, getDefaultState().with(AGE, MAX_AGE), Block.NOTIFY_LISTENERS);
		world.syncWorldEvent(WorldEvents.CHORUS_FLOWER_DIES, pos, 0);
	}

	private static boolean isSurroundedByAir(WorldView world, BlockPos pos, @Nullable Direction exceptDirection) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			if (direction != exceptDirection && !world.isAir(pos.offset(direction))) {
				return false;
			}
		}

		return true;
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
		if (direction != Direction.UP && !state.canPlaceAt(world, pos)) {
			tickView.scheduleBlockTick(pos, this, 1);
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
		BlockState belowState = world.getBlockState(pos.down());
		if (belowState.isOf(plantBlock) || belowState.isOf(Blocks.END_STONE)) {
			return true;
		}

		if (!belowState.isAir()) {
			return false;
		}

		boolean foundNeighborStem = false;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockState neighborState = world.getBlockState(pos.offset(direction));
			if (neighborState.isOf(plantBlock)) {
				if (foundNeighborStem) {
					return false;
				}

				foundNeighborStem = true;
			} else if (!neighborState.isAir()) {
				return false;
			}
		}

		return foundNeighborStem;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	/**
	 * Генерирует структуру хоруса начиная с указанной позиции.
	 * <p>
	 * Используется при генерации мира для создания начального ствола и ветвей.
	 * Параметр {@code size} ограничивает горизонтальный разброс ветвей от корня.
	 *
	 * @param world  мир, в котором генерируется структура
	 * @param pos    начальная позиция (основание)
	 * @param random источник случайности
	 * @param size   максимальный горизонтальный радиус ветвления от корня
	 */
	public static void generate(WorldAccess world, BlockPos pos, Random random, int size) {
		world.setBlockState(
				pos,
				ChorusPlantBlock.withConnectionProperties(world, pos, Blocks.CHORUS_PLANT.getDefaultState()),
				Block.NOTIFY_LISTENERS
		);
		generate(world, pos, random, pos, size, 0);
	}

	private static void generate(
			WorldAccess world,
			BlockPos pos,
			Random random,
			BlockPos rootPos,
			int size,
			int layer
	) {
		Block chorusPlant = Blocks.CHORUS_PLANT;
		int height = random.nextInt(4) + 1;
		if (layer == 0) {
			height++;
		}

		for (int step = 0; step < height; step++) {
			BlockPos current = pos.up(step + 1);
			if (!isSurroundedByAir(world, current, null)) {
				return;
			}

			world.setBlockState(
					current,
					ChorusPlantBlock.withConnectionProperties(world, current, chorusPlant.getDefaultState()),
					Block.NOTIFY_LISTENERS
			);
			world.setBlockState(
					current.down(),
					ChorusPlantBlock.withConnectionProperties(world, current.down(), chorusPlant.getDefaultState()),
					Block.NOTIFY_LISTENERS
			);
		}

		boolean didBranch = false;
		if (layer < 4) {
			int branchCount = random.nextInt(4);
			if (layer == 0) {
				branchCount++;
			}

			for (int attempt = 0; attempt < branchCount; attempt++) {
				Direction direction = Direction.Type.HORIZONTAL.random(random);
				BlockPos branchPos = pos.up(height).offset(direction);
				if (Math.abs(branchPos.getX() - rootPos.getX()) < size
						&& Math.abs(branchPos.getZ() - rootPos.getZ()) < size
						&& world.isAir(branchPos)
						&& world.isAir(branchPos.down())
						&& isSurroundedByAir(world, branchPos, direction.getOpposite())
				) {
					didBranch = true;
					world.setBlockState(
							branchPos,
							ChorusPlantBlock.withConnectionProperties(world, branchPos, chorusPlant.getDefaultState()),
							Block.NOTIFY_LISTENERS
					);
					BlockPos connectBack = branchPos.offset(direction.getOpposite());
					world.setBlockState(
							connectBack,
							ChorusPlantBlock.withConnectionProperties(world, connectBack, chorusPlant.getDefaultState()),
							Block.NOTIFY_LISTENERS
					);
					generate(world, branchPos, random, rootPos, size, layer + 1);
				}
			}
		}

		if (!didBranch) {
			world.setBlockState(pos.up(height), Blocks.CHORUS_FLOWER.getDefaultState().with(AGE, MAX_AGE), Block.NOTIFY_LISTENERS);
		}
	}

	@Override
	protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
		BlockPos blockPos = hit.getBlockPos();
		if (world instanceof ServerWorld serverWorld && projectile.canModifyAt(serverWorld, blockPos)
				&& projectile.canBreakBlocks(serverWorld)) {
			world.breakBlock(blockPos, true, projectile);
		}
	}
}
