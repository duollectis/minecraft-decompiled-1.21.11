package net.minecraft.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.fabricmc.fabric.api.block.v1.FabricBlock;
import net.minecraft.SharedConstants;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.collection.IdList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Базовый класс для всех блоков в игре. Управляет состояниями блока ({@link BlockState}),
 * дропом предметов, взаимодействием с сущностями и рендером граней.
 *
 * <p>Каждый блок регистрируется в {@link net.minecraft.registry.Registries#BLOCK} и может
 * иметь набор {@link net.minecraft.state.property.Property}, определяющих его состояния.
 * Флаги обновления (NOTIFY_*, FORCE_STATE и т.д.) управляют поведением при смене состояния.</p>
 */
public class Block extends AbstractBlock implements ItemConvertible, FabricBlock {

	public static final MapCodec<Block> CODEC = createCodec(Block::new);
	private static final Logger LOGGER = LogUtils.getLogger();
	private final RegistryEntry.Reference<Block> registryEntry = Registries.BLOCK.createEntry(this);
	public static final IdList<BlockState> STATE_IDS = new IdList<>();
	private static final LoadingCache<VoxelShape, Boolean> FULL_CUBE_SHAPE_CACHE = CacheBuilder
			.newBuilder()
			.maximumSize(512L)
			.weakKeys()
			.build(new CacheLoader<VoxelShape, Boolean>() {
				public Boolean load(
						VoxelShape voxelShape
				) {
					return !VoxelShapes.matchesAnywhere(
							VoxelShapes.fullCube(),
							voxelShape,
							BooleanBiFunction.NOT_SAME
					);
				}
			});
	public static final int NOTIFY_NEIGHBORS = 1;
	public static final int NOTIFY_LISTENERS = 2;
	public static final int NO_REDRAW = 4;
	public static final int REDRAW_ON_MAIN_THREAD = 8;
	public static final int FORCE_STATE = 16;
	public static final int SKIP_DROPS = 32;
	public static final int MOVED = 64;
	public static final int SKIP_REDSTONE_WIRE_STATE_REPLACEMENT = 128;
	public static final int SKIP_BLOCK_ENTITY_REPLACED_CALLBACK = 256;
	public static final int SKIP_BLOCK_ADDED_CALLBACK = 512;
	@Block.SetBlockStateFlag
	public static final int SKIP_REDRAW_AND_BLOCK_ENTITY_REPLACED_CALLBACK = 260;
	@Block.SetBlockStateFlag
	public static final int NOTIFY_ALL = 3;
	@Block.SetBlockStateFlag
	public static final int NOTIFY_ALL_AND_REDRAW = 11;
	@Block.SetBlockStateFlag
	public static final int FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS = 816;
	public static final float INDESTRUCTIBLE_HARDNESS = -1.0F;
	public static final float INSTANT_BREAK_HARDNESS = 0.0F;
	protected final StateManager<Block, BlockState> stateManager;
	private BlockState defaultState;
	private @Nullable Item cachedItem;
	private static final int FACE_CULL_MAP_SIZE = 256;
	private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.VoxelShapePair>>
			FACE_CULL_MAP =
			ThreadLocal.withInitial(() -> {
				// Переопределяем rehash чтобы запретить авторасширение кэша — размер фиксирован
				Object2ByteLinkedOpenHashMap<Block.VoxelShapePair> map =
						new Object2ByteLinkedOpenHashMap<Block.VoxelShapePair>(FACE_CULL_MAP_SIZE, 0.25F) {
							@Override
							protected void rehash(int newN) {
							}
						};
				map.defaultReturnValue((byte) 127);
				return map;
			});

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	public static int getRawIdFromState(@Nullable BlockState state) {
		if (state == null) {
			return 0;
		}

		int rawId = STATE_IDS.getRawId(state);
		return rawId == -1 ? 0 : rawId;
	}

	public static BlockState getStateFromRawId(int stateId) {
		BlockState blockState = STATE_IDS.get(stateId);
		return blockState == null ? Blocks.AIR.getDefaultState() : blockState;
	}

	public static Block getBlockFromItem(@Nullable Item item) {
		return item instanceof BlockItem ? ((BlockItem) item).getBlock() : Blocks.AIR;
	}

	/**
	 * Перемещает сущности вверх перед сменой блока, если новый блок занимает больше пространства.
	 * Используется при установке блоков, чтобы не допустить застревания сущностей внутри.
	 */
	public static BlockState pushEntitiesUpBeforeBlockChange(
			BlockState from,
			BlockState to,
			WorldAccess world,
			BlockPos pos
	) {
		VoxelShape newCollision = VoxelShapes
				.combine(
						from.getCollisionShape(world, pos),
						to.getCollisionShape(world, pos),
						BooleanBiFunction.ONLY_SECOND
				)
				.offset(pos);

		if (newCollision.isEmpty()) {
			return to;
		}

		for (Entity entity : world.getOtherEntities(null, newCollision.getBoundingBox())) {
			double pushOffset = VoxelShapes.calculateMaxOffset(
					Direction.Axis.Y,
					entity.getBoundingBox().offset(0.0, 1.0, 0.0),
					List.of(newCollision),
					-1.0
			);
			entity.requestTeleportOffset(0.0, 1.0 + pushOffset, 0.0);
		}

		return to;
	}

	public static VoxelShape createCuboidShape(
			double minX,
			double minY,
			double minZ,
			double maxX,
			double maxY,
			double maxZ
	) {
		return VoxelShapes.cuboid(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
	}

	/**
	 * Создаёт shape array.
	 *
	 * @param size size
	 * @param indexToShape index to shape
	 *
	 * @return VoxelShape[] — результат операции
	 */
	public static VoxelShape[] createShapeArray(int size, IntFunction<VoxelShape> indexToShape) {
		return IntStream.rangeClosed(0, size).mapToObj(indexToShape).toArray(VoxelShape[]::new);
	}

	/**
	 * Создаёт cube shape.
	 *
	 * @param size size
	 *
	 * @return VoxelShape — результат операции
	 */
	public static VoxelShape createCubeShape(double size) {
		return createCuboidShape(size, size, size);
	}

	/**
	 * Создаёт cuboid shape.
	 *
	 * @param sizeX size x
	 * @param sizeY size y
	 * @param sizeZ size z
	 *
	 * @return VoxelShape — результат операции
	 */
	public static VoxelShape createCuboidShape(double sizeX, double sizeY, double sizeZ) {
		double d = sizeY / 2.0;
		return createColumnShape(sizeX, sizeZ, 8.0 - d, 8.0 + d);
	}

	/**
	 * Создаёт column shape.
	 *
	 * @param sizeXz size xz
	 * @param minY min y
	 * @param maxY max y
	 *
	 * @return VoxelShape — результат операции
	 */
	public static VoxelShape createColumnShape(double sizeXz, double minY, double maxY) {
		return createColumnShape(sizeXz, sizeXz, minY, maxY);
	}

	/**
	 * Создаёт column shape.
	 *
	 * @param sizeX size x
	 * @param sizeZ size z
	 * @param minY min y
	 * @param maxY max y
	 *
	 * @return VoxelShape — результат операции
	 */
	public static VoxelShape createColumnShape(double sizeX, double sizeZ, double minY, double maxY) {
		double d = sizeX / 2.0;
		double e = sizeZ / 2.0;
		return createCuboidShape(8.0 - d, minY, 8.0 - e, 8.0 + d, maxY, 8.0 + e);
	}

	/**
	 * Создаёт cuboid z shape.
	 *
	 * @param sizeXy size xy
	 * @param minZ min z
	 * @param maxZ max z
	 *
	 * @return VoxelShape — результат операции
	 */
	public static VoxelShape createCuboidZShape(double sizeXy, double minZ, double maxZ) {
		return createCuboidZShape(sizeXy, sizeXy, minZ, maxZ);
	}

	/**
	 * Создаёт cuboid z shape.
	 *
	 * @param sizeX size x
	 * @param sizeY size y
	 * @param minZ min z
	 * @param maxZ max z
	 *
	 * @return VoxelShape — результат операции
	 */
	public static VoxelShape createCuboidZShape(double sizeX, double sizeY, double minZ, double maxZ) {
		double d = sizeY / 2.0;
		return createCuboidZShape(sizeX, 8.0 - d, 8.0 + d, minZ, maxZ);
	}

	/**
	 * Создаёт cuboid z shape.
	 *
	 * @param sizeX size x
	 * @param minY min y
	 * @param maxY max y
	 * @param minZ min z
	 * @param maxZ max z
	 *
	 * @return VoxelShape — результат операции
	 */
	public static VoxelShape createCuboidZShape(double sizeX, double minY, double maxY, double minZ, double maxZ) {
		double d = sizeX / 2.0;
		return createCuboidShape(8.0 - d, minY, minZ, 8.0 + d, maxY, maxZ);
	}

	/**
	 * Post process state.
	 *
	 * @param state state
	 * @param world world
	 * @param pos pos
	 *
	 * @return BlockState — результат операции
	 */
	public static BlockState postProcessState(BlockState state, WorldAccess world, BlockPos pos) {
		BlockState blockState = state;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (Direction direction : DIRECTIONS) {
			mutable.set(pos, direction);
			blockState =
					blockState.getStateForNeighborUpdate(
							world,
							world,
							pos,
							direction,
							mutable,
							world.getBlockState(mutable),
							world.getRandom()
					);
		}

		return blockState;
	}

	public static void replace(
			BlockState state,
			BlockState newState,
			WorldAccess world,
			BlockPos pos,
			@Block.SetBlockStateFlag int flags
	) {
		replace(state, newState, world, pos, flags, SKIP_BLOCK_ADDED_CALLBACK);
	}

	public static void replace(
			BlockState state,
			BlockState newState,
			WorldAccess world,
			BlockPos pos,
			@Block.SetBlockStateFlag int flags,
			int maxUpdateDepth
	) {
		if (newState != state) {
			if (newState.isAir()) {
				if (!world.isClient()) {
					world.breakBlock(pos, (flags & SKIP_DROPS) == 0, null, maxUpdateDepth);
				}
			}
			else {
				world.setBlockState(pos, newState, flags & ~SKIP_DROPS, maxUpdateDepth);
			}
		}
	}

	public Block(AbstractBlock.Settings settings) {
		super(settings);
		StateManager.Builder<Block, BlockState> builder = new StateManager.Builder<>(this);
		this.appendProperties(builder);
		this.stateManager = builder.build(Block::getDefaultState, BlockState::new);
		this.setDefaultState(this.stateManager.getDefaultState());
		if (SharedConstants.isDevelopment) {
			String string = this.getClass().getSimpleName();
			if (!string.endsWith("Block")) {
				LOGGER.error("Block classes should end with Block and {} doesn't.", string);
			}
		}
	}

	/**
	 * Проверяет возможность not connect.
	 *
	 * @param state state
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public static boolean cannotConnect(BlockState state) {
		return state.getBlock() instanceof LeavesBlock
				|| state.isOf(Blocks.BARRIER)
				|| state.isOf(Blocks.CARVED_PUMPKIN)
				|| state.isOf(Blocks.JACK_O_LANTERN)
				|| state.isOf(Blocks.MELON)
				|| state.isOf(Blocks.PUMPKIN)
				|| state.isIn(BlockTags.SHULKER_BOXES);
	}

	protected static boolean generateBlockInteractLoot(
			ServerWorld world,
			RegistryKey<LootTable> lootTable,
			BlockState state,
			@Nullable BlockEntity blockEntity,
			@Nullable ItemStack tool,
			@Nullable Entity interactingEntity,
			BiConsumer<ServerWorld, ItemStack> lootConsumer
	) {
		return generateLoot(
				world,
				lootTable,
				context -> context.add(LootContextParameters.BLOCK_STATE, state)
				                  .addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity)
				                  .addOptional(LootContextParameters.INTERACTING_ENTITY, interactingEntity)
				                  .addOptional(LootContextParameters.TOOL, tool)
				                  .build(LootContextTypes.BLOCK_INTERACT),
				lootConsumer
		);
	}

	protected static boolean generateLoot(
			ServerWorld world,
			RegistryKey<LootTable> lootTable,
			Function<LootWorldContext.Builder, LootWorldContext> contextFactory,
			BiConsumer<ServerWorld, ItemStack> lootConsumer
	) {
		LootTable lootTable2 = world.getServer().getReloadableRegistries().getLootTable(lootTable);
		LootWorldContext lootWorldContext = contextFactory.apply(new LootWorldContext.Builder(world));
		List<ItemStack> list = lootTable2.generateLoot(lootWorldContext);
		if (list.isEmpty()) {
			return false;
		}

		list.forEach(stack -> lootConsumer.accept(world, stack));
		return true;
	}

	/**
	 * Определяет, следует ли draw side.
	 *
	 * @param state state
	 * @param otherState other state
	 * @param side side
	 *
	 * @return boolean — результат операции
	 */
	/**
	 * Определяет, нужно ли рендерить грань блока со стороны {@code side}.
	 * Использует LRU-кэш пар форм для ускорения повторных проверок.
	 */
	public static boolean shouldDrawSide(BlockState state, BlockState otherState, Direction side) {
		VoxelShape neighborCullFace = otherState.getCullingFace(side.getOpposite());

		if (neighborCullFace == VoxelShapes.fullCube()) {
			return false;
		}

		if (state.isSideInvisible(otherState, side)) {
			return false;
		}

		if (neighborCullFace == VoxelShapes.empty()) {
			return true;
		}

		VoxelShape ownCullFace = state.getCullingFace(side);

		if (ownCullFace == VoxelShapes.empty()) {
			return true;
		}

		Block.VoxelShapePair shapePair = new Block.VoxelShapePair(ownCullFace, neighborCullFace);
		Object2ByteLinkedOpenHashMap<Block.VoxelShapePair> cullCache = FACE_CULL_MAP.get();
		byte cachedResult = cullCache.getAndMoveToFirst(shapePair);

		if (cachedResult != 127) {
			return cachedResult != 0;
		}

		boolean shouldDraw = VoxelShapes.matchesAnywhere(ownCullFace, neighborCullFace, BooleanBiFunction.ONLY_FIRST);

		if (cullCache.size() == FACE_CULL_MAP_SIZE) {
			cullCache.removeLastByte();
		}

		cullCache.putAndMoveToFirst(shapePair, (byte) (shouldDraw ? 1 : 0));
		return shouldDraw;
	}

	public static boolean hasTopRim(BlockView world, BlockPos pos) {
		return world.getBlockState(pos).isSideSolid(world, pos, Direction.UP, SideShapeType.RIGID);
	}

	/**
	 * Side covers small square.
	 *
	 * @param world world
	 * @param pos pos
	 * @param side side
	 *
	 * @return boolean — результат операции
	 */
	public static boolean sideCoversSmallSquare(WorldView world, BlockPos pos, Direction side) {
		BlockState blockState = world.getBlockState(pos);

		if (side == Direction.DOWN && blockState.isIn(BlockTags.UNSTABLE_BOTTOM_CENTER)) {
			return false;
		}

		return blockState.isSideSolid(world, pos, side, SideShapeType.CENTER);
	}

	public static boolean isFaceFullSquare(VoxelShape shape, Direction side) {
		VoxelShape voxelShape = shape.getFace(side);
		return isShapeFullCube(voxelShape);
	}

	public static boolean isShapeFullCube(VoxelShape shape) {
		return FULL_CUBE_SHAPE_CACHE.getUnchecked(shape);
	}

	/**
	 * Random display tick.
	 *
	 * @param state state
	 * @param world world
	 * @param pos pos
	 * @param random random
	 */
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
	}

	/**
	 * Обрабатывает событие broken.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 */
	public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
	}

	public static List<ItemStack> getDroppedStacks(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			@Nullable BlockEntity blockEntity
	) {
		LootWorldContext.Builder builder = new LootWorldContext.Builder(world)
				.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
				.add(LootContextParameters.TOOL, ItemStack.EMPTY)
				.addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
		return state.getDroppedStacks(builder);
	}

	public static List<ItemStack> getDroppedStacks(
			BlockState state,
			ServerWorld world,
			BlockPos pos,
			@Nullable BlockEntity blockEntity,
			@Nullable Entity entity,
			ItemStack stack
	) {
		LootWorldContext.Builder builder = new LootWorldContext.Builder(world)
				.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
				.add(LootContextParameters.TOOL, stack)
				.addOptional(LootContextParameters.THIS_ENTITY, entity)
				.addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity);
		return state.getDroppedStacks(builder);
	}

	/**
	 * Бросает stacks.
	 *
	 * @param state state
	 * @param world world
	 * @param pos pos
	 */
	public static void dropStacks(BlockState state, World world, BlockPos pos) {
		if (world instanceof ServerWorld serverWorld) {
			getDroppedStacks(state, serverWorld, pos, null).forEach(stack -> dropStack(world, pos, stack));
			state.onStacksDropped(serverWorld, pos, ItemStack.EMPTY, true);
		}
	}

	public static void dropStacks(
			BlockState state,
			WorldAccess world,
			BlockPos pos,
			@Nullable BlockEntity blockEntity
	) {
		if (world instanceof ServerWorld serverWorld) {
			getDroppedStacks(state, serverWorld, pos, blockEntity)
					.forEach(stack -> dropStack(serverWorld, pos, stack));
			state.onStacksDropped(serverWorld, pos, ItemStack.EMPTY, true);
		}
	}

	public static void dropStacks(
			BlockState state,
			World world,
			BlockPos pos,
			@Nullable BlockEntity blockEntity,
			@Nullable Entity entity,
			ItemStack tool
	) {
		if (world instanceof ServerWorld serverWorld) {
			getDroppedStacks(state, serverWorld, pos, blockEntity, entity, tool)
					.forEach(stack -> dropStack(world, pos, stack));
			state.onStacksDropped(serverWorld, pos, tool, true);
		}
	}

	/**
	 * Бросает stack.
	 *
	 * @param world world
	 * @param pos pos
	 * @param stack stack
	 */
	public static void dropStack(World world, BlockPos pos, ItemStack stack) {
		double halfHeight = EntityType.ITEM.getHeight() / 2.0;
		double spawnX = pos.getX() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25);
		double spawnY = pos.getY() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25) - halfHeight;
		double spawnZ = pos.getZ() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25);
		dropStack(world, () -> new ItemEntity(world, spawnX, spawnY, spawnZ, stack), stack);
	}

	/**
	 * Бросает stack.
	 *
	 * @param world world
	 * @param pos pos
	 * @param direction direction
	 * @param stack stack
	 */
	public static void dropStack(World world, BlockPos pos, Direction direction, ItemStack stack) {
		int offsetX = direction.getOffsetX();
		int offsetY = direction.getOffsetY();
		int offsetZ = direction.getOffsetZ();
		double halfWidth = EntityType.ITEM.getWidth() / 2.0;
		double halfHeight = EntityType.ITEM.getHeight() / 2.0;
		double spawnX = pos.getX() + 0.5 + (offsetX == 0
				? MathHelper.nextDouble(world.random, -0.25, 0.25)
				: offsetX * (0.5 + halfWidth));
		double spawnY = pos.getY() + 0.5 + (offsetY == 0
				? MathHelper.nextDouble(world.random, -0.25, 0.25)
				: offsetY * (0.5 + halfHeight)) - halfHeight;
		double spawnZ = pos.getZ() + 0.5 + (offsetZ == 0
				? MathHelper.nextDouble(world.random, -0.25, 0.25)
				: offsetZ * (0.5 + halfWidth));
		double velocityX = offsetX == 0 ? MathHelper.nextDouble(world.random, -0.1, 0.1) : offsetX * 0.1;
		double velocityY = offsetY == 0 ? MathHelper.nextDouble(world.random, 0.0, 0.1) : offsetY * 0.1 + 0.1;
		double velocityZ = offsetZ == 0 ? MathHelper.nextDouble(world.random, -0.1, 0.1) : offsetZ * 0.1;
		dropStack(world, () -> new ItemEntity(world, spawnX, spawnY, spawnZ, stack, velocityX, velocityY, velocityZ), stack);
	}

	private static void dropStack(World world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack) {
		if (world instanceof ServerWorld serverWorld && !stack.isEmpty() && serverWorld
				.getGameRules()
				.getValue(GameRules.DO_TILE_DROPS)) {
			ItemEntity itemEntity = itemEntitySupplier.get();
			itemEntity.setToDefaultPickupDelay();
			world.spawnEntity(itemEntity);
		}
	}

	/**
	 * Бросает experience.
	 *
	 * @param world world
	 * @param pos pos
	 * @param size size
	 */
	protected void dropExperience(ServerWorld world, BlockPos pos, int size) {
		if (world.getGameRules().getValue(GameRules.DO_TILE_DROPS)) {
			ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), size);
		}
	}

	public float getBlastResistance() {
		return this.resistance;
	}

	/**
	 * Обрабатывает событие destroyed by explosion.
	 *
	 * @param world world
	 * @param pos pos
	 * @param explosion explosion
	 */
	public void onDestroyedByExplosion(ServerWorld world, BlockPos pos, Explosion explosion) {
	}

	/**
	 * Обрабатывает событие stepped on.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 * @param entity entity
	 */
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
	}

	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState();
	}

	public void afterBreak(
			World world,
			PlayerEntity player,
			BlockPos pos,
			BlockState state,
			@Nullable BlockEntity blockEntity,
			ItemStack tool
	) {
		player.incrementStat(Stats.MINED.getOrCreateStat(this));
		player.addExhaustion(0.005F);
		dropStacks(state, world, pos, blockEntity, player, tool);
	}

	public void onPlaced(
			World world,
			BlockPos pos,
			BlockState state,
			@Nullable LivingEntity placer,
			ItemStack itemStack
	) {
	}

	/**
	 * Проверяет возможность mob spawn inside.
	 *
	 * @param state state
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canMobSpawnInside(BlockState state) {
		return !state.isSolid() && !state.isLiquid();
	}

	public MutableText getName() {
		return Text.translatable(this.getTranslationKey());
	}

	/**
	 * Обрабатывает событие landed upon.
	 *
	 * @param world world
	 * @param state state
	 * @param pos pos
	 * @param entity entity
	 * @param fallDistance fall distance
	 */
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		entity.handleFallDamage(fallDistance, 1.0F, entity.getDamageSources().fall());
	}

	/**
	 * Обрабатывает событие entity land.
	 *
	 * @param world world
	 * @param entity entity
	 */
	public void onEntityLand(BlockView world, Entity entity) {
		entity.setVelocity(entity.getVelocity().multiply(1.0, 0.0, 1.0));
	}

	public float getSlipperiness() {
		return this.slipperiness;
	}

	public float getVelocityMultiplier() {
		return this.velocityMultiplier;
	}

	public float getJumpVelocityMultiplier() {
		return this.jumpVelocityMultiplier;
	}

	/**
	 * Создаёт (спавнит) break particles.
	 *
	 * @param world world
	 * @param player player
	 * @param pos pos
	 * @param state state
	 */
	protected void spawnBreakParticles(World world, PlayerEntity player, BlockPos pos, BlockState state) {
		world.syncWorldEvent(player, 2001, pos, getRawIdFromState(state));
	}

	/**
	 * Обрабатывает событие break.
	 *
	 * @param world world
	 * @param pos pos
	 * @param state state
	 * @param player player
	 *
	 * @return BlockState — результат операции
	 */
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		this.spawnBreakParticles(world, player, pos, state);
		if (state.isIn(BlockTags.GUARDED_BY_PIGLINS) && world instanceof ServerWorld serverWorld) {
			PiglinBrain.onGuardedBlockInteracted(serverWorld, player, false);
		}

		world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(player, state));
		return state;
	}

	/**
	 * Precipitation tick.
	 *
	 * @param state state
	 * @param world world
	 * @param pos pos
	 * @param precipitation precipitation
	 */
	public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
	}

	/**
	 * Определяет, следует ли drop items on explosion.
	 *
	 * @param explosion explosion
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldDropItemsOnExplosion(Explosion explosion) {
		return true;
	}

	/**
	 * Append properties.
	 *
	 * @param builder builder
	 */
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
	}

	public StateManager<Block, BlockState> getStateManager() {
		return this.stateManager;
	}

	protected final void setDefaultState(BlockState state) {
		this.defaultState = state;
	}

	public final BlockState getDefaultState() {
		return this.defaultState;
	}

	public final BlockState getStateWithProperties(BlockState state) {
		BlockState blockState = this.getDefaultState();

		for (Property<?> property : state.getBlock().getStateManager().getProperties()) {
			if (blockState.contains(property)) {
				blockState = copyProperty(state, blockState, property);
			}
		}

		return blockState;
	}

	private static <T extends Comparable<T>> BlockState copyProperty(
			BlockState source,
			BlockState target,
			Property<T> property
	) {
		return target.with(property, source.get(property));
	}

	@Override
	public Item asItem() {
		if (this.cachedItem == null) {
			this.cachedItem = Item.fromBlock(this);
		}

		return this.cachedItem;
	}

	public boolean hasDynamicBounds() {
		return this.dynamicBounds;
	}

	@Override
	public String toString() {
		return "Block{" + Registries.BLOCK.getEntry(this).getIdAsString() + "}";
	}

	@Override
	protected Block asBlock() {
		return this;
	}

	/**
	 * Создаёт shape function.
	 *
	 * @param stateToShape state to shape
	 *
	 * @return Function — результат операции
	 */
	protected Function<BlockState, VoxelShape> createShapeFunction(Function<BlockState, VoxelShape> stateToShape) {
		return this.stateManager
				.getStates()
				.stream()
				.collect(ImmutableMap.toImmutableMap(Function.identity(), stateToShape))::get;
	}

	protected Function<BlockState, VoxelShape> createShapeFunction(
			Function<BlockState, VoxelShape> stateToShape,
			Property<?>... properties
	) {
		Map<? extends Property<?>, Object> map = Arrays.stream(properties)
		                                               .collect(Collectors.toMap(
				                                               property -> property,
				                                               property -> property.getValues().getFirst()
		                                               ));
		ImmutableMap<BlockState, VoxelShape> immutableMap = this.stateManager
				.getStates()
				.stream()
				.filter(state -> map
						.entrySet()
						.stream()
						.allMatch(entry -> state.get((Property<?>) entry.getKey()) == entry.getValue()))
				.collect(ImmutableMap.toImmutableMap(Function.identity(), stateToShape));
		return state -> {
			for (Entry<? extends Property<?>, Object> entry : map.entrySet()) {
				state = applyValueToState(state, (Property<?>) entry.getKey(), entry.getValue());
			}

			return (VoxelShape) immutableMap.get(state);
		};
	}

	@SuppressWarnings("unchecked")
	private static <S extends State<?, S>, T extends Comparable<T>> S applyValueToState(
			S state,
			Property<T> property,
			Object value
	) {
		return state.with(property, (T) value);
	}

	@Deprecated
	public RegistryEntry.Reference<Block> getRegistryEntry() {
		return this.registryEntry;
	}

	/**
	 * Бросает experience when mined.
	 *
	 * @param world world
	 * @param pos pos
	 * @param tool tool
	 * @param experience experience
	 */
	protected void dropExperienceWhenMined(ServerWorld world, BlockPos pos, ItemStack tool, IntProvider experience) {
		int amount = EnchantmentHelper.getBlockExperience(world, tool, experience.get(world.getRandom()));

		if (amount > 0) {
			dropExperience(world, pos, amount);
		}
	}

	/**
	 * Маркерная аннотация для параметров и полей, принимающих битовые флаги смены состояния блока
	 * (комбинации констант {@code NOTIFY_*}, {@code FORCE_STATE}, {@code SKIP_DROPS} и т.д.).
	 */
	@Retention(RetentionPolicy.CLASS)
	@Target(
			{
					ElementType.FIELD,
					ElementType.PARAMETER,
					ElementType.LOCAL_VARIABLE,
					ElementType.METHOD,
					ElementType.TYPE_USE
			}
	)
	public @interface SetBlockStateFlag {
	}

	/**
	 * Пара форм для кэша отсечения граней. Сравнение по идентичности объектов (==),
	 * так как VoxelShape — синглтоны, и equals по значению здесь избыточен.
	 */
	record VoxelShapePair(VoxelShape first, VoxelShape second) {

		@Override
		public boolean equals(Object o) {
			return o instanceof Block.VoxelShapePair other && first == other.first && second == other.second;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(first) * 31 + System.identityHashCode(second);
		}
	}
}
