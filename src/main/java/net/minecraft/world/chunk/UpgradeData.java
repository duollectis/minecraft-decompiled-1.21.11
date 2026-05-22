package net.minecraft.world.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.*;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.Tick;
import org.slf4j.Logger;

import java.util.*;

/**
 * Данные обновления чанка при загрузке из старых версий мира.
 * Хранит список блоков в центре чанка и по его сторонам, которые нужно
 * пересчитать через {@link Logic#getUpdatedState} после полной загрузки соседних чанков.
 */
public class UpgradeData {

	// Флаги обновления блока: NOTIFY_LISTENERS | NO_RERENDER (см. Block.NOTIFY_*)
	private static final int BLOCK_UPDATE_FLAGS = 18;

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final UpgradeData NO_UPGRADE_DATA = new UpgradeData(EmptyBlockView.INSTANCE);
	private static final String INDICES_KEY = "Indices";
	private static final EightWayDirection[] EIGHT_WAYS = EightWayDirection.values();
	private static final Codec<List<Tick<Block>>> BLOCK_TICKS_CODEC =
			Tick.createCodec(Registries.BLOCK.getCodec().orElse(Blocks.AIR)).listOf();
	private static final Codec<List<Tick<Fluid>>> FLUID_TICKS_CODEC =
			Tick.createCodec(Registries.FLUID.getCodec().orElse(Fluids.EMPTY)).listOf();

	private final EnumSet<EightWayDirection> sidesToUpgrade = EnumSet.noneOf(EightWayDirection.class);
	private final List<Tick<Block>> blockTicks = Lists.newArrayList();
	private final List<Tick<Fluid>> fluidTicks = Lists.newArrayList();
	private final int[][] centerIndicesToUpgrade;

	static final Map<Block, UpgradeData.Logic> BLOCK_TO_LOGIC = new IdentityHashMap<>();
	static final Set<UpgradeData.Logic> CALLBACK_LOGICS = Sets.newHashSet();

	private UpgradeData(HeightLimitView world) {
		centerIndicesToUpgrade = new int[world.countVerticalSections()][];
	}

	public UpgradeData(NbtCompound nbt, HeightLimitView world) {
		this(world);
		nbt.getCompound(INDICES_KEY).ifPresent(indices -> {
			for (int i = 0; i < centerIndicesToUpgrade.length; i++) {
				centerIndicesToUpgrade[i] = indices.getIntArray(String.valueOf(i)).orElse(null);
			}
		});

		int sidesBitmask = nbt.getInt("Sides", 0);
		for (EightWayDirection direction : EightWayDirection.values()) {
			if ((sidesBitmask & 1 << direction.ordinal()) != 0) {
				sidesToUpgrade.add(direction);
			}
		}

		nbt.<List<Tick<Block>>>get("neighbor_block_ticks", BLOCK_TICKS_CODEC).ifPresent(blockTicks::addAll);
		nbt.<List<Tick<Fluid>>>get("neighbor_fluid_ticks", FLUID_TICKS_CODEC).ifPresent(fluidTicks::addAll);
	}

	private UpgradeData(UpgradeData data) {
		sidesToUpgrade.addAll(data.sidesToUpgrade);
		blockTicks.addAll(data.blockTicks);
		fluidTicks.addAll(data.fluidTicks);
		centerIndicesToUpgrade = new int[data.centerIndicesToUpgrade.length][];

		for (int i = 0; i < data.centerIndicesToUpgrade.length; i++) {
			int[] section = data.centerIndicesToUpgrade[i];
			centerIndicesToUpgrade[i] = section != null ? IntArrays.copy(section) : null;
		}
	}

