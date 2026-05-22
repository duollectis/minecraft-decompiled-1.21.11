package net.minecraft.entity.ai.pathing;

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.debug.SubscriberTracker;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Базовый класс навигации существа: управляет поиском пути, следованием по нему
 * и таймаутами при зависании.
 */
public abstract class EntityNavigation {

	private static final int RECALCULATE_COOLDOWN = 20;
	private static final int PATH_TIMEOUT_TICKS = 100;
	private static final float PATH_TIMEOUT_DISTANCE_FACTOR = 0.25F;

	protected final MobEntity entity;
	protected final World world;
	protected @Nullable Path currentPath;
	protected double speed;
	protected int tickCount;
	protected int pathStartTime;
	protected Vec3d pathStartPos = Vec3d.ZERO;
	protected Vec3i lastNodePosition = Vec3i.ZERO;
	protected long currentNodeMs;
	protected long lastActiveTickMs;
	protected double currentNodeTimeout;
	protected float nodeReachProximity = 0.5F;
	protected boolean inRecalculationCooldown;
	protected long lastRecalculateTime;
	protected PathNodeMaker nodeMaker;
	private @Nullable BlockPos currentTarget;
	private int currentDistance;
	private float rangeMultiplier = 1.0F;
	private final PathNodeNavigator pathNodeNavigator;
	private boolean nearPathStartPos;
	private float maxFollowRange = 16.0F;

	public EntityNavigation(MobEntity entity, World world) {
		this.entity = entity;
		this.world = world;
		pathNodeNavigator = createPathNodeNavigator(
				MathHelper.floor(entity.getAttributeBaseValue(EntityAttributes.FOLLOW_RANGE) * 16.0)
		);

		if (world instanceof ServerWorld serverWorld) {
			SubscriberTracker tracker = serverWorld.getServer().getSubscriberTracker();
			pathNodeNavigator.setShouldSendDebugData(
					() -> tracker.hasSubscriber(DebugSubscriptionTypes.ENTITY_PATHS)
			);
		}
	}

	public void updateRange() {
		int newRange = MathHelper.floor(getMaxFollowRange() * 16.0F);
		pathNodeNavigator.setRange(newRange);
	}

	public void setMaxFollowRange(float maxFollowRange) {
		this.maxFollowRange = maxFollowRange;
		updateRange();
	}

	private float getMaxFollowRange() {
		return Math.max((float) entity.getAttributeValue(EntityAttributes.FOLLOW_RANGE), maxFollowRange);
	}

	public void resetRangeMultiplier() {
		rangeMultiplier = 1.0F;
	}

	public void setRangeMultiplier(float rangeMultiplier) {
		this.rangeMultiplier = rangeMultiplier;
	}

	public @Nullable BlockPos getTargetPos() {
		return currentTarget;
	}

	protected abstract PathNodeNavigator createPathNodeNavigator(int range);

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * Пересчитывает путь к текущей цели, если прошло достаточно времени с последнего пересчёта.
	 * Устанавливает флаг {@code inRecalculationCooldown}, если кулдаун ещё не истёк.
	 */
	public void recalculatePath() {
		if (world.getTime() - lastRecalculateTime > RECALCULATE_COOLDOWN) {
			if (currentTarget != null) {
				currentPath = null;
				currentPath = findPathTo(currentTarget, currentDistance);
				lastRecalculateTime = world.getTime();
				inRecalculationCooldown = false;
			}
		} else {
			inRecalculationCooldown = true;
		}
	}

	public final @Nullable Path findPathTo(double x, double y, double z, int distance) {
		return findPathTo(BlockPos.ofFloored(x, y, z), distance);
	}

	public @Nullable Path findPathToAny(Stream<BlockPos> positions, int distance) {
		return findPathTo(positions.collect(Collectors.toSet()), 8, false, distance);
	}

	public @Nullable Path findPathTo(Set<BlockPos> positions, int distance) {
		return findPathTo(positions, 8, false, distance);
	}

	public @Nullable Path findPathTo(BlockPos target, int distance) {
		return findPathTo(ImmutableSet.of(target), 8, false, distance);
	}

	public @Nullable Path findPathTo(BlockPos target, int minDistance, int maxDistance) {
		return findPathToAny(ImmutableSet.of(target), 8, false, minDistance, maxDistance);
	}

