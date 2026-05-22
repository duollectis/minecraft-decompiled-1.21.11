package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Блок огня, распространяющийся по воспламеняемым блокам.
 * <p>Каждый тик огонь стареет (свойство {@link #AGE}), пытается поджечь соседние блоки
 * и гасится дождём. Шансы горения и распространения задаются через
 * {@link #registerFlammableBlock(Block, int, int)} и хранятся в отдельных картах.</p>
 */
public class FireBlock extends AbstractFireBlock {

	public static final MapCodec<FireBlock> CODEC = createCodec(FireBlock::new);
	public static final int MAX_AGE = 15;
	/** Флаги обновления блока: уведомить соседей, но не перерисовывать (Block.NOTIFY_ALL | Block.NO_REDRAW). */
	private static final int UPDATE_FLAGS_NO_RERENDER = 260;
	public static final IntProperty AGE = Properties.AGE_15;
	public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
	public static final BooleanProperty EAST = ConnectingBlock.EAST;
	public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
	public static final BooleanProperty WEST = ConnectingBlock.WEST;
	public static final BooleanProperty UP = ConnectingBlock.UP;
	public static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = ConnectingBlock.FACING_PROPERTIES
			.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != Direction.DOWN)
			.collect(Util.toMap());
	private final Function<BlockState, VoxelShape> shapeFunction;
	private static final int WOOD_FLAMMABILITY = 60;
	private static final int LEAVES_FLAMMABILITY = 30;
	private static final int WOOL_FLAMMABILITY = 15;
	private static final int STONE_FLAMMABILITY = 5;
	private static final int DRY_GRASS_FLAMMABILITY = 100;
	private static final int PLANKS_FLAMMABILITY = 60;
	private static final int FENCE_FLAMMABILITY = 20;
	private static final int COAL_FLAMMABILITY = 5;
	private final Object2IntMap<Block> burnChances = new Object2IntOpenHashMap();
	private final Object2IntMap<Block> spreadChances = new Object2IntOpenHashMap();

	@Override
	public MapCodec<FireBlock> getCodec() {
		return CODEC;
	}

	public FireBlock(AbstractBlock.Settings settings) {
		super(settings, 1.0F);
		this.setDefaultState(
				this.stateManager
						.getDefaultState()
						.with(AGE, 0)
						.with(NORTH, false)
						.with(EAST, false)
						.with(SOUTH, false)
						.with(WEST, false)
						.with(UP, false)
		);
		this.shapeFunction = this.createShapeFunction();
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		Map<Direction, VoxelShape> map = VoxelShapes.createFacingShapeMap(Block.createCuboidZShape(16.0, 0.0, 1.0));
		return this.createShapeFunction(
				state -> {
					VoxelShape voxelShape = VoxelShapes.empty();

					for (Entry<Direction, BooleanProperty> entry : DIRECTION_PROPERTIES.entrySet()) {
						if (state.get(entry.getValue())) {
							voxelShape = VoxelShapes.union(voxelShape, map.get(entry.getKey()));
						}
					}

					return voxelShape.isEmpty() ? BASE_SHAPE : voxelShape;
				}, AGE
		);
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
		return this.canPlaceAt(state, world, pos) ? this.getStateWithAge(world, pos, state.get(AGE))
		                                          : Blocks.AIR.getDefaultState();
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shapeFunction.apply(state);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getStateForPosition(ctx.getWorld(), ctx.getBlockPos());
	}

	protected BlockState getStateForPosition(BlockView world, BlockPos pos) {
		BlockPos blockPos = pos.down();
		BlockState blockState = world.getBlockState(blockPos);
		if (!this.isFlammable(blockState) && !blockState.isSideSolidFullSquare(world, blockPos, Direction.UP)) {
			BlockState blockState2 = this.getDefaultState();

			for (Direction direction : Direction.values()) {
				BooleanProperty booleanProperty = DIRECTION_PROPERTIES.get(direction);
				if (booleanProperty != null) {
					blockState2 =
							blockState2.with(
									booleanProperty,
									this.isFlammable(world.getBlockState(pos.offset(direction)))
							);
				}
			}

			return blockState2;
		}
		else {
			return this.getDefaultState();
		}
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos blockPos = pos.down();
		return world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, Direction.UP)
				|| this.areBlocksAroundFlammable(world, pos);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		world.scheduleBlockTick(pos, this, getFireTickDelay(world.random));
		if (world.canFireSpread(pos)) {
			if (!state.canPlaceAt(world, pos)) {
				world.removeBlock(pos, false);
			}

			BlockState blockState = world.getBlockState(pos.down());
			boolean bl = blockState.isIn(world.getDimension().infiniburn());
			int i = state.get(AGE);
			if (!bl && world.isRaining() && this.isRainingAround(world, pos) && random.nextFloat() < 0.2F + i * 0.03F) {
				world.removeBlock(pos, false);
			}
			else {
				int j = Math.min(15, i + random.nextInt(3) / 2);
				if (i != j) {
					state = state.with(AGE, j);
					world.setBlockState(pos, state, UPDATE_FLAGS_NO_RERENDER);
				}

				if (!bl) {
					if (!this.areBlocksAroundFlammable(world, pos)) {
						BlockPos blockPos = pos.down();
						if (!world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, Direction.UP)
								|| i > 3) {
							world.removeBlock(pos, false);
						}

						return;
					}

					if (i == MAX_AGE && random.nextInt(4) == 0 && !this.isFlammable(world.getBlockState(pos.down()))) {
						world.removeBlock(pos, false);
						return;
					}
				}

				boolean
						bl2 =
						world
								.getEnvironmentAttributes()
								.getAttributeValue(EnvironmentAttributes.INCREASED_FIRE_BURNOUT_GAMEPLAY, pos);
				int k = bl2 ? -50 : 0;
				this.trySpreadingFire(world, pos.east(), 300 + k, random, i);
				this.trySpreadingFire(world, pos.west(), 300 + k, random, i);
				this.trySpreadingFire(world, pos.down(), 250 + k, random, i);
				this.trySpreadingFire(world, pos.up(), 250 + k, random, i);
				this.trySpreadingFire(world, pos.north(), 300 + k, random, i);
				this.trySpreadingFire(world, pos.south(), 300 + k, random, i);
				BlockPos.Mutable mutable = new BlockPos.Mutable();

				for (int l = -1; l <= 1; l++) {
					for (int m = -1; m <= 1; m++) {
						for (int n = -1; n <= 4; n++) {
							if (l != 0 || n != 0 || m != 0) {
								int o = DRY_GRASS_FLAMMABILITY;
								if (n > 1) {
									o += (n - 1) * DRY_GRASS_FLAMMABILITY;
								}

								mutable.set(pos, l, n, m);
								int p = this.getBurnChance(world, mutable);
								if (p > 0) {
									int q = (p + 40 + world.getDifficulty().getId() * 7) / (i + LEAVES_FLAMMABILITY);
									if (bl2) {
										q /= 2;
									}

									if (q > 0 && random.nextInt(o) <= q && (!world.isRaining() || !this.isRainingAround(
											world,
											mutable
									)
									)) {
										int r = Math.min(15, i + random.nextInt(5) / 4);
										world.setBlockState(mutable, this.getStateWithAge(world, mutable, r), 3);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	protected boolean isRainingAround(World world, BlockPos pos) {
		return world.hasRain(pos) || world.hasRain(pos.west()) || world.hasRain(pos.east())
				|| world.hasRain(pos.north()) || world.hasRain(pos.south());
	}

	private int getSpreadChance(BlockState state) {
		return state.contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED) ? 0
		                                                                                   : this.spreadChances.getInt(
				                                                                                   state.getBlock());
	}

	private int getBurnChance(BlockState state) {
		return state.contains(Properties.WATERLOGGED) && state.get(Properties.WATERLOGGED) ? 0
		                                                                                   : this.burnChances.getInt(
				                                                                                   state.getBlock());
	}

	private void trySpreadingFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge) {
		int i = this.getSpreadChance(world.getBlockState(pos));
		if (random.nextInt(spreadFactor) < i) {
			BlockState blockState = world.getBlockState(pos);
			if (random.nextInt(currentAge + 10) < 5 && !world.hasRain(pos)) {
				int j = Math.min(currentAge + random.nextInt(5) / 4, 15);
				world.setBlockState(pos, this.getStateWithAge(world, pos, j), 3);
			}
			else {
				world.removeBlock(pos, false);
			}

			Block block = blockState.getBlock();
			if (block instanceof TntBlock) {
				TntBlock.primeTnt(world, pos);
			}
		}
	}

	private BlockState getStateWithAge(WorldView world, BlockPos pos, int age) {
		BlockState blockState = getState(world, pos);
		return blockState.isOf(Blocks.FIRE) ? blockState.with(AGE, age) : blockState;
	}

	private boolean areBlocksAroundFlammable(BlockView world, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (this.isFlammable(world.getBlockState(pos.offset(direction)))) {
				return true;
			}
		}

		return false;
	}

	private int getBurnChance(WorldView world, BlockPos pos) {
		if (!world.isAir(pos)) {
			return 0;
		}
		else {
			int i = 0;

			for (Direction direction : Direction.values()) {
				BlockState blockState = world.getBlockState(pos.offset(direction));
				i = Math.max(this.getBurnChance(blockState), i);
			}

			return i;
		}
	}

	@Override
	protected boolean isFlammable(BlockState state) {
		return this.getBurnChance(state) > 0;
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		world.scheduleBlockTick(pos, this, getFireTickDelay(world.random));
	}

	private static int getFireTickDelay(Random random) {
		return LEAVES_FLAMMABILITY + random.nextInt(10);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE, NORTH, EAST, SOUTH, WEST, UP);
	}

	public void registerFlammableBlock(Block block, int burnChance, int spreadChance) {
		this.burnChances.put(block, burnChance);
		this.spreadChances.put(block, spreadChance);
	}

	public static void registerDefaultFlammables() {
		FireBlock fireBlock = (FireBlock) Blocks.FIRE;
		fireBlock.registerFlammableBlock(Blocks.OAK_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_PLANKS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_MOSAIC, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OAK_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_MOSAIC_SLAB, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OAK_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_FENCE_GATE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OAK_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_FENCE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OAK_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_MOSAIC_STAIRS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OAK_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_BLOCK, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_OAK_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_SPRUCE_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_BIRCH_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_JUNGLE_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_ACACIA_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_CHERRY_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_DARK_OAK_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_PALE_OAK_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_MANGROVE_LOG, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_BAMBOO_BLOCK, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_OAK_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_SPRUCE_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_BIRCH_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_JUNGLE_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_ACACIA_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_CHERRY_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_DARK_OAK_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_PALE_OAK_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.STRIPPED_MANGROVE_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.OAK_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_WOOD, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_ROOTS, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OAK_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BOOKSHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.TNT, WOOL_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SHORT_GRASS, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.FERN, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DEAD_BUSH, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SHORT_DRY_GRASS, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.TALL_DRY_GRASS, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SUNFLOWER, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LILAC, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ROSE_BUSH, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PEONY, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.TALL_GRASS, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LARGE_FERN, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DANDELION, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.POPPY, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OPEN_EYEBLOSSOM, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CLOSED_EYEBLOSSOM, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BLUE_ORCHID, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ALLIUM, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.AZURE_BLUET, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.RED_TULIP, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ORANGE_TULIP, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.WHITE_TULIP, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PINK_TULIP, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OXEYE_DAISY, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CORNFLOWER, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LILY_OF_THE_VALLEY, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.TORCHFLOWER, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PITCHER_PLANT, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.WITHER_ROSE, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PINK_PETALS, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.WILDFLOWERS, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LEAF_LITTER, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CACTUS_FLOWER, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.WHITE_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ORANGE_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MAGENTA_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LIGHT_BLUE_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.YELLOW_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LIME_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PINK_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.GRAY_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LIGHT_GRAY_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CYAN_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PURPLE_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BLUE_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BROWN_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.GREEN_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.RED_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BLACK_WOOL, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.VINE, WOOL_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.COAL_BLOCK, 5, 5);
		fireBlock.registerFlammableBlock(Blocks.HAY_BLOCK, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.TARGET, WOOL_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.WHITE_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ORANGE_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MAGENTA_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LIGHT_BLUE_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.YELLOW_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LIME_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PINK_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.GRAY_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.LIGHT_GRAY_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CYAN_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PURPLE_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BLUE_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BROWN_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.GREEN_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.RED_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BLACK_CARPET, WOOD_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_MOSS_BLOCK, 5, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_MOSS_CARPET, 5, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_HANGING_MOSS, 5, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DRIED_KELP_BLOCK, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO, WOOD_FLAMMABILITY, 60);
		fireBlock.registerFlammableBlock(Blocks.SCAFFOLDING, WOOD_FLAMMABILITY, 60);
		fireBlock.registerFlammableBlock(Blocks.LECTERN, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.COMPOSTER, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SWEET_BERRY_BUSH, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BEEHIVE, 5, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BEE_NEST, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.AZALEA_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.FLOWERING_AZALEA_LEAVES, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CAVE_VINES, MAX_AGE, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CAVE_VINES_PLANT, MAX_AGE, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPORE_BLOSSOM, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.AZALEA, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.FLOWERING_AZALEA, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIG_DRIPLEAF, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIG_DRIPLEAF_STEM, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SMALL_DRIPLEAF, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.HANGING_ROOTS, LEAVES_FLAMMABILITY, WOOD_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.GLOW_LICHEN, WOOL_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.FIREFLY_BUSH, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BUSH, WOOD_FLAMMABILITY, DRY_GRASS_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.ACACIA_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BAMBOO_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.BIRCH_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.CHERRY_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.DARK_OAK_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.JUNGLE_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.MANGROVE_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.OAK_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.PALE_OAK_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
		fireBlock.registerFlammableBlock(Blocks.SPRUCE_SHELF, LEAVES_FLAMMABILITY, FENCE_FLAMMABILITY);
	}
}
