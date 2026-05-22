package net.minecraft.block.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldView;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Трёхмерный шаблон блоков, используемый для поиска структур в мире.
 * <p>
 * Хранит массив предикатов {@code pattern[depth][height][width]}, каждый из которых
 * проверяет состояние блока в соответствующей ячейке шаблона. Поиск выполняется
 * методом {@link #searchAround} путём перебора всех позиций и ориентаций вокруг
 * заданной точки.
 */
public class BlockPattern {

	private final Predicate<CachedBlockPosition>[][][] pattern;
	private final int depth;
	private final int height;
	private final int width;

	public BlockPattern(Predicate<CachedBlockPosition>[][][] pattern) {
		this.pattern = pattern;
		this.depth = pattern.length;

		if (this.depth > 0) {
			this.height = pattern[0].length;
			this.width = this.height > 0 ? pattern[0][0].length : 0;
		} else {
			this.height = 0;
			this.width = 0;
		}
	}

	public int getDepth() {
		return depth;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	@VisibleForTesting
	public Predicate<CachedBlockPosition>[][][] getPattern() {
		return pattern;
	}

	@VisibleForTesting
	public BlockPattern.@Nullable Result testTransform(
		WorldView world,
		BlockPos frontTopLeft,
		Direction forwards,
		Direction up
	) {
		LoadingCache<BlockPos, CachedBlockPosition> cache = makeCache(world, false);
		return testTransform(frontTopLeft, forwards, up, cache);
	}

	private BlockPattern.@Nullable Result testTransform(
		BlockPos frontTopLeft,
		Direction forwards,
		Direction up,
		LoadingCache<BlockPos, CachedBlockPosition> cache
	) {
		for (int widthIndex = 0; widthIndex < width; widthIndex++) {
			for (int heightIndex = 0; heightIndex < height; heightIndex++) {
				for (int depthIndex = 0; depthIndex < depth; depthIndex++) {
					BlockPos translated = translate(frontTopLeft, forwards, up, widthIndex, heightIndex, depthIndex);
					CachedBlockPosition cached = cache.getUnchecked(translated);

					if (pattern[depthIndex][heightIndex][widthIndex].test(cached) == false) {
						return null;
					}
				}
			}
		}

		return new BlockPattern.Result(frontTopLeft, forwards, up, cache, width, height, depth);
	}

	/**
	 * Ищет совпадение шаблона в мире вокруг заданной позиции.
	 * <p>
	 * Перебирает все позиции в кубе размером {@code max(width, height, depth)} вокруг {@code pos},
	 * а также все допустимые комбинации направлений {@code forwards} и {@code up}.
	 *
	 * @param world мир для поиска
	 * @param pos   центральная позиция поиска
	 * @return результат совпадения или {@code null}, если шаблон не найден
	 */
	public BlockPattern.@Nullable Result searchAround(WorldView world, BlockPos pos) {
		LoadingCache<BlockPos, CachedBlockPosition> cache = makeCache(world, false);
		int searchRadius = Math.max(Math.max(width, height), depth);

		for (BlockPos blockPos : BlockPos.iterate(pos, pos.add(searchRadius - 1, searchRadius - 1, searchRadius - 1))) {
			for (Direction forwards : Direction.values()) {
				for (Direction up : Direction.values()) {
					if (up == forwards || up == forwards.getOpposite()) {
						continue;
					}

					BlockPattern.Result result = testTransform(blockPos, forwards, up, cache);

					if (result != null) {
						return result;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Создаёт кэш состояний блоков для ускорения многократных обращений при проверке шаблона.
	 *
	 * @param world     мир, из которого читаются состояния блоков
	 * @param forceLoad принудительно загружать чанки при обращении
	 * @return кэш, отображающий {@link BlockPos} → {@link CachedBlockPosition}
	 */
	public static LoadingCache<BlockPos, CachedBlockPosition> makeCache(WorldView world, boolean forceLoad) {
		return CacheBuilder.newBuilder().build(new BlockPattern.BlockStateCacheLoader(world, forceLoad));
	}

	/**
	 * Вычисляет абсолютную позицию блока в мире по координатам внутри шаблона.
	 * <p>
	 * Использует векторное произведение {@code forwards × up} для получения оси «влево»,
	 * затем складывает смещения по трём осям шаблона.
	 *
	 * @param pos            опорная позиция (передний верхний левый угол шаблона)
	 * @param forwards       направление «вперёд» шаблона
	 * @param up             направление «вверх» шаблона
	 * @param offsetLeft     смещение по оси «влево» (ширина)
	 * @param offsetDown     смещение по оси «вниз» (высота)
	 * @param offsetForwards смещение по оси «вперёд» (глубина)
	 * @return абсолютная позиция блока в мире
	 * @throws IllegalArgumentException если {@code forwards} и {@code up} коллинеарны
	 */
	protected static BlockPos translate(
		BlockPos pos,
		Direction forwards,
		Direction up,
		int offsetLeft,
		int offsetDown,
		int offsetForwards
	) {
		if (forwards == up || forwards == up.getOpposite()) {
			throw new IllegalArgumentException("Invalid forwards & up combination");
		}

		Vec3i forwardVec = new Vec3i(forwards.getOffsetX(), forwards.getOffsetY(), forwards.getOffsetZ());
		Vec3i upVec = new Vec3i(up.getOffsetX(), up.getOffsetY(), up.getOffsetZ());
		Vec3i leftVec = forwardVec.crossProduct(upVec);

		return pos.add(
			upVec.getX() * -offsetDown + leftVec.getX() * offsetLeft + forwardVec.getX() * offsetForwards,
			upVec.getY() * -offsetDown + leftVec.getY() * offsetLeft + forwardVec.getY() * offsetForwards,
			upVec.getZ() * -offsetDown + leftVec.getZ() * offsetLeft + forwardVec.getZ() * offsetForwards
		);
	}

	/**
	 * Загрузчик кэша, создающий {@link CachedBlockPosition} по запросу для заданной позиции.
	 */
	static class BlockStateCacheLoader extends CacheLoader<BlockPos, CachedBlockPosition> {

		private final WorldView world;
		private final boolean forceLoad;

		public BlockStateCacheLoader(WorldView world, boolean forceLoad) {
			this.world = world;
			this.forceLoad = forceLoad;
		}

		@Override
		public CachedBlockPosition load(BlockPos blockPos) {
			return new CachedBlockPosition(world, blockPos, forceLoad);
		}
	}

	/**
	 * Результат успешного совпадения шаблона с блоками в мире.
	 * <p>
	 * Хранит ориентацию шаблона (позицию переднего верхнего левого угла, направления
	 * {@code forwards} и {@code up}) и кэш состояний блоков для быстрого доступа
	 * через {@link #translate}.
	 */
	public static class Result {

		private final BlockPos frontTopLeft;
		private final Direction forwards;
		private final Direction up;
		private final LoadingCache<BlockPos, CachedBlockPosition> cache;
		private final int width;
		private final int height;
		private final int depth;

		public Result(
			BlockPos frontTopLeft,
			Direction forwards,
			Direction up,
			LoadingCache<BlockPos, CachedBlockPosition> cache,
			int width,
			int height,
			int depth
		) {
			this.frontTopLeft = frontTopLeft;
			this.forwards = forwards;
			this.up = up;
			this.cache = cache;
			this.width = width;
			this.height = height;
			this.depth = depth;
		}

		public BlockPos getFrontTopLeft() {
			return frontTopLeft;
		}

		public Direction getForwards() {
			return forwards;
		}

		public Direction getUp() {
			return up;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public int getDepth() {
			return depth;
		}

		/**
		 * Возвращает кэшированное состояние блока по координатам внутри шаблона.
		 *
		 * @param offsetLeft     смещение по оси «влево»
		 * @param offsetDown     смещение по оси «вниз»
		 * @param offsetForwards смещение по оси «вперёд»
		 * @return кэшированная позиция блока с его состоянием
		 */
		public CachedBlockPosition translate(int offsetLeft, int offsetDown, int offsetForwards) {
			return cache.getUnchecked(
				BlockPattern.translate(frontTopLeft, forwards, up, offsetLeft, offsetDown, offsetForwards)
			);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.add("up", up)
				.add("forwards", forwards)
				.add("frontTopLeft", frontTopLeft)
				.toString();
		}
	}
}