	/**
	 * Применяет все накопленные обновления блоков к загруженному чанку:
	 * обновляет центральные блоки, граничные стороны и планирует тики блоков/жидкостей.
	 */
	public void upgrade(WorldChunk chunk) {
		upgradeCenter(chunk);

		for (EightWayDirection side : EIGHT_WAYS) {
			upgradeSide(chunk, side);
		}

		World world = chunk.getWorld();
		blockTicks.forEach(tick -> {
			Block block = tick.type() == Blocks.AIR ? world.getBlockState(tick.pos()).getBlock() : tick.type();
			world.scheduleBlockTick(tick.pos(), block, tick.delay(), tick.priority());
		});
		fluidTicks.forEach(tick -> {
			Fluid fluid = tick.type() == Fluids.EMPTY ? world.getFluidState(tick.pos()).getFluid() : tick.type();
			world.scheduleFluidTick(tick.pos(), fluid, tick.delay(), tick.priority());
		});
		CALLBACK_LOGICS.forEach(logic -> logic.postUpdate(world));
	}

	private static void upgradeSide(WorldChunk chunk, EightWayDirection side) {
		World world = chunk.getWorld();
		if (!chunk.getUpgradeData().sidesToUpgrade.remove(side)) {
			return;
		}

		Set<Direction> directions = side.getDirections();
		boolean hasEast = directions.contains(Direction.EAST);
		boolean hasWest = directions.contains(Direction.WEST);
		boolean hasSouth = directions.contains(Direction.SOUTH);
		boolean hasNorth = directions.contains(Direction.NORTH);
		boolean isSingleDirection = directions.size() == 1;

		ChunkPos chunkPos = chunk.getPos();
		int startX = chunkPos.getStartX() + (!isSingleDirection || !hasNorth && !hasSouth ? (hasWest ? 0 : 15) : 1);
		int endX = chunkPos.getStartX() + (!isSingleDirection || !hasNorth && !hasSouth ? (hasWest ? 0 : 15) : 14);
		int startZ = chunkPos.getStartZ() + (!isSingleDirection || !hasEast && !hasWest ? (hasNorth ? 0 : 15) : 1);
		int endZ = chunkPos.getStartZ() + (!isSingleDirection || !hasEast && !hasWest ? (hasNorth ? 0 : 15) : 14);

		Direction[] allDirections = Direction.values();
		BlockPos.Mutable neighborPos = new BlockPos.Mutable();

		for (BlockPos blockPos : BlockPos.iterate(startX, world.getBottomY(), startZ, endX, world.getTopYInclusive(), endZ)) {
			BlockState state = world.getBlockState(blockPos);

			for (Direction direction : allDirections) {
				neighborPos.set(blockPos, direction);
				state = applyAdjacentBlock(state, direction, world, blockPos, neighborPos);
			}

			Block.replace(world.getBlockState(blockPos), state, world, blockPos, BLOCK_UPDATE_FLAGS);
		}
	}

	private static BlockState applyAdjacentBlock(
			BlockState oldState,
			Direction dir,
			WorldAccess world,
			BlockPos currentPos,
			BlockPos otherPos
	) {
		return BLOCK_TO_LOGIC.getOrDefault(oldState.getBlock(), UpgradeData.BuiltinLogic.DEFAULT)
				.getUpdatedState(oldState, dir, world.getBlockState(otherPos), world, currentPos, otherPos);
	}

	private void upgradeCenter(WorldChunk chunk) {
		BlockPos.Mutable blockPos = new BlockPos.Mutable();
		BlockPos.Mutable neighborPos = new BlockPos.Mutable();
		ChunkPos chunkPos = chunk.getPos();
		WorldAccess world = chunk.getWorld();

		for (int sectionIndex = 0; sectionIndex < centerIndicesToUpgrade.length; sectionIndex++) {
			ChunkSection section = chunk.getSection(sectionIndex);
			int[] indices = centerIndicesToUpgrade[sectionIndex];
			centerIndicesToUpgrade[sectionIndex] = null;

			if (indices == null || indices.length == 0) {
				continue;
			}

			Direction[] allDirections = Direction.values();
			PalettedContainer<BlockState> blockStates = section.getBlockStateContainer();
			int sectionY = chunk.sectionIndexToCoord(sectionIndex);
			int sectionBottomY = ChunkSectionPos.getBlockCoord(sectionY);

			for (int packedIndex : indices) {
				int localX = packedIndex & 15;
				int localY = packedIndex >> 8 & 15;
				int localZ = packedIndex >> 4 & 15;
				blockPos.set(chunkPos.getStartX() + localX, sectionBottomY + localY, chunkPos.getStartZ() + localZ);

				BlockState state = blockStates.get(packedIndex);

				for (Direction direction : allDirections) {
					neighborPos.set(blockPos, direction);
					if (ChunkSectionPos.getSectionCoord(blockPos.getX()) == chunkPos.x
							&& ChunkSectionPos.getSectionCoord(blockPos.getZ()) == chunkPos.z
					) {
						state = applyAdjacentBlock(state, direction, world, blockPos, neighborPos);
					}
				}

				Block.replace(blockStates.get(packedIndex), state, world, blockPos, BLOCK_UPDATE_FLAGS);
			}
		}

		for (int i = 0; i < centerIndicesToUpgrade.length; i++) {
			if (centerIndicesToUpgrade[i] != null) {
				LOGGER.warn(
						"Discarding update data for section {} for chunk ({} {})",
						world.sectionIndexToCoord(i),
						chunkPos.x,
						chunkPos.z
				);
			}

			centerIndicesToUpgrade[i] = null;
		}
	}

