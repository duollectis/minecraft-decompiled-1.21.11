package net.minecraft.structure;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Генератор структуры «Зарытое сокровище».
 * <p>
 * Размещает один сундук с лутом под поверхностью океанского дна или пляжа.
 * Алгоритм ищет подходящую твёрдую породу (песчаник, камень, андезит и т.д.)
 * и вставляет сундук, заполняя соседние воздушные/жидкостные блоки для корректного
 * погружения структуры в ландшафт.
 */
public class BuriedTreasureGenerator {

	/**
	 * Единственный фрагмент структуры зарытого сокровища — сундук с лутом.
	 */
	public static class Piece extends StructurePiece {

		/** Флаг обновления блока с уведомлением клиента и соседей. */
		private static final int BLOCK_UPDATE_FLAGS = 3;

		public Piece(BlockPos pos) {
			super(StructurePieceType.BURIED_TREASURE, 0, new BlockBox(pos));
		}

		public Piece(NbtCompound nbt) {
			super(StructurePieceType.BURIED_TREASURE, nbt);
		}

		@Override
		protected void writeNbt(StructureContext context, NbtCompound nbt) {
		}

		@Override
		public void generate(
				StructureWorldAccess world,
				StructureAccessor structureAccessor,
				ChunkGenerator chunkGenerator,
				Random random,
				BlockBox chunkBox,
				ChunkPos chunkPos,
				BlockPos pivot
		) {
			int topY = world.getTopY(
					Heightmap.Type.OCEAN_FLOOR_WG,
					boundingBox.getMinX(),
					boundingBox.getMinZ()
			);
			BlockPos.Mutable mutable = new BlockPos.Mutable(boundingBox.getMinX(), topY, boundingBox.getMinZ());

			while (mutable.getY() > world.getBottomY()) {
				BlockState currentState = world.getBlockState(mutable);
				BlockState belowState = world.getBlockState(mutable.down());

				if (isSuitableBase(belowState)) {
					BlockState fillState = currentState.isAir() || isLiquid(currentState)
							? Blocks.SAND.getDefaultState()
							: currentState;

					for (Direction direction : Direction.values()) {
						BlockPos neighborPos = mutable.offset(direction);
						BlockState neighborState = world.getBlockState(neighborPos);

						if (neighborState.isAir() || isLiquid(neighborState)) {
							BlockState belowNeighbor = world.getBlockState(neighborPos.down());
							boolean neighborHasNoGround = belowNeighbor.isAir() || isLiquid(belowNeighbor);

							if (neighborHasNoGround && direction != Direction.UP) {
								world.setBlockState(neighborPos, currentState, BLOCK_UPDATE_FLAGS);
							} else {
								world.setBlockState(neighborPos, fillState, BLOCK_UPDATE_FLAGS);
							}
						}
					}

					boundingBox = new BlockBox(mutable);
					addChest(world, chunkBox, random, mutable, LootTables.BURIED_TREASURE_CHEST, null);
					return;
				}

				mutable.move(0, -1, 0);
			}
		}

		private static boolean isSuitableBase(BlockState state) {
			return state == Blocks.SANDSTONE.getDefaultState()
					|| state == Blocks.STONE.getDefaultState()
					|| state == Blocks.ANDESITE.getDefaultState()
					|| state == Blocks.GRANITE.getDefaultState()
					|| state == Blocks.DIORITE.getDefaultState();
		}

		private static boolean isLiquid(BlockState state) {
			return state == Blocks.WATER.getDefaultState() || state == Blocks.LAVA.getDefaultState();
		}
	}
}
