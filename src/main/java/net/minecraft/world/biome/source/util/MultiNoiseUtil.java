package net.minecraft.world.biome.source.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Утилиты для многомерного шумового размещения биомов.
 * Биомы описываются 7-мерными гиперкубами в пространстве параметров:
 * температура, влажность, континентальность, эрозия, глубина, странность, смещение.
 */
public class MultiNoiseUtil {

	private static final boolean DEBUG_ENABLED = false;
	private static final float TO_LONG_FACTOR = 10000.0F;

	@VisibleForTesting
	protected static final int HYPERCUBE_DIMENSION = 7;

	public static MultiNoiseUtil.NoiseValuePoint createNoiseValuePoint(
		float temperatureNoise,
		float humidityNoise,
		float continentalnessNoise,
		float erosionNoise,
		float depth,
		float weirdnessNoise
	) {
		return new MultiNoiseUtil.NoiseValuePoint(
			toLong(temperatureNoise),
			toLong(humidityNoise),
			toLong(continentalnessNoise),
			toLong(erosionNoise),
			toLong(depth),
			toLong(weirdnessNoise)
		);
	}

	public static MultiNoiseUtil.NoiseHypercube createNoiseHypercube(
		float temperature,
		float humidity,
		float continentalness,
		float erosion,
		float depth,
		float weirdness,
		float offset
	) {
		return new MultiNoiseUtil.NoiseHypercube(
			MultiNoiseUtil.ParameterRange.of(temperature),
			MultiNoiseUtil.ParameterRange.of(humidity),
			MultiNoiseUtil.ParameterRange.of(continentalness),
			MultiNoiseUtil.ParameterRange.of(erosion),
			MultiNoiseUtil.ParameterRange.of(depth),
			MultiNoiseUtil.ParameterRange.of(weirdness),
			toLong(offset)
		);
	}

	public static MultiNoiseUtil.NoiseHypercube createNoiseHypercube(
		MultiNoiseUtil.ParameterRange temperature,
		MultiNoiseUtil.ParameterRange humidity,
		MultiNoiseUtil.ParameterRange continentalness,
		MultiNoiseUtil.ParameterRange erosion,
		MultiNoiseUtil.ParameterRange depth,
		MultiNoiseUtil.ParameterRange weirdness,
		float offset
	) {
		return new MultiNoiseUtil.NoiseHypercube(
			temperature,
			humidity,
			continentalness,
			erosion,
			depth,
			weirdness,
			toLong(offset)
		);
	}

	/** Переводит float-значение параметра в long для точных целочисленных вычислений расстояний. */
	public static long toLong(float value) {
		return (long) (value * TO_LONG_FACTOR);
	}

	/** Переводит long-значение параметра обратно в float. */
	public static float toFloat(long value) {
		return (float) value / TO_LONG_FACTOR;
	}

	public static MultiNoiseUtil.MultiNoiseSampler createEmptyMultiNoiseSampler() {
		DensityFunction zero = DensityFunctionTypes.zero();
		return new MultiNoiseUtil.MultiNoiseSampler(zero, zero, zero, zero, zero, zero, List.of());
	}

	public static BlockPos findFittestPosition(
		List<MultiNoiseUtil.NoiseHypercube> noises,
		MultiNoiseUtil.MultiNoiseSampler sampler
	) {
		return new MultiNoiseUtil.FittestPositionFinder(noises, sampler).bestResult.location();
	}

	/**
	 * Список записей биомов с их шумовыми параметрами.
	 * Поддерживает как линейный поиск ({@link #getValueSimple}), так и поиск через дерево ({@link #getValue}).
	 */
	public static class Entries<T> {

		private final List<Pair<MultiNoiseUtil.NoiseHypercube, T>> entries;
		private final MultiNoiseUtil.SearchTree<T> tree;

