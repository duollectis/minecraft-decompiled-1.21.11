package net.minecraft.entity.ai.pathing;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.ChunkCache;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * Построитель узлов пути для наземных существ.
 * Определяет тип каждого блока для алгоритма A* с учётом размера существа,
 * возможности плавания, открывания дверей и перешагивания заборов.
 */
public class LandPathNodeMaker extends PathNodeMaker {

	public static final double Y_OFFSET = 0.5;
	private static final double MIN_STEP_HEIGHT = 1.125;

	private final Long2ObjectMap<PathNodeType> nodeTypes = new Long2ObjectOpenHashMap<>();
	private final Object2BooleanMap<Box> collidedBoxes = new Object2BooleanOpenHashMap<>();
	private final PathNode[] successors = new PathNode[Direction.Type.HORIZONTAL.getFacingCount()];

	@Override
	public void init(ChunkCache cachedWorld, MobEntity entity) {
		super.init(cachedWorld, entity);
		entity.onStartPathfinding();
	}

	@Override
	public void clear() {
		entity.onFinishPathfinding();
		nodeTypes.clear();
		collidedBoxes.clear();
		super.clear();
	}

	/**
	 * Определяет стартовый узел пути с учётом текущего состояния существа:
	 * плавание в воде, стояние на земле или падение в воздухе.
	 */
	@Override
	public PathNode getStart() {
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int startY = entity.getBlockY();
		BlockState blockState = context.getBlockState(mutable.set(entity.getX(), (double) startY, entity.getZ()));

		if (!entity.canWalkOnFluid(blockState.getFluidState())) {
			if (canSwim() && entity.isTouchingWater()) {
				while (true) {
					if (!blockState.isOf(Blocks.WATER)
							&& blockState.getFluidState() != Fluids.WATER.getStill(false)) {
						startY--;
						break;
					}

					blockState = context.getBlockState(
							mutable.set(entity.getX(), (double) (++startY), entity.getZ())
					);
				}
			} else if (entity.isOnGround()) {
				startY = MathHelper.floor(entity.getY() + 0.5);
			} else {
				mutable.set(entity.getX(), entity.getY() + 1.0, entity.getZ());

				while (mutable.getY() > context.getWorld().getBottomY()) {
					startY = mutable.getY();
					mutable.setY(mutable.getY() - 1);
					BlockState below = context.getBlockState(mutable);

					if (!below.isAir() && !below.canPathfindThrough(NavigationType.LAND)) {
						break;
					}
				}
			}
		} else {
			while (entity.canWalkOnFluid(blockState.getFluidState())) {
				blockState = context.getBlockState(
						mutable.set(entity.getX(), (double) (++startY), entity.getZ())
				);
			}

			startY--;
		}

		BlockPos entityPos = entity.getBlockPos();

		if (!canPathThrough(mutable.set(entityPos.getX(), startY, entityPos.getZ()))) {
			Box box = entity.getBoundingBox();

			if (canPathThrough(mutable.set(box.minX, (double) startY, box.minZ))
					|| canPathThrough(mutable.set(box.minX, (double) startY, box.maxZ))
					|| canPathThrough(mutable.set(box.maxX, (double) startY, box.minZ))
					|| canPathThrough(mutable.set(box.maxX, (double) startY, box.maxZ))) {
				return getStart(mutable);
			}
		}

		return getStart(new BlockPos(entityPos.getX(), startY, entityPos.getZ()));
	}

	protected PathNode getStart(BlockPos pos) {
		PathNode node = getNode(pos);
		node.type = getNodeType(node.x, node.y, node.z);
		node.penalty = entity.getPathfindingPenalty(node.type);
		return node;
	}

	protected boolean canPathThrough(BlockPos pos) {
		PathNodeType nodeType = getNodeType(pos.getX(), pos.getY(), pos.getZ());
		return nodeType != PathNodeType.OPEN && entity.getPathfindingPenalty(nodeType) >= 0.0F;
	}

	@Override
	public TargetPathNode getNode(double x, double y, double z) {
		return createNode(x, y, z);
	}

