package net.minecraft.structure;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Базовый абстрактный класс для всех кусков структуры.
 * Хранит ограничивающий блок, ориентацию, зеркало и поворот,
 * а также предоставляет утилиты для размещения блоков с учётом трансформации.
 */
public abstract class StructurePiece {

	private static final Set<Block> BLOCKS_NEEDING_POST_PROCESSING = ImmutableSet.<Block>builder()
		.add(Blocks.NETHER_BRICK_FENCE)
		.add(Blocks.TORCH)
		.add(Blocks.WALL_TORCH)
		.add(Blocks.OAK_FENCE)
		.add(Blocks.SPRUCE_FENCE)
		.add(Blocks.DARK_OAK_FENCE)
		.add(Blocks.PALE_OAK_FENCE)
		.add(Blocks.ACACIA_FENCE)
		.add(Blocks.BIRCH_FENCE)
		.add(Blocks.JUNGLE_FENCE)
		.add(Blocks.LADDER)
		.add(Blocks.IRON_BARS)
		.build();

	protected static final BlockState AIR = Blocks.CAVE_AIR.getDefaultState();

	private static final int CHUNK_SIZE = 15;
	private static final int NO_ORIENTATION = -1;

	protected BlockBox boundingBox;
	private @Nullable Direction facing;
	private BlockMirror mirror;
	private BlockRotation rotation;
	protected int chainLength;
	private final StructurePieceType type;

	protected StructurePiece(StructurePieceType type, int length, BlockBox boundingBox) {
		this.type = type;
		this.chainLength = length;
		this.boundingBox = boundingBox;
	}

	public StructurePiece(StructurePieceType type, NbtCompound nbt) {
		this(type, nbt.getInt("GD", 0), nbt.<BlockBox>get("BB", BlockBox.CODEC).orElseThrow());
		int orientationIndex = nbt.getInt("O", 0);
		setOrientation(orientationIndex == NO_ORIENTATION ? null : Direction.fromHorizontalQuarterTurns(orientationIndex));
	}

	/**
	 * Создаёт ограничивающий блок по начальной позиции, ориентации и размерам.
	 * Для оси Z ширина и глубина остаются как есть, для оси X — меняются местами.
	 */
	protected static BlockBox createBox(int x, int y, int z, Direction orientation, int width, int height, int depth) {
		return orientation.getAxis() == Direction.Axis.Z
			? new BlockBox(x, y, z, x + width - 1, y + height - 1, z + depth - 1)
			: new BlockBox(x, y, z, x + depth - 1, y + height - 1, z + width - 1);
	}

	protected static Direction getRandomHorizontalDirection(Random random) {
		return Direction.Type.HORIZONTAL.random(random);
	}