		public static <T> Codec<MultiNoiseUtil.Entries<T>> createCodec(MapCodec<T> entryCodec) {
			return Codecs.nonEmptyList(
				RecordCodecBuilder.<Pair<MultiNoiseUtil.NoiseHypercube, T>>create(
					instance -> instance.group(
						MultiNoiseUtil.NoiseHypercube.CODEC
							.fieldOf("parameters")
							.forGetter((Pair<MultiNoiseUtil.NoiseHypercube, T> pair) -> pair.getFirst()),
						entryCodec.forGetter((Pair<MultiNoiseUtil.NoiseHypercube, T> pair) -> pair.getSecond())
					)
					.apply(instance, Pair::of)
				)
				.listOf()
			)
			.xmap(MultiNoiseUtil.Entries::new, MultiNoiseUtil.Entries::getEntries);
		}

		public Entries(List<Pair<MultiNoiseUtil.NoiseHypercube, T>> entries) {
			this.entries = entries;
			this.tree = MultiNoiseUtil.SearchTree.create(entries);
		}

		public List<Pair<MultiNoiseUtil.NoiseHypercube, T>> getEntries() {
			return entries;
		}

		public T get(MultiNoiseUtil.NoiseValuePoint point) {
			return getValue(point);
		}

		/**
		 * Линейный поиск ближайшего биома — O(n). Используется только в тестах.
		 * В продакшне используется {@link #getValue} через дерево поиска.
		 */
		@VisibleForTesting
		public T getValueSimple(MultiNoiseUtil.NoiseValuePoint point) {
			Iterator<Pair<MultiNoiseUtil.NoiseHypercube, T>> iterator = getEntries().iterator();
			Pair<MultiNoiseUtil.NoiseHypercube, T> first = iterator.next();
			long minDistance = first.getFirst().getSquaredDistance(point);
			T best = first.getSecond();

			while (iterator.hasNext()) {
				Pair<MultiNoiseUtil.NoiseHypercube, T> candidate = iterator.next();
				long distance = candidate.getFirst().getSquaredDistance(point);

				if (distance < minDistance) {
					minDistance = distance;
					best = candidate.getSecond();
				}
			}

			return best;
		}

		public T getValue(MultiNoiseUtil.NoiseValuePoint point) {
			return getValue(point, MultiNoiseUtil.SearchTree.TreeNode::getSquaredDistance);
		}

		protected T getValue(
			MultiNoiseUtil.NoiseValuePoint point,
			MultiNoiseUtil.NodeDistanceFunction<T> distanceFunction
		) {
			return tree.get(point, distanceFunction);
		}
	}

	/**
	 * Алгоритм поиска позиции с наилучшим соответствием шумовым параметрам спавна.
	 * Использует спиральный обход с двумя проходами: грубый (шаг 512) и точный (шаг 32).
	 */
	static class FittestPositionFinder {

		private static final long SEARCH_RANGE = 2048L;

		MultiNoiseUtil.FittestPositionFinder.Result bestResult;

		FittestPositionFinder(List<MultiNoiseUtil.NoiseHypercube> noises, MultiNoiseUtil.MultiNoiseSampler sampler) {
			bestResult = calculateFitness(noises, sampler, 0, 0);
			findFittest(noises, sampler, 2048.0F, 512.0F);
			findFittest(noises, sampler, 512.0F, 32.0F);
		}

		private void findFittest(
			List<MultiNoiseUtil.NoiseHypercube> noises,
			MultiNoiseUtil.MultiNoiseSampler sampler,
			float maxDistance,
			float step
		) {
			float angle = 0.0F;
			float radius = step;
			BlockPos center = bestResult.location();

			while (radius <= maxDistance) {
				int candidateX = center.getX() + (int) (Math.sin(angle) * radius);
				int candidateZ = center.getZ() + (int) (Math.cos(angle) * radius);
				MultiNoiseUtil.FittestPositionFinder.Result candidate = calculateFitness(noises, sampler, candidateX, candidateZ);

				if (candidate.fitness() < bestResult.fitness()) {
					bestResult = candidate;
				}

				angle += step / radius;

				if (angle > Math.PI * 2) {
					angle = 0.0F;
					radius += step;
				}
			}
		}

