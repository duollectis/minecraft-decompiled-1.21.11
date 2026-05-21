package net.minecraft.world.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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
 * {@code UpgradeData}.
 */
public class UpgradeData {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final UpgradeData NO_UPGRADE_DATA = new UpgradeData(EmptyBlockView.INSTANCE);
	private static final String INDICES_KEY = "Indices";
	private static final EightWayDirection[] EIGHT_WAYS = EightWayDirection.values();
	private static final Codec<List<Tick<Block>>>
			BLOCK_TICKS_CODEC =
			Tick.createCodec(Registries.BLOCK.getCodec().orElse(Blocks.AIR)).listOf();
	private static final Codec<List<Tick<Fluid>>>
			FLUID_TICKS_CODEC =
			Tick.createCodec(Registries.FLUID.getCodec().orElse(Fluids.EMPTY)).listOf();
	private final EnumSet<EightWayDirection> sidesToUpgrade = EnumSet.noneOf(EightWayDirection.class);
	private final List<Tick<Block>> blockTicks = Lists.newArrayList();
	private final List<Tick<Fluid>> fluidTicks = Lists.newArrayList();
	private final int[][] centerIndicesToUpgrade;
	static final Map<Block, UpgradeData.Logic> BLOCK_TO_LOGIC = new IdentityHashMap<>();
	static final Set<UpgradeData.Logic> CALLBACK_LOGICS = Sets.newHashSet();

	private UpgradeData(HeightLimitView world) {
		this.centerIndicesToUpgrade = new int[world.countVerticalSections()][];
	}

	public UpgradeData(NbtCompound nbt, HeightLimitView world) {
		this(world);
		nbt.getCompound("Indices").ifPresent(indices -> {
			for (int ix = 0; ix < this.centerIndicesToUpgrade.length; ix++) {
				this.centerIndicesToUpgrade[ix] = indices.getIntArray(String.valueOf(ix)).orElse(null);
			}
		});
		int i = nbt.getInt("Sides", 0);

		for (EightWayDirection eightWayDirection : EightWayDirection.values()) {
			if ((i & 1 << eightWayDirection.ordinal()) != 0) {
				this.sidesToUpgrade.add(eightWayDirection);
			}
		}

		nbt.<List<Tick<Block>>>get("neighbor_block_ticks", BLOCK_TICKS_CODEC).ifPresent(this.blockTicks::addAll);
		nbt.<List<Tick<Fluid>>>get("neighbor_fluid_ticks", FLUID_TICKS_CODEC).ifPresent(this.fluidTicks::addAll);
	}

	private UpgradeData(UpgradeData data) {
		this.sidesToUpgrade.addAll(data.sidesToUpgrade);
		this.blockTicks.addAll(data.blockTicks);
		this.fluidTicks.addAll(data.fluidTicks);
		this.centerIndicesToUpgrade = new int[data.centerIndicesToUpgrade.length][];

		for (int i = 0; i < data.centerIndicesToUpgrade.length; i++) {
			int[] is = data.centerIndicesToUpgrade[i];
			this.centerIndicesToUpgrade[i] = is != null ? IntArrays.copy(is) : null;
		}
	}

	public void upgrade(WorldChunk chunk) {
		this.upgradeCenter(chunk);

		for (EightWayDirection eightWayDirection : EIGHT_WAYS) {
			upgradeSide(chunk, eightWayDirection);
		}

		World world = chunk.getWorld();
		this.blockTicks.forEach(tick -> {
			Block block = tick.type() == Blocks.AIR ? world.getBlockState(tick.pos()).getBlock() : tick.type();
			world.scheduleBlockTick(tick.pos(), block, tick.delay(), tick.priority());
		});
		this.fluidTicks.forEach(tick -> {
			Fluid fluid = tick.type() == Fluids.EMPTY ? world.getFluidState(tick.pos()).getFluid() : tick.type();
			world.scheduleFluidTick(tick.pos(), fluid, tick.delay(), tick.priority());
		});
		CALLBACK_LOGICS.forEach(logic -> logic.postUpdate(world));
	}