	@Override
	public int getSuccessors(PathNode[] successors, PathNode node) {
		int count = 0;
		int maxYStep = 0;
		PathNodeType aboveType = getNodeType(node.x, node.y + 1, node.z);
		PathNodeType currentType = getNodeType(node.x, node.y, node.z);

		if (entity.getPathfindingPenalty(aboveType) >= 0.0F && currentType != PathNodeType.STICKY_HONEY) {
			maxYStep = MathHelper.floor(Math.max(1.0F, entity.getStepHeight()));
		}

		double feetY = getFeetY(new BlockPos(node.x, node.y, node.z));

		for (Direction direction : Direction.Type.HORIZONTAL) {
			PathNode adjacent = getPathNode(
					node.x + direction.getOffsetX(),
					node.y,
					node.z + direction.getOffsetZ(),
					maxYStep,
					feetY,
					direction,
					currentType
			);
			this.successors[direction.getHorizontalQuarterTurns()] = adjacent;

			if (isValidAdjacentSuccessor(adjacent, node)) {
				successors[count++] = adjacent;
			}
		}

		for (Direction dir : Direction.Type.HORIZONTAL) {
			Direction rotated = dir.rotateYClockwise();

			if (isValidDiagonalSuccessor(
					node,
					this.successors[dir.getHorizontalQuarterTurns()],
					this.successors[rotated.getHorizontalQuarterTurns()]
			)) {
				PathNode diagonal = getPathNode(
						node.x + dir.getOffsetX() + rotated.getOffsetX(),
						node.y,
						node.z + dir.getOffsetZ() + rotated.getOffsetZ(),
						maxYStep,
						feetY,
						dir,
						currentType
				);

				if (isValidDiagonalSuccessor(diagonal)) {
					successors[count++] = diagonal;
				}
			}
		}

		return count;
	}

	protected boolean isValidAdjacentSuccessor(@Nullable PathNode node, PathNode successor) {
		return node != null && !node.visited && (node.penalty >= 0.0F || successor.penalty < 0.0F);
	}

	protected boolean isValidDiagonalSuccessor(PathNode xNode, @Nullable PathNode zNode, @Nullable PathNode xDiagNode) {
		if (xDiagNode == null || zNode == null || xDiagNode.y > xNode.y || zNode.y > xNode.y) {
			return false;
		}

		if (zNode.type == PathNodeType.WALKABLE_DOOR || xDiagNode.type == PathNodeType.WALKABLE_DOOR) {
			return false;
		}

		boolean bothFences = xDiagNode.type == PathNodeType.FENCE
				&& zNode.type == PathNodeType.FENCE
				&& entity.getWidth() < 0.5;

		return (xDiagNode.y < xNode.y || xDiagNode.penalty >= 0.0F || bothFences)
				&& (zNode.y < xNode.y || zNode.penalty >= 0.0F || bothFences);
	}

	protected boolean isValidDiagonalSuccessor(@Nullable PathNode node) {
		if (node == null || node.visited) {
			return false;
		}

		return node.type != PathNodeType.WALKABLE_DOOR && node.penalty >= 0.0F;
	}

	private static boolean isBlocked(PathNodeType nodeType) {
		return nodeType == PathNodeType.FENCE
				|| nodeType == PathNodeType.DOOR_WOOD_CLOSED
				|| nodeType == PathNodeType.DOOR_IRON_CLOSED;
	}

	private boolean isBlocked(PathNode node) {
		Box box = entity.getBoundingBox();
		Vec3d step = new Vec3d(
				node.x - entity.getX() + box.getLengthX() / 2.0,
				node.y - entity.getY() + box.getLengthY() / 2.0,
				node.z - entity.getZ() + box.getLengthZ() / 2.0
		);
		int steps = MathHelper.ceil(step.length() / box.getAverageSideLength());
		step = step.multiply(1.0F / steps);

		for (int i = 1; i <= steps; i++) {
			box = box.offset(step);

			if (checkBoxCollision(box)) {
				return false;
			}
		}

		return true;
	}

	protected double getFeetY(BlockPos pos) {
		BlockView world = context.getWorld();
		return (canSwim() || isAmphibious()) && world.getFluidState(pos).isIn(FluidTags.WATER)
				? pos.getY() + 0.5
				: getFeetY(world, pos);
	}

	public static double getFeetY(BlockView world, BlockPos pos) {
		BlockPos below = pos.down();
		VoxelShape shape = world.getBlockState(below).getCollisionShape(world, below);
		return below.getY() + (shape.isEmpty() ? 0.0 : shape.getMax(Direction.Axis.Y));
	}

	protected boolean isAmphibious() {
		return false;
	}

