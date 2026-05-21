package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

/**
 * {@code ComposterBlock}.
 */
public class ComposterBlock extends Block implements InventoryProvider {

	public static final MapCodec<ComposterBlock> CODEC = createCodec(ComposterBlock::new);
	public static final int NUM_LEVELS = 8;
	public static final int MIN_LEVEL = 0;
	public static final int MAX_LEVEL = 7;
	public static final IntProperty LEVEL = Properties.LEVEL_8;
	public static final Object2FloatMap<ItemConvertible> ITEM_TO_LEVEL_INCREASE_CHANCE = new Object2FloatOpenHashMap();
	private static final int INNER_DIAMETER = 12;
	private static final VoxelShape[] COLLISION_SHAPES_BY_LEVEL = Util.make(
			() -> {
				VoxelShape[] voxelShapes = Block.createShapeArray(
						8,
						level -> VoxelShapes.combineAndSimplify(
								VoxelShapes.fullCube(),
								Block.createColumnShape(12.0, Math.clamp((long) (1 + level * 2), 2, 16), 16.0),
								BooleanBiFunction.ONLY_FIRST
						)
				);
				voxelShapes[8] = voxelShapes[7];
				return voxelShapes;
			}
	);

	@Override
	public MapCodec<ComposterBlock> getCodec() {
		return CODEC;
	}

