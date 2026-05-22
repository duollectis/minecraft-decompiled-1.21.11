package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Маршрут навигации существа — упорядоченный список узлов пути от старта до цели.
 * Хранит текущую позицию в маршруте и отладочную информацию для визуализации.
 */
public final class Path {

	public static final PacketCodec<PacketByteBuf, Path> PACKET_CODEC =
			PacketCodec.ofStatic((buf, path) -> path.toBuf(buf), Path::fromBuf);

	private final List<PathNode> nodes;
	private Path.@Nullable DebugNodeInfo debugNodeInfos;
	private int currentNodeIndex;
	private final BlockPos target;
	private final float manhattanDistanceFromTarget;
	private final boolean reachesTarget;

	public Path(List<PathNode> nodes, BlockPos target, boolean reachesTarget) {
		this.nodes = nodes;
		this.target = target;
		manhattanDistanceFromTarget = nodes.isEmpty()
				? Float.MAX_VALUE
				: nodes.get(nodes.size() - 1).getManhattanDistance(target);
		this.reachesTarget = reachesTarget;
	}

	public void next() {
		currentNodeIndex++;
	}

	public boolean isStart() {
		return currentNodeIndex <= 0;
	}

	public boolean isFinished() {
		return currentNodeIndex >= nodes.size();
	}

	public @Nullable PathNode getEnd() {
		return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
	}

	public PathNode getNode(int index) {
		return nodes.get(index);
	}

	public void setLength(int length) {
		if (nodes.size() > length) {
			nodes.subList(length, nodes.size()).clear();
		}
	}

	public void setNode(int index, PathNode node) {
		nodes.set(index, node);
	}

	public int getLength() {
		return nodes.size();
	}

	public int getCurrentNodeIndex() {
		return currentNodeIndex;
	}

	public void setCurrentNodeIndex(int nodeIndex) {
		currentNodeIndex = nodeIndex;
	}

	/**
	 * Вычисляет мировую позицию узла с учётом ширины существа (центрирование по X/Z).
	 */
	public Vec3d getNodePosition(Entity entity, int index) {
		PathNode node = nodes.get(index);
		double halfWidth = (int) (entity.getWidth() + 1.0F) * 0.5;
		return new Vec3d(node.x + halfWidth, node.y, node.z + halfWidth);
	}

	public BlockPos getNodePos(int index) {
		return nodes.get(index).getBlockPos();
	}

	public Vec3d getNodePosition(Entity entity) {
		return getNodePosition(entity, currentNodeIndex);
	}

	public BlockPos getCurrentNodePos() {
		return nodes.get(currentNodeIndex).getBlockPos();
	}

	public PathNode getCurrentNode() {
		return nodes.get(currentNodeIndex);
	}

	public @Nullable PathNode getLastNode() {
		return currentNodeIndex > 0 ? nodes.get(currentNodeIndex - 1) : null;
	}

	public boolean equalsPath(@Nullable Path path) {
		return path != null && nodes.equals(path.nodes);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Path other) {
			return currentNodeIndex == other.currentNodeIndex
					&& debugNodeInfos == other.debugNodeInfos
					&& reachesTarget == other.reachesTarget
					&& target.equals(other.target)
					&& nodes.equals(other.nodes);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return currentNodeIndex + nodes.hashCode() * 31;
	}

	public boolean reachesTarget() {
		return reachesTarget;
	}

	@Debug
	void setDebugInfo(PathNode[] debugNodes, PathNode[] debugSecondNodes, Set<TargetPathNode> debugTargetNodes) {
		debugNodeInfos = new Path.DebugNodeInfo(debugNodes, debugSecondNodes, debugTargetNodes);
	}

	public Path.@Nullable DebugNodeInfo getDebugNodeInfos() {
		return debugNodeInfos;
	}

	/**
	 * Сериализует путь в буфер пакета. Требует наличия отладочных данных.
	 */
	public void toBuf(PacketByteBuf buf) {
		if (debugNodeInfos == null || debugNodeInfos.targetNodes.isEmpty()) {
			throw new IllegalStateException("Missing debug data");
		}

		buf.writeBoolean(reachesTarget);
		buf.writeInt(currentNodeIndex);
		buf.writeBlockPos(target);
		buf.writeCollection(nodes, (bufx, node) -> node.write(bufx));
		debugNodeInfos.write(buf);
	}

	public static Path fromBuf(PacketByteBuf buf) {
		boolean reachesTarget = buf.readBoolean();
		int nodeIndex = buf.readInt();
		BlockPos targetPos = buf.readBlockPos();
		List<PathNode> nodeList = buf.readList(PathNode::fromBuf);
		Path.DebugNodeInfo debugInfo = Path.DebugNodeInfo.fromBuf(buf);
		Path path = new Path(nodeList, targetPos, reachesTarget);
		path.debugNodeInfos = debugInfo;
		path.currentNodeIndex = nodeIndex;
		return path;
	}

	@Override
	public String toString() {
		return "Path(length=" + nodes.size() + ")";
	}

	public BlockPos getTarget() {
		return target;
	}

	public float getManhattanDistanceFromTarget() {
		return manhattanDistanceFromTarget;
	}

	static PathNode[] nodesFromBuf(PacketByteBuf buf) {
		PathNode[] result = new PathNode[buf.readVarInt()];

		for (int i = 0; i < result.length; i++) {
			result[i] = PathNode.fromBuf(buf);
		}

		return result;
	}

	static void write(PacketByteBuf buf, PathNode[] nodes) {
		buf.writeVarInt(nodes.length);

		for (PathNode node : nodes) {
			node.write(buf);
		}
	}

	public Path copy() {
		Path copy = new Path(nodes, target, reachesTarget);
		copy.debugNodeInfos = debugNodeInfos;
		copy.currentNodeIndex = currentNodeIndex;
		return copy;
	}

	/** Отладочная информация о состоянии алгоритма A* для визуализации пути. */
	public record DebugNodeInfo(PathNode[] openSet, PathNode[] closedSet, Set<TargetPathNode> targetNodes) {

		public void write(PacketByteBuf buf) {
			buf.writeCollection(targetNodes, (bufx, node) -> node.write(bufx));
			Path.write(buf, openSet);
			Path.write(buf, closedSet);
		}

		public static Path.DebugNodeInfo fromBuf(PacketByteBuf buf) {
			HashSet<TargetPathNode> targets = buf.readCollection(HashSet::new, TargetPathNode::fromBuffer);
			PathNode[] openNodes = Path.nodesFromBuf(buf);
			PathNode[] closedNodes = Path.nodesFromBuf(buf);
			return new Path.DebugNodeInfo(openNodes, closedNodes, targets);
		}
	}
}
