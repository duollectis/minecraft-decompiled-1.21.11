package net.minecraft.entity.boss.dragon.phase;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Фаза таранного удара по игроку. Дракон летит прямо к заданной точке;
 * после достижения или столкновения возвращается в {@link HoldingPatternPhase}.
 */
public class ChargingPlayerPhase extends AbstractPhase {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int CHARGE_COMPLETE_TICKS = 10;
	private static final double MIN_DIST_SQ = 100.0;
	private static final double MAX_DIST_SQ = 22500.0;

	private @Nullable Vec3d pathTarget;
	private int chargingTicks;

	public ChargingPlayerPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void serverTick(ServerWorld world) {
		if (pathTarget == null) {
			LOGGER.warn("Aborting charge player as no target was set.");
			dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
			return;
		}

		if (chargingTicks > 0 && chargingTicks++ >= CHARGE_COMPLETE_TICKS) {
			dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
			return;
		}

		double distSq = pathTarget.squaredDistanceTo(dragon.getX(), dragon.getY(), dragon.getZ());
		if (distSq < MIN_DIST_SQ || distSq > MAX_DIST_SQ || dragon.horizontalCollision || dragon.verticalCollision) {
			chargingTicks++;
		}
	}

	@Override
	public void beginPhase() {
		pathTarget = null;
		chargingTicks = 0;
	}

	public void setPathTarget(Vec3d target) {
		pathTarget = target;
	}

	@Override
	public float getMaxYAcceleration() {
		return 3.0F;
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return pathTarget;
	}

	@Override
	public PhaseType<ChargingPlayerPhase> getType() {
		return PhaseType.CHARGING_PLAYER;
	}
}
