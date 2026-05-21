package net.minecraft.entity.ai.pathing;

import net.minecraft.network.PacketByteBuf;

/**
 * {@code TargetPathNode}.
 */
public class TargetPathNode extends PathNode {

	private float nearestNodeDistance = Float.MAX_VALUE;
	private PathNode nearestNode;
	private boolean reached;

	public TargetPathNode(PathNode node) {
		super(node.x, node.y, node.z);
	}

	public TargetPathNode(int i, int j, int k) {
		super(i, j, k);
	}

	/**
	 * Обновляет nearest node.
	 *
	 * @param distance distance
	 * @param node node
	 */
	public void updateNearestNode(float distance, PathNode node) {
		if (distance < this.nearestNodeDistance) {
			this.nearestNodeDistance = distance;
			this.nearestNode = node;
		}
	}

	public PathNode getNearestNode() {
		return this.nearestNode;
	}

	/**
	 * Mark reached.
	 */
	public void markReached() {
		this.reached = true;
	}

	public boolean isReached() {
		return this.reached;
	}

	/**
	 * From buffer.
	 *
	 * @param buffer buffer
	 *
	 * @return TargetPathNode — результат операции
	 */
	public static TargetPathNode fromBuffer(PacketByteBuf buffer) {
		TargetPathNode targetPathNode = new TargetPathNode(buffer.readInt(), buffer.readInt(), buffer.readInt());
		readFromBuf(buffer, targetPathNode);
		return targetPathNode;
	}
}
