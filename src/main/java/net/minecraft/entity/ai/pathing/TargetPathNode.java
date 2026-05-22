package net.minecraft.entity.ai.pathing;

import net.minecraft.network.PacketByteBuf;

/**
 * Целевой узел пути в алгоритме A*.
 * Отслеживает ближайший достигнутый узел и факт достижения цели.
 */
public class TargetPathNode extends PathNode {

	private float nearestNodeDistance = Float.MAX_VALUE;
	private PathNode nearestNode;
	private boolean reached;

	public TargetPathNode(PathNode node) {
		super(node.x, node.y, node.z);
	}

	public TargetPathNode(int x, int y, int z) {
		super(x, y, z);
	}

	/**
	 * Обновляет ближайший узел, если переданное расстояние меньше текущего минимума.
	 */
	public void updateNearestNode(float distance, PathNode node) {
		if (distance < nearestNodeDistance) {
			nearestNodeDistance = distance;
			nearestNode = node;
		}
	}

	public PathNode getNearestNode() {
		return nearestNode;
	}

	public void markReached() {
		reached = true;
	}

	public boolean isReached() {
		return reached;
	}

	public static TargetPathNode fromBuffer(PacketByteBuf buffer) {
		TargetPathNode node = new TargetPathNode(buffer.readInt(), buffer.readInt(), buffer.readInt());
		readFromBuf(buffer, node);
		return node;
	}
}