	protected @Nullable PathNode getPathNode(
			int x,
			int y,
			int z,
			int maxYStep,
			double lastFeetY,
			Direction direction,
			PathNodeType nodeType
	) {
		PathNode result = null;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		double feetY = getFeetY(mutable.set(x, y, z));

		if (feetY - lastFeetY > getStepHeight()) {
			return null;
		}

		PathNodeType adjacentType = getNodeType(x, y, z);
		float penalty = entity.getPathfindingPenalty(adjacentType);

		if (penalty >= 0.0F) {
			result = getNodeWith(x, y, z, adjacentType, penalty);
		}

		if (isBlocked(nodeType) && result != null && result.penalty >= 0.0F && !isBlocked(result)) {
			result = null;
		}

		if (adjacentType == PathNodeType.WALKABLE || isAmphibious() && adjacentType == PathNodeType.WATER) {
			return result;
		}

		if ((result == null || result.penalty < 0.0F)
				&& maxYStep > 0
				&& (adjacentType != PathNodeType.FENCE || canWalkOverFences())
				&& adjacentType != PathNodeType.UNPASSABLE_RAIL
				&& adjacentType != PathNodeType.TRAPDOOR
				&& adjacentType != PathNodeType.POWDER_SNOW) {
			return getJumpOnTopNode(x, y, z, maxYStep, lastFeetY, direction, nodeType, mutable);
		}

		if (!isAmphibious() && adjacentType == PathNodeType.WATER && !canSwim()) {
			return getNonWaterNodeBelow(x, y, z, result);
		}

		if (adjacentType == PathNodeType.OPEN) {
			return getOpenNode(x, y, z);
		}

		if (isBlocked(adjacentType) && result == null) {
			return getNodeWith(x, y, z, adjacentType);
		}

		return result;
	}

	private double getStepHeight() {
		return Math.max(MIN_STEP_HEIGHT, (double) entity.getStepHeight());
	}

	private PathNode getNodeWith(int x, int y, int z, PathNodeType type, float penalty) {
		PathNode node = getNode(x, y, z);
		node.type = type;
		node.penalty = Math.max(node.penalty, penalty);
		return node;
	}

	private PathNode getBlockedNode(int x, int y, int z) {
		PathNode node = getNode(x, y, z);
		node.type = PathNodeType.BLOCKED;
		node.penalty = -1.0F;
		return node;
	}

	private PathNode getNodeWith(int x, int y, int z, PathNodeType type) {
		PathNode node = getNode(x, y, z);
		node.visited = true;
		node.type = type;
		node.penalty = type.getDefaultPenalty();
		return node;
	}

	private @Nullable PathNode getJumpOnTopNode(
			int x,
			int y,
			int z,
			int maxYStep,
			double lastFeetY,
			Direction direction,
			PathNodeType nodeType,
			BlockPos.Mutable mutablePos
	) {
		PathNode above = getPathNode(x, y + 1, z, maxYStep - 1, lastFeetY, direction, nodeType);

		if (above == null) {
			return null;
		}

		if (entity.getWidth() >= 1.0F) {
			return above;
		}

		if (above.type != PathNodeType.OPEN && above.type != PathNodeType.WALKABLE) {
			return above;
		}

		double centerX = x - direction.getOffsetX() + 0.5;
		double centerZ = z - direction.getOffsetZ() + 0.5;
		double halfWidth = entity.getWidth() / 2.0;
		Box box = new Box(
				centerX - halfWidth,
				getFeetY(mutablePos.set(centerX, (double) (y + 1), centerZ)) + 0.001,
				centerZ - halfWidth,
				centerX + halfWidth,
				entity.getHeight() + getFeetY(mutablePos.set(
						(double) above.x, (double) above.y, (double) above.z
				)) - 0.002,
				centerZ + halfWidth
		);

		return checkBoxCollision(box) ? null : above;
	}

	private @Nullable PathNode getNonWaterNodeBelow(int x, int y, int z, @Nullable PathNode node) {
		y--;

		while (y > entity.getEntityWorld().getBottomY()) {
			PathNodeType nodeType = getNodeType(x, y, z);

			if (nodeType != PathNodeType.WATER) {
				return node;
			}

			node = getNodeWith(x, y, z, nodeType, entity.getPathfindingPenalty(nodeType));
			y--;
		}

		return node;
	}