	private static void upgradeSide(WorldChunk chunk, EightWayDirection side) {
		World world = chunk.getWorld();
		if (chunk.getUpgradeData().sidesToUpgrade.remove(side)) {
			Set<Direction> set = side.getDirections();
			int i = 0;
			int j = 15;
			boolean bl = set.contains(Direction.EAST);
			boolean bl2 = set.contains(Direction.WEST);
			boolean bl3 = set.contains(Direction.SOUTH);
			boolean bl4 = set.contains(Direction.NORTH);
			boolean bl5 = set.size() == 1;
			ChunkPos chunkPos = chunk.getPos();
			int k = chunkPos.getStartX() + (!bl5 || !bl4 && !bl3 ? (bl2 ? 0 : 15) : 1);
			int l = chunkPos.getStartX() + (!bl5 || !bl4 && !bl3 ? (bl2 ? 0 : 15) : 14);
			int m = chunkPos.getStartZ() + (!bl5 || !bl && !bl2 ? (bl4 ? 0 : 15) : 1);
			int n = chunkPos.getStartZ() + (!bl5 || !bl && !bl2 ? (bl4 ? 0 : 15) : 14);
			Direction[] directions = Direction.values();
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for (BlockPos blockPos : BlockPos.iterate(k, world.getBottomY(), m, l, world.getTopYInclusive(), n)) {
				BlockState blockState = world.getBlockState(blockPos);
				BlockState blockState2 = blockState;

				for (Direction direction : directions) {
					mutable.set(blockPos, direction);
					blockState2 = applyAdjacentBlock(blockState2, direction, world, blockPos, mutable);
				}

				Block.replace(blockState, blockState2, world, blockPos, 18);
			}
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
		                     .getUpdatedState(
				                     oldState,
				                     dir,
				                     world.getBlockState(otherPos),
				                     world,
				                     currentPos,
				                     otherPos
		                     );
	}

	private void upgradeCenter(WorldChunk chunk) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockPos.Mutable mutable2 = new BlockPos.Mutable();
		ChunkPos chunkPos = chunk.getPos();
		WorldAccess worldAccess = chunk.getWorld();

		for (int i = 0; i < this.centerIndicesToUpgrade.length; i++) {
			ChunkSection chunkSection = chunk.getSection(i);
			int[] is = this.centerIndicesToUpgrade[i];
			this.centerIndicesToUpgrade[i] = null;
			if (is != null && is.length > 0) {
				Direction[] directions = Direction.values();
				PalettedContainer<BlockState> palettedContainer = chunkSection.getBlockStateContainer();
				int j = chunk.sectionIndexToCoord(i);
				int k = ChunkSectionPos.getBlockCoord(j);

				for (int l : is) {
					int m = l & 15;
					int n = l >> 8 & 15;
					int o = l >> 4 & 15;
					mutable.set(chunkPos.getStartX() + m, k + n, chunkPos.getStartZ() + o);
					BlockState blockState = palettedContainer.get(l);
					BlockState blockState2 = blockState;

					for (Direction direction : directions) {
						mutable2.set(mutable, direction);
						if (ChunkSectionPos.getSectionCoord(mutable.getX()) == chunkPos.x
								&& ChunkSectionPos.getSectionCoord(mutable.getZ()) == chunkPos.z) {
							blockState2 = applyAdjacentBlock(blockState2, direction, worldAccess, mutable, mutable2);
						}
					}

					Block.replace(blockState, blockState2, worldAccess, mutable, 18);
				}
			}
		}

		for (int ix = 0; ix < this.centerIndicesToUpgrade.length; ix++) {
			if (this.centerIndicesToUpgrade[ix] != null) {
				LOGGER.warn(
						"Discarding update data for section {} for chunk ({} {})",
						new Object[]{worldAccess.sectionIndexToCoord(ix), chunkPos.x, chunkPos.z}
				);
			}

			this.centerIndicesToUpgrade[ix] = null;
		}
	}

	public boolean isDone() {
		for (int[] is : this.centerIndicesToUpgrade) {
			if (is != null) {
				return false;
			}
		}

		return this.sidesToUpgrade.isEmpty();
	}

	public NbtCompound toNbt() {
		NbtCompound nbtCompound = new NbtCompound();
		NbtCompound nbtCompound2 = new NbtCompound();

		for (int i = 0; i < this.centerIndicesToUpgrade.length; i++) {
			String string = String.valueOf(i);
			if (this.centerIndicesToUpgrade[i] != null && this.centerIndicesToUpgrade[i].length != 0) {
				nbtCompound2.putIntArray(string, this.centerIndicesToUpgrade[i]);
			}
		}

		if (!nbtCompound2.isEmpty()) {
			nbtCompound.put("Indices", nbtCompound2);
		}

		int ix = 0;

		for (EightWayDirection eightWayDirection : this.sidesToUpgrade) {
			ix |= 1 << eightWayDirection.ordinal();
		}

		nbtCompound.putByte("Sides", (byte) ix);
		if (!this.blockTicks.isEmpty()) {
			nbtCompound.put("neighbor_block_ticks", BLOCK_TICKS_CODEC, this.blockTicks);
		}

		if (!this.fluidTicks.isEmpty()) {
			nbtCompound.put("neighbor_fluid_ticks", FLUID_TICKS_CODEC, this.fluidTicks);
		}

		return nbtCompound;
	}

