package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jspecify.annotations.Nullable;

/**
 * Фаза патрулирования — дракон летит по кольцевому маршруту вокруг острова.
 * Периодически переходит в атаку на игрока ({@link StrafePlayerPhase})
 * или начинает снижение ({@link LandingApproachPhase}).
 */
public class HoldingPatternPhase extends AbstractPhase {

	private static final TargetPredicate PLAYERS_IN_RANGE_PREDICATE =
			TargetPredicate.createAttackable().ignoreVisibility();
	private static final double MIN_PATH_DIST_SQ = 100.0;
	private static final double MAX_PATH_DIST_SQ = 22500.0;
	private static final double PLAYER_DIST_SCALE = 512.0;
	private static final double DEFAULT_PLAYER_DIST = 64.0;
	private static final int PATH_DIRECTION_CHANGE_CHANCE = 8;
	private static final int PATH_DIRECTION_OFFSET = 6;
	private static final int OUTER_RING_SIZE = 12;
	private static final int INNER_RING_MASK = 7;
	private static final float PATH_Y_RANDOM_RANGE = 20.0F;

	private @Nullable Path path;
	private @Nullable Vec3d pathTarget;
	private boolean shouldFindNewPath;

	public HoldingPatternPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public PhaseType<HoldingPatternPhase> getType() {
		return PhaseType.HOLDING_PATTERN;
	}

	@Override
	public void serverTick(ServerWorld world) {
		double distSq = pathTarget == null
				? 0.0
				: pathTarget.squaredDistanceTo(dragon.getX(), dragon.getY(), dragon.getZ());

		if (distSq < MIN_PATH_DIST_SQ
				|| distSq > MAX_PATH_DIST_SQ
				|| dragon.horizontalCollision
				|| dragon.verticalCollision) {
			tickInRange(world);
		}
	}

	@Override
	public void beginPhase() {
		path = null;
		pathTarget = null;
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return pathTarget;
	}

	private void tickInRange(ServerWorld world) {
		if (path != null && path.isFinished()) {
			BlockPos portalTop = world.getTopPosition(
					Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
					EndPortalFeature.offsetOrigin(dragon.getFightOrigin())
			);
			int aliveCrystals = dragon.getFight() == null ? 0 : dragon.getFight().getAliveEndCrystals();

			if (dragon.getRandom().nextInt(aliveCrystals + 3) == 0) {
				dragon.getPhaseManager().setPhase(PhaseType.LANDING_APPROACH);
				return;
			}

			PlayerEntity nearestPlayer = world.getClosestPlayer(
					PLAYERS_IN_RANGE_PREDICATE, dragon, portalTop.getX(), portalTop.getY(), portalTop.getZ()
			);
			double playerDistFactor = nearestPlayer != null
					? portalTop.getSquaredDistance(nearestPlayer.getEntityPos()) / PLAYER_DIST_SCALE
					: DEFAULT_PLAYER_DIST;

			if (nearestPlayer != null
					&& (dragon.getRandom().nextInt((int) (playerDistFactor + 2.0)) == 0
					|| dragon.getRandom().nextInt(aliveCrystals + 2) == 0)) {
				strafePlayer(nearestPlayer);
				return;
			}
		}

		if (path == null || path.isFinished()) {
			int nearestNode = dragon.getNearestPathNodeIndex();
			int targetNode = nearestNode;

			if (dragon.getRandom().nextInt(PATH_DIRECTION_CHANGE_CHANCE) == 0) {
				shouldFindNewPath = !shouldFindNewPath;
				targetNode = nearestNode + PATH_DIRECTION_OFFSET;
			}

			targetNode = shouldFindNewPath ? targetNode + 1 : targetNode - 1;
			targetNode = normalizeNodeIndex(targetNode);

			path = dragon.findPath(nearestNode, targetNode, null);
			if (path != null) {
				path.next();
			}
		}

		followPath();
	}

	private int normalizeNodeIndex(int nodeIdx) {
		if (dragon.getFight() != null && dragon.getFight().getAliveEndCrystals() >= 0) {
			nodeIdx %= OUTER_RING_SIZE;
			if (nodeIdx < 0) {
				nodeIdx += OUTER_RING_SIZE;
			}
		} else {
			nodeIdx -= OUTER_RING_SIZE;
			nodeIdx &= INNER_RING_MASK;
			nodeIdx += OUTER_RING_SIZE;
		}

		return nodeIdx;
	}

	private void strafePlayer(PlayerEntity player) {
		dragon.getPhaseManager().setPhase(PhaseType.STRAFE_PLAYER);
		dragon.getPhaseManager().create(PhaseType.STRAFE_PLAYER).setTargetEntity(player);
	}

	private void followPath() {
		if (path == null || path.isFinished()) {
			return;
		}

		Vec3i nodePos = path.getCurrentNodePos();
		path.next();

		double targetY;
		do {
			targetY = nodePos.getY() + dragon.getRandom().nextFloat() * PATH_Y_RANDOM_RANGE;
		} while (targetY < nodePos.getY());

		pathTarget = new Vec3d(nodePos.getX(), targetY, nodePos.getZ());
	}

	@Override
	public void crystalDestroyed(
			EndCrystalEntity crystal,
			BlockPos pos,
			DamageSource source,
			@Nullable PlayerEntity player
	) {
		if (player != null && dragon.canTarget(player)) {
			strafePlayer(player);
		}
	}
}