	private PathNode getOpenNode(int x, int y, int z) {
		for (int fallY = y - 1; fallY >= entity.getEntityWorld().getBottomY(); fallY--) {
			if (y - fallY > entity.getSafeFallDistance()) {
				return getBlockedNode(x, fallY, z);
			}

			PathNodeType nodeType = getNodeType(x, fallY, z);
			float penalty = entity.getPathfindingPenalty(nodeType);

			if (nodeType != PathNodeType.OPEN) {
				return penalty >= 0.0F
						? getNodeWith(x, fallY, z, nodeType, penalty)
						: getBlockedNode(x, fallY, z);
			}
		}

		return getBlockedNode(x, y, z);
	}

	private boolean checkBoxCollision(Box box) {
		return collidedBoxes.computeIfAbsent(box, b -> !context.getWorld().isSpaceEmpty(entity, box));
	}

	protected PathNodeType getNodeType(int x, int y, int z) {
		return nodeTypes.computeIfAbsent(
				BlockPos.asLong(x, y, z),
				pos -> getNodeType(context, x, y, z, entity)
		);
	}

	/**
	 * Определяет тип узла с учётом всего объёма существа (entityBlockXSize × entityBlockYSize × entityBlockZSize).
	 * Приоритет: заборы > непроходимые рельсы > наихудший тип по штрафу.
	 */
	@Override
	public PathNodeType getNodeType(PathContext context, int x, int y, int z, MobEntity mob) {
		Set<PathNodeType> collidingTypes = getCollidingNodeTypes(context, x, y, z);

		if (collidingTypes.contains(PathNodeType.FENCE)) {
			return PathNodeType.FENCE;
		}

		if (collidingTypes.contains(PathNodeType.UNPASSABLE_RAIL)) {
			return PathNodeType.UNPASSABLE_RAIL;
		}

		PathNodeType best = PathNodeType.BLOCKED;

		for (PathNodeType type : collidingTypes) {
			if (mob.getPathfindingPenalty(type) < 0.0F) {
				return type;
			}

			if (mob.getPathfindingPenalty(type) >= mob.getPathfindingPenalty(best)) {
				best = type;
			}
		}

		return entityBlockXSize <= 1
				&& best != PathNodeType.OPEN
				&& mob.getPathfindingPenalty(best) == 0.0F
				&& getDefaultNodeType(context, x, y, z) == PathNodeType.OPEN
				? PathNodeType.OPEN
				: best;
	}

	public Set<PathNodeType> getCollidingNodeTypes(PathContext context, int x, int y, int z) {
		EnumSet<PathNodeType> result = EnumSet.noneOf(PathNodeType.class);
		boolean canEnterDoors = canEnterOpenDoors();

		for (int dx = 0; dx < entityBlockXSize; dx++) {
			for (int dy = 0; dy < entityBlockYSize; dy++) {
				for (int dz = 0; dz < entityBlockZSize; dz++) {
					int wx = dx + x;
					int wy = dy + y;
					int wz = dz + z;
					PathNodeType nodeType = getDefaultNodeType(context, wx, wy, wz);
					BlockPos entityPos = entity.getBlockPos();

					if (nodeType == PathNodeType.DOOR_WOOD_CLOSED && canOpenDoors() && canEnterDoors) {
						nodeType = PathNodeType.WALKABLE_DOOR;
					}

					if (nodeType == PathNodeType.DOOR_OPEN && !canEnterDoors) {
						nodeType = PathNodeType.BLOCKED;
					}

					if (nodeType == PathNodeType.RAIL
							&& getDefaultNodeType(context, entityPos.getX(), entityPos.getY(), entityPos.getZ()) != PathNodeType.RAIL
							&& getDefaultNodeType(context, entityPos.getX(), entityPos.getY() - 1, entityPos.getZ()) != PathNodeType.RAIL) {
						nodeType = PathNodeType.UNPASSABLE_RAIL;
					}

					result.add(nodeType);
				}
			}
		}

		return result;
	}

