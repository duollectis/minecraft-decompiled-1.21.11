package net.minecraft.client.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ToIntFunction;

/**
 * Потокобезопасный кэш цветов биомов для клиентского рендеринга.
 * Хранит вычисленные цвета по чанкам и секциям Y, избегая повторных вычислений.
 */
@Environment(EnvType.CLIENT)
public class BiomeColorCache {

	private static final int MAX_ENTRY_SIZE = 256;

	private final ThreadLocal<BiomeColorCache.Last> last = ThreadLocal.withInitial(BiomeColorCache.Last::new);
	private final Long2ObjectLinkedOpenHashMap<BiomeColorCache.Colors>
			colors =
			new Long2ObjectLinkedOpenHashMap<>(256, 0.25F);
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final ToIntFunction<BlockPos> colorFactory;

	/**
	 * @param colorFactory функция вычисления цвета биома по позиции блока
	 */
	public BiomeColorCache(ToIntFunction<BlockPos> colorFactory) {
		this.colorFactory = colorFactory;
	}

	/**
	 * Возвращает цвет биома для указанной позиции блока.
	 * Использует потоко-локальный кэш последнего чанка для ускорения повторных запросов.
	 *
	 * @param pos позиция блока
	 * @return цвет биома в формате ARGB
	 */
	public int getBiomeColor(BlockPos pos) {
		int i = ChunkSectionPos.getSectionCoord(pos.getX());
		int j = ChunkSectionPos.getSectionCoord(pos.getZ());
		BiomeColorCache.Last lastEntry = this.last.get();

		if (lastEntry.x != i || lastEntry.z != j || lastEntry.colors == null || lastEntry.colors.needsCacheRefresh()) {
			lastEntry.x = i;
			lastEntry.z = j;
			lastEntry.colors = this.getColorArray(i, j);
		}

		int[] is = lastEntry.colors.get(pos.getY());
		int k = pos.getX() & 15;
		int l = pos.getZ() & 15;
		int m = l << 4 | k;
		int n = is[m];

		if (n != -1) {
			return n;
		}

		int o = this.colorFactory.applyAsInt(pos);
		is[m] = o;
		return o;
	}

	/**
	 * Сбрасывает кэш для чанка и его соседей (3×3 область).
	 *
	 * @param chunkX координата X чанка
	 * @param chunkZ координата Z чанка
	 */
	public void reset(int chunkX, int chunkZ) {
		try {
			this.lock.writeLock().lock();

			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					long l = ChunkPos.toLong(chunkX + i, chunkZ + j);
					BiomeColorCache.Colors entry = (BiomeColorCache.Colors) this.colors.remove(l);

					if (entry != null) {
						entry.setNeedsCacheRefresh();
					}
				}
			}
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Полностью сбрасывает весь кэш цветов.
	 */
	public void reset() {
		try {
			this.lock.writeLock().lock();
			this.colors.values().forEach(BiomeColorCache.Colors::setNeedsCacheRefresh);
			this.colors.clear();
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Возвращает массив цветов для чанка, создавая новый при необходимости.
	 * Использует двухфазную блокировку (read → write) для минимизации конкуренции.
	 */
	private BiomeColorCache.Colors getColorArray(int chunkX, int chunkZ) {
		long l = ChunkPos.toLong(chunkX, chunkZ);
		this.lock.readLock().lock();

		try {
			BiomeColorCache.Colors existing = (BiomeColorCache.Colors) this.colors.get(l);

			if (existing != null) {
				return existing;
			}
		}
		finally {
			this.lock.readLock().unlock();
		}

		this.lock.writeLock().lock();

		BiomeColorCache.Colors result;

		try {
			BiomeColorCache.Colors existing = (BiomeColorCache.Colors) this.colors.get(l);

			if (existing == null) {
				result = new BiomeColorCache.Colors();

				if (this.colors.size() >= 256) {
					BiomeColorCache.Colors evicted = (BiomeColorCache.Colors) this.colors.removeFirst();

					if (evicted != null) {
						evicted.setNeedsCacheRefresh();
					}
				}

				this.colors.put(l, result);
				return result;
			}

			result = existing;
		}
		finally {
			this.lock.writeLock().unlock();
		}

		return result;
	}

	/**
	 * Хранит массивы цветов для одного чанка, разбитые по секциям Y.
	 */
	@Environment(EnvType.CLIENT)
	static class Colors {

		private final Int2ObjectArrayMap<int[]> colors = new Int2ObjectArrayMap<>(16);
		private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		private static final int XZ_COLORS_SIZE = MathHelper.square(16);
		private volatile boolean needsCacheRefresh;

		/**
		 * Возвращает массив цветов для указанной секции Y, создавая при необходимости.
		 *
		 * @param y координата секции Y
		 * @return массив цветов размером 16×16
		 */
		public int[] get(int y) {
			this.lock.readLock().lock();

			try {
				int[] is = (int[]) this.colors.get(y);

				if (is != null) {
					return is;
				}
			}
			finally {
				this.lock.readLock().unlock();
			}

			this.lock.writeLock().lock();

			int[] var12;

			try {
				var12 = (int[]) this.colors.computeIfAbsent(y, yx -> this.createDefault());
			}
			finally {
				this.lock.writeLock().unlock();
			}

			return var12;
		}

		/**
		 * Создаёт массив цветов по умолчанию, заполненный значением -1 (не вычислено).
		 */
		private int[] createDefault() {
			int[] is = new int[XZ_COLORS_SIZE];
			Arrays.fill(is, -1);
			return is;
		}

		/**
		 * @return {@code true}, если кэш устарел и требует обновления
		 */
		public boolean needsCacheRefresh() {
			return this.needsCacheRefresh;
		}

		/**
		 * Помечает кэш как устаревший.
		 */
		public void setNeedsCacheRefresh() {
			this.needsCacheRefresh = true;
		}
	}

	/**
	 * Потоко-локальный кэш последнего запрошенного чанка для быстрого повторного доступа.
	 */
	@Environment(EnvType.CLIENT)
	static class Last {

		public int x = Integer.MIN_VALUE;
		public int z = Integer.MIN_VALUE;
		BiomeColorCache.@Nullable Colors colors;

		private Last() {
		}
	}
}