	public boolean isDone() {
		for (int[] section : centerIndicesToUpgrade) {
			if (section != null) {
				return false;
			}
		}

		return sidesToUpgrade.isEmpty();
	}

	public NbtCompound toNbt() {
		NbtCompound root = new NbtCompound();
		NbtCompound indicesNbt = new NbtCompound();

		for (int i = 0; i < centerIndicesToUpgrade.length; i++) {
			if (centerIndicesToUpgrade[i] != null && centerIndicesToUpgrade[i].length != 0) {
				indicesNbt.putIntArray(String.valueOf(i), centerIndicesToUpgrade[i]);
			}
		}

		if (!indicesNbt.isEmpty()) {
			root.put(INDICES_KEY, indicesNbt);
		}

		int sidesBitmask = 0;
		for (EightWayDirection direction : sidesToUpgrade) {
			sidesBitmask |= 1 << direction.ordinal();
		}

		root.putByte("Sides", (byte) sidesBitmask);

		if (!blockTicks.isEmpty()) {
			root.put("neighbor_block_ticks", BLOCK_TICKS_CODEC, blockTicks);
		}

		if (!fluidTicks.isEmpty()) {
			root.put("neighbor_fluid_ticks", FLUID_TICKS_CODEC, fluidTicks);
		}

		return root;
	}

	public UpgradeData copy() {
		return this == NO_UPGRADE_DATA ? NO_UPGRADE_DATA : new UpgradeData(this);
	}

