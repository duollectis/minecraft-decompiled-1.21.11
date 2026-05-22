package net.minecraft.entity.ai.pathing;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkCache;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Построитель узлов пути для летающих существ.
 * Поддерживает движение во всех 26 направлениях (включая диагонали по Y).
 * Маленькие существа используют случайные позиции для выхода из застрявшего состояния.
 */
public class BirdPathNodeMaker extends LandPathNodeMaker {

	private static final float SMALL_ENTITY_SIZE_THRESHOLD = 1.0F;
	private static final float ESCAPE_BOX_EXPANSION = 1.1F;
	private static final int ESCAPE_RANDOM_POSITIONS_COUNT = 10;

	private final Long2ObjectMap<PathNodeType> pathNodes = new Long2ObjectOpenHashMap<>();

	@Override
	public void init(ChunkCache cachedWorld, MobEntity entity) {
		super.init(cachedWorld, entity);
		pathNodes.clear();
		entity.onStartPathfinding();
	}

	@Override
	public void clear() {
		entity.onFinishPathfinding();
		pathNodes.clear();
		super.clear();
	}

	@Override
	public PathNode getStart() {
		int startY;

		if (canSwim() && entity.isTouchingWater()) {
			startY = entity.getBlockY();
			BlockPos.Mutable mutable = new BlockPos.Mutable(entity.getX(), (double) startY, entity.getZ());

			for (BlockState blockState = context.getBlockState(mutable);
				 blockState.isOf(Blocks.WATER);
				 blockState = context.getBlockState(mutable)) {
				mutable.set(entity.getX(), (double) (++startY), entity.getZ());
			}
		} else {
			startY = MathHelper.floor(entity.getY() + 0.5);
		}

		BlockPos startPos = BlockPos.ofFloored(entity.getX(), startY, entity.getZ());

		if (!canPathThrough(startPos)) {
			for (BlockPos escapePos : getPotentialEscapePositions(entity)) {
				if (canPathThrough(escapePos)) {
					return super.getStart(escapePos);
				}
			}
		}

		return super.getStart(startPos);
	}

	@Override
	protected boolean canPathThrough(BlockPos pos) {
		PathNodeType nodeType = getNodeType(pos.getX(), pos.getY(), pos.getZ());
		return entity.getPathfindingPenalty(nodeType) >= 0.0F;
	}

	@Override
	public TargetPathNode getNode(double x, double y, double z) {
		return createNode(x, y, z);
	}

