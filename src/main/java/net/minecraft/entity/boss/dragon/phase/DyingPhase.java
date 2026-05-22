package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jspecify.annotations.Nullable;

/**
 * Финальная фаза гибели дракона. Дракон летит к центру портала;
 * при достижении или столкновении здоровье обнуляется, запуская анимацию смерти.
 */
public class DyingPhase extends AbstractPhase {

	private static final int EXPLOSION_PARTICLE_INTERVAL = 10;
	private static final float EXPLOSION_SPREAD_XZ = 8.0F;
	private static final float EXPLOSION_SPREAD_Y = 4.0F;
	private static final double MIN_DIST_SQ = 100.0;
	private static final double MAX_DIST_SQ = 22500.0;

	private @Nullable Vec3d target;
	private int ticks;

	public DyingPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void clientTick() {
		if (ticks++ % EXPLOSION_PARTICLE_INTERVAL == 0) {
			float offsetX = (dragon.getRandom().nextFloat() - 0.5F) * EXPLOSION_SPREAD_XZ;
			float offsetY = (dragon.getRandom().nextFloat() - 0.5F) * EXPLOSION_SPREAD_Y;
			float offsetZ = (dragon.getRandom().nextFloat() - 0.5F) * EXPLOSION_SPREAD_XZ;
			dragon.getEntityWorld().addParticleClient(
					ParticleTypes.EXPLOSION_EMITTER,
					dragon.getX() + offsetX,
					dragon.getY() + 2.0 + offsetY,
					dragon.getZ() + offsetZ,
					0.0, 0.0, 0.0
			);
		}
	}

	@Override
	public void serverTick(ServerWorld world) {
		ticks++;

		if (target == null) {
			BlockPos portalTop = world.getTopPosition(
					Heightmap.Type.MOTION_BLOCKING,
					EndPortalFeature.offsetOrigin(dragon.getFightOrigin())
			);
			target = Vec3d.ofBottomCenter(portalTop);
		}

		double distSq = target.squaredDistanceTo(dragon.getX(), dragon.getY(), dragon.getZ());
		boolean inRange = distSq >= MIN_DIST_SQ && distSq <= MAX_DIST_SQ;
		boolean noCollision = !dragon.horizontalCollision && !dragon.verticalCollision;

		dragon.setHealth(inRange && noCollision ? 1.0F : 0.0F);
	}

	@Override
	public void beginPhase() {
		target = null;
		ticks = 0;
	}

	@Override
	public float getMaxYAcceleration() {
		return 3.0F;
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return target;
	}

	@Override
	public PhaseType<DyingPhase> getType() {
		return PhaseType.DYING;
	}
}