	/**
	 * Сериализует кусок структуры в NBT для сохранения.
	 */
	public final NbtCompound toNbt(StructureContext context) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", Registries.STRUCTURE_PIECE.getId(getType()).toString());
		nbt.put("BB", BlockBox.CODEC, boundingBox);
		Direction direction = getFacing();
		nbt.putInt("O", direction == null ? NO_ORIENTATION : direction.getHorizontalQuarterTurns());
		nbt.putInt("GD", chainLength);
		writeNbt(context, nbt);
		return nbt;
	}

	protected abstract void writeNbt(StructureContext context, NbtCompound nbt);

	public void fillOpenings(StructurePiece start, StructurePiecesHolder holder, Random random) {
	}

	public abstract void generate(
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockBox chunkBox,
		ChunkPos chunkPos,
		BlockPos pivot
	);

	public BlockBox getBoundingBox() {
		return boundingBox;
	}

	public int getChainLength() {
		return chainLength;
	}

	public void setChainLength(int chainLength) {
		this.chainLength = chainLength;
	}

	/**
	 * Проверяет, пересекается ли кусок с заданным чанком с учётом отступа.
	 */
	public boolean intersectsChunk(ChunkPos pos, int offset) {
		int startX = pos.getStartX();
		int startZ = pos.getStartZ();
		return boundingBox.intersectsXZ(
			startX - offset,
			startZ - offset,
			startX + CHUNK_SIZE + offset,
			startZ + CHUNK_SIZE + offset
		);
	}

	public BlockPos getCenter() {
		return new BlockPos(boundingBox.getCenter());
	}

	protected BlockPos.Mutable offsetPos(int x, int y, int z) {
		return new BlockPos.Mutable(applyXTransform(x, z), applyYTransform(y), applyZTransform(x, z));
	}

	protected int applyXTransform(int x, int z) {
		Direction direction = getFacing();
		if (direction == null) {
			return x;
		}

		return switch (direction) {
			case NORTH, SOUTH -> boundingBox.getMinX() + x;
			case WEST -> boundingBox.getMaxX() - z;
			case EAST -> boundingBox.getMinX() + z;
			default -> x;
		};
	}

	protected int applyYTransform(int y) {
		return getFacing() == null ? y : y + boundingBox.getMinY();
	}

	protected int applyZTransform(int x, int z) {
		Direction direction = getFacing();
		if (direction == null) {
			return z;
		}

		return switch (direction) {
			case NORTH -> boundingBox.getMaxZ() - z;
			case SOUTH -> boundingBox.getMinZ() + z;
			case WEST, EAST -> boundingBox.getMinZ() + x;
			default -> z;
		};
	}

	/**
	 * Размещает блок в мире с учётом трансформации, зеркала и поворота.
	 * Блоки, требующие пост-обработки (заборы, факелы), помечаются для неё.
	 */
	protected void addBlock(StructureWorldAccess world, BlockState block, int x, int y, int z, BlockBox box) {
		BlockPos blockPos = offsetPos(x, y, z);
		if (!box.contains(blockPos) || !canAddBlock(world, x, y, z, box)) {
			return;
		}

		if (mirror != BlockMirror.NONE) {
			block = block.mirror(mirror);
		}

		if (rotation != BlockRotation.NONE) {
			block = block.rotate(rotation);
		}

		world.setBlockState(blockPos, block, 2);

		FluidState fluidState = world.getFluidState(blockPos);
		if (!fluidState.isEmpty()) {
			world.scheduleFluidTick(blockPos, fluidState.getFluid(), 0);
		}

		if (BLOCKS_NEEDING_POST_PROCESSING.contains(block.getBlock())) {
			world.getChunk(blockPos).markBlockForPostProcessing(blockPos);
		}
	}

	protected boolean canAddBlock(WorldView world, int x, int y, int z, BlockBox box) {
		return true;
	}

	protected BlockState getBlockAt(BlockView world, int x, int y, int z, BlockBox box) {
		BlockPos blockPos = offsetPos(x, y, z);
		return !box.contains(blockPos) ? Blocks.AIR.getDefaultState() : world.getBlockState(blockPos);
	}

	protected boolean isUnderSeaLevel(WorldView world, int x, int z, int y, BlockBox box) {
		BlockPos blockPos = offsetPos(x, z + 1, y);
		return box.contains(blockPos) && blockPos.getY() < world.getTopY(
			Heightmap.Type.OCEAN_FLOOR_WG,
			blockPos.getX(),
			blockPos.getZ()
		);
	}

	/**
	 * Заполняет прямоугольную область воздухом.
	 */
	protected void fill(
		StructureWorldAccess world,
		BlockBox bounds,
		int minX,
		int minY,
		int minZ,
		int maxX,
		int maxY,
		int maxZ
	) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					addBlock(world, Blocks.AIR.getDefaultState(), x, y, z, bounds);
				}
			}
		}
	}

	/**
	 * Заполняет прямоугольную область с контуром из одного блока и заливкой другим.
	 *
	 * @param cantReplaceAir если {@code true}, пропускает воздушные блоки
	 */
	protected void fillWithOutline(
		StructureWorldAccess world,
		BlockBox box,
		int minX,
		int minY,
		int minZ,
		int maxX,
		int maxY,
		int maxZ,
		BlockState outline,
		BlockState inside,
		boolean cantReplaceAir
	) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (cantReplaceAir && getBlockAt(world, x, y, z, box).isAir()) {
						continue;
					}

					boolean isEdge = y == minY || y == maxY || x == minX || x == maxX || z == minZ || z == maxZ;
					addBlock(world, isEdge ? outline : inside, x, y, z, box);
				}
			}
		}
	}

	protected void fillWithOutline(
		StructureWorldAccess world,
		BlockBox box,
		BlockBox fillBox,
		BlockState outline,
		BlockState inside,
		boolean cantReplaceAir
	) {
		fillWithOutline(
			world,
			box,
			fillBox.getMinX(),
			fillBox.getMinY(),
			fillBox.getMinZ(),
			fillBox.getMaxX(),
			fillBox.getMaxY(),
			fillBox.getMaxZ(),
			outline,
			inside,
			cantReplaceAir
		);
	}

	/**
	 * Заполняет прямоугольную область с рандомизацией блоков через {@link BlockRandomizer}.
	 */
	protected void fillWithOutline(
		StructureWorldAccess world,
		BlockBox box,
		int minX,
		int minY,
		int minZ,
		int maxX,
		int maxY,
		int maxZ,
		boolean cantReplaceAir,
		Random random,
		StructurePiece.BlockRandomizer randomizer
	) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (cantReplaceAir && getBlockAt(world, x, y, z, box).isAir()) {
						continue;
					}

					boolean isEdge = y == minY || y == maxY || x == minX || x == maxX || z == minZ || z == maxZ;
					randomizer.setBlock(random, x, y, z, isEdge);
					addBlock(world, randomizer.getBlock(), x, y, z, box);
				}
			}
		}
	}

	protected void fillWithOutline(
		StructureWorldAccess world,
		BlockBox box,
		BlockBox fillBox,
		boolean cantReplaceAir,
		Random random,
		StructurePiece.BlockRandomizer randomizer
	) {
		fillWithOutline(
			world,
			box,
			fillBox.getMinX(),
			fillBox.getMinY(),
			fillBox.getMinZ(),
			fillBox.getMaxX(),
			fillBox.getMaxY(),
			fillBox.getMaxZ(),
			cantReplaceAir,
			random,
			randomizer
		);
	}

	/**
	 * Заполняет область с вероятностным пропуском блоков и ограничением по уровню моря.
	 *
	 * @param blockChance       вероятность размещения каждого блока
	 * @param cantReplaceAir    пропускать ли воздушные блоки
	 * @param stayBelowSeaLevel размещать ли только ниже уровня моря
	 */
	protected void fillWithOutlineUnderSeaLevel(
		StructureWorldAccess world,
		BlockBox box,
		Random random,
		float blockChance,
		int minX,
		int minY,
		int minZ,
		int maxX,
		int maxY,
		int maxZ,
		BlockState outline,
		BlockState inside,
		boolean cantReplaceAir,
		boolean stayBelowSeaLevel
	) {
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					if (random.nextFloat() > blockChance) {
						continue;
					}

					if (cantReplaceAir && getBlockAt(world, x, y, z, box).isAir()) {
						continue;
					}

					if (stayBelowSeaLevel && !isUnderSeaLevel(world, x, y, z, box)) {
						continue;
					}

					boolean isEdge = y == minY || y == maxY || x == minX || x == maxX || z == minZ || z == maxZ;
					addBlock(world, isEdge ? outline : inside, x, y, z, box);
				}
			}
		}
	}

	protected void addBlockWithRandomThreshold(
		StructureWorldAccess world,
		BlockBox bounds,
		Random random,
		float threshold,
		int x,
		int y,
		int z,
		BlockState state
	) {
		if (random.nextFloat() < threshold) {
			addBlock(world, state, x, y, z, bounds);
		}
	}

	/**
	 * Заполняет полуэллипсоид блоками.
	 * Использует уравнение эллипсоида для определения принадлежности точки.
	 */
	protected void fillHalfEllipsoid(
		StructureWorldAccess world,
		BlockBox bounds,
		int minX,
		int minY,
		int minZ,
		int maxX,
		int maxY,
		int maxZ,
		BlockState block,
		boolean cantReplaceAir
	) {
		float width = maxX - minX + 1;
		float height = maxY - minY + 1;
		float depth = maxZ - minZ + 1;
		float centerX = minX + width / 2.0F;
		float centerZ = minZ + depth / 2.0F;

		for (int y = minY; y <= maxY; y++) {
			float relY = (y - minY) / height;

			for (int x = minX; x <= maxX; x++) {
				float relX = (x - centerX) / (width * 0.5F);

				for (int z = minZ; z <= maxZ; z++) {
					float relZ = (z - centerZ) / (depth * 0.5F);
					if (cantReplaceAir && getBlockAt(world, x, y, z, bounds).isAir()) {
						continue;
					}

					float distSq = relX * relX + relY * relY + relZ * relZ;
					if (distSq <= 1.05F) {
						addBlock(world, block, x, y, z, bounds);
					}
				}
			}
		}
	}

	/**
	 * Заполняет столбец блоками вниз до тех пор, пока блок можно заменить.
	 */
	protected void fillDownwards(StructureWorldAccess world, BlockState state, int x, int y, int z, BlockBox box) {
		BlockPos.Mutable mutable = offsetPos(x, y, z);
		if (!box.contains(mutable)) {
			return;
		}

		while (canReplace(world.getBlockState(mutable)) && mutable.getY() > world.getBottomY() + 1) {
			world.setBlockState(mutable, state, 2);
			mutable.move(Direction.DOWN);
		}
	}

	protected boolean canReplace(BlockState state) {
		return state.isAir()
			|| state.isLiquid()
			|| state.isOf(Blocks.GLOW_LICHEN)
			|| state.isOf(Blocks.SEAGRASS)
			|| state.isOf(Blocks.TALL_SEAGRASS);
	}

	protected boolean addChest(
		StructureWorldAccess world,
		BlockBox boundingBox,
		Random random,
		int x,
		int y,
		int z,
		RegistryKey<LootTable> lootTable
	) {
		return addChest(world, boundingBox, random, offsetPos(x, y, z), lootTable, null);
	}

	/**
	 * Ориентирует сундук так, чтобы он смотрел от ближайшей непрозрачной стены.
	 * Если стен несколько или ни одной — оставляет текущее направление.
	 */
	public static BlockState orientateChest(BlockView world, BlockPos pos, BlockState state) {
		Direction solidWallDirection = null;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockState neighbor = world.getBlockState(pos.offset(direction));
			if (neighbor.isOf(Blocks.CHEST)) {
				return state;
			}

			if (neighbor.isOpaqueFullCube()) {
				if (solidWallDirection != null) {
					solidWallDirection = null;
					break;
				}

				solidWallDirection = direction;
			}
		}

		if (solidWallDirection != null) {
			return state.with(HorizontalFacingBlock.FACING, solidWallDirection.getOpposite());
		}

		Direction facing = state.get(HorizontalFacingBlock.FACING);
		BlockPos frontPos = pos.offset(facing);
		if (world.getBlockState(frontPos).isOpaqueFullCube()) {
			facing = facing.getOpposite();
			frontPos = pos.offset(facing);
		}

		if (world.getBlockState(frontPos).isOpaqueFullCube()) {
			facing = facing.rotateYClockwise();
			frontPos = pos.offset(facing);
		}

		if (world.getBlockState(frontPos).isOpaqueFullCube()) {
			facing = facing.getOpposite();
		}

		return state.with(HorizontalFacingBlock.FACING, facing);
	}

	protected boolean addChest(
		ServerWorldAccess world,
		BlockBox boundingBox,
		Random random,
		BlockPos pos,
		RegistryKey<LootTable> lootTable,
		@Nullable BlockState block
	) {
		if (!boundingBox.contains(pos) || world.getBlockState(pos).isOf(Blocks.CHEST)) {
			return false;
		}

		if (block == null) {
			block = orientateChest(world, pos, Blocks.CHEST.getDefaultState());
		}

		world.setBlockState(pos, block, 2);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof ChestBlockEntity chest) {
			chest.setLootTable(lootTable, random.nextLong());
		}

		return true;
	}

	protected boolean addDispenser(
		StructureWorldAccess world,
		BlockBox boundingBox,
		Random random,
		int x,
		int y,
		int z,
		Direction facing,
		RegistryKey<LootTable> lootTable
	) {
		BlockPos blockPos = offsetPos(x, y, z);
		if (!boundingBox.contains(blockPos) || world.getBlockState(blockPos).isOf(Blocks.DISPENSER)) {
			return false;
		}

		addBlock(
			world,
			Blocks.DISPENSER.getDefaultState().with(DispenserBlock.FACING, facing),
			x,
			y,
			z,
			boundingBox
		);
		BlockEntity blockEntity = world.getBlockEntity(blockPos);
		if (blockEntity instanceof DispenserBlockEntity dispenser) {
			dispenser.setLootTable(lootTable, random.nextLong());
		}

		return true;
	}

	public void translate(int x, int y, int z) {
		boundingBox.move(x, y, z);
	}

	/**
	 * Вычисляет общий ограничивающий блок для потока кусков структуры.
	 *
	 * @throws IllegalStateException если поток пуст
	 */
	public static BlockBox boundingBox(Stream<StructurePiece> pieces) {
		return BlockBox.encompass(pieces.map(StructurePiece::getBoundingBox)::iterator)
			.orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox without pieces"));
	}

	/**
	 * Возвращает первый кусок из списка, чей ограничивающий блок пересекается с заданным.
	 */
	public static @Nullable StructurePiece firstIntersecting(List<StructurePiece> pieces, BlockBox box) {
		for (StructurePiece piece : pieces) {
			if (piece.getBoundingBox().intersects(box)) {
				return piece;
			}
		}

		return null;
	}

	public @Nullable Direction getFacing() {
		return facing;
	}

	/**
	 * Устанавливает ориентацию куска и вычисляет соответствующие зеркало и поворот.
	 * NORTH — базовое направление без трансформаций.
	 */
	public void setOrientation(@Nullable Direction orientation) {
		facing = orientation;
		if (orientation == null) {
			rotation = BlockRotation.NONE;
			mirror = BlockMirror.NONE;
			return;
		}

		switch (orientation) {
			case SOUTH:
				mirror = BlockMirror.LEFT_RIGHT;
				rotation = BlockRotation.NONE;
				break;
			case WEST:
				mirror = BlockMirror.LEFT_RIGHT;
				rotation = BlockRotation.CLOCKWISE_90;
				break;
			case EAST:
				mirror = BlockMirror.NONE;
				rotation = BlockRotation.CLOCKWISE_90;
				break;
			default:
				mirror = BlockMirror.NONE;
				rotation = BlockRotation.NONE;
		}
	}

	public BlockRotation getRotation() {
		return rotation;
	}

	public BlockMirror getMirror() {
		return mirror;
	}

	public StructurePieceType getType() {
		return type;
	}

	/**
	 * Абстрактный рандомизатор блоков для заполнения областей с вариативностью.
	 * Реализации определяют логику выбора блока по координатам и флагу грани.
	 */
	public abstract static class BlockRandomizer {

		protected BlockState block = Blocks.AIR.getDefaultState();

		/**
		 * @param placeBlock {@code true}, если блок находится на грани (контуре) области
		 */
		public abstract void setBlock(Random random, int x, int y, int z, boolean placeBlock);

		public BlockState getBlock() {
			return block;
		}
	}
}