	/**
	 * Генерирует преемников во всех 26 направлениях (6 осевых + 12 плоских диагоналей + 8 объёмных диагоналей).
	 * Диагональные переходы разрешены только если оба промежуточных узла проходимы.
	 */
	@Override
	public int getSuccessors(PathNode[] successors, PathNode node) {
		int count = 0;

		// 6 осевых направлений
		PathNode south = getPassableNode(node.x, node.y, node.z + 1);
		PathNode west = getPassableNode(node.x - 1, node.y, node.z);
		PathNode east = getPassableNode(node.x + 1, node.y, node.z);
		PathNode north = getPassableNode(node.x, node.y, node.z - 1);
		PathNode up = getPassableNode(node.x, node.y + 1, node.z);
		PathNode down = getPassableNode(node.x, node.y - 1, node.z);

		if (unvisited(south)) { successors[count++] = south; }
		if (unvisited(west)) { successors[count++] = west; }
		if (unvisited(east)) { successors[count++] = east; }
		if (unvisited(north)) { successors[count++] = north; }
		if (unvisited(up)) { successors[count++] = up; }
		if (unvisited(down)) { successors[count++] = down; }

		// 12 плоских и вертикальных диагоналей
		PathNode upSouth = getPassableNode(node.x, node.y + 1, node.z + 1);
		PathNode upWest = getPassableNode(node.x - 1, node.y + 1, node.z);
		PathNode upEast = getPassableNode(node.x + 1, node.y + 1, node.z);
		PathNode upNorth = getPassableNode(node.x, node.y + 1, node.z - 1);
		PathNode downSouth = getPassableNode(node.x, node.y - 1, node.z + 1);
		PathNode downWest = getPassableNode(node.x - 1, node.y - 1, node.z);
		PathNode downEast = getPassableNode(node.x + 1, node.y - 1, node.z);
		PathNode downNorth = getPassableNode(node.x, node.y - 1, node.z - 1);
		PathNode eastNorth = getPassableNode(node.x + 1, node.y, node.z - 1);
		PathNode eastSouth = getPassableNode(node.x + 1, node.y, node.z + 1);
		PathNode westNorth = getPassableNode(node.x - 1, node.y, node.z - 1);
		PathNode westSouth = getPassableNode(node.x - 1, node.y, node.z + 1);

		if (unvisited(upSouth) && isPassable(south) && isPassable(up)) { successors[count++] = upSouth; }
		if (unvisited(upWest) && isPassable(west) && isPassable(up)) { successors[count++] = upWest; }
		if (unvisited(upEast) && isPassable(east) && isPassable(up)) { successors[count++] = upEast; }
		if (unvisited(upNorth) && isPassable(north) && isPassable(up)) { successors[count++] = upNorth; }
		if (unvisited(downSouth) && isPassable(south) && isPassable(down)) { successors[count++] = downSouth; }
		if (unvisited(downWest) && isPassable(west) && isPassable(down)) { successors[count++] = downWest; }
		if (unvisited(downEast) && isPassable(east) && isPassable(down)) { successors[count++] = downEast; }
		if (unvisited(downNorth) && isPassable(north) && isPassable(down)) { successors[count++] = downNorth; }
		if (unvisited(eastNorth) && isPassable(north) && isPassable(east)) { successors[count++] = eastNorth; }
		if (unvisited(eastSouth) && isPassable(south) && isPassable(east)) { successors[count++] = eastSouth; }
		if (unvisited(westNorth) && isPassable(north) && isPassable(west)) { successors[count++] = westNorth; }
		if (unvisited(westSouth) && isPassable(south) && isPassable(west)) { successors[count++] = westSouth; }

		// 8 объёмных диагоналей
		PathNode upEastNorth = getPassableNode(node.x + 1, node.y + 1, node.z - 1);
		PathNode upEastSouth = getPassableNode(node.x + 1, node.y + 1, node.z + 1);
		PathNode upWestNorth = getPassableNode(node.x - 1, node.y + 1, node.z - 1);
		PathNode upWestSouth = getPassableNode(node.x - 1, node.y + 1, node.z + 1);
		PathNode downEastNorth = getPassableNode(node.x + 1, node.y - 1, node.z - 1);
		PathNode downEastSouth = getPassableNode(node.x + 1, node.y - 1, node.z + 1);
		PathNode downWestNorth = getPassableNode(node.x - 1, node.y - 1, node.z - 1);
		PathNode downWestSouth = getPassableNode(node.x - 1, node.y - 1, node.z + 1);

		if (unvisited(upEastNorth) && isPassable(eastNorth) && isPassable(north) && isPassable(east) && isPassable(up) && isPassable(upNorth) && isPassable(upEast)) { successors[count++] = upEastNorth; }
		if (unvisited(upEastSouth) && isPassable(eastSouth) && isPassable(south) && isPassable(east) && isPassable(up) && isPassable(upSouth) && isPassable(upEast)) { successors[count++] = upEastSouth; }
		if (unvisited(upWestNorth) && isPassable(westNorth) && isPassable(north) && isPassable(west) && isPassable(up) && isPassable(upNorth) && isPassable(upWest)) { successors[count++] = upWestNorth; }
		if (unvisited(upWestSouth) && isPassable(westSouth) && isPassable(south) && isPassable(west) && isPassable(up) && isPassable(upSouth) && isPassable(upWest)) { successors[count++] = upWestSouth; }
		if (unvisited(downEastNorth) && isPassable(eastNorth) && isPassable(north) && isPassable(east) && isPassable(down) && isPassable(downNorth) && isPassable(downEast)) { successors[count++] = downEastNorth; }
		if (unvisited(downEastSouth) && isPassable(eastSouth) && isPassable(south) && isPassable(east) && isPassable(down) && isPassable(downSouth) && isPassable(downEast)) { successors[count++] = downEastSouth; }
		if (unvisited(downWestNorth) && isPassable(westNorth) && isPassable(north) && isPassable(west) && isPassable(down) && isPassable(downNorth) && isPassable(downWest)) { successors[count++] = downWestNorth; }
		if (unvisited(downWestSouth) && isPassable(westSouth) && isPassable(south) && isPassable(west) && isPassable(down) && isPassable(downSouth) && isPassable(downWest)) { successors[count++] = downWestSouth; }

		return count;
	}