	public UpgradeData copy() {
		return this == NO_UPGRADE_DATA ? NO_UPGRADE_DATA : new UpgradeData(this);
	}

	/**
	 * {@code BuiltinLogic}.
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
				if (otherState.isOf(oldState.getBlock())
						&& direction.getAxis().isHorizontal()
						&& oldState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE
						&& otherState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
					Direction direction2 = oldState.get(ChestBlock.FACING);
					if (direction.getAxis() != direction2.getAxis()
							&& direction2 == otherState.get(ChestBlock.FACING)) {
						ChestType
								chestType =
								direction == direction2.rotateYClockwise() ? ChestType.LEFT : ChestType.RIGHT;
						world.setBlockState(
								otherPos,
								otherState.with(ChestBlock.CHEST_TYPE, chestType.getOpposite()),
								18
						);
						if (direction2 == Direction.NORTH || direction2 == Direction.EAST) {
							BlockEntity blockEntity = world.getBlockEntity(currentPos);
							BlockEntity blockEntity2 = world.getBlockEntity(otherPos);
							if (blockEntity instanceof ChestBlockEntity && blockEntity2 instanceof ChestBlockEntity) {
								ChestBlockEntity.copyInventory(
										(ChestBlockEntity) blockEntity,
										(ChestBlockEntity) blockEntity2
								);
							}
						}

						return oldState.with(ChestBlock.CHEST_TYPE, chestType);
					}
				}

				return oldState;
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
			private final ThreadLocal<List<ObjectSet<BlockPos>>>
					distanceToPositions =
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
				BlockState blockState = oldState.getStateForNeighborUpdate(
						world, world, currentPos, direction, otherPos, world.getBlockState(otherPos), world.getRandom()
				);
				if (oldState != blockState) {
					int i = blockState.get(Properties.DISTANCE_1_7);
					List<ObjectSet<BlockPos>> list = this.distanceToPositions.get();
					if (list.isEmpty()) {
						for (int j = 0; j < 7; j++) {
							list.add(new ObjectOpenHashSet());
						}
					}

					list.get(i).add(currentPos.toImmutable());
				}

				return oldState;
			}

			@Override
			public void postUpdate(WorldAccess world) {
				BlockPos.Mutable mutable = new BlockPos.Mutable();
				List<ObjectSet<BlockPos>> list = this.distanceToPositions.get();

				for (int i = 2; i < list.size(); i++) {
					int j = i - 1;
					ObjectSet<BlockPos> objectSet = list.get(j);
					ObjectSet<BlockPos> objectSet2 = list.get(i);
					ObjectIterator var8 = objectSet.iterator();

					while (var8.hasNext()) {
						BlockPos blockPos = (BlockPos) var8.next();
						BlockState blockState = world.getBlockState(blockPos);
						if (blockState.get(Properties.DISTANCE_1_7) >= j) {
							world.setBlockState(blockPos, blockState.with(Properties.DISTANCE_1_7, j), 18);
							if (i != 7) {
								for (Direction direction : DIRECTIONS) {
									mutable.set(blockPos, direction);
									BlockState blockState2 = world.getBlockState(mutable);
									if (blockState2.contains(Properties.DISTANCE_1_7)
											&& blockState.get(Properties.DISTANCE_1_7) > i) {
										objectSet2.add(mutable.toImmutable());
									}
								}
							}
						}
					}
				}

				list.clear();
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
				if (oldState.get(StemBlock.AGE) == 7) {
					Block block = oldState.isOf(Blocks.PUMPKIN_STEM) ? Blocks.PUMPKIN : Blocks.MELON;
					if (otherState.isOf(block)) {
						return (oldState.isOf(Blocks.PUMPKIN_STEM) ? Blocks.ATTACHED_PUMPKIN_STEM
						                                           : Blocks.ATTACHED_MELON_STEM
						)
								.getDefaultState()
								.with(HorizontalFacingBlock.FACING, direction);
					}
				}

				return oldState;
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

	/**
	 * {@code Logic}.
	 */
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
