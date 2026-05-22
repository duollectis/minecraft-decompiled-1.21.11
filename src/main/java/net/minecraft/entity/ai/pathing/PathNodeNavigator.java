package net.minecraft.entity.ai.pathing;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.SampleType;
import net.minecraft.world.chunk.ChunkCache;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Реализация алгоритма A* для поиска пути к одной или нескольким целевым позициям.
 * Использует {@link PathMinHeap} как приоритетную очередь открытых узлов.
 */
public class PathNodeNavigator {

	private static final float TARGET_DISTANCE_MULTIPLIER = 1.5F;

	private final PathNode[] successors = new PathNode[32];
	private int range;
	private final PathNodeMaker pathNodeMaker;
	private final PathMinHeap minHeap = new PathMinHeap();
	private BooleanSupplier shouldSendDebugData = () -> false;

	public PathNodeNavigator(PathNodeMaker pathNodeMaker, int range) {
		this.pathNodeMaker = pathNodeMaker;
		this.range = range;
	}

	public void setShouldSendDebugData(BooleanSupplier shouldSendDebugData) {
		this.shouldSendDebugData = shouldSendDebugData;
	}

	public void setRange(int range) {
		this.range = range;
	}

	/**
	 * Ищет путь от позиции существа к ближайшей из заданных целевых позиций.
	 *
	 * @param world кэш чанков для чтения блоков
	 * @param mob существо-навигатор
	 * @param positions множество целевых позиций
	 * @param followRange максимальная дальность следования
	 * @param distance допустимое расстояние до цели
	 * @param rangeMultiplier множитель диапазона поиска
	 * @return найденный путь или {@code null}
	 */
	public @Nullable Path findPathToAny(
			ChunkCache world,
			MobEntity mob,
			Set<BlockPos> positions,
			float followRange,
			int distance,
			float rangeMultiplier
	) {
		minHeap.clear();
		pathNodeMaker.init(world, mob);
		PathNode startNode = pathNodeMaker.getStart();

		if (startNode == null) {
			return null;
		}

		Map<TargetPathNode, BlockPos> targetMap = positions.stream()
				.collect(Collectors.toMap(
						pos -> pathNodeMaker.getNode((double) pos.getX(), (double) pos.getY(), (double) pos.getZ()),
						Function.identity()
				));

		Path path = findPathToAny(startNode, targetMap, followRange, distance, rangeMultiplier);
		pathNodeMaker.clear();
		return path;
	}

	private @Nullable Path findPathToAny(
			PathNode startNode,
			Map<TargetPathNode, BlockPos> positions,
			float followRange,
			int distance,
			float rangeMultiplier
	) {
		Profiler profiler = Profilers.get();
		profiler.push("find_path");
		profiler.markSampleType(SampleType.PATH_FINDING);

		Set<TargetPathNode> targets = positions.keySet();
		startNode.penalizedPathLength = 0.0F;
		startNode.distanceToNearestTarget = calculateDistances(startNode, targets);
		startNode.heapWeight = startNode.distanceToNearestTarget;
		minHeap.clear();
		minHeap.push(startNode);

		boolean sendDebug = shouldSendDebugData.getAsBoolean();
		Set<PathNode> visitedNodes = sendDebug ? new HashSet<>() : Set.of();
		int iterations = 0;
		Set<TargetPathNode> reachedTargets = Sets.newHashSetWithExpectedSize(targets.size());
		int maxIterations = (int) (range * rangeMultiplier);

		while (!minHeap.isEmpty()) {
			if (++iterations >= maxIterations) {
				break;
			}

			PathNode current = minHeap.pop();
			current.visited = true;

			for (TargetPathNode target : targets) {
				if (current.getManhattanDistance(target) <= distance) {
					target.markReached();
					reachedTargets.add(target);
				}
			}

			if (!reachedTargets.isEmpty()) {
				break;
			}

			if (sendDebug) {
				visitedNodes.add(current);
			}

			if (current.getDistance(startNode) < followRange) {
				int successorCount = pathNodeMaker.getSuccessors(successors, current);

				for (int idx = 0; idx < successorCount; idx++) {
					PathNode successor = successors[idx];
					float stepCost = getDistance(current, successor);
					successor.pathLength = current.pathLength + stepCost;
					float penalizedLength = current.penalizedPathLength + stepCost + successor.penalty;

					if (successor.pathLength < followRange
							&& (!successor.isInHeap() || penalizedLength < successor.penalizedPathLength)) {
						successor.previous = current;
						successor.penalizedPathLength = penalizedLength;
						successor.distanceToNearestTarget = calculateDistances(successor, targets) * TARGET_DISTANCE_MULTIPLIER;

						if (successor.isInHeap()) {
							minHeap.setNodeWeight(
									successor,
									successor.penalizedPathLength + successor.distanceToNearestTarget
							);
						} else {
							successor.heapWeight = successor.penalizedPathLength + successor.distanceToNearestTarget;
							minHeap.push(successor);
						}
					}
				}
			}
		}

		Optional<Path> result = reachedTargets.isEmpty()
				? targets.stream()
				.map(node -> createPath(node.getNearestNode(), positions.get(node), false))
				.min(Comparator.comparingDouble(Path::getManhattanDistanceFromTarget)
						.thenComparingInt(Path::getLength))
				: reachedTargets.stream()
				.map(node -> createPath(node.getNearestNode(), positions.get(node), true))
				.min(Comparator.comparingInt(Path::getLength));

		profiler.pop();

		if (result.isEmpty()) {
			return null;
		}

		Path path = result.get();

		if (sendDebug) {
			path.setDebugInfo(minHeap.getNodes(), visitedNodes.toArray(PathNode[]::new), targets);
		}

		return path;
	}

	protected float getDistance(PathNode a, PathNode b) {
		return a.getDistance(b);
	}

	private float calculateDistances(PathNode node, Set<TargetPathNode> targets) {
		float minDistance = Float.MAX_VALUE;

		for (TargetPathNode target : targets) {
			float distance = node.getDistance(target);
			target.updateNearestNode(distance, node);
			minDistance = Math.min(distance, minDistance);
		}

		return minDistance;
	}

	private Path createPath(PathNode endNode, BlockPos target, boolean reachesTarget) {
		List<PathNode> nodes = Lists.newArrayList();
		PathNode current = endNode;
		nodes.add(0, endNode);

		while (current.previous != null) {
			current = current.previous;
			nodes.add(0, current);
		}

		return new Path(nodes, target, reachesTarget);
	}
}
