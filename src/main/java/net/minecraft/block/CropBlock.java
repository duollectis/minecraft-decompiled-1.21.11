package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.rule.GameRules;

/**
 * Базовый блок сельскохозяйственной культуры. Растёт случайными тиками при достаточном
 * освещении (≥9) и влажности почвы. Скорость роста зависит от количества влажных
 * грядок вокруг и наличия соседних культур того же вида.
 */
public class CropBlock extends PlantBlock implements Fertilizable {

	public static final MapCodec<CropBlock> CODEC = createCodec(CropBlock::new);
	public static final int MAX_AGE = 7;
	public static final IntProperty AGE = Properties.AGE_7;
	private static final VoxelShape[]
			SHAPES_BY_AGE =
			Block.createShapeArray(7, age -> Block.createColumnShape(16.0, 0.0, 2 + age * 2));

	@Override
	public MapCodec<? extends CropBlock> getCodec() {
		return CODEC;
	}

	public CropBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(getAgeProperty(), 0));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_AGE[getAge(state)];
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOf(Blocks.FARMLAND);
	}

	protected IntProperty getAgeProperty() {
		return AGE;
	}

	public int getMaxAge() {
		return 7;
	}

	public int getAge(BlockState state) {
		return state.get(getAgeProperty());
	}

	public BlockState withAge(int age) {
		return getDefaultState().with(getAgeProperty(), age);
	}

	public final boolean isMature(BlockState state) {
		return getAge(state) >= getMaxAge();
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return isMature(state) == false;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBaseLightLevel(pos, 0) < 9) {
			return;
		}

		int age = getAge(state);

		if (age >= getMaxAge()) {
			return;
		}

		float moisture = getAvailableMoisture(this, world, pos);

		if (random.nextInt((int) (25.0F / moisture) + 1) == 0) {
			world.setBlockState(pos, withAge(age + 1), Block.NOTIFY_LISTENERS);
		}
	}

	public void applyGrowth(World world, BlockPos pos, BlockState state) {
		int newAge = Math.min(getMaxAge(), getAge(state) + getGrowthAmount(world));

		world.setBlockState(pos, withAge(newAge), Block.NOTIFY_LISTENERS);
	}

	protected int getGrowthAmount(World world) {
		return MathHelper.nextInt(world.random, 2, 5);
	}

	/**
	 * Вычисляет коэффициент влажности для блока культуры. Учитывает влажность грядок
	 * в радиусе 1 блока и штрафует за соседние культуры того же вида (конкуренция за ресурсы).
	 */
	protected static float getAvailableMoisture(Block block, BlockView world, BlockPos pos) {
		float moisture = 1.0F;
		BlockPos below = pos.down();

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				float bonus = 0.0F;
				BlockState ground = world.getBlockState(below.add(dx, 0, dz));

				if (ground.isOf(Blocks.FARMLAND)) {
					bonus = ground.get(FarmlandBlock.MOISTURE) > 0 ? 3.0F : 1.0F;
				}

				if (dx != 0 || dz != 0) {
					bonus /= 4.0F;
				}

				moisture += bonus;
			}
		}

		BlockPos north = pos.north();
		BlockPos south = pos.south();
		BlockPos west = pos.west();
		BlockPos east = pos.east();
		boolean hasEastWest = world.getBlockState(west).isOf(block) || world.getBlockState(east).isOf(block);
		boolean hasNorthSouth = world.getBlockState(north).isOf(block) || world.getBlockState(south).isOf(block);

		if (hasEastWest && hasNorthSouth) {
			moisture /= 2.0F;
			return moisture;
		}

		boolean hasDiagonal = world.getBlockState(west.north()).isOf(block)
			|| world.getBlockState(east.north()).isOf(block)
			|| world.getBlockState(east.south()).isOf(block)
			|| world.getBlockState(west.south()).isOf(block);

		if (hasDiagonal) {
			moisture /= 2.0F;
		}

		return moisture;
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return hasEnoughLightAt(world, pos) && super.canPlaceAt(state, world, pos);
	}


	protected static boolean hasEnoughLightAt(WorldView world, BlockPos pos) {
		return world.getBaseLightLevel(pos, 0) >= 8;
	}

	@Override
	protected void onEntityCollision(
			BlockState state,
			World world,
			BlockPos pos,
			Entity entity,
			EntityCollisionHandler handler,
			boolean firstCollision
	) {
		if (world instanceof ServerWorld serverWorld
				&& entity instanceof RavagerEntity
				&& serverWorld.getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
			serverWorld.breakBlock(pos, true, entity);
		}

		super.onEntityCollision(state, world, pos, entity, handler, firstCollision);
	}

	protected ItemConvertible getSeedsItem() {
		return Items.WHEAT_SEEDS;
	}

	@Override
	protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(getSeedsItem());
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return isMature(state) == false;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		applyGrowth(world, pos, state);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}
}