	public @Nullable Path findPathTo(Entity entity, int distance) {
		return findPathTo(ImmutableSet.of(entity.getBlockPos()), 16, true, distance);
	}

	protected @Nullable Path findPathTo(Set<BlockPos> positions, int range, boolean useHeadPos, int distance) {
		return findPathToAny(positions, range, useHeadPos, distance, getMaxFollowRange());
	}

	/**
	 * Основной метод поиска пути к множеству позиций.
	 * Создаёт кэш чанков вокруг существа и запускает алгоритм A*.
	 *
	 * @param positions целевые позиции
	 * @param range дополнительный радиус кэша чанков
	 * @param useHeadPos использовать позицию головы как старт (для существ, атакующих сверху)
	 * @param distance допустимое расстояние до цели
	 * @param followRange максимальная дальность следования
	 * @return найденный путь или {@code null}
	 */
	protected @Nullable Path findPathToAny(
			Set<BlockPos> positions,
			int range,
			boolean useHeadPos,
			int distance,
			float followRange
	) {
		if (positions.isEmpty()) {
			return null;
		}

		if (entity.getY() < world.getBottomY()) {
			return null;
		}

		if (!isAtValidPosition()) {
			return null;
		}

		if (currentPath != null && !currentPath.isFinished() && positions.contains(currentTarget)) {
			return currentPath;
		}

		Profiler profiler = Profilers.get();
		profiler.push("pathfind");
		BlockPos startPos = useHeadPos ? entity.getBlockPos().up() : entity.getBlockPos();
		int chunkRadius = (int) (followRange + range);
		ChunkCache chunkCache = new ChunkCache(
				world,
				startPos.add(-chunkRadius, -chunkRadius, -chunkRadius),
				startPos.add(chunkRadius, chunkRadius, chunkRadius)
		);
		Path path = pathNodeNavigator.findPathToAny(chunkCache, entity, positions, followRange, distance, rangeMultiplier);
		profiler.pop();

		if (path != null && path.getTarget() != null) {
			currentTarget = path.getTarget();
			currentDistance = distance;
			resetNode();
		}

		return path;
	}

	public boolean startMovingTo(double x, double y, double z, double speed) {
		return startMovingAlong(findPathTo(x, y, z, 1), speed);
	}

	public boolean startMovingTo(double x, double y, double z, int distance, double speed) {
		return startMovingAlong(findPathTo(x, y, z, distance), speed);
	}

	public boolean startMovingTo(Entity entity, double speed) {
		Path path = findPathTo(entity, 1);
		return path != null && startMovingAlong(path, speed);
	}

	/**
	 * Начинает движение по заданному пути с указанной скоростью.
	 *
	 * @param path путь для следования (может быть {@code null})
	 * @param speed скорость движения
	 * @return {@code true} если движение успешно начато
	 */
	public boolean startMovingAlong(@Nullable Path path, double speed) {
		if (path == null) {
			currentPath = null;
			return false;
		}

		if (!path.equalsPath(currentPath)) {
			currentPath = path;
		}

		if (isIdle()) {
			return false;
		}

		adjustPath();

		if (currentPath.getLength() <= 0) {
			return false;
		}

		this.speed = speed;
		Vec3d pos = getPos();
		pathStartTime = tickCount;
		pathStartPos = pos;
		return true;
	}

	public @Nullable Path getCurrentPath() {
		return currentPath;
	}

	/**
	 * Обновляет навигацию: продвигает по пути, проверяет таймауты, управляет движением.
	 */
	public void tick() {
		tickCount++;

		if (inRecalculationCooldown) {
			recalculatePath();
		}

		if (isIdle()) {
			return;
		}

		if (isAtValidPosition()) {
			continueFollowingPath();
		} else if (currentPath != null && !currentPath.isFinished()) {
			Vec3d pos = getPos();
			Vec3d nodePos = currentPath.getNodePosition(entity);

			if (pos.y > nodePos.y
					&& !entity.isOnGround()
					&& MathHelper.floor(pos.x) == MathHelper.floor(nodePos.x)
					&& MathHelper.floor(pos.z) == MathHelper.floor(nodePos.z)) {
				currentPath.next();
			}
		}

		if (!isIdle()) {
			Vec3d targetPos = currentPath.getNodePosition(entity);
			entity.getMoveControl().moveTo(targetPos.x, adjustTargetY(targetPos), targetPos.z, speed);
		}
	}

