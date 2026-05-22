package net.minecraft.entity.boss.dragon.phase;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Фаза атаки на игрока огненным шаром. Дракон летит к цели и при достаточном
 * выравнивании (угол < 10°, видимость 5 тиков подряд) выпускает {@link DragonFireballEntity}.
 */
public class StrafePlayerPhase extends AbstractPhase {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int FIREBALL_SIGHT_TICKS = 5;
	private static final double MIN_PATH_DIST_SQ = 100.0;
	private static final double MAX_PATH_DIST_SQ = 22500.0;
	private static final double FIREBALL_SIGHT_RANGE_SQ = 4096.0;
	private static final double FIREBALL_ALIGNMENT_THRESHOLD = 10.0F;
	private static final double FIREBALL_OFFSET = 1.0;
	private static final double HEIGHT_APPROACH_MIN = 0.4;
	private static final double HEIGHT_APPROACH_SCALE = 80.0;
	private static final double HEIGHT_APPROACH_MAX = 10.0;
	private static final int PATH_DIRECTION_CHANGE_CHANCE = 8;
	private static final int PATH_DIRECTION_OFFSET = 6;
	private static final int OUTER_RING_SIZE = 12;
	private static final int INNER_RING_MASK = 7;
	private static final float PATH_Y_RANDOM_RANGE = 20.0F;
	private static final int WORLD_EVENT_FIREBALL = 1017;

	private int seenTargetTicks;
	private @Nullable Path path;
	private @Nullable Vec3d pathTarget;
	private @Nullable LivingEntity target;
	private boolean shouldFindNewPath;

	public StrafePlayerPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void serverTick(ServerWorld world) {
		if (target == null) {
			LOGGER.warn("Skipping player strafe phase because no player was found");
			dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
			return;
		}

		if (path != null && path.isFinished()) {
			double targetX = target.getX();
			double targetZ = target.getZ();
			double relX = targetX - dragon.getX();
			double relZ = targetZ - dragon.getZ();
			double horizDist = Math.sqrt(relX * relX + relZ * relZ);
			double heightOffset = Math.min(HEIGHT_APPROACH_MIN + horizDist / HEIGHT_APPROACH_SCALE - 1.0, HEIGHT_APPROACH_MAX);
			pathTarget = new Vec3d(targetX, target.getY() + heightOffset, targetZ);
		}

		double distSq = pathTarget == null
				? 0.0
				: pathTarget.squaredDistanceTo(dragon.getX(), dragon.getY(), dragon.getZ());

		if (distSq < MIN_PATH_DIST_SQ || distSq > MAX_PATH_DIST_SQ) {
			updatePath();
		}

		if (target.squaredDistanceTo(dragon) < FIREBALL_SIGHT_RANGE_SQ) {
			if (dragon.canSee(target)) {
				seenTargetTicks++;
				tryFireFireball(world);
			} else if (seenTargetTicks > 0) {
				seenTargetTicks--;
			}
		} else if (seenTargetTicks > 0) {
			seenTargetTicks--;
		}
	}

	private void tryFireFireball(ServerWorld world) {
		Vec3d toTarget = new Vec3d(
				target.getX() - dragon.getX(),
				0.0,
				target.getZ() - dragon.getZ()
		).normalize();
		Vec3d dragonDir = new Vec3d(
				MathHelper.sin(dragon.getYaw() * (float) (Math.PI / 180.0)),
				0.0,
				-MathHelper.cos(dragon.getYaw() * (float) (Math.PI / 180.0))
		).normalize();

		float angleDot = (float) dragonDir.dotProduct(toTarget);
		float angleDeg = (float) (Math.acos(angleDot) * 180.0F / (float) Math.PI) + 0.5F;

		if (seenTargetTicks < FIREBALL_SIGHT_TICKS || angleDeg < 0.0F || angleDeg >= FIREBALL_ALIGNMENT_THRESHOLD) {
			return;
		}

		Vec3d rotVec = dragon.getRotationVec(1.0F);
		double originX = dragon.head.getX() - rotVec.x * FIREBALL_OFFSET;
		double originY = dragon.head.getBodyY(0.5) + 0.5;
		double originZ = dragon.head.getZ() - rotVec.z * FIREBALL_OFFSET;
		Vec3d direction = new Vec3d(
				target.getX() - originX,
				target.getBodyY(0.5) - originY,
				target.getZ() - originZ
		).normalize();

		if (!dragon.isSilent()) {
			world.syncWorldEvent(null, WORLD_EVENT_FIREBALL, dragon.getBlockPos(), 0);
		}

		DragonFireballEntity fireball = new DragonFireballEntity(world, dragon, direction);
		fireball.refreshPositionAndAngles(originX, originY, originZ, 0.0F, 0.0F);
		world.spawnEntity(fireball);
		seenTargetTicks = 0;

		if (path != null) {
			while (!path.isFinished()) {
				path.next();
			}
		}

		dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
	}

	private void updatePath() {
		if (path == null || path.isFinished()) {
			int nearestNode = dragon.getNearestPathNodeIndex();
			int targetNode = nearestNode;

			if (dragon.getRandom().nextInt(PATH_DIRECTION_CHANGE_CHANCE) == 0) {
				shouldFindNewPath = !shouldFindNewPath;
				targetNode = nearestNode + PATH_DIRECTION_OFFSET;
			}

			targetNode = shouldFindNewPath ? targetNode + 1 : targetNode - 1;

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
			if (path != null) {
				path.next();
			}
		}

		followPath();
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
	public void beginPhase() {
		seenTargetTicks = 0;
		pathTarget = null;
		path = null;
		target = null;
	}

	public void setTargetEntity(LivingEntity targetEntity) {
		target = targetEntity;
		int nearestNode = dragon.getNearestPathNodeIndex();
		int targetNode = dragon.getNearestPathNodeIndex(target.getX(), target.getY(), target.getZ());
		int targetBlockX = target.getBlockX();
		int targetBlockZ = target.getBlockZ();
		double relX = targetBlockX - dragon.getX();
		double relZ = targetBlockZ - dragon.getZ();
		double horizDist = Math.sqrt(relX * relX + relZ * relZ);
		double heightOffset = Math.min(HEIGHT_APPROACH_MIN + horizDist / HEIGHT_APPROACH_SCALE - 1.0, HEIGHT_APPROACH_MAX);
		int approachY = MathHelper.floor(target.getY() + heightOffset);
		PathNode approachNode = new PathNode(targetBlockX, approachY, targetBlockZ);
		path = dragon.findPath(nearestNode, targetNode, approachNode);

		if (path != null) {
			path.next();
			followPath();
		}
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return pathTarget;
	}

	@Override
	public PhaseType<StrafePlayerPhase> getType() {
		return PhaseType.STRAFE_PLAYER;
	}
}
