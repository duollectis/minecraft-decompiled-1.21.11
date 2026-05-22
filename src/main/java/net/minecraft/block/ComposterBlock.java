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
 * Блок компостера — принимает органические предметы и постепенно заполняется (уровни 0–7).
 * При достижении уровня 7 через 20 тиков переходит на уровень 8 (готов к сбору).
 * На уровне 8 при взаимодействии выбрасывает костную муку и сбрасывается до 0.
 * Поддерживает автоматизацию через хоппер (SidedInventory).
 */
public class ComposterBlock extends Block implements InventoryProvider {

	public static final MapCodec<ComposterBlock> CODEC = createCodec(ComposterBlock::new);
	public static final int NUM_LEVELS = 8;
	public static final int MIN_LEVEL = 0;
	public static final int MAX_LEVEL = 7;
	public static final int FULL_LEVEL = 8;
	public static final int READY_TICK_DELAY = 20;
	public static final IntProperty LEVEL = Properties.LEVEL_8;
	public static final Object2FloatMap<ItemConvertible> ITEM_TO_LEVEL_INCREASE_CHANCE = new Object2FloatOpenHashMap();
	private static final int INNER_DIAMETER = 12;
	private static final VoxelShape[] COLLISION_SHAPES_BY_LEVEL = Util.make(
			() -> {
				VoxelShape[] shapes = Block.createShapeArray(
						NUM_LEVELS,
						level -> VoxelShapes.combineAndSimplify(
								VoxelShapes.fullCube(),
								Block.createColumnShape(INNER_DIAMETER, Math.clamp((long) (1 + level * 2), 2, 16), 16.0),
								BooleanBiFunction.ONLY_FIRST
						)
				);
				shapes[FULL_LEVEL] = shapes[MAX_LEVEL];
				return shapes;
			}
	);

	@Override
	public MapCodec<ComposterBlock> getCodec() {
		return CODEC;
	}