	private boolean isPassable(@Nullable PathNode node) {
		return node != null && node.penalty >= 0.0F;
	}

	private boolean unvisited(@Nullable PathNode node) {
		return node != null && !node.visited;
	}

	protected @Nullable PathNode getPassableNode(int x, int y, int z) {
		PathNodeType nodeType = getNodeType(x, y, z);
		float penalty = entity.getPathfindingPenalty(nodeType);

		if (penalty < 0.0F) {
			return null;
		}

		PathNode node = getNode(x, y, z);
		node.type = nodeType;
		node.penalty = Math.max(node.penalty, penalty);

		if (nodeType == PathNodeType.WALKABLE) {
			node.penalty++;
		}

		return node;
	}

	@Override
	protected PathNodeType getNodeType(int x, int y, int z) {
		return pathNodes.computeIfAbsent(
				BlockPos.asLong(x, y, z),
				pos -> getNodeType(context, x, y, z, entity)
		);
	}

	@Override
	public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
		PathNodeType nodeType = context.getNodeType(x, y, z);

		if (nodeType != PathNodeType.OPEN || y < context.getWorld().getBottomY() + 1) {
			return nodeType;
		}

		BlockPos below = new BlockPos(x, y - 1, z);
		PathNodeType belowType = context.getNodeType(below.getX(), below.getY(), below.getZ());

		if (belowType == PathNodeType.DAMAGE_FIRE || belowType == PathNodeType.LAVA) {
			nodeType = PathNodeType.DAMAGE_FIRE;
		} else if (belowType == PathNodeType.DAMAGE_OTHER) {
			nodeType = PathNodeType.DAMAGE_OTHER;
		} else if (belowType == PathNodeType.COCOA) {
			nodeType = PathNodeType.COCOA;
		} else if (belowType == PathNodeType.FENCE) {
			if (!below.equals(context.getEntityPos())) {
				nodeType = PathNodeType.FENCE;
			}
		} else {
			nodeType = belowType != PathNodeType.WALKABLE
					&& belowType != PathNodeType.OPEN
					&& belowType != PathNodeType.WATER
					? PathNodeType.WALKABLE
					: PathNodeType.OPEN;
		}

		if (nodeType == PathNodeType.WALKABLE || nodeType == PathNodeType.OPEN) {
			nodeType = getNodeTypeFromNeighbors(context, x, y, z, nodeType);
		}

		return nodeType;
	}

	/**
	 * Для маленьких существ (меньше 1 блока) генерирует случайные позиции в расширенном AABB.
	 * Для крупных — возвращает 4 угла ограничивающего прямоугольника.
	 */
	private Iterable<BlockPos> getPotentialEscapePositions(MobEntity entity) {
		Box box = entity.getBoundingBox();
		boolean isSmall = box.getAverageSideLength() < SMALL_ENTITY_SIZE_THRESHOLD;

		if (!isSmall) {
			return List.of(
					BlockPos.ofFloored(box.minX, entity.getBlockY(), box.minZ),
					BlockPos.ofFloored(box.minX, entity.getBlockY(), box.maxZ),
					BlockPos.ofFloored(box.maxX, entity.getBlockY(), box.minZ),
					BlockPos.ofFloored(box.maxX, entity.getBlockY(), box.maxZ)
			);
		}

		double expandZ = Math.max(0.0, ESCAPE_BOX_EXPANSION - box.getLengthZ());
		double expandX = Math.max(0.0, ESCAPE_BOX_EXPANSION - box.getLengthX());
		double expandY = Math.max(0.0, ESCAPE_BOX_EXPANSION - box.getLengthY());
		Box expanded = box.expand(expandX, expandY, expandZ);

		return BlockPos.iterateRandomly(
				entity.getRandom(),
				ESCAPE_RANDOM_POSITIONS_COUNT,
				MathHelper.floor(expanded.minX),
				MathHelper.floor(expanded.minY),
				MathHelper.floor(expanded.minZ),
				MathHelper.floor(expanded.maxX),
				MathHelper.floor(expanded.maxY),
				MathHelper.floor(expanded.maxZ)
		);
	}
}
