package net.minecraft.entity.ai.pathing;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkCache;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Построитель узлов пути для водных существ (кальмары, рыбы и т.п.).
 * Разрешает движение только через блоки воды; опционально допускает
 * узлы типа {@link PathNodeType#BREACH} для существ, умеющих выпрыгивать из воды.
 */
public class WaterPathNodeMaker extends PathNodeMaker {

	private static final float AIR_PENALTY = 8.0F;

	private final boolean canJumpOutOfWater;
	private final Long2ObjectMap<PathNodeType> nodePosToType = new Long2ObjectOpenHashMap<>();

	public WaterPathNodeMaker(boolean canJumpOutOfWater) {
		this.canJumpOutOfWater = canJumpOutOfWater;
	}

	@Override
	public void init(ChunkCache cachedWorld, MobEntity entity) {
		super.init(cachedWorld, entity);
		nodePosToType.clear();
	}

	@Override
	public void clear() {
		super.clear();
		nodePosToType.clear();
	}

	@Override
	public PathNode getStart() {
		return getNode(
				MathHelper.floor(entity.getBoundingBox().minX),
				MathHelper.floor(entity.getBoundingBox().minY + 0.5),
				MathHelper.floor(entity.getBoundingBox().minZ)
		);
	}

	@Override
	public TargetPathNode getNode(double x, double y, double z) {
		return createNode(x, y, z);
	}

	@Override
	public int getSuccessors(PathNode[] successors, PathNode node) {
		int count = 0;
		Map<Direction, PathNode> neighborMap = Maps.newEnumMap(Direction.class);

		for (Direction direction : Direction.values()) {
			PathNode neighbor = getPassableNode(
					node.x + direction.getOffsetX(),
					node.y + direction.getOffsetY(),
					node.z + direction.getOffsetZ()
			);
			neighborMap.put(direction, neighbor);

			if (hasNotVisited(neighbor)) {
				successors[count++] = neighbor;
			}
		}

		for (Direction horizontal : Direction.Type.HORIZONTAL) {
			Direction clockwise = horizontal.rotateYClockwise();

			if (hasPenalty(neighborMap.get(horizontal)) && hasPenalty(neighborMap.get(clockwise))) {
				PathNode diagonal = getPassableNode(
						node.x + horizontal.getOffsetX() + clockwise.getOffsetX(),
						node.y,
						node.z + horizontal.getOffsetZ() + clockwise.getOffsetZ()
				);

				if (hasNotVisited(diagonal)) {
					successors[count++] = diagonal;
				}
			}
		}

		return count;
	}

	protected boolean hasNotVisited(@Nullable PathNode node) {
		return node != null && !node.visited;
	}

	private static boolean hasPenalty(@Nullable PathNode node) {
		return node != null && node.penalty >= 0.0F;
	}

	/**
	 * Возвращает проходимый узел в позиции (x, y, z), если тип узла допустим
	 * для данного существа. Добавляет штраф {@link #AIR_PENALTY} за нахождение
	 * в воздушном блоке (тип BREACH без жидкости).
	 */
	protected @Nullable PathNode getPassableNode(int x, int y, int z) {
		PathNodeType nodeType = addPathNodePos(x, y, z);
		boolean isAllowed = (canJumpOutOfWater && nodeType == PathNodeType.BREACH)
				|| nodeType == PathNodeType.WATER;

		if (!isAllowed) {
			return null;
		}

		float penalty = entity.getPathfindingPenalty(nodeType);

		if (penalty < 0.0F) {
			return null;
		}

		PathNode pathNode = getNode(x, y, z);
		pathNode.type = nodeType;
		pathNode.penalty = Math.max(pathNode.penalty, penalty);

		if (context.getWorld().getFluidState(new BlockPos(x, y, z)).isEmpty()) {
			pathNode.penalty += AIR_PENALTY;
		}

		return pathNode;
	}

	protected PathNodeType addPathNodePos(int x, int y, int z) {
		return nodePosToType.computeIfAbsent(
				BlockPos.asLong(x, y, z),
				pos -> getDefaultNodeType(context, x, y, z)
		);
	}

	@Override
	public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
		return getNodeType(context, x, y, z, entity);
	}

	/**
	 * Определяет тип узла с учётом всего объёма существа.
	 * Возвращает {@link PathNodeType#BREACH} для воздушных блоков (выпрыгивание),
	 * {@link PathNodeType#WATER} для водных и {@link PathNodeType#BLOCKED} для остальных.
	 */
	@Override
	public PathNodeType getNodeType(PathContext context, int x, int y, int z, MobEntity mob) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int bx = x; bx < x + entityBlockXSize; bx++) {
			for (int by = y; by < y + entityBlockYSize; by++) {
				for (int bz = z; bz < z + entityBlockZSize; bz++) {
					BlockState blockState = context.getBlockState(mutable.set(bx, by, bz));
					FluidState fluidState = blockState.getFluidState();

					if (fluidState.isEmpty() && blockState.canPathfindThrough(NavigationType.WATER) && blockState.isAir()) {
						return PathNodeType.BREACH;
					}

					if (!fluidState.isIn(FluidTags.WATER)) {
						return PathNodeType.BLOCKED;
					}
				}
			}
		}

		BlockState finalState = context.getBlockState(mutable);
		return finalState.canPathfindThrough(NavigationType.WATER) ? PathNodeType.WATER : PathNodeType.BLOCKED;
	}
}
