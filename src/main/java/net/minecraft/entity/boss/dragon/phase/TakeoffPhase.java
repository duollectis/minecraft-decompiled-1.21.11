package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jspecify.annotations.Nullable;

/**
 * Фаза взлёта. Дракон поднимается от портала и переходит в {@link HoldingPatternPhase}
 * как только удаляется от портала на достаточное расстояние.
 */
public class TakeoffPhase extends AbstractPhase {

	private static final double TAKEOFF_DISTANCE = 10.0;
	private static final double APPROACH_RADIUS = 40.0;
	private static final double APPROACH_HEIGHT = 105.0;
	private static final int OUTER_RING_SIZE = 12;
	private static final int INNER_RING_MASK = 7;
	private static final float PATH_Y_RANDOM_RANGE = 20.0F;

	private boolean shouldFindNewPath;
	private @Nullable Path path;
	private @Nullable Vec3d pathTarget;

	public TakeoffPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void serverTick(ServerWorld world) {
		if (shouldFindNewPath || path == null) {
			shouldFindNewPath = false;
			updatePath();
			return;
		}

		BlockPos portalTop = world.getTopPosition(
				Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
				EndPortalFeature.offsetOrigin(dragon.getFightOrigin())
		);

		if (!portalTop.isWithinDistance(dragon.getEntityPos(), TAKEOFF_DISTANCE)) {
			dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
		}
	}

	@Override
	public void beginPhase() {
		shouldFindNewPath = true;
		path = null;
		pathTarget = null;
	}

	private void updatePath() {
		int nearestNode = dragon.getNearestPathNodeIndex();
		Vec3d lookDir = dragon.getRotationVectorFromPhase(1.0F);
		int targetNode = dragon.getNearestPathNodeIndex(-lookDir.x * APPROACH_RADIUS, APPROACH_HEIGHT, -lookDir.z * APPROACH_RADIUS);

		if (dragon.getFight() != null && dragon.getFight().getAliveEndCrystals() > 0) {
			targetNode %= OUTER_RING_SIZE;
			if (targetNode < 0) {
				targetNode += OUTER_RING_SIZE;
			}
		} else {
			targetNode -= OUTER_RING_SIZE;
			targetNode &= INNER_RING_MASK;
			targetNode += OUTER_RING_SIZE;
		}

		path = dragon.findPath(nearestNode, targetNode, null);
		followPath();
	}

	private void followPath() {
		if (path == null) {
			return;
		}

		path.next();

		if (path.isFinished()) {
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
	public @Nullable Vec3d getPathTarget() {
		return pathTarget;
	}

	@Override
	public PhaseType<TakeoffPhase> getType() {
		return PhaseType.TAKEOFF;
	}
}