	/**
	 * Встроенные стратегии обновления блоков при апгрейде чанка.
	 */
	static enum BuiltinLogic implements UpgradeData.Logic {
		BLACKLIST(
				Blocks.OBSERVER,
				Blocks.NETHER_PORTAL,
				Blocks.WHITE_CONCRETE_POWDER,
				Blocks.ORANGE_CONCRETE_POWDER,
				Blocks.MAGENTA_CONCRETE_POWDER,
				Blocks.LIGHT_BLUE_CONCRETE_POWDER,
				Blocks.YELLOW_CONCRETE_POWDER,
				Blocks.LIME_CONCRETE_POWDER,
				Blocks.PINK_CONCRETE_POWDER,
				Blocks.GRAY_CONCRETE_POWDER,
				Blocks.LIGHT_GRAY_CONCRETE_POWDER,
				Blocks.CYAN_CONCRETE_POWDER,
				Blocks.PURPLE_CONCRETE_POWDER,
				Blocks.BLUE_CONCRETE_POWDER,
				Blocks.BROWN_CONCRETE_POWDER,
				Blocks.GREEN_CONCRETE_POWDER,
				Blocks.RED_CONCRETE_POWDER,
				Blocks.BLACK_CONCRETE_POWDER,
				Blocks.ANVIL,
				Blocks.CHIPPED_ANVIL,
				Blocks.DAMAGED_ANVIL,
				Blocks.DRAGON_EGG,
				Blocks.GRAVEL,
				Blocks.SAND,
				Blocks.RED_SAND,
				Blocks.OAK_SIGN,
				Blocks.SPRUCE_SIGN,
				Blocks.BIRCH_SIGN,
				Blocks.ACACIA_SIGN,
				Blocks.CHERRY_SIGN,
				Blocks.JUNGLE_SIGN,
				Blocks.DARK_OAK_SIGN,
				Blocks.PALE_OAK_SIGN,
				Blocks.OAK_WALL_SIGN,
				Blocks.SPRUCE_WALL_SIGN,
				Blocks.BIRCH_WALL_SIGN,
				Blocks.ACACIA_WALL_SIGN,
				Blocks.JUNGLE_WALL_SIGN,
				Blocks.DARK_OAK_WALL_SIGN,
				Blocks.PALE_OAK_WALL_SIGN,
				Blocks.OAK_HANGING_SIGN,
				Blocks.SPRUCE_HANGING_SIGN,
				Blocks.BIRCH_HANGING_SIGN,
				Blocks.ACACIA_HANGING_SIGN,
				Blocks.JUNGLE_HANGING_SIGN,
				Blocks.DARK_OAK_HANGING_SIGN,
				Blocks.PALE_OAK_HANGING_SIGN,
				Blocks.OAK_WALL_HANGING_SIGN,
				Blocks.SPRUCE_WALL_HANGING_SIGN,
				Blocks.BIRCH_WALL_HANGING_SIGN,
				Blocks.ACACIA_WALL_HANGING_SIGN,
				Blocks.JUNGLE_WALL_HANGING_SIGN,
				Blocks.DARK_OAK_WALL_HANGING_SIGN,
				Blocks.PALE_OAK_WALL_HANGING_SIGN
		) {
			@Override
			public BlockState getUpdatedState(
					BlockState oldState,
					Direction direction,
					BlockState otherState,
					WorldAccess world,
					BlockPos currentPos,
					BlockPos otherPos
			) {
				return oldState;
			}
		},
		DEFAULT {
			@Override
			public BlockState getUpdatedState(
					BlockState oldState,
					Direction direction,
					BlockState otherState,
					WorldAccess world,
					BlockPos currentPos,
					BlockPos otherPos
			) {
				return oldState.getStateForNeighborUpdate(
						world,
						world,
						currentPos,
						direction,
						otherPos,
						world.getBlockState(otherPos),
						world.getRandom()
				);
			}
		},
		CHEST(Blocks.CHEST, Blocks.TRAPPED_CHEST) {
			@Override
			public BlockState getUpdatedState(
					BlockState oldState,
					Direction direction,
					BlockState otherState,
					WorldAccess world,
					BlockPos currentPos,
					BlockPos otherPos
			) {
				if (!otherState.isOf(oldState.getBlock())
						|| !direction.getAxis().isHorizontal()
						|| oldState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE
						|| otherState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE
				) {
					return oldState;
				}

				Direction facing = oldState.get(ChestBlock.FACING);
				if (direction.getAxis() == facing.getAxis() || facing != otherState.get(ChestBlock.FACING)) {
					return oldState;
				}

				ChestType chestType = direction == facing.rotateYClockwise() ? ChestType.LEFT : ChestType.RIGHT;
				world.setBlockState(otherPos, otherState.with(ChestBlock.CHEST_TYPE, chestType.getOpposite()), BLOCK_UPDATE_FLAGS);

				if (facing == Direction.NORTH || facing == Direction.EAST) {
					BlockEntity current = world.getBlockEntity(currentPos);
					BlockEntity other = world.getBlockEntity(otherPos);
					if (current instanceof ChestBlockEntity currentChest && other instanceof ChestBlockEntity otherChest) {
						ChestBlockEntity.copyInventory(currentChest, otherChest);
					}
				}

				return oldState.with(ChestBlock.CHEST_TYPE, chestType);
			}
		},
		LEAVES(
				true,
				Blocks.ACACIA_LEAVES,
				Blocks.CHERRY_LEAVES,
				Blocks.BIRCH_LEAVES,
				Blocks.PALE_OAK_LEAVES,
				Blocks.DARK_OAK_LEAVES,
				Blocks.JUNGLE_LEAVES,
				Blocks.OAK_LEAVES,
				Blocks.SPRUCE_LEAVES
		) {
			private final ThreadLocal<List<ObjectSet<BlockPos>>> distanceToPositions =
					ThreadLocal.withInitial(() -> Lists.newArrayListWithCapacity(7));

			@Override
			public BlockState getUpdatedState(
					BlockState oldState,
					Direction direction,
					BlockState otherState,
					WorldAccess world,
					BlockPos currentPos,
					BlockPos otherPos
			) {
				BlockState updated = oldState.getStateForNeighborUpdate(
						world, world, currentPos, direction, otherPos, world.getBlockState(otherPos), world.getRandom()
				);

				if (oldState == updated) {
					return oldState;
				}

				int distance = updated.get(Properties.DISTANCE_1_7);
				List<ObjectSet<BlockPos>> buckets = distanceToPositions.get();
				if (buckets.isEmpty()) {
					for (int i = 0; i < 7; i++) {
						buckets.add(new ObjectOpenHashSet<>());
					}
				}

				buckets.get(distance).add(currentPos.toImmutable());
				return oldState;
			}

			@Override
			public void postUpdate(WorldAccess world) {
				BlockPos.Mutable mutable = new BlockPos.Mutable();
				List<ObjectSet<BlockPos>> buckets = distanceToPositions.get();

				for (int distance = 2; distance < buckets.size(); distance++) {
					int prevDistance = distance - 1;
					ObjectSet<BlockPos> prevBucket = buckets.get(prevDistance);
					ObjectSet<BlockPos> nextBucket = buckets.get(distance);

					for (BlockPos pos : prevBucket) {
						BlockState state = world.getBlockState(pos);
						if (state.get(Properties.DISTANCE_1_7) >= prevDistance) {
							world.setBlockState(pos, state.with(Properties.DISTANCE_1_7, prevDistance), BLOCK_UPDATE_FLAGS);

							if (distance != 7) {
								for (Direction dir : DIRECTIONS) {
									mutable.set(pos, dir);
									BlockState neighborState = world.getBlockState(mutable);
									if (neighborState.contains(Properties.DISTANCE_1_7)
											&& state.get(Properties.DISTANCE_1_7) > distance
									) {
										nextBucket.add(mutable.toImmutable());
									}
								}
							}
						}
					}
				}

				buckets.clear();
			}
		},
		STEM_BLOCK(Blocks.MELON_STEM, Blocks.PUMPKIN_STEM) {
			@Override
			public BlockState getUpdatedState(
					BlockState oldState,
					Direction direction,
					BlockState otherState,
					WorldAccess world,
					BlockPos currentPos,
					BlockPos otherPos
			) {
				if (oldState.get(StemBlock.AGE) != 7) {
					return oldState;
				}

				Block fruit = oldState.isOf(Blocks.PUMPKIN_STEM) ? Blocks.PUMPKIN : Blocks.MELON;
				if (!otherState.isOf(fruit)) {
					return oldState;
				}

				Block attachedStem = oldState.isOf(Blocks.PUMPKIN_STEM)
						? Blocks.ATTACHED_PUMPKIN_STEM
						: Blocks.ATTACHED_MELON_STEM;
				return attachedStem.getDefaultState().with(HorizontalFacingBlock.FACING, direction);
			}
		};

		public static final Direction[] DIRECTIONS = Direction.values();

		BuiltinLogic(final Block... blocks) {
			this(false, blocks);
		}

		BuiltinLogic(final boolean addCallback, final Block... blocks) {
			for (Block block : blocks) {
				UpgradeData.BLOCK_TO_LOGIC.put(block, this);
			}

			if (addCallback) {
				UpgradeData.CALLBACK_LOGICS.add(this);
			}
		}
	}

	public interface Logic {

		BlockState getUpdatedState(
				BlockState oldState,
				Direction direction,
				BlockState otherState,
				WorldAccess world,
				BlockPos currentPos,
				BlockPos otherPos
		);

		default void postUpdate(WorldAccess world) {
		}
	}
}
