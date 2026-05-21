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
 * {@code Path}.
 */
public final class Path {

	public static final PacketCodec<PacketByteBuf, Path>
			PACKET_CODEC =
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
		this.manhattanDistanceFromTarget =
				nodes.isEmpty() ? Float.MAX_VALUE
				                : this.nodes.get(this.nodes.size() - 1).getManhattanDistance(this.target);
		this.reachesTarget = reachesTarget;
	}

	/**
	 * Next.
	 */
	public void next() {
		this.currentNodeIndex++;
	}

	public boolean isStart() {
		return this.currentNodeIndex <= 0;
	}

	public boolean isFinished() {
		return this.currentNodeIndex >= this.nodes.size();
	}

	public @Nullable PathNode getEnd() {
		return !this.nodes.isEmpty() ? this.nodes.get(this.nodes.size() - 1) : null;
	}

	public PathNode getNode(int index) {
		return this.nodes.get(index);
	}

	public void setLength(int length) {
		if (this.nodes.size() > length) {
			this.nodes.subList(length, this.nodes.size()).clear();
		}
	}

	public void setNode(int index, PathNode node) {
		this.nodes.set(index, node);
	}

	public int getLength() {
		return this.nodes.size();
	}

	public int getCurrentNodeIndex() {
		return this.currentNodeIndex;
	}

	public void setCurrentNodeIndex(int nodeIndex) {
		this.currentNodeIndex = nodeIndex;
	}

	public Vec3d getNodePosition(Entity entity, int index) {
		PathNode pathNode = this.nodes.get(index);
		double d = pathNode.x + (int) (entity.getWidth() + 1.0F) * 0.5;
		double e = pathNode.y;
		double f = pathNode.z + (int) (entity.getWidth() + 1.0F) * 0.5;
		return new Vec3d(d, e, f);
	}

	public BlockPos getNodePos(int index) {
		return this.nodes.get(index).getBlockPos();
	}

	public Vec3d getNodePosition(Entity entity) {
		return this.getNodePosition(entity, this.currentNodeIndex);
	}

	public BlockPos getCurrentNodePos() {
		return this.nodes.get(this.currentNodeIndex).getBlockPos();
	}

	public PathNode getCurrentNode() {
		return this.nodes.get(this.currentNodeIndex);
	}

	public @Nullable PathNode getLastNode() {
		return this.currentNodeIndex > 0 ? this.nodes.get(this.currentNodeIndex - 1) : null;
	}

	/**
	 * Проверяет равенство с path.
	 *
	 * @param path path
	 *
	 * @return boolean — результат операции
	 */
	public boolean equalsPath(@Nullable Path path) {
		return path != null && this.nodes.equals(path.nodes);
	}

	@Override
	public boolean equals(Object o) {
		return !(o instanceof Path path)
		       ? false
		       : this.currentNodeIndex == path.currentNodeIndex
		         && this.debugNodeInfos == path.debugNodeInfos
		         && this.reachesTarget == path.reachesTarget
		         && this.target.equals(path.target)
		         && this.nodes.equals(path.nodes);
	}

	@Override
	public int hashCode() {
		return this.currentNodeIndex + this.nodes.hashCode() * 31;
	}

	/**
	 * Reaches target.
	 *
	 * @return boolean — результат операции
	 */
	public boolean reachesTarget() {
		return this.reachesTarget;
	}

	@Debug
	void setDebugInfo(PathNode[] debugNodes, PathNode[] debugSecondNodes, Set<TargetPathNode> debugTargetNodes) {
		this.debugNodeInfos = new Path.DebugNodeInfo(debugNodes, debugSecondNodes, debugTargetNodes);
	}

	public Path.@Nullable DebugNodeInfo getDebugNodeInfos() {
		return this.debugNodeInfos;
	}

	/**
	 * To buf.
	 *
	 * @param buf buf
	 */
	public void toBuf(PacketByteBuf buf) {
		if (this.debugNodeInfos != null && !this.debugNodeInfos.targetNodes.isEmpty()) {
			buf.writeBoolean(this.reachesTarget);
			buf.writeInt(this.currentNodeIndex);
			buf.writeBlockPos(this.target);
			buf.writeCollection(this.nodes, (bufx, node) -> node.write(bufx));
			this.debugNodeInfos.write(buf);
		}
		else {
			throw new IllegalStateException("Missing debug data");
		}
	}

	/**
	 * From buf.
	 *
	 * @param buf buf
	 *
	 * @return Path — результат операции
	 */
	public static Path fromBuf(PacketByteBuf buf) {
		boolean bl = buf.readBoolean();
		int i = buf.readInt();
		BlockPos blockPos = buf.readBlockPos();
		List<PathNode> list = buf.readList(PathNode::fromBuf);
		Path.DebugNodeInfo debugNodeInfo = Path.DebugNodeInfo.fromBuf(buf);
		Path path = new Path(list, blockPos, bl);
		path.debugNodeInfos = debugNodeInfo;
		path.currentNodeIndex = i;
		return path;
	}

	@Override
	public String toString() {
		return "Path(length=" + this.nodes.size() + ")";
	}

	public BlockPos getTarget() {
		return this.target;
	}

	public float getManhattanDistanceFromTarget() {
		return this.manhattanDistanceFromTarget;
	}

	static PathNode[] nodesFromBuf(PacketByteBuf buf) {
		PathNode[] pathNodes = new PathNode[buf.readVarInt()];

		for (int i = 0; i < pathNodes.length; i++) {
			pathNodes[i] = PathNode.fromBuf(buf);
		}

		return pathNodes;
	}

	static void write(PacketByteBuf buf, PathNode[] nodes) {
		buf.writeVarInt(nodes.length);

		for (PathNode pathNode : nodes) {
			pathNode.write(buf);
		}
	}

	/**
	 * Copy.
	 *
	 * @return Path — результат операции
	 */
	public Path copy() {
		Path path = new Path(this.nodes, this.target, this.reachesTarget);
		path.debugNodeInfos = this.debugNodeInfos;
		path.currentNodeIndex = this.currentNodeIndex;
		return path;
	}

	/**
	 * {@code DebugNodeInfo}.
	 */
	public record DebugNodeInfo(PathNode[] openSet, PathNode[] closedSet, Set<TargetPathNode> targetNodes) {

		/**
		 * Write.
		 *
		 * @param buf buf
		 */
		public void write(PacketByteBuf buf) {
			buf.writeCollection(this.targetNodes, (bufx, node) -> node.write(bufx));
			Path.write(buf, this.openSet);
			Path.write(buf, this.closedSet);
		}

		public static Path.DebugNodeInfo fromBuf(PacketByteBuf buf) {
			HashSet<TargetPathNode> hashSet = buf.readCollection(HashSet::new, TargetPathNode::fromBuffer);
			PathNode[] pathNodes = Path.nodesFromBuf(buf);
			PathNode[] pathNodes2 = Path.nodesFromBuf(buf);
			return new Path.DebugNodeInfo(pathNodes, pathNodes2, hashSet);
		}
	}
}