	/**
	 * Регистрирует default compostable items.
	 */
	public static void registerDefaultCompostableItems() {
		ITEM_TO_LEVEL_INCREASE_CHANCE.defaultReturnValue(-1.0F);
		float f = 0.3F;
		float g = 0.5F;
		float h = 0.65F;
		float i = 0.85F;
		float j = 1.0F;
		registerCompostableItem(0.3F, Items.JUNGLE_LEAVES);
		registerCompostableItem(0.3F, Items.OAK_LEAVES);
		registerCompostableItem(0.3F, Items.SPRUCE_LEAVES);
		registerCompostableItem(0.3F, Items.DARK_OAK_LEAVES);
		registerCompostableItem(0.3F, Items.PALE_OAK_LEAVES);
		registerCompostableItem(0.3F, Items.ACACIA_LEAVES);
		registerCompostableItem(0.3F, Items.CHERRY_LEAVES);
		registerCompostableItem(0.3F, Items.BIRCH_LEAVES);
		registerCompostableItem(0.3F, Items.AZALEA_LEAVES);
		registerCompostableItem(0.3F, Items.MANGROVE_LEAVES);
		registerCompostableItem(0.3F, Items.OAK_SAPLING);
		registerCompostableItem(0.3F, Items.SPRUCE_SAPLING);
		registerCompostableItem(0.3F, Items.BIRCH_SAPLING);
		registerCompostableItem(0.3F, Items.JUNGLE_SAPLING);
		registerCompostableItem(0.3F, Items.ACACIA_SAPLING);
		registerCompostableItem(0.3F, Items.CHERRY_SAPLING);
		registerCompostableItem(0.3F, Items.DARK_OAK_SAPLING);
		registerCompostableItem(0.3F, Items.PALE_OAK_SAPLING);
		registerCompostableItem(0.3F, Items.MANGROVE_PROPAGULE);
		registerCompostableItem(0.3F, Items.BEETROOT_SEEDS);
		registerCompostableItem(0.3F, Items.DRIED_KELP);
		registerCompostableItem(0.3F, Items.SHORT_GRASS);
		registerCompostableItem(0.3F, Items.KELP);
		registerCompostableItem(0.3F, Items.MELON_SEEDS);
		registerCompostableItem(0.3F, Items.PUMPKIN_SEEDS);
		registerCompostableItem(0.3F, Items.SEAGRASS);
		registerCompostableItem(0.3F, Items.SWEET_BERRIES);
		registerCompostableItem(0.3F, Items.GLOW_BERRIES);
		registerCompostableItem(0.3F, Items.WHEAT_SEEDS);
		registerCompostableItem(0.3F, Items.MOSS_CARPET);
		registerCompostableItem(0.3F, Items.PALE_MOSS_CARPET);
		registerCompostableItem(0.3F, Items.PALE_HANGING_MOSS);
		registerCompostableItem(0.3F, Items.PINK_PETALS);
		registerCompostableItem(0.3F, Items.WILDFLOWERS);
		registerCompostableItem(0.3F, Items.LEAF_LITTER);
		registerCompostableItem(0.3F, Items.SMALL_DRIPLEAF);
		registerCompostableItem(0.3F, Items.HANGING_ROOTS);
		registerCompostableItem(0.3F, Items.MANGROVE_ROOTS);
		registerCompostableItem(0.3F, Items.TORCHFLOWER_SEEDS);
		registerCompostableItem(0.3F, Items.PITCHER_POD);
		registerCompostableItem(0.3F, Items.FIREFLY_BUSH);
		registerCompostableItem(0.3F, Items.BUSH);
		registerCompostableItem(0.3F, Items.CACTUS_FLOWER);
		registerCompostableItem(0.3F, Items.SHORT_DRY_GRASS);
		registerCompostableItem(0.3F, Items.TALL_DRY_GRASS);
		registerCompostableItem(0.5F, Items.DRIED_KELP_BLOCK);
		registerCompostableItem(0.5F, Items.TALL_GRASS);
		registerCompostableItem(0.5F, Items.FLOWERING_AZALEA_LEAVES);
		registerCompostableItem(0.5F, Items.CACTUS);
		registerCompostableItem(0.5F, Items.SUGAR_CANE);
		registerCompostableItem(0.5F, Items.VINE);
		registerCompostableItem(0.5F, Items.NETHER_SPROUTS);
		registerCompostableItem(0.5F, Items.WEEPING_VINES);
		registerCompostableItem(0.5F, Items.TWISTING_VINES);
		registerCompostableItem(0.5F, Items.MELON_SLICE);
		registerCompostableItem(0.5F, Items.GLOW_LICHEN);
		registerCompostableItem(0.65F, Items.SEA_PICKLE);
		registerCompostableItem(0.65F, Items.LILY_PAD);
		registerCompostableItem(0.65F, Items.PUMPKIN);
		registerCompostableItem(0.65F, Items.CARVED_PUMPKIN);
		registerCompostableItem(0.65F, Items.MELON);
		registerCompostableItem(0.65F, Items.APPLE);
		registerCompostableItem(0.65F, Items.BEETROOT);
		registerCompostableItem(0.65F, Items.CARROT);
		registerCompostableItem(0.65F, Items.COCOA_BEANS);
		registerCompostableItem(0.65F, Items.POTATO);
		registerCompostableItem(0.65F, Items.WHEAT);
		registerCompostableItem(0.65F, Items.BROWN_MUSHROOM);
		registerCompostableItem(0.65F, Items.RED_MUSHROOM);
		registerCompostableItem(0.65F, Items.MUSHROOM_STEM);
		registerCompostableItem(0.65F, Items.CRIMSON_FUNGUS);
		registerCompostableItem(0.65F, Items.WARPED_FUNGUS);
		registerCompostableItem(0.65F, Items.NETHER_WART);
		registerCompostableItem(0.65F, Items.CRIMSON_ROOTS);
		registerCompostableItem(0.65F, Items.WARPED_ROOTS);
		registerCompostableItem(0.65F, Items.SHROOMLIGHT);
		registerCompostableItem(0.65F, Items.DANDELION);
		registerCompostableItem(0.65F, Items.POPPY);
		registerCompostableItem(0.65F, Items.BLUE_ORCHID);
		registerCompostableItem(0.65F, Items.ALLIUM);
		registerCompostableItem(0.65F, Items.AZURE_BLUET);
		registerCompostableItem(0.65F, Items.RED_TULIP);
		registerCompostableItem(0.65F, Items.ORANGE_TULIP);
		registerCompostableItem(0.65F, Items.WHITE_TULIP);
		registerCompostableItem(0.65F, Items.PINK_TULIP);
		registerCompostableItem(0.65F, Items.OXEYE_DAISY);
		registerCompostableItem(0.65F, Items.CORNFLOWER);
		registerCompostableItem(0.65F, Items.LILY_OF_THE_VALLEY);
		registerCompostableItem(0.65F, Items.WITHER_ROSE);
		registerCompostableItem(0.65F, Items.OPEN_EYEBLOSSOM);
		registerCompostableItem(0.65F, Items.CLOSED_EYEBLOSSOM);
		registerCompostableItem(0.65F, Items.FERN);
		registerCompostableItem(0.65F, Items.SUNFLOWER);
		registerCompostableItem(0.65F, Items.LILAC);
		registerCompostableItem(0.65F, Items.ROSE_BUSH);
		registerCompostableItem(0.65F, Items.PEONY);
		registerCompostableItem(0.65F, Items.LARGE_FERN);
		registerCompostableItem(0.65F, Items.SPORE_BLOSSOM);
		registerCompostableItem(0.65F, Items.AZALEA);
		registerCompostableItem(0.65F, Items.MOSS_BLOCK);
		registerCompostableItem(0.65F, Items.PALE_MOSS_BLOCK);
		registerCompostableItem(0.65F, Items.BIG_DRIPLEAF);
		registerCompostableItem(0.85F, Items.HAY_BLOCK);
		registerCompostableItem(0.85F, Items.BROWN_MUSHROOM_BLOCK);
		registerCompostableItem(0.85F, Items.RED_MUSHROOM_BLOCK);
		registerCompostableItem(0.85F, Items.NETHER_WART_BLOCK);
		registerCompostableItem(0.85F, Items.WARPED_WART_BLOCK);
		registerCompostableItem(0.85F, Items.FLOWERING_AZALEA);
		registerCompostableItem(0.85F, Items.BREAD);
		registerCompostableItem(0.85F, Items.BAKED_POTATO);
		registerCompostableItem(0.85F, Items.COOKIE);
		registerCompostableItem(0.85F, Items.TORCHFLOWER);
		registerCompostableItem(0.85F, Items.PITCHER_PLANT);
		registerCompostableItem(1.0F, Items.CAKE);
		registerCompostableItem(1.0F, Items.PUMPKIN_PIE);
	}

