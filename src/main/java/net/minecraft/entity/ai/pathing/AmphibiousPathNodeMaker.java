package net.minecraft.entity.ai.pathing;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkCache;
import org.jspecify.annotations.Nullable;

/**
 * Построитель узлов пути для земноводных существ (черепахи, лягушки и т.д.).
 * Временно изменяет штрафы пути для воды и суши, восстанавливая их при очистке.
 * Поддерживает вертикальное движение в воде.
 */
public class AmphibiousPathNodeMaker extends LandPathNodeMaker {

	private static final float WALKABLE_PENALTY_IN_WATER = 6.0F;
	private static final float WATER_BORDER_PENALTY = 4.0F;

	private final boolean penalizeDeepWater;
	private float oldWalkablePenalty;
	private float oldWaterBorderPenalty;

	public AmphibiousPathNodeMaker(boolean penalizeDeepWater) {
		this.penalizeDeepWater = penalizeDeepWater;
	}

	/**
	 * Временно повышает штраф за ходьбу по суше, чтобы существо предпочитало воду.
	 */
	@Override
	public void init(ChunkCache cachedWorld, MobEntity entity) {
		super.init(cachedWorld, entity);
		entity.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
		oldWalkablePenalty = entity.getPathfindingPenalty(PathNodeType.WALKABLE);
		entity.setPathfindingPenalty(PathNodeType.WALKABLE, WALKABLE_PENALTY_IN_WATER);
		oldWaterBorderPenalty = entity.getPathfindingPenalty(PathNodeType.WATER_BORDER);
		entity.setPathfindingPenalty(PathNodeType.WATER_BORDER, WATER_BORDER_PENALTY);
	}

	@Override
	public void clear() {
		entity.setPathfindingPenalty(PathNodeType.WALKABLE, oldWalkablePenalty);
		entity.setPathfindingPenalty(PathNodeType.WATER_BORDER, oldWaterBorderPenalty);
		super.clear();
	}

	@Override
	public PathNode getStart() {
		if (!entity.isTouchingWater()) {
			return super.getStart();
		}

		return getStart(new BlockPos(
				MathHelper.floor(entity.getBoundingBox().minX),
				MathHelper.floor(entity.getBoundingBox().minY + 0.5),
				MathHelper.floor(entity.getBoundingBox().minZ)
		));
	}

	@Override
	public TargetPathNode getNode(double x, double y, double z) {
		return createNode(x, y + 0.5, z);
	}

	/**
	 * Добавляет вертикальные узлы (вверх/вниз) к стандартным горизонтальным.
	 * Для глубокой воды (ниже уровня моря - 10) добавляет штраф, если включён {@code penalizeDeepWater}.
	 */
	@Override
	public int getSuccessors(PathNode[] successors, PathNode node) {
		int count = super.getSuccessors(successors, node);
		PathNodeType aboveType = getNodeType(node.x, node.y + 1, node.z);
		PathNodeType currentType = getNodeType(node.x, node.y, node.z);
		int maxYStep = entity.getPathfindingPenalty(aboveType) >= 0.0F && currentType != PathNodeType.STICKY_HONEY
				? MathHelper.floor(Math.max(1.0F, entity.getStepHeight()))
				: 0;

		double feetY = getFeetY(new BlockPos(node.x, node.y, node.z));
		PathNode upNode = getPathNode(node.x, node.y + 1, node.z, Math.max(0, maxYStep - 1), feetY, Direction.UP, currentType);
		PathNode downNode = getPathNode(node.x, node.y - 1, node.z, maxYStep, feetY, Direction.DOWN, currentType);

		if (isValidAquaticAdjacentSuccessor(upNode, node)) {
			successors[count++] = upNode;
		}

		if (isValidAquaticAdjacentSuccessor(downNode, node) && currentType != PathNodeType.TRAPDOOR) {
			successors[count++] = downNode;
		}

		for (int idx = 0; idx < count; idx++) {
			PathNode successor = successors[idx];

			if (successor.type == PathNodeType.WATER
					&& penalizeDeepWater
					&& successor.y < entity.getEntityWorld().getSeaLevel() - 10) {
				successor.penalty++;
			}
		}

		return count;
	}

	private boolean isValidAquaticAdjacentSuccessor(@Nullable PathNode node, PathNode successor) {
		return isValidAdjacentSuccessor(node, successor) && node.type == PathNodeType.WATER;
	}

	@Override
	protected boolean isAmphibious() {
		return true;
	}

	/**
	 * Для водных блоков проверяет соседей: если рядом есть непроходимый блок — это граница воды.
	 */
	@Override
	public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
		PathNodeType nodeType = context.getNodeType(x, y, z);

		if (nodeType != PathNodeType.WATER) {
			return super.getDefaultNodeType(context, x, y, z);
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (Direction direction : Direction.values()) {
			mutable.set(x, y, z).move(direction);
			PathNodeType neighborType = context.getNodeType(mutable.getX(), mutable.getY(), mutable.getZ());

			if (neighborType == PathNodeType.BLOCKED) {
				return PathNodeType.WATER_BORDER;
			}
		}

		return PathNodeType.WATER;
	}
}
