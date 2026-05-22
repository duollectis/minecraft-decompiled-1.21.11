package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jspecify.annotations.Nullable;

/**
 * Фаза захода на посадку. Дракон летит к порталу, учитывая позицию ближайшего игрока
 * для выбора оптимального узла подлёта, затем переходит в {@link LandingPhase}.
 */
public class LandingApproachPhase extends AbstractPhase {

	private static final TargetPredicate PLAYERS_IN_RANGE_PREDICATE =
			TargetPredicate.createAttackable().ignoreVisibility();
	private static final double MIN_PATH_DIST_SQ = 100.0;
	private static final double MAX_PATH_DIST_SQ = 22500.0;
	private static final double APPROACH_RADIUS = 40.0;
	private static final double APPROACH_HEIGHT = 105.0;
	private static final float PATH_Y_RANDOM_RANGE = 20.0F;

	private @Nullable Path path;
	private @Nullable Vec3d pathTarget;

	public LandingApproachPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public PhaseType<LandingApproachPhase> getType() {
		return PhaseType.LANDING_APPROACH;
	}

	@Override
	public void beginPhase() {
		path = null;
		pathTarget = null;
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
			updatePath(world);
		}
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return pathTarget;
	}

	private void updatePath(ServerWorld world) {
		if (path == null || path.isFinished()) {
			int nearestNode = dragon.getNearestPathNodeIndex();
			BlockPos portalTop = world.getTopPosition(
					Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
					EndPortalFeature.offsetOrigin(dragon.getFightOrigin())
			);
			PlayerEntity nearestPlayer = world.getClosestPlayer(
					PLAYERS_IN_RANGE_PREDICATE, dragon, portalTop.getX(), portalTop.getY(), portalTop.getZ()
			);

			int approachNode;
			if (nearestPlayer != null) {
				Vec3d playerDir = new Vec3d(nearestPlayer.getX(), 0.0, nearestPlayer.getZ()).normalize();
				approachNode = dragon.getNearestPathNodeIndex(-playerDir.x * APPROACH_RADIUS, APPROACH_HEIGHT, -playerDir.z * APPROACH_RADIUS);
			} else {
				approachNode = dragon.getNearestPathNodeIndex(APPROACH_RADIUS, portalTop.getY(), 0.0);
			}

			PathNode landingNode = new PathNode(portalTop.getX(), portalTop.getY(), portalTop.getZ());
			path = dragon.findPath(nearestNode, approachNode, landingNode);

			if (path != null) {
				path.next();
			}
		}

		followPath();

		if (path != null && path.isFinished()) {
			dragon.getPhaseManager().setPhase(PhaseType.LANDING);
		}
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
}
