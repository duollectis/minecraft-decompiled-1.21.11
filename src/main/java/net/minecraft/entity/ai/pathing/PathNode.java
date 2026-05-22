package net.minecraft.entity.ai.pathing;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Узел пути в алгоритме A*.
 * Хранит координаты, метрики пути и ссылку на предыдущий узел для восстановления маршрута.
 */
public class PathNode {

	public final int x;
	public final int y;
	public final int z;
	private final int hashCode;
	public int heapIndex = -1;
	public float penalizedPathLength;
	public float distanceToNearestTarget;
	public float heapWeight;
	public @Nullable PathNode previous;
	public boolean visited;
	public float pathLength;
	public float penalty;
	public PathNodeType type = PathNodeType.BLOCKED;

	public PathNode(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		hashCode = hash(x, y, z);
	}

	/** Создаёт копию узла с новыми координатами, сохраняя все метрики пути. */
	public PathNode copyWithNewPosition(int x, int y, int z) {
		PathNode copy = new PathNode(x, y, z);
		copy.heapIndex = heapIndex;
		copy.penalizedPathLength = penalizedPathLength;
		copy.distanceToNearestTarget = distanceToNearestTarget;
		copy.heapWeight = heapWeight;
		copy.previous = previous;
		copy.visited = visited;
		copy.pathLength = pathLength;
		copy.penalty = penalty;
		copy.type = type;
		return copy;
	}

	/**
	 * Вычисляет хэш-код для координат узла.
	 * Упаковывает x, y, z в одно int-значение с учётом знаков координат.
	 */
	public static int hash(int x, int y, int z) {
		return y & 0xFF
				| (x & 32767) << 8
				| (z & 32767) << 24
				| (x < 0 ? Integer.MIN_VALUE : 0)
				| (z < 0 ? 32768 : 0);
	}

	public float getDistance(PathNode node) {
		float dx = node.x - x;
		float dy = node.y - y;
		float dz = node.z - z;
		return MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public float getHorizontalDistance(PathNode node) {
		float dx = node.x - x;
		float dz = node.z - z;
		return MathHelper.sqrt(dx * dx + dz * dz);
	}

	public float getDistance(BlockPos pos) {
		float dx = pos.getX() - x;
		float dy = pos.getY() - y;
		float dz = pos.getZ() - z;
		return MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public float getSquaredDistance(PathNode node) {
		float dx = node.x - x;
		float dy = node.y - y;
		float dz = node.z - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public float getSquaredDistance(BlockPos pos) {
		float dx = pos.getX() - x;
		float dy = pos.getY() - y;
		float dz = pos.getZ() - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public float getManhattanDistance(PathNode node) {
		float dx = Math.abs(node.x - x);
		float dy = Math.abs(node.y - y);
		float dz = Math.abs(node.z - z);
		return dx + dy + dz;
	}

	public float getManhattanDistance(BlockPos pos) {
		float dx = Math.abs(pos.getX() - x);
		float dy = Math.abs(pos.getY() - y);
		float dz = Math.abs(pos.getZ() - z);
		return dx + dy + dz;
	}

	public BlockPos getBlockPos() {
		return new BlockPos(x, y, z);
	}

	public Vec3d getPos() {
		return new Vec3d(x, y, z);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof PathNode other) {
			return hashCode == other.hashCode && x == other.x && y == other.y && z == other.z;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public boolean isInHeap() {
		return heapIndex >= 0;
	}

	@Override
	public String toString() {
		return "Node{x=" + x + ", y=" + y + ", z=" + z + "}";
	}

	public void write(PacketByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeFloat(pathLength);
		buf.writeFloat(penalty);
		buf.writeBoolean(visited);
		buf.writeEnumConstant(type);
		buf.writeFloat(heapWeight);
	}

	public static PathNode fromBuf(PacketByteBuf buf) {
		PathNode node = new PathNode(buf.readInt(), buf.readInt(), buf.readInt());
		readFromBuf(buf, node);
		return node;
	}

	protected static void readFromBuf(PacketByteBuf buf, PathNode target) {
		target.pathLength = buf.readFloat();
		target.penalty = buf.readFloat();
		target.visited = buf.readBoolean();
		target.type = buf.readEnumConstant(PathNodeType.class);
		target.heapWeight = buf.readFloat();
	}
}
