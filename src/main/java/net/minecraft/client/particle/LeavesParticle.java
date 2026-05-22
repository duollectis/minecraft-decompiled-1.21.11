package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code LeavesParticle}.
 */
public class LeavesParticle extends BillboardParticle {

	private static final float SPEED_SCALE = 0.0025F;
	private static final int MAX_AGE = 300;
	private static final int MAX_AGE_TICKS = 300;
	private float angularVelocity = (float) Math.toRadians(this.random.nextBoolean() ? -30.0 : 30.0);
	private final float angularAcceleration = (float) Math.toRadians(this.random.nextBoolean() ? -5.0 : 5.0);
	private final float windAmplitude;
	private final boolean windOscillationEnabled;
	private final boolean driftEnabled;
	private final double driftX;
	private final double driftZ;
	private final double oscillationFrequency;

	protected LeavesParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			Sprite sprite,
			float gravity,
			float f,
			boolean bl,
			boolean bl2,
			float size,
			float initialYVelocity
	) {
		super(world, x, y, z, sprite);
		this.windAmplitude = f;
		this.windOscillationEnabled = bl;
		this.driftEnabled = bl2;
		this.maxAge = MAX_AGE;
		this.gravityStrength = gravity * 1.2F * SPEED_SCALE;
		float g = size * (this.random.nextBoolean() ? 0.05F : 0.075F);
		this.scale = g;
		this.setBoundingBoxSpacing(g, g);
		this.velocityMultiplier = 1.0F;
		this.velocityY = -initialYVelocity;
		float h = this.random.nextFloat();
		this.driftX = Math.cos(Math.toRadians(h * 60.0F)) * this.windAmplitude;
		this.driftZ = Math.sin(Math.toRadians(h * 60.0F)) * this.windAmplitude;
		this.oscillationFrequency = Math.toRadians(1000.0F + h * 3000.0F);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		if (this.maxAge-- <= 0) {
			this.markDead();
		}

		if (!this.dead) {
			float f = 300 - this.maxAge;
			float g = Math.min(f / 300.0F, 1.0F);
			double d = 0.0;
			double e = 0.0;
			if (this.driftEnabled) {
				d += this.driftX * Math.pow(g, 1.25);
				e += this.driftZ * Math.pow(g, 1.25);
			}

			if (this.windOscillationEnabled) {
				d += g * Math.cos(g * this.oscillationFrequency) * this.windAmplitude;
				e += g * Math.sin(g * this.oscillationFrequency) * this.windAmplitude;
			}

			this.velocityX += d * SPEED_SCALE;
			this.velocityZ += e * SPEED_SCALE;
			this.velocityY = this.velocityY - this.gravityStrength;
			this.angularVelocity = this.angularVelocity + this.angularAcceleration / 20.0F;
			this.lastZRotation = this.zRotation;
			this.zRotation = this.zRotation + this.angularVelocity / 20.0F;
			this.move(this.velocityX, this.velocityY, this.velocityZ);
			if (this.onGround || this.maxAge < 299 && (this.velocityX == 0.0 || this.velocityZ == 0.0)) {
				this.markDead();
			}

			if (!this.dead) {
				this.velocityX = this.velocityX * this.velocityMultiplier;
				this.velocityY = this.velocityY * this.velocityMultiplier;
				this.velocityZ = this.velocityZ * this.velocityMultiplier;
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CherryLeavesFactory}.
	 */
	public static class CherryLeavesFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public CherryLeavesFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				SimpleParticleType simpleParticleType,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			return new LeavesParticle(
					clientWorld,
					d,
					e,
					f,
					this.spriteProvider.getSprite(random),
					0.25F,
					2.0F,
					false,
					true,
					1.0F,
					0.0F
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PaleOakLeavesFactory}.
	 */
	public static class PaleOakLeavesFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public PaleOakLeavesFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				SimpleParticleType simpleParticleType,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			return new LeavesParticle(
					clientWorld,
					d,
					e,
					f,
					this.spriteProvider.getSprite(random),
					0.07F,
					10.0F,
					true,
					false,
					2.0F,
					0.021F
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TintedLeavesFactory}.
	 */
	public static class TintedLeavesFactory implements ParticleFactory<TintedParticleEffect> {

		private final SpriteProvider spriteProvider;

		public TintedLeavesFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				TintedParticleEffect tintedParticleEffect,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			LeavesParticle leavesParticle = new LeavesParticle(
					clientWorld, d, e, f, this.spriteProvider.getSprite(random), 0.07F, 10.0F, true, false, 2.0F, 0.021F
			);
			leavesParticle.setColor(
					tintedParticleEffect.getRed(),
					tintedParticleEffect.getGreen(),
					tintedParticleEffect.getBlue()
			);
			return leavesParticle;
		}
	}
}