	/**
	 * Регистрирует все стандартные компостируемые предметы с их шансами повышения уровня.
	 * Вызывается один раз при инициализации игры.
	 */
	public static void registerDefaultCompostableItems() {
		ITEM_TO_LEVEL_INCREASE_CHANCE.defaultReturnValue(-1.0F);
		float low = 0.3F;
		float medium = 0.5F;
		float high = 0.65F;
		float veryHigh = 0.85F;
		float guaranteed = 1.0F;
		registerCompostableItem(low, Items.JUNGLE_LEAVES);
		registerCompostableItem(low, Items.OAK_LEAVES);
		registerCompostableItem(low, Items.SPRUCE_LEAVES);
		registerCompostableItem(low, Items.DARK_OAK_LEAVES);
		registerCompostableItem(low, Items.PALE_OAK_LEAVES);
		registerCompostableItem(low, Items.ACACIA_LEAVES);
		registerCompostableItem(low, Items.CHERRY_LEAVES);
		registerCompostableItem(low, Items.BIRCH_LEAVES);
		registerCompostableItem(low, Items.AZALEA_LEAVES);
		registerCompostableItem(low, Items.MANGROVE_LEAVES);
		registerCompostableItem(low, Items.OAK_SAPLING);
		registerCompostableItem(low, Items.SPRUCE_SAPLING);
		registerCompostableItem(low, Items.BIRCH_SAPLING);
		registerCompostableItem(low, Items.JUNGLE_SAPLING);
		registerCompostableItem(low, Items.ACACIA_SAPLING);
		registerCompostableItem(low, Items.CHERRY_SAPLING);
		registerCompostableItem(low, Items.DARK_OAK_SAPLING);
		registerCompostableItem(low, Items.PALE_OAK_SAPLING);
		registerCompostableItem(low, Items.MANGROVE_PROPAGULE);
		registerCompostableItem(low, Items.BEETROOT_SEEDS);
		registerCompostableItem(low, Items.DRIED_KELP);
		registerCompostableItem(low, Items.SHORT_GRASS);
		registerCompostableItem(low, Items.KELP);
		registerCompostableItem(low, Items.MELON_SEEDS);
		registerCompostableItem(low, Items.PUMPKIN_SEEDS);
		registerCompostableItem(low, Items.SEAGRASS);
		registerCompostableItem(low, Items.SWEET_BERRIES);
		registerCompostableItem(low, Items.GLOW_BERRIES);
		registerCompostableItem(low, Items.WHEAT_SEEDS);
		registerCompostableItem(low, Items.MOSS_CARPET);
		registerCompostableItem(low, Items.PALE_MOSS_CARPET);
		registerCompostableItem(low, Items.PALE_HANGING_MOSS);
		registerCompostableItem(low, Items.PINK_PETALS);
		registerCompostableItem(low, Items.WILDFLOWERS);
		registerCompostableItem(low, Items.LEAF_LITTER);
		registerCompostableItem(low, Items.SMALL_DRIPLEAF);
		registerCompostableItem(low, Items.HANGING_ROOTS);
		registerCompostableItem(low, Items.MANGROVE_ROOTS);
		registerCompostableItem(low, Items.TORCHFLOWER_SEEDS);
		registerCompostableItem(low, Items.PITCHER_POD);
		registerCompostableItem(low, Items.FIREFLY_BUSH);
		registerCompostableItem(low, Items.BUSH);
		registerCompostableItem(low, Items.CACTUS_FLOWER);
		registerCompostableItem(low, Items.SHORT_DRY_GRASS);
		registerCompostableItem(low, Items.TALL_DRY_GRASS);
		registerCompostableItem(medium, Items.DRIED_KELP_BLOCK);
		registerCompostableItem(medium, Items.TALL_GRASS);
		registerCompostableItem(medium, Items.FLOWERING_AZALEA_LEAVES);
		registerCompostableItem(medium, Items.CACTUS);
		registerCompostableItem(medium, Items.SUGAR_CANE);
		registerCompostableItem(medium, Items.VINE);
		registerCompostableItem(medium, Items.NETHER_SPROUTS);
		registerCompostableItem(medium, Items.WEEPING_VINES);
		registerCompostableItem(medium, Items.TWISTING_VINES);
		registerCompostableItem(medium, Items.MELON_SLICE);
		registerCompostableItem(medium, Items.GLOW_LICHEN);
		registerCompostableItem(high, Items.SEA_PICKLE);
		registerCompostableItem(high, Items.LILY_PAD);
		registerCompostableItem(high, Items.PUMPKIN);
		registerCompostableItem(high, Items.CARVED_PUMPKIN);
		registerCompostableItem(high, Items.MELON);
		registerCompostableItem(high, Items.APPLE);
		registerCompostableItem(high, Items.BEETROOT);
		registerCompostableItem(high, Items.CARROT);
		registerCompostableItem(high, Items.COCOA_BEANS);
		registerCompostableItem(high, Items.POTATO);
		registerCompostableItem(high, Items.WHEAT);
		registerCompostableItem(high, Items.BROWN_MUSHROOM);
		registerCompostableItem(high, Items.RED_MUSHROOM);
		registerCompostableItem(high, Items.MUSHROOM_STEM);
		registerCompostableItem(high, Items.CRIMSON_FUNGUS);
		registerCompostableItem(high, Items.WARPED_FUNGUS);
		registerCompostableItem(high, Items.NETHER_WART);
		registerCompostableItem(high, Items.CRIMSON_ROOTS);
		registerCompostableItem(high, Items.WARPED_ROOTS);
		registerCompostableItem(high, Items.SHROOMLIGHT);
		registerCompostableItem(high, Items.DANDELION);
		registerCompostableItem(high, Items.POPPY);
		registerCompostableItem(high, Items.BLUE_ORCHID);
		registerCompostableItem(high, Items.ALLIUM);
		registerCompostableItem(high, Items.AZURE_BLUET);
		registerCompostableItem(high, Items.RED_TULIP);
		registerCompostableItem(high, Items.ORANGE_TULIP);
		registerCompostableItem(high, Items.WHITE_TULIP);
		registerCompostableItem(high, Items.PINK_TULIP);
		registerCompostableItem(high, Items.OXEYE_DAISY);
		registerCompostableItem(high, Items.CORNFLOWER);
		registerCompostableItem(high, Items.LILY_OF_THE_VALLEY);
		registerCompostableItem(high, Items.WITHER_ROSE);
		registerCompostableItem(high, Items.OPEN_EYEBLOSSOM);
		registerCompostableItem(high, Items.CLOSED_EYEBLOSSOM);
		registerCompostableItem(high, Items.FERN);
		registerCompostableItem(high, Items.SUNFLOWER);
		registerCompostableItem(high, Items.LILAC);
		registerCompostableItem(high, Items.ROSE_BUSH);
		registerCompostableItem(high, Items.PEONY);
		registerCompostableItem(high, Items.LARGE_FERN);
		registerCompostableItem(high, Items.SPORE_BLOSSOM);
		registerCompostableItem(high, Items.AZALEA);
		registerCompostableItem(high, Items.MOSS_BLOCK);
		registerCompostableItem(high, Items.PALE_MOSS_BLOCK);
		registerCompostableItem(high, Items.BIG_DRIPLEAF);
		registerCompostableItem(veryHigh, Items.HAY_BLOCK);
		registerCompostableItem(veryHigh, Items.BROWN_MUSHROOM_BLOCK);
		registerCompostableItem(veryHigh, Items.RED_MUSHROOM_BLOCK);
		registerCompostableItem(veryHigh, Items.NETHER_WART_BLOCK);
		registerCompostableItem(veryHigh, Items.WARPED_WART_BLOCK);
		registerCompostableItem(veryHigh, Items.FLOWERING_AZALEA);
		registerCompostableItem(veryHigh, Items.BREAD);
		registerCompostableItem(veryHigh, Items.BAKED_POTATO);
		registerCompostableItem(veryHigh, Items.COOKIE);
		registerCompostableItem(veryHigh, Items.TORCHFLOWER);
		registerCompostableItem(veryHigh, Items.PITCHER_PLANT);
		registerCompostableItem(guaranteed, Items.CAKE);
		registerCompostableItem(guaranteed, Items.PUMPKIN_PIE);
	}