	protected double adjustTargetY(Vec3d pos) {
		BlockPos blockPos = BlockPos.ofFloored(pos);
		return world.getBlockState(blockPos.down()).isAir()
				? pos.y
				: LandPathNodeMaker.getFeetY(world, blockPos);
	}

	protected void continueFollowingPath() {
		Vec3d pos = getPos();
		nodeReachProximity = entity.getWidth() > 0.75F
				? entity.getWidth() / 2.0F
				: 0.75F - entity.getWidth() / 2.0F;
		Vec3i nodePos = currentPath.getCurrentNodePos();
		double dx = Math.abs(entity.getX() - (nodePos.getX() + 0.5));
		double dy = Math.abs(entity.getY() - nodePos.getY());
		double dz = Math.abs(entity.getZ() - (nodePos.getZ() + 0.5));
		boolean nodeReached = dx < nodeReachProximity && dz < nodeReachProximity && dy < 1.0;

		if (nodeReached || canJumpToNext(currentPath.getCurrentNode().type) && shouldJumpToNextNode(pos)) {
			currentPath.next();
		}

		checkTimeouts(pos);
	}

	private boolean shouldJumpToNextNode(Vec3d currentPos) {
		if (currentPath.getCurrentNodeIndex() + 1 >= currentPath.getLength()) {
			return false;
		}

		Vec3d currentNodeCenter = Vec3d.ofBottomCenter(currentPath.getCurrentNodePos());

		if (!currentPos.isInRange(currentNodeCenter, 2.0)) {
			return false;
		}

		if (canPathDirectlyThrough(currentPos, currentPath.getNodePosition(entity))) {
			return true;
		}

		Vec3d nextNodeCenter = Vec3d.ofBottomCenter(
				currentPath.getNodePos(currentPath.getCurrentNodeIndex() + 1)
		);
		Vec3d toCurrentNode = currentNodeCenter.subtract(currentPos);
		Vec3d toNextNode = nextNodeCenter.subtract(currentPos);
		double distToCurrentSq = toCurrentNode.lengthSquared();
		double distToNextSq = toNextNode.lengthSquared();
		boolean nextIsCloser = distToNextSq < distToCurrentSq;
		boolean currentIsVeryClose = distToCurrentSq < 0.5;

		if (!nextIsCloser && !currentIsVeryClose) {
			return false;
		}

		return toNextNode.normalize().dotProduct(toCurrentNode.normalize()) < 0.0;
	}

	/**
	 * Проверяет таймауты: останавливает существо, если оно застряло на месте.
	 * Использует два механизма: общий таймаут пути и таймаут на одном узле.
	 */
	protected void checkTimeouts(Vec3d currentPos) {
		if (tickCount - pathStartTime > PATH_TIMEOUT_TICKS) {
			float movementSpeed = entity.getMovementSpeed();
			float effectiveSpeed = movementSpeed >= 1.0F ? movementSpeed : movementSpeed * movementSpeed;
			float timeoutRadius = effectiveSpeed * 100.0F * PATH_TIMEOUT_DISTANCE_FACTOR;

			if (currentPos.squaredDistanceTo(pathStartPos) < timeoutRadius * timeoutRadius) {
				nearPathStartPos = true;
				stop();
			} else {
				nearPathStartPos = false;
			}

			pathStartTime = tickCount;
			pathStartPos = currentPos;
		}

		if (currentPath != null && !currentPath.isFinished()) {
			Vec3i nodePos = currentPath.getCurrentNodePos();
			long currentTime = world.getTime();

			if (nodePos.equals(lastNodePosition)) {
				currentNodeMs += currentTime - lastActiveTickMs;
			} else {
				lastNodePosition = nodePos;
				double distToNode = currentPos.distanceTo(Vec3d.ofBottomCenter(lastNodePosition));
				currentNodeTimeout = entity.getMovementSpeed() > 0.0F
						? distToNode / entity.getMovementSpeed() * 20.0
						: 0.0;
			}

			if (currentNodeTimeout > 0.0 && currentNodeMs > currentNodeTimeout * 3.0) {
				resetNodeAndStop();
			}

			lastActiveTickMs = currentTime;
		}
	}

