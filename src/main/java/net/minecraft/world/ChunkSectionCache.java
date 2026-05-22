package net.minecraft.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.jspecify.annotations.Nullable;

/**
 * Кэш секций чанков с блокировкой для безопасного многопоточного доступа.
 * <p>
 * При первом обращении к секции она загружается из мира, блокируется
 * и помещается в кэш. При закрытии все заблокированные секции разблокируются.
 * Используется в алгоритмах, требующих быстрого доступа к блокам
 * без повторной загрузки чанков.
 */
public class ChunkSectionCache implements AutoCloseable {

	private final WorldAccess world;
	private final Long2ObjectMap<ChunkSection> cache = new Long2ObjectOpenHashMap<>();
	private @Nullable ChunkSection cachedSection;
	private long cachedSectionPos;

	public ChunkSectionCache(WorldAccess world) {
		this.world = world;
	}

	/**
	 * Возвращает секцию чанка, содержащую указанную позицию блока.
	 * <p>
	 * Результат кэшируется по позиции секции. Секция блокируется при первой загрузке.
	 *
	 * @param pos позиция блока
	 * @return секция чанка или {@code null} если позиция вне вертикальных границ мира
	 */
	public @Nullable ChunkSection getSection(BlockPos pos) {
		int sectionIndex = world.getSectionIndex(pos.getY());
		if (sectionIndex < 0 || sectionIndex >= world.countVerticalSections()) {
			return null;
		}

		long sectionLong = ChunkSectionPos.toLong(pos);
		if (cachedSection != null && cachedSectionPos == sectionLong) {
			return cachedSection;
		}

		cachedSection = cache.computeIfAbsent(sectionLong, key -> {
			Chunk chunk = world.getChunk(
				ChunkSectionPos.getSectionCoord(pos.getX()),
				ChunkSectionPos.getSectionCoord(pos.getZ())
			);
			ChunkSection section = chunk.getSection(sectionIndex);
			section.lock();
			return section;
		});
		cachedSectionPos = sectionLong;
		return cachedSection;
	}

	public BlockState getBlockState(BlockPos pos) {
		ChunkSection section = getSection(pos);
		if (section == null) {
			return Blocks.AIR.getDefaultState();
		}

		int localX = ChunkSectionPos.getLocalCoord(pos.getX());
		int localY = ChunkSectionPos.getLocalCoord(pos.getY());
		int localZ = ChunkSectionPos.getLocalCoord(pos.getZ());
		return section.getBlockState(localX, localY, localZ);
	}

	@Override
	public void close() {
		for (ChunkSection section : cache.values()) {
			section.unlock();
		}
	}
}
