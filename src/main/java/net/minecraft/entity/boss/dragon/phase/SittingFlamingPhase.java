package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Фаза дыхания огнём. Дракон создаёт {@link AreaEffectCloudEntity} с эффектом мгновенного урона
 * и испускает частицы дыхания. После {@code MAX_TIMES_RUN} повторений переходит в {@link TakeoffPhase}.
 */
public class SittingFlamingPhase extends AbstractSittingPhase {

	private static final int DURATION = 200;
	private static final int MAX_TIMES_RUN = 4;
	private static final int BREATH_SPAWN_TICK = 10;
	private static final int BREATH_PARTICLE_GROUPS = 8;
	private static final int BREATH_PARTICLE_STREAMS = 6;
	private static final float BREATH_SPREAD = 0.08F;
	private static final float BREATH_VERTICAL = 0.6F;
	private static final float BREATH_ROTATION_STEP = (float) (Math.PI / 16);
	private static final float BREATH_INITIAL_ROTATION = (float) (-Math.PI / 4);
	private static final float CLOUD_RADIUS = 5.0F;
	private static final float CLOUD_POTION_SCALE = 0.25F;
	private static final double CLOUD_FORWARD_OFFSET = 5.0;

	private int ticks;
	private int timesRun;
	private @Nullable AreaEffectCloudEntity dragonBreathCloud;

	public SittingFlamingPhase(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Override
	public void clientTick() {
		ticks++;

		if (ticks % 2 != 0 || ticks >= BREATH_SPAWN_TICK) {
			return;
		}

		Vec3d lookDir = dragon.getRotationVectorFromPhase(1.0F).normalize();
		lookDir.rotateY(BREATH_INITIAL_ROTATION);
		double headX = dragon.head.getX();
		double headY = dragon.head.getBodyY(0.5);
		double headZ = dragon.head.getZ();

		for (int groupIdx = 0; groupIdx < BREATH_PARTICLE_GROUPS; groupIdx++) {
			double spawnX = headX + dragon.getRandom().nextGaussian() / 2.0;
			double spawnY = headY + dragon.getRandom().nextGaussian() / 2.0;
			double spawnZ = headZ + dragon.getRandom().nextGaussian() / 2.0;

			for (int streamIdx = 0; streamIdx < BREATH_PARTICLE_STREAMS; streamIdx++) {
				dragon.getEntityWorld().addParticleClient(
						DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 1.0F),
						spawnX, spawnY, spawnZ,
						-lookDir.x * BREATH_SPREAD * streamIdx,
						-lookDir.y * BREATH_VERTICAL,
						-lookDir.z * BREATH_SPREAD * streamIdx
				);
			}

			lookDir.rotateY(BREATH_ROTATION_STEP);
		}
	}

	@Override
	public void serverTick(ServerWorld world) {
		ticks++;

		if (ticks >= DURATION) {
			dragon.getPhaseManager().setPhase(
					timesRun >= MAX_TIMES_RUN ? PhaseType.TAKEOFF : PhaseType.SITTING_SCANNING
			);
			return;
		}

		if (ticks != BREATH_SPAWN_TICK) {
			return;
		}

		Vec3d headDir = new Vec3d(
				dragon.head.getX() - dragon.getX(),
				0.0,
				dragon.head.getZ() - dragon.getZ()
		).normalize();

		double cloudX = dragon.head.getX() + headDir.x * CLOUD_FORWARD_OFFSET / 2.0;
		double cloudZ = dragon.head.getZ() + headDir.z * CLOUD_FORWARD_OFFSET / 2.0;
		double headY = dragon.head.getBodyY(0.5);
		double cloudY = headY;
		BlockPos.Mutable scanPos = new BlockPos.Mutable(cloudX, headY, cloudZ);

		while (world.isAir(scanPos)) {
			if (--cloudY < 0.0) {
				cloudY = headY;
				break;
			}

			scanPos.set(cloudX, cloudY, cloudZ);
		}

		cloudY = MathHelper.floor(cloudY) + 1;
		dragonBreathCloud = new AreaEffectCloudEntity(world, cloudX, cloudY, cloudZ);
		dragonBreathCloud.setOwner(dragon);
		dragonBreathCloud.setRadius(CLOUD_RADIUS);
		dragonBreathCloud.setDuration(DURATION);
		dragonBreathCloud.setParticleType(DragonBreathParticleEffect.of(ParticleTypes.DRAGON_BREATH, 1.0F));
		dragonBreathCloud.setPotionDurationScale(CLOUD_POTION_SCALE);
		dragonBreathCloud.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE));
		world.spawnEntity(dragonBreathCloud);
	}

	@Override
	public void beginPhase() {
		ticks = 0;
		timesRun++;
	}

	@Override
	public void endPhase() {
		if (dragonBreathCloud == null) {
			return;
		}

		dragonBreathCloud.discard();
		dragonBreathCloud = null;
	}

	@Override
	public PhaseType<SittingFlamingPhase> getType() {
		return PhaseType.SITTING_FLAMING;
	}

	public void reset() {
		timesRun = 0;
	}
}