	private void resetNodeAndStop() {
		resetNode();
		stop();
	}

	private void resetNode() {
		lastNodePosition = Vec3i.ZERO;
		currentNodeMs = 0L;
		currentNodeTimeout = 0.0;
		nearPathStartPos = false;
	}

	public boolean isIdle() {
		return currentPath == null || currentPath.isFinished();
	}

	public boolean isFollowingPath() {
		return !isIdle();
	}

	public void stop() {
		currentPath = null;
	}

	protected abstract Vec3d getPos();

	protected abstract boolean isAtValidPosition();

	/**
	 * Корректирует путь: поднимает узлы внутри котлов на один блок вверх,
	 * чтобы существо не застревало в них.
	 */
	protected void adjustPath() {
		if (currentPath == null) {
			return;
		}

		for (int i = 0; i < currentPath.getLength(); i++) {
			PathNode node = currentPath.getNode(i);
			PathNode nextNode = i + 1 < currentPath.getLength() ? currentPath.getNode(i + 1) : null;
			BlockState blockState = world.getBlockState(new BlockPos(node.x, node.y, node.z));

			if (blockState.isIn(BlockTags.CAULDRONS)) {
				currentPath.setNode(i, node.copyWithNewPosition(node.x, node.y + 1, node.z));

				if (nextNode != null && node.y >= nextNode.y) {
					currentPath.setNode(i + 1, node.copyWithNewPosition(nextNode.x, node.y + 1, nextNode.z));
				}
			}
		}
	}

	protected boolean canPathDirectlyThrough(Vec3d origin, Vec3d target) {
		return false;
	}

	public boolean canJumpToNext(PathNodeType nodeType) {
		return nodeType != PathNodeType.DANGER_FIRE
				&& nodeType != PathNodeType.DANGER_OTHER
				&& nodeType != PathNodeType.WALKABLE_DOOR;
	}

	/**
	 * Проверяет, не пересекает ли прямая линия между двумя точками твёрдые блоки.
	 *
	 * @param entity существо для рейкаста
	 * @param startPos начальная точка
	 * @param entityPos конечная точка (позиция ног)
	 * @param includeFluids учитывать ли жидкости как препятствия
	 * @return {@code true} если путь свободен
	 */
	protected static boolean doesNotCollide(MobEntity entity, Vec3d startPos, Vec3d entityPos, boolean includeFluids) {
		Vec3d midPoint = new Vec3d(entityPos.x, entityPos.y + entity.getHeight() * 0.5, entityPos.z);
		return entity.getEntityWorld()
				.raycast(new RaycastContext(
						startPos,
						midPoint,
						RaycastContext.ShapeType.COLLIDER,
						includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
						entity
				))
				.getType() == HitResult.Type.MISS;
	}

	public boolean isValidPosition(BlockPos pos) {
		BlockPos below = pos.down();
		return world.getBlockState(below).isOpaqueFullCube();
	}

	public PathNodeMaker getNodeMaker() {
		return nodeMaker;
	}

	public void setCanSwim(boolean canSwim) {
		nodeMaker.setCanSwim(canSwim);
	}

	public boolean canSwim() {
		return nodeMaker.canSwim();
	}

	/**
	 * Определяет, нужно ли пересчитать путь при изменении блока в позиции {@code pos}.
	 * Проверяет, находится ли позиция в пределах текущего пути.
	 */
	public boolean shouldRecalculatePath(BlockPos pos) {
		if (inRecalculationCooldown) {
			return false;
		}

		if (currentPath == null || currentPath.isFinished() || currentPath.getLength() == 0) {
			return false;
		}

		PathNode endNode = currentPath.getEnd();
		Vec3d midPoint = new Vec3d(
				(endNode.x + entity.getX()) / 2.0,
				(endNode.y + entity.getY()) / 2.0,
				(endNode.z + entity.getZ()) / 2.0
		);
		return pos.isWithinDistance(midPoint, currentPath.getLength() - currentPath.getCurrentNodeIndex());
	}

	public float getNodeReachProximity() {
		return nodeReachProximity;
	}

	public boolean isNearPathStartPos() {
		return nearPathStartPos;
	}

	public abstract boolean canControlOpeningDoors();

	public void setCanOpenDoors(boolean canOpenDoors) {
		nodeMaker.setCanOpenDoors(canOpenDoors);
	}
}