	@Override
	public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
		return getLandNodeType(context, new BlockPos.Mutable(x, y, z));
	}

	public static PathNodeType getLandNodeType(MobEntity entity, BlockPos pos) {
		return getLandNodeType(new PathContext(entity.getEntityWorld(), entity), pos.mutableCopy());
	}

	/**
	 * Определяет тип узла для наземной навигации с учётом блока под ногами.
	 * Если блок открытый — анализирует блок ниже для определения опасности.
	 */
	public static PathNodeType getLandNodeType(PathContext context, BlockPos.Mutable pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		PathNodeType nodeType = context.getNodeType(x, y, z);

		if (nodeType != PathNodeType.OPEN || y < context.getWorld().getBottomY() + 1) {
			return nodeType;
		}

		return switch (context.getNodeType(x, y - 1, z)) {
			case OPEN, WATER, LAVA, WALKABLE -> PathNodeType.OPEN;
			case DAMAGE_FIRE -> PathNodeType.DAMAGE_FIRE;
			case DAMAGE_OTHER -> PathNodeType.DAMAGE_OTHER;
			case STICKY_HONEY -> PathNodeType.STICKY_HONEY;
			case POWDER_SNOW -> PathNodeType.DANGER_POWDER_SNOW;
			case DAMAGE_CAUTIOUS -> PathNodeType.DAMAGE_CAUTIOUS;
			case TRAPDOOR -> PathNodeType.DANGER_TRAPDOOR;
			default -> getNodeTypeFromNeighbors(context, x, y, z, PathNodeType.WALKABLE);
		};
	}

	/**
	 * Проверяет соседние блоки на наличие опасных типов (огонь, лава, вода, осторожность).
	 * Используется для определения «опасной» зоны рядом с опасным блоком.
	 */
	public static PathNodeType getNodeTypeFromNeighbors(
			PathContext context,
			int x,
			int y,
			int z,
			PathNodeType fallback
	) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}

					PathNodeType neighborType = context.getNodeType(x + dx, y + dy, z + dz);

					if (neighborType == PathNodeType.DAMAGE_OTHER) {
						return PathNodeType.DANGER_OTHER;
					}

					if (neighborType == PathNodeType.DAMAGE_FIRE || neighborType == PathNodeType.LAVA) {
						return PathNodeType.DANGER_FIRE;
					}

					if (neighborType == PathNodeType.WATER) {
						return PathNodeType.WATER_BORDER;
					}

					if (neighborType == PathNodeType.DAMAGE_CAUTIOUS) {
						return PathNodeType.DAMAGE_CAUTIOUS;
					}
				}
			}
		}

		return fallback;
	}

	/**
	 * Определяет базовый тип узла по состоянию блока без учёта соседей и размера существа.
	 */
	protected static PathNodeType getCommonNodeType(BlockView world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		Block block = blockState.getBlock();

		if (blockState.isAir()) {
			return PathNodeType.OPEN;
		}

		if (blockState.isIn(BlockTags.TRAPDOORS)
				|| blockState.isOf(Blocks.LILY_PAD)
				|| blockState.isOf(Blocks.BIG_DRIPLEAF)) {
			return PathNodeType.TRAPDOOR;
		}

		if (blockState.isOf(Blocks.POWDER_SNOW)) {
			return PathNodeType.POWDER_SNOW;
		}

		if (blockState.isOf(Blocks.CACTUS) || blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
			return PathNodeType.DAMAGE_OTHER;
		}

		if (blockState.isOf(Blocks.HONEY_BLOCK)) {
			return PathNodeType.STICKY_HONEY;
		}

		if (blockState.isOf(Blocks.COCOA)) {
			return PathNodeType.COCOA;
		}

		if (blockState.isOf(Blocks.WITHER_ROSE) || blockState.isOf(Blocks.POINTED_DRIPSTONE)) {
			return PathNodeType.DAMAGE_CAUTIOUS;
		}

		FluidState fluidState = blockState.getFluidState();

		if (fluidState.isIn(FluidTags.LAVA)) {
			return PathNodeType.LAVA;
		}

		if (isFireDamaging(blockState)) {
			return PathNodeType.DAMAGE_FIRE;
		}

		if (block instanceof DoorBlock doorBlock) {
			return blockState.get(DoorBlock.OPEN)
					? PathNodeType.DOOR_OPEN
					: doorBlock.getBlockSetType().canOpenByHand()
					? PathNodeType.DOOR_WOOD_CLOSED
					: PathNodeType.DOOR_IRON_CLOSED;
		}

		if (block instanceof AbstractRailBlock) {
			return PathNodeType.RAIL;
		}

		if (block instanceof LeavesBlock) {
			return PathNodeType.LEAVES;
		}

		if (blockState.isIn(BlockTags.FENCES)
				|| blockState.isIn(BlockTags.WALLS)
				|| (block instanceof FenceGateBlock && !blockState.get(FenceGateBlock.OPEN))) {
			return PathNodeType.FENCE;
		}

		return blockState.canPathfindThrough(NavigationType.LAND)
				? (fluidState.isIn(FluidTags.WATER) ? PathNodeType.WATER : PathNodeType.OPEN)
				: PathNodeType.BLOCKED;
	}
}
