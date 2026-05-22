package net.minecraft.world.chunk.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;

/**
 * Хранилище данных блочного освещения.
 * Управляет массивами nibble-данных для каждой секции чанка,
 * возвращая уровень блочного света для заданной позиции блока.
 */
public class BlockLightStorage extends LightStorage<BlockLightStorage.Data> {

	protected BlockLightStorage(ChunkProvider chunkProvider) {
		super(LightType.BLOCK, chunkProvider, new BlockLightStorage.Data(new Long2ObjectOpenHashMap()));
	}

	@Override
	protected int getLight(long blockPos) {
		long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);
		ChunkNibbleArray section = getLightSection(sectionPos, false);

		return section == null
				? 0
				: section.get(
						ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos)),
						ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos)),
						ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos))
				);
	}

	/**
	 * Данные блочного освещения — карта секций к nibble-массивам.
	 */
	protected static final class Data extends ChunkToNibbleArrayMap<BlockLightStorage.Data> {

		public Data(Long2ObjectOpenHashMap<ChunkNibbleArray> arrays) {
			super(arrays);
		}

		public BlockLightStorage.Data copy() {
			return new BlockLightStorage.Data(this.arrays.clone());
		}
	}
}
