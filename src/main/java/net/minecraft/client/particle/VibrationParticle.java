package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.event.PositionSource;
import org.joml.Quaternionf;

import java.util.Optional;

/**
 * Частица вибрации (Vibration), летящая к источнику сигнала Sculk Sensor.
 * Рендерится дважды с зеркальными кватернионами, создавая двустороннюю «крылатую» форму.
 * Ориентация в пространстве обновляется каждый тик по направлению к целевой точке.
 */
@Environment(EnvType.CLIENT)
public class VibrationParticle extends BillboardParticle {

	private static final float PARTICLE_SCALE = 0.3F;
	private static final int FULL_BRIGHTNESS = 240;
	// Угловая скорость колебания крыльев: 2π * 0.05 рад/тик
	private static final float WING_OSCILLATION_SPEED = 0.05F;
	private static final float PITCH_OFFSET = (float) (Math.PI / 2);

	private final PositionSource vibration;
	private float targetYaw;
	private float prevYaw;
	private float targetPitch;
	private float prevPitch;

	VibrationParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			PositionSource vibration,
			int maxAge,
			Sprite sprite
	) {
		super(world, x, y, z, 0.0, 0.0, 0.0, sprite);
		this.scale = PARTICLE_SCALE;
		this.vibration = vibration;
		this.maxAge = maxAge;

		Optional<Vec3d> targetPos = vibration.getPos(world);
		if (targetPos.isPresent()) {
			Vec3d pos = targetPos.get();
			double dx = x - pos.getX();
			double dy = y - pos.getY();
			double dz = z - pos.getZ();
			this.prevYaw = this.targetYaw = (float) MathHelper.atan2(dx, dz);
			this.prevPitch = this.targetPitch = (float) MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz));
		}
	}

	/**
	 * Рендерит частицу дважды с зеркальными кватернионами для создания двустороннего эффекта.
	 * Угол колебания крыльев вычисляется через синус от текущего возраста.
	 */
	@Override
	public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
		float wingAngle = MathHelper.sin((age + tickProgress - (float) (Math.PI * 2)) * WING_OSCILLATION_SPEED) * 2.0F;
		float yaw = MathHelper.lerp(tickProgress, prevYaw, targetYaw);
		float pitch = MathHelper.lerp(tickProgress, prevPitch, targetPitch) + PITCH_OFFSET;

		Quaternionf rotation = new Quaternionf();
		rotation.rotationY(yaw).rotateX(-pitch).rotateY(wingAngle);
		this.render(submittable, camera, rotation, tickProgress);

		rotation.rotationY((float) -Math.PI + yaw).rotateX(pitch).rotateY(wingAngle);
		this.render(submittable, camera, rotation, tickProgress);
	}

	@Override
	public int getBrightness(float tint) {
		return FULL_BRIGHTNESS;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;

		if (age++ >= maxAge) {
			this.markDead();
			return;
		}

		Optional<Vec3d> targetPos = vibration.getPos(world);
		if (targetPos.isEmpty()) {
			this.markDead();
			return;
		}

		Vec3d pos = targetPos.get();
		int remaining = maxAge - age;
		double lerpFactor = 1.0 / remaining;
		this.x = MathHelper.lerp(lerpFactor, this.x, pos.getX());
		this.y = MathHelper.lerp(lerpFactor, this.y, pos.getY());
		this.z = MathHelper.lerp(lerpFactor, this.z, pos.getZ());

		double dx = this.x - pos.getX();
		double dy = this.y - pos.getY();
		double dz = this.z - pos.getZ();
		this.prevYaw = targetYaw;
		this.targetYaw = (float) MathHelper.atan2(dx, dz);
		this.prevPitch = targetPitch;
		this.targetPitch = (float) MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz));
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<VibrationParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
				VibrationParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			VibrationParticle particle = new VibrationParticle(
					world,
					x,
					y,
					z,
					effect.getVibration(),
					effect.getArrivalInTicks(),
					spriteProvider.getSprite(random)
			);
			particle.setAlpha(1.0F);
			return particle;
		}
	}
}