	private static void registerCompostableItem(float levelIncreaseChance, ItemConvertible item) {
		ITEM_TO_LEVEL_INCREASE_CHANCE.put(item.asItem(), levelIncreaseChance);
	}

	public ComposterBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(LEVEL, 0));
	}

	/**
	 * Play effects.
	 *
	 * @param world world
	 * @param pos pos
	 * @param fill fill
	 */
	public static void playEffects(World world, BlockPos pos, boolean fill) {
		BlockState blockState = world.getBlockState(pos);
		world.playSoundAtBlockCenterClient(
				pos,
				fill ? SoundEvents.BLOCK_COMPOSTER_FILL_SUCCESS : SoundEvents.BLOCK_COMPOSTER_FILL,
				SoundCategory.BLOCKS,
				1.0F,
				1.0F,
				false
		);
		double d = blockState.getOutlineShape(world, pos).getEndingCoord(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
		double e = 2.0;
		double f = 0.1875;
		double g = 0.625;
		Random random = world.getRandom();

		for (int i = 0; i < 10; i++) {
			double h = random.nextGaussian() * 0.02;
			double j = random.nextGaussian() * 0.02;
			double k = random.nextGaussian() * 0.02;
			world.addParticleClient(
					ParticleTypes.COMPOSTER,
					pos.getX() + 0.1875 + 0.625 * random.nextFloat(),
					pos.getY() + d + random.nextFloat() * (1.0 - d),
					pos.getZ() + 0.1875 + 0.625 * random.nextFloat(),
					h,
					j,
					k
			);
		}
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return COLLISION_SHAPES_BY_LEVEL[state.get(LEVEL)];
	}

	@Override
	protected VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
		return VoxelShapes.fullCube();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return COLLISION_SHAPES_BY_LEVEL[0];
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (state.get(LEVEL) == 7) {
			world.scheduleBlockTick(pos, state.getBlock(), 20);
		}
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
		int i = state.get(LEVEL);
		if (i < 8 && ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem())) {
			if (i < 7 && !world.isClient()) {
				BlockState blockState = addToComposter(player, state, world, pos, stack);
				world.syncWorldEvent(1500, pos, state != blockState ? 1 : 0);
				player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
				stack.decrementUnlessCreative(1, player);
			}

			return ActionResult.SUCCESS;
		}
		else {
			return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
		}
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		int i = state.get(LEVEL);
		if (i == 8) {
			emptyFullComposter(player, state, world, pos);
			return ActionResult.SUCCESS;
		}
		else {
			return ActionResult.PASS;
		}
	}

	/**
	 * Compost.
	 *
	 * @param user user
	 * @param state state
	 * @param world world
	 * @param stack stack
	 * @param pos pos
	 *
	 * @return BlockState — результат операции
	 */
	public static BlockState compost(Entity user, BlockState state, ServerWorld world, ItemStack stack, BlockPos pos) {
		int i = state.get(LEVEL);
		if (i < 7 && ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem())) {
			BlockState blockState = addToComposter(user, state, world, pos, stack);
			stack.decrement(1);
			return blockState;
		}
		else {
			return state;
		}
	}

	/**
	 * Empty full composter.
	 *
	 * @param user user
	 * @param state state
	 * @param world world
	 * @param pos pos
	 *
	 * @return BlockState — результат операции
	 */
	public static BlockState emptyFullComposter(Entity user, BlockState state, World world, BlockPos pos) {
		if (!world.isClient()) {
			Vec3d vec3d = Vec3d.add(pos, 0.5, 1.01, 0.5).addHorizontalRandom(world.random, 0.7F);
			ItemEntity
					itemEntity =
					new ItemEntity(world, vec3d.getX(), vec3d.getY(), vec3d.getZ(), new ItemStack(Items.BONE_MEAL));
			itemEntity.setToDefaultPickupDelay();
			world.spawnEntity(itemEntity);
		}

		BlockState blockState = emptyComposter(user, state, world, pos);
		world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		return blockState;
	}

	static BlockState emptyComposter(@Nullable Entity user, BlockState state, WorldAccess world, BlockPos pos) {
		BlockState blockState = state.with(LEVEL, 0);
		world.setBlockState(pos, blockState, 3);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, blockState));
		return blockState;
	}

	static BlockState addToComposter(
			@Nullable Entity user,
			BlockState state,
			WorldAccess world,
			BlockPos pos,
			ItemStack stack
	) {
		int i = state.get(LEVEL);
		float f = ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(stack.getItem());
		if ((i != 0 || !(f > 0.0F)) && !(world.getRandom().nextDouble() < f)) {
			return state;
		}
		else {
			int j = i + 1;
			BlockState blockState = state.with(LEVEL, j);
			world.setBlockState(pos, blockState, 3);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, blockState));
			if (j == 7) {
				world.scheduleBlockTick(pos, state.getBlock(), 20);
			}

			return blockState;
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(LEVEL) == 7) {
			world.setBlockState(pos, state.cycle(LEVEL), 3);
			world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_READY, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
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
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	@Override
	public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
		int i = state.get(LEVEL);
		if (i == 8) {
			return new ComposterBlock.FullComposterInventory(state, world, pos, new ItemStack(Items.BONE_MEAL));
		}
		else {
			return (SidedInventory) (i < 7 ? new ComposterBlock.ComposterInventory(state, world, pos)
			                               : new ComposterBlock.DummyInventory()
			);
		}
	}

	/**
	 * {@code ComposterInventory}.
	 */
	static class ComposterInventory extends SimpleInventory implements SidedInventory {

		private final BlockState state;
		private final WorldAccess world;
		private final BlockPos pos;
		private boolean dirty;

		public ComposterInventory(BlockState state, WorldAccess world, BlockPos pos) {
			super(1);
			this.state = state;
			this.world = world;
			this.pos = pos;
		}

		@Override
		public int getMaxCountPerStack() {
			return 1;
		}

		@Override
		public int[] getAvailableSlots(Direction side) {
			return side == Direction.UP ? new int[]{0} : new int[0];
		}

		@Override
		public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
			return !this.dirty && dir == Direction.UP
					&& ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem());
		}

		@Override
		public boolean canExtract(int slot, ItemStack stack, Direction dir) {
			return false;
		}

		@Override
		public void markDirty() {
			ItemStack itemStack = this.getStack(0);
			if (!itemStack.isEmpty()) {
				this.dirty = true;
				BlockState
						blockState =
						ComposterBlock.addToComposter(null, this.state, this.world, this.pos, itemStack);
				this.world.syncWorldEvent(1500, this.pos, blockState != this.state ? 1 : 0);
				this.removeStack(0);
			}
		}
	}

	/**
	 * {@code DummyInventory}.
	 */
	static class DummyInventory extends SimpleInventory implements SidedInventory {

		public DummyInventory() {
			super(0);
		}

		@Override
		public int[] getAvailableSlots(Direction side) {
			return new int[0];
		}

		@Override
		public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
			return false;
		}

		@Override
		public boolean canExtract(int slot, ItemStack stack, Direction dir) {
			return false;
		}
	}

	/**
	 * {@code FullComposterInventory}.
	 */
	static class FullComposterInventory extends SimpleInventory implements SidedInventory {

		private final BlockState state;
		private final WorldAccess world;
		private final BlockPos pos;
		private boolean dirty;

		public FullComposterInventory(BlockState state, WorldAccess world, BlockPos pos, ItemStack outputItem) {
			super(outputItem);
			this.state = state;
			this.world = world;
			this.pos = pos;
		}

		@Override
		public int getMaxCountPerStack() {
			return 1;
		}

		@Override
		public int[] getAvailableSlots(Direction side) {
			return side == Direction.DOWN ? new int[]{0} : new int[0];
		}

		@Override
		public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
			return false;
		}

		@Override
		public boolean canExtract(int slot, ItemStack stack, Direction dir) {
			return !this.dirty && dir == Direction.DOWN && stack.isOf(Items.BONE_MEAL);
		}

		@Override
		public void markDirty() {
			ComposterBlock.emptyComposter(null, this.state, this.world, this.pos);
			this.dirty = true;
		}
	}
}