	private static void registerCompostableItem(float levelIncreaseChance, ItemConvertible item) {
		ITEM_TO_LEVEL_INCREASE_CHANCE.put(item.asItem(), levelIncreaseChance);
	}

	public ComposterBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(LEVEL, 0));
	}

	/**
	 * Воспроизводит звук и частицы компостирования на клиенте.
	 *
	 * @param world мир
	 * @param pos   позиция компостера
	 * @param fill  {@code true} — успешное добавление (звук заполнения), {@code false} — обычный звук
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
		double topY = blockState.getOutlineShape(world, pos).getEndingCoord(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
		double innerMin = 0.1875;
		double innerSize = 0.625;
		Random random = world.getRandom();

		for (int particle = 0; particle < 10; particle++) {
			double velX = random.nextGaussian() * 0.02;
			double velY = random.nextGaussian() * 0.02;
			double velZ = random.nextGaussian() * 0.02;
			world.addParticleClient(
					ParticleTypes.COMPOSTER,
					pos.getX() + innerMin + innerSize * random.nextFloat(),
					pos.getY() + topY + random.nextFloat() * (1.0 - topY),
					pos.getZ() + innerMin + innerSize * random.nextFloat(),
					velX,
					velY,
					velZ
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
		if (state.get(LEVEL) == MAX_LEVEL) {
			world.scheduleBlockTick(pos, state.getBlock(), READY_TICK_DELAY);
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
		int level = state.get(LEVEL);

		if (level >= FULL_LEVEL || !ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem())) {
			return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
		}

		if (level < MAX_LEVEL && !world.isClient()) {
			BlockState newState = addToComposter(player, state, world, pos, stack);
			world.syncWorldEvent(1500, pos, state != newState ? 1 : 0);
			player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
			stack.decrementUnlessCreative(1, player);
		}

		return ActionResult.SUCCESS;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (state.get(LEVEL) != FULL_LEVEL) {
			return ActionResult.PASS;
		}

		emptyFullComposter(player, state, world, pos);

		return ActionResult.SUCCESS;
	}

	/**
	 * Добавляет предмет в компостер через хоппер или автоматизацию.
	 * Уменьшает стак на 1 при успешном добавлении.
	 *
	 * @param user  сущность, инициировавшая компостирование (может быть null)
	 * @param state текущее состояние компостера
	 * @param world серверный мир
	 * @param stack стак предмета для компостирования
	 * @param pos   позиция компостера
	 * @return новое состояние блока (или то же, если уровень не изменился)
	 */
	public static BlockState compost(Entity user, BlockState state, ServerWorld world, ItemStack stack, BlockPos pos) {
		int level = state.get(LEVEL);

		if (level >= MAX_LEVEL || !ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem())) {
			return state;
		}

		BlockState newState = addToComposter(user, state, world, pos, stack);
		stack.decrement(1);

		return newState;
	}

	/**
	 * Опустошает заполненный компостер (уровень 8): выбрасывает костную муку и сбрасывает уровень до 0.
	 *
	 * @param user  сущность, взаимодействующая с компостером
	 * @param state текущее состояние блока
	 * @param world мир
	 * @param pos   позиция компостера
	 * @return новое состояние блока с уровнем 0
	 */
	public static BlockState emptyFullComposter(Entity user, BlockState state, World world, BlockPos pos) {
		if (!world.isClient()) {
			Vec3d spawnPos = Vec3d.add(pos, 0.5, 1.01, 0.5).addHorizontalRandom(world.random, 0.7F);
			ItemEntity boneMeal = new ItemEntity(
					world,
					spawnPos.getX(),
					spawnPos.getY(),
					spawnPos.getZ(),
					new ItemStack(Items.BONE_MEAL)
			);
			boneMeal.setToDefaultPickupDelay();
			world.spawnEntity(boneMeal);
		}

		BlockState emptied = emptyComposter(user, state, world, pos);
		world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);

		return emptied;
	}

	static BlockState emptyComposter(@Nullable Entity user, BlockState state, WorldAccess world, BlockPos pos) {
		BlockState emptied = state.with(LEVEL, MIN_LEVEL);
		world.setBlockState(pos, emptied, Block.NOTIFY_ALL);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, emptied));

		return emptied;
	}

	static BlockState addToComposter(
			@Nullable Entity user,
			BlockState state,
			WorldAccess world,
			BlockPos pos,
			ItemStack stack
	) {
		int level = state.get(LEVEL);
		float chance = ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(stack.getItem());

		if ((level != MIN_LEVEL || !(chance > 0.0F)) && !(world.getRandom().nextDouble() < chance)) {
			return state;
		}

		int newLevel = level + 1;
		BlockState newState = state.with(LEVEL, newLevel);
		world.setBlockState(pos, newState, Block.NOTIFY_ALL);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(user, newState));

		if (newLevel == MAX_LEVEL) {
			world.scheduleBlockTick(pos, state.getBlock(), READY_TICK_DELAY);
		}

		return newState;
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(LEVEL) == MAX_LEVEL) {
			world.setBlockState(pos, state.cycle(LEVEL), Block.NOTIFY_ALL);
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
		int level = state.get(LEVEL);

		if (level == FULL_LEVEL) {
			return new FullComposterInventory(state, world, pos, new ItemStack(Items.BONE_MEAL));
		}

		return level < MAX_LEVEL
				? new ComposterInventory(state, world, pos)
				: new DummyInventory();
	}

	/** Инвентарь компостера, принимающий органические предметы сверху через хоппер. */
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
			return !dirty && dir == Direction.UP
					&& ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem());
		}

		@Override
		public boolean canExtract(int slot, ItemStack stack, Direction dir) {
			return false;
		}

		@Override
		public void markDirty() {
			ItemStack inserted = getStack(0);

			if (inserted.isEmpty()) {
				return;
			}

			dirty = true;
			BlockState newState = ComposterBlock.addToComposter(null, state, world, pos, inserted);
			world.syncWorldEvent(1500, pos, newState != state ? 1 : 0);
			removeStack(0);
		}
	}

	/** Заглушка-инвентарь для компостера на уровне 7 (ожидает готовности). */
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

	/** Инвентарь готового компостера (уровень 8), выдающий костную муку снизу через хоппер. */
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
			return !dirty && dir == Direction.DOWN && stack.isOf(Items.BONE_MEAL);
		}

		@Override
		public void markDirty() {
			ComposterBlock.emptyComposter(null, state, world, pos);
			dirty = true;
		}
	}
}