		private static MultiNoiseUtil.FittestPositionFinder.Result calculateFitness(
			List<MultiNoiseUtil.NoiseHypercube> noises,
			MultiNoiseUtil.MultiNoiseSampler sampler,
			int x,
			int z
		) {
			MultiNoiseUtil.NoiseValuePoint fullPoint =
				sampler.sample(BiomeCoords.fromBlock(x), 0, BiomeCoords.fromBlock(z));

			// Игнорируем глубину при поиске позиции спавна — ищем только по поверхностным параметрам
			MultiNoiseUtil.NoiseValuePoint surfacePoint = new MultiNoiseUtil.NoiseValuePoint(
				fullPoint.temperatureNoise(),
				fullPoint.humidityNoise(),
				fullPoint.continentalnessNoise(),
				fullPoint.erosionNoise(),
				0L,
				fullPoint.weirdnessNoise()
			);

			long minHypercubeDistance = Long.MAX_VALUE;

			for (MultiNoiseUtil.NoiseHypercube hypercube : noises) {
				minHypercubeDistance = Math.min(minHypercubeDistance, hypercube.getSquaredDistance(surfacePoint));
			}

			long distanceFromOriginSquared = MathHelper.square((long) x) + MathHelper.square((long) z);
			// Взвешиваем расстояние от начала координат, чтобы предпочитать позиции ближе к центру
			long fitness = minHypercubeDistance * MathHelper.square(SEARCH_RANGE) + distanceFromOriginSquared;
			return new MultiNoiseUtil.FittestPositionFinder.Result(new BlockPos(x, 0, z), fitness);
		}

		record Result(BlockPos location, long fitness) {
		}
	}

	/**
	 * Сэмплер многомерного шума для определения биома в заданной точке мира.
	 * Каждое поле соответствует одному измерению пространства параметров биомов.
	 */
	public record MultiNoiseSampler(
		DensityFunction temperature,
		DensityFunction humidity,
		DensityFunction continentalness,
		DensityFunction erosion,
		DensityFunction depth,
		DensityFunction weirdness,
		List<MultiNoiseUtil.NoiseHypercube> spawnTarget
	) {

		public MultiNoiseUtil.NoiseValuePoint sample(int x, int y, int z) {
			int blockX = BiomeCoords.toBlock(x);
			int blockY = BiomeCoords.toBlock(y);
			int blockZ = BiomeCoords.toBlock(z);
			DensityFunction.UnblendedNoisePos noisePos = new DensityFunction.UnblendedNoisePos(blockX, blockY, blockZ);
			return MultiNoiseUtil.createNoiseValuePoint(
				(float) temperature.sample(noisePos),
				(float) humidity.sample(noisePos),
				(float) continentalness.sample(noisePos),
				(float) erosion.sample(noisePos),
				(float) depth.sample(noisePos),
				(float) weirdness.sample(noisePos)
			);
		}

		/**
		 * Ищет позицию спавна с наилучшим соответствием целевым шумовым параметрам.
		 * Возвращает {@link BlockPos#ORIGIN}, если список целевых параметров пуст.
		 */
		public BlockPos findBestSpawnPosition() {
			return spawnTarget.isEmpty()
				? BlockPos.ORIGIN
				: MultiNoiseUtil.findFittestPosition(spawnTarget, this);
		}
	}

	interface NodeDistanceFunction<T> {

		long getDistance(MultiNoiseUtil.SearchTree.TreeNode<T> node, long[] otherParameters);
	}

