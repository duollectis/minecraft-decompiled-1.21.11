package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jspecify.annotations.Nullable;

/**
 * Фаза посадки. Дракон снижается к вершине портала, испуская частицы дыхания.
 * По достижении цели переходит в {@link SittingScanningPhase}.
 */
public class LandingPhase extends AbstractPhase {

	private static final double LANDING_ARRIVAL_DIST_SQ = 1.0;
	private static final int BREATH_PARTICLE_COUNT = 8;
	private static final float BREATH_PARTICLE_SPREAD = 0.08F;
	private static final float BREATH_PARTICLE_VERTICAL = 0.3F;
	private static final float BREATH_ROTATION_STEP = (float) (Math.PI / 16);
	private static final float BREATH_INITIAL_ROTATION = (float) (-Math.PI / 4);

	private @Nullable Vec3d target;

	public LandingPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void clientTick() {
		Vec3d lookDir = dragon.getRotationVectorFromPhase(1.0F).normalize();
		lookDir.rotateY(BREATH_INITIAL_ROTATION);
		double headX = dragon.head.getX();
		double headY = dragon.head.getBodyY(0.5);
		double headZ = dragon.head.getZ();

		for (int particleIdx = 0; particleIdx < BREATH_PARTICLE_COUNT; particleIdx++) {
			Random random = dragon.getRandom();
			double spawnX = headX + random.nextGaussian() / 2.0;
			double spawnY = headY + random.nextGaussian() / 2.0;
			double spawnZ = headZ + random.nextGaussian() / 2.0;
			Vec3d velocity = dragon.getVelocity();
			dragon.getEntityWorld().addParticleClient(
					DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 1.0F),
					spawnX, spawnY, spawnZ,
					-lookDir.x * BREATH_PARTICLE_SPREAD + velocity.x,
					-lookDir.y * BREATH_PARTICLE_VERTICAL + velocity.y,
					-lookDir.z * BREATH_PARTICLE_SPREAD + velocity.z
			);
			lookDir.rotateY(BREATH_ROTATION_STEP);
		}
	}

	@Override
	public void serverTick(ServerWorld world) {
		if (target == null) {
			target = Vec3d.ofBottomCenter(
					world.getTopPosition(
							Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
							EndPortalFeature.offsetOrigin(dragon.getFightOrigin())
					)
			);
		}

		if (target.squaredDistanceTo(dragon.getX(), dragon.getY(), dragon.getZ()) < LANDING_ARRIVAL_DIST_SQ) {
			dragon.getPhaseManager().create(PhaseType.SITTING_FLAMING).reset();
			dragon.getPhaseManager().setPhase(PhaseType.SITTING_SCANNING);
		}
	}

	@Override
	public float getMaxYAcceleration() {
		return 1.5F;
	}

	@Override
	public float getYawAcceleration() {
		float speed = (float) dragon.getVelocity().horizontalLength() + 1.0F;
		float capped = Math.min(speed, 40.0F);
		return capped / speed;
	}

	@Override
	public void beginPhase() {
		target = null;
	}

	@Override
	public @Nullable Vec3d getPathTarget() {
		return target;
	}

	@Override
	public PhaseType<LandingPhase> getType() {
		return PhaseType.LANDING;
	}
}
