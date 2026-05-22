package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Фаза сканирования. Дракон поворачивается к ближайшему игроку в радиусе 20 блоков.
 * При обнаружении игрока вблизи переходит в {@link SittingAttackingPhase},
 * по истечении таймаута — в {@link TakeoffPhase} или {@link ChargingPlayerPhase}.
 */
public class SittingScanningPhase extends AbstractSittingPhase {

	private static final int DURATION = 100;
	private static final int ATTACK_TRIGGER_TICKS = 25;
	private static final double CLOSE_PLAYER_HORIZONTAL_RANGE = 20.0;
	private static final double CLOSE_PLAYER_HEIGHT_RANGE = 10.0;
	private static final double FAR_PLAYER_RANGE = 150.0;
	private static final float MAX_YAW_DELTA = 100.0F;
	private static final float MAX_DIST_FOR_FULL_TURN = 40.0F;
	private static final float ALIGNMENT_THRESHOLD = 10.0F;
	private static final float YAW_DAMPING = 0.8F;

	private static final TargetPredicate PLAYER_WITHIN_RANGE_PREDICATE =
			TargetPredicate.createAttackable().setBaseMaxDistance(FAR_PLAYER_RANGE);

	private final TargetPredicate closePlayerPredicate;
	private int ticks;

	public SittingScanningPhase(EnderDragonEntity dragon) {
		super(dragon);
		closePlayerPredicate = TargetPredicate.createAttackable()
				.setBaseMaxDistance(CLOSE_PLAYER_HORIZONTAL_RANGE)
				.setPredicate((player, world) ->
						Math.abs(player.getY() - dragon.getY()) <= CLOSE_PLAYER_HEIGHT_RANGE);
	}

	@Override
	public void serverTick(ServerWorld world) {
		ticks++;
		LivingEntity closePlayer = world.getClosestPlayer(
				closePlayerPredicate, dragon, dragon.getX(), dragon.getY(), dragon.getZ()
		);

		if (closePlayer != null) {
			if (ticks > ATTACK_TRIGGER_TICKS) {
				dragon.getPhaseManager().setPhase(PhaseType.SITTING_ATTACKING);
			} else {
				trackPlayer(closePlayer);
			}

			return;
		}

		if (ticks < DURATION) {
			return;
		}

		LivingEntity farPlayer = world.getClosestPlayer(
				PLAYER_WITHIN_RANGE_PREDICATE, dragon, dragon.getX(), dragon.getY(), dragon.getZ()
		);
		dragon.getPhaseManager().setPhase(PhaseType.TAKEOFF);

		if (farPlayer != null) {
			dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
			dragon.getPhaseManager()
					.create(PhaseType.CHARGING_PLAYER)
					.setPathTarget(new Vec3d(farPlayer.getX(), farPlayer.getY(), farPlayer.getZ()));
		}
	}

	private void trackPlayer(LivingEntity player) {
		Vec3d toPlayer = new Vec3d(
				player.getX() - dragon.getX(),
				0.0,
				player.getZ() - dragon.getZ()
		).normalize();
		Vec3d dragonDir = new Vec3d(
				MathHelper.sin(dragon.getYaw() * (float) (Math.PI / 180.0)),
				0.0,
				-MathHelper.cos(dragon.getYaw() * (float) (Math.PI / 180.0))
		).normalize();

		float angleDot = (float) dragonDir.dotProduct(toPlayer);
		float angleDeg = (float) (Math.acos(angleDot) * 180.0F / (float) Math.PI) + 0.5F;

		if (angleDeg < 0.0F || angleDeg <= ALIGNMENT_THRESHOLD) {
			return;
		}

		double relX = player.getX() - dragon.head.getX();
		double relZ = player.getZ() - dragon.head.getZ();
		double yawDelta = MathHelper.clamp(
				MathHelper.wrapDegrees(180.0 - MathHelper.atan2(relX, relZ) * 180.0F / (float) Math.PI - dragon.getYaw()),
				-MAX_YAW_DELTA, MAX_YAW_DELTA
		);

		dragon.yawAcceleration *= YAW_DAMPING;
		float horizDist = (float) Math.sqrt(relX * relX + relZ * relZ) + 1.0F;
		float cappedDist = Math.min(horizDist, MAX_DIST_FOR_FULL_TURN);
		dragon.yawAcceleration += (float) yawDelta * (0.7F / cappedDist / horizDist);
		dragon.setYaw(dragon.getYaw() + dragon.yawAcceleration);
	}

	@Override
	public void beginPhase() {
		ticks = 0;
	}

	@Override
	public PhaseType<SittingScanningPhase> getType() {
		return PhaseType.SITTING_SCANNING;
	}
}