	/**
	 * 7-мерный гиперкуб, описывающий диапазон шумовых параметров для одного биома.
	 * Расстояние до точки вычисляется как сумма квадратов расстояний по каждому измерению.
	 */
	public record NoiseHypercube(
		MultiNoiseUtil.ParameterRange temperature,
		MultiNoiseUtil.ParameterRange humidity,
		MultiNoiseUtil.ParameterRange continentalness,
		MultiNoiseUtil.ParameterRange erosion,
		MultiNoiseUtil.ParameterRange depth,
		MultiNoiseUtil.ParameterRange weirdness,
		long offset
	) {

		public static final Codec<MultiNoiseUtil.NoiseHypercube> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				MultiNoiseUtil.ParameterRange.CODEC.fieldOf("temperature").forGetter(cube -> cube.temperature),
				MultiNoiseUtil.ParameterRange.CODEC.fieldOf("humidity").forGetter(cube -> cube.humidity),
				MultiNoiseUtil.ParameterRange.CODEC.fieldOf("continentalness").forGetter(cube -> cube.continentalness),
				MultiNoiseUtil.ParameterRange.CODEC.fieldOf("erosion").forGetter(cube -> cube.erosion),
				MultiNoiseUtil.ParameterRange.CODEC.fieldOf("depth").forGetter(cube -> cube.depth),
				MultiNoiseUtil.ParameterRange.CODEC.fieldOf("weirdness").forGetter(cube -> cube.weirdness),
				Codec.floatRange(0.0F, 1.0F)
					.fieldOf("offset")
					.xmap(MultiNoiseUtil::toLong, MultiNoiseUtil::toFloat)
					.forGetter(cube -> cube.offset)
			)
			.apply(instance, MultiNoiseUtil.NoiseHypercube::new)
		);

		long getSquaredDistance(MultiNoiseUtil.NoiseValuePoint point) {
			return MathHelper.square(temperature.getDistance(point.temperatureNoise))
				+ MathHelper.square(humidity.getDistance(point.humidityNoise))
				+ MathHelper.square(continentalness.getDistance(point.continentalnessNoise))
				+ MathHelper.square(erosion.getDistance(point.erosionNoise))
				+ MathHelper.square(depth.getDistance(point.depth))
				+ MathHelper.square(weirdness.getDistance(point.weirdnessNoise))
				+ MathHelper.square(offset);
		}

		protected List<MultiNoiseUtil.ParameterRange> getParameters() {
			return ImmutableList.of(
				temperature,
				humidity,
				continentalness,
				erosion,
				depth,
				weirdness,
				new MultiNoiseUtil.ParameterRange(offset, offset)
			);
		}
	}

	/**
	 * Точка в 7-мерном пространстве шумовых параметров, соответствующая конкретной позиции в мире.
	 */
	public record NoiseValuePoint(
		long temperatureNoise,
		long humidityNoise,
		long continentalnessNoise,
		long erosionNoise,
		long depth,
		long weirdnessNoise
	) {

		@VisibleForTesting
		protected long[] getNoiseValueList() {
			return new long[]{
				temperatureNoise,
				humidityNoise,
				continentalnessNoise,
				erosionNoise,
				depth,
				weirdnessNoise,
				0L
			};
		}
	}

	/**
	 * Диапазон значений одного шумового параметра [min, max].
	 * Расстояние до точки равно нулю, если точка внутри диапазона,
	 * иначе — расстоянию до ближайшей границы.
	 */
	public record ParameterRange(long min, long max) {

		public static final Codec<MultiNoiseUtil.ParameterRange> CODEC = Codecs.createCodecForPairObject(
			Codec.floatRange(-2.0F, 2.0F),
			"min",
			"max",
			(min, max) -> min.compareTo(max) > 0
				? DataResult.error(() -> "Cannon construct interval, min > max (" + min + " > " + max + ")")
				: DataResult.success(new MultiNoiseUtil.ParameterRange(
					MultiNoiseUtil.toLong(min),
					MultiNoiseUtil.toLong(max)
				)),
			range -> MultiNoiseUtil.toFloat(range.min()),
			range -> MultiNoiseUtil.toFloat(range.max())
		);

		public static MultiNoiseUtil.ParameterRange of(float point) {
			return of(point, point);
		}

		public static MultiNoiseUtil.ParameterRange of(float min, float max) {
			if (min > max) {
				throw new IllegalArgumentException("min > max: " + min + " " + max);
			}

			return new MultiNoiseUtil.ParameterRange(MultiNoiseUtil.toLong(min), MultiNoiseUtil.toLong(max));
		}

		public static MultiNoiseUtil.ParameterRange combine(
			MultiNoiseUtil.ParameterRange first,
			MultiNoiseUtil.ParameterRange second
		) {
			if (first.min() > second.max()) {
				throw new IllegalArgumentException("min > max: " + first + " " + second);
			}

			return new MultiNoiseUtil.ParameterRange(first.min(), second.max());
		}

		@Override
		public String toString() {
			return min == max
				? String.format(Locale.ROOT, "%d", min)
				: String.format(Locale.ROOT, "[%d-%d]", min, max);
		}

		public long getDistance(long noise) {
			long distanceToMax = noise - max;
			long distanceToMin = min - noise;
			return distanceToMax > 0L ? distanceToMax : Math.max(distanceToMin, 0L);
		}

		public long getDistance(MultiNoiseUtil.ParameterRange other) {
			long distanceToMax = other.min() - max;
			long distanceToMin = min - other.max();
			return distanceToMax > 0L ? distanceToMax : Math.max(distanceToMin, 0L);
		}

		public MultiNoiseUtil.ParameterRange combine(MultiNoiseUtil.@Nullable ParameterRange other) {
			return other == null
				? this
				: new MultiNoiseUtil.ParameterRange(Math.min(min, other.min()), Math.max(max, other.max()));
		}
	}

	/**
	 * KD-дерево для эффективного поиска ближайшего биома в 7-мерном пространстве параметров.
	 * Узлы дерева хранят охватывающие диапазоны параметров для быстрого отсечения ветвей.
	 */
	protected static final class SearchTree<T> {

		private static final int MAX_NODES_FOR_SIMPLE_TREE = 6;

		private final MultiNoiseUtil.SearchTree.TreeNode<T> firstNode;
		private final ThreadLocal<MultiNoiseUtil.SearchTree.@Nullable TreeLeafNode<T>> previousResultNode =
			new ThreadLocal<>();

		private SearchTree(MultiNoiseUtil.SearchTree.TreeNode<T> firstNode) {
			this.firstNode = firstNode;
		}

		/**
		 * Строит дерево поиска из списка записей биомов.
		 * Требует ровно 7 параметров на запись (размерность гиперкуба).
		 */
		public static <T> MultiNoiseUtil.SearchTree<T> create(List<Pair<MultiNoiseUtil.NoiseHypercube, T>> entries) {
			if (entries.isEmpty()) {
				throw new IllegalArgumentException("Need at least one value to build the search tree.");
			}

			int paramCount = entries.get(0).getFirst().getParameters().size();

			if (paramCount != HYPERCUBE_DIMENSION) {
				throw new IllegalStateException("Expecting parameter space to be 7, got " + paramCount);
			}

			List<MultiNoiseUtil.SearchTree.TreeLeafNode<T>> leaves = entries.stream()
				.map(entry -> new MultiNoiseUtil.SearchTree.TreeLeafNode<>(entry.getFirst(), entry.getSecond()))
				.collect(Collectors.toCollection(ArrayList::new));

			return new MultiNoiseUtil.SearchTree<>(createNode(paramCount, leaves));
		}

		private static <T> MultiNoiseUtil.SearchTree.TreeNode<T> createNode(
			int paramCount,
			List<? extends MultiNoiseUtil.SearchTree.TreeNode<T>> subTree
		) {
			if (subTree.isEmpty()) {
				throw new IllegalStateException("Need at least one child to build a node");
			}

			if (subTree.size() == 1) {
				return subTree.get(0);
			}

			if (subTree.size() <= MAX_NODES_FOR_SIMPLE_TREE) {
				// Для малых поддеревьев сортируем по сумме абсолютных значений центров диапазонов
				subTree.sort(Comparator.comparingLong(node -> {
					long sum = 0L;

					for (int dim = 0; dim < paramCount; dim++) {
						MultiNoiseUtil.ParameterRange range = node.parameters[dim];
						sum += Math.abs((range.min() + range.max()) / 2L);
					}

					return sum;
				}));
				return new MultiNoiseUtil.SearchTree.TreeBranchNode<>(subTree);
			}

			// Для больших поддеревьев выбираем ось разбиения с минимальной суммой длин диапазонов
			long minRangeSum = Long.MAX_VALUE;
			int bestAxis = -1;
			List<MultiNoiseUtil.SearchTree.TreeBranchNode<T>> bestBatches = null;

			for (int axis = 0; axis < paramCount; axis++) {
				sortTree(subTree, paramCount, axis, false);
				List<MultiNoiseUtil.SearchTree.TreeBranchNode<T>> batches = getBatchedTree(subTree);
				long rangeSum = 0L;

				for (MultiNoiseUtil.SearchTree.TreeBranchNode<T> batch : batches) {
					rangeSum += getRangeLengthSum(batch.parameters);
				}

				if (minRangeSum > rangeSum) {
					minRangeSum = rangeSum;
					bestAxis = axis;
					bestBatches = batches;
				}
			}

			sortTree(bestBatches, paramCount, bestAxis, true);
			return new MultiNoiseUtil.SearchTree.TreeBranchNode<>(
				bestBatches
					.stream()
					.map(batch -> createNode(paramCount, Arrays.asList(batch.subTree)))
					.collect(Collectors.toList())
			);
		}

		private static <T> void sortTree(
			List<? extends MultiNoiseUtil.SearchTree.TreeNode<T>> subTree,
			int paramCount,
			int primaryAxis,
			boolean useAbsoluteValue
		) {
			Comparator<MultiNoiseUtil.SearchTree.TreeNode<T>> comparator =
				createNodeComparator(primaryAxis, useAbsoluteValue);

			for (int offset = 1; offset < paramCount; offset++) {
				comparator = comparator.thenComparing(
					createNodeComparator((primaryAxis + offset) % paramCount, useAbsoluteValue)
				);
			}

			subTree.sort(comparator);
		}

		private static <T> Comparator<MultiNoiseUtil.SearchTree.TreeNode<T>> createNodeComparator(
			int axis,
			boolean useAbsoluteValue
		) {
			return Comparator.comparingLong(node -> {
				MultiNoiseUtil.ParameterRange range = node.parameters[axis];
				long center = (range.min() + range.max()) / 2L;
				return useAbsoluteValue ? Math.abs(center) : center;
			});
		}

		private static <T> List<MultiNoiseUtil.SearchTree.TreeBranchNode<T>> getBatchedTree(
			List<? extends MultiNoiseUtil.SearchTree.TreeNode<T>> nodes
		) {
			List<MultiNoiseUtil.SearchTree.TreeBranchNode<T>> batches = Lists.newArrayList();
			List<MultiNoiseUtil.SearchTree.TreeNode<T>> currentBatch = Lists.newArrayList();
			int batchSize = (int) Math.pow(
				MAX_NODES_FOR_SIMPLE_TREE,
				Math.floor(Math.log(nodes.size() - 0.01) / Math.log(MAX_NODES_FOR_SIMPLE_TREE))
			);

			for (MultiNoiseUtil.SearchTree.TreeNode<T> node : nodes) {
				currentBatch.add(node);

				if (currentBatch.size() >= batchSize) {
					batches.add(new MultiNoiseUtil.SearchTree.TreeBranchNode<>(currentBatch));
					currentBatch = Lists.newArrayList();
				}
			}

			if (!currentBatch.isEmpty()) {
				batches.add(new MultiNoiseUtil.SearchTree.TreeBranchNode<>(currentBatch));
			}

			return batches;
		}

		private static long getRangeLengthSum(MultiNoiseUtil.ParameterRange[] parameters) {
			long sum = 0L;

			for (MultiNoiseUtil.ParameterRange range : parameters) {
				sum += Math.abs(range.max() - range.min());
			}

			return sum;
		}

		static <T> List<MultiNoiseUtil.ParameterRange> getEnclosingParameters(
			List<? extends MultiNoiseUtil.SearchTree.TreeNode<T>> subTree
		) {
			if (subTree.isEmpty()) {
				throw new IllegalArgumentException("SubTree needs at least one child");
			}

			List<MultiNoiseUtil.ParameterRange> enclosing = Lists.newArrayList();

			for (int dim = 0; dim < HYPERCUBE_DIMENSION; dim++) {
				enclosing.add(null);
			}

			for (MultiNoiseUtil.SearchTree.TreeNode<T> node : subTree) {
				for (int dim = 0; dim < HYPERCUBE_DIMENSION; dim++) {
					enclosing.set(dim, node.parameters[dim].combine(enclosing.get(dim)));
				}
			}

			return enclosing;
		}

		public T get(MultiNoiseUtil.NoiseValuePoint point, MultiNoiseUtil.NodeDistanceFunction<T> distanceFunction) {
			long[] noiseValues = point.getNoiseValueList();
			MultiNoiseUtil.SearchTree.TreeLeafNode<T> result =
				firstNode.getResultingNode(noiseValues, previousResultNode.get(), distanceFunction);
			previousResultNode.set(result);
			return result.value;
		}

		static final class TreeBranchNode<T> extends MultiNoiseUtil.SearchTree.TreeNode<T> {

			final MultiNoiseUtil.SearchTree.TreeNode<T>[] subTree;

			protected TreeBranchNode(List<? extends MultiNoiseUtil.SearchTree.TreeNode<T>> list) {
				this(MultiNoiseUtil.SearchTree.getEnclosingParameters(list), list);
			}

			protected TreeBranchNode(
				List<MultiNoiseUtil.ParameterRange> parameters,
				List<? extends MultiNoiseUtil.SearchTree.TreeNode<T>> subTree
			) {
				super(parameters);
				this.subTree = subTree.toArray(new MultiNoiseUtil.SearchTree.TreeNode[0]);
			}

			@Override
			protected MultiNoiseUtil.SearchTree.TreeLeafNode<T> getResultingNode(
				long[] otherParameters,
				MultiNoiseUtil.SearchTree.@Nullable TreeLeafNode<T> alternative,
				MultiNoiseUtil.NodeDistanceFunction<T> distanceFunction
			) {
				long bestDistance = alternative == null
					? Long.MAX_VALUE
					: distanceFunction.getDistance(alternative, otherParameters);
				MultiNoiseUtil.SearchTree.TreeLeafNode<T> bestLeaf = alternative;

				for (MultiNoiseUtil.SearchTree.TreeNode<T> child : subTree) {
					long childDistance = distanceFunction.getDistance(child, otherParameters);

					if (bestDistance > childDistance) {
						MultiNoiseUtil.SearchTree.TreeLeafNode<T> childLeaf =
							child.getResultingNode(otherParameters, bestLeaf, distanceFunction);
						long leafDistance = child == childLeaf
							? childDistance
							: distanceFunction.getDistance(childLeaf, otherParameters);

						if (bestDistance > leafDistance) {
							bestDistance = leafDistance;
							bestLeaf = childLeaf;
						}
					}
				}

				return bestLeaf;
			}
		}

		static final class TreeLeafNode<T> extends MultiNoiseUtil.SearchTree.TreeNode<T> {

			final T value;

			TreeLeafNode(MultiNoiseUtil.NoiseHypercube parameters, T value) {
				super(parameters.getParameters());
				this.value = value;
			}

			@Override
			protected MultiNoiseUtil.SearchTree.TreeLeafNode<T> getResultingNode(
				long[] otherParameters,
				MultiNoiseUtil.SearchTree.@Nullable TreeLeafNode<T> alternative,
				MultiNoiseUtil.NodeDistanceFunction<T> distanceFunction
			) {
				return this;
			}
		}

		abstract static class TreeNode<T> {

			protected final MultiNoiseUtil.ParameterRange[] parameters;

			protected TreeNode(List<MultiNoiseUtil.ParameterRange> parameters) {
				this.parameters = parameters.toArray(new MultiNoiseUtil.ParameterRange[0]);
			}

			protected abstract MultiNoiseUtil.SearchTree.TreeLeafNode<T> getResultingNode(
				long[] otherParameters,
				MultiNoiseUtil.SearchTree.@Nullable TreeLeafNode<T> alternative,
				MultiNoiseUtil.NodeDistanceFunction<T> distanceFunction
			);

			protected long getSquaredDistance(long[] otherParameters) {
				long sum = 0L;

				for (int dim = 0; dim < HYPERCUBE_DIMENSION; dim++) {
					sum += MathHelper.square(parameters[dim].getDistance(otherParameters[dim]));
				}

				return sum;
			}

			@Override
			public String toString() {
				return Arrays.toString((Object[]) parameters);
			}
		}
	}
}
