package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DragonBreathParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code DragonBreathParticle}.
 */
public class DragonBreathParticle extends BillboardParticle {

	private static final int MIN_COLOR = 11993298;
	private static final int MAX_COLOR = 14614777;
	private static final float MIN_RED = 0.7176471F;
	private static final float MIN_GREEN = 0.0F;
	private static final float MIN_BLUE = 0.8235294F;
	private static final float MAX_RED = 0.8745098F;
	private static final float MAX_GREEN = 0.0F;
	private static final float MAX_BLUE = 0.9764706F;
	private boolean reachedGround;
	private final SpriteProvider spriteProvider;

	DragonBreathParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider.getFirst());
		this.velocityMultiplier = 0.96F;
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		this.red = MathHelper.nextFloat(this.random, 0.7176471F, 0.8745098F);
		this.green = MathHelper.nextFloat(this.random, 0.0F, 0.0F);
		this.blue = MathHelper.nextFloat(this.random, 0.8235294F, 0.9764706F);
		this.scale *= 0.75F;
		this.maxAge = (int) (20.0 / (this.random.nextFloat() * 0.8 + 0.2));
		this.reachedGround = false;
		this.collidesWithWorld = false;
		this.spriteProvider = spriteProvider;
		this.updateSprite(spriteProvider);
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		if (this.age++ >= this.maxAge) {
			this.markDead();
		}
		else {
			this.updateSprite(this.spriteProvider);
			if (this.onGround) {
				this.velocityY = 0.0;
				this.reachedGround = true;
			}

			if (this.reachedGround) {
				this.velocityY += 0.002;
			}

			this.move(this.velocityX, this.velocityY, this.velocityZ);
			if (this.y == this.lastY) {
				this.velocityX *= 1.1;
				this.velocityZ *= 1.1;
			}

			this.velocityX = this.velocityX * this.velocityMultiplier;
			this.velocityZ = this.velocityZ * this.velocityMultiplier;
			if (this.reachedGround) {
				this.velocityY = this.velocityY * this.velocityMultiplier;
			}
		}
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * 32.0F, 0.0F, 1.0F);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<DragonBreathParticleEffect> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				DragonBreathParticleEffect dragonBreathParticleEffect,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			DragonBreathParticle
					dragonBreathParticle =
					new DragonBreathParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			dragonBreathParticle.move(dragonBreathParticleEffect.getPower());
			return dragonBreathParticle;
		}
	}
}
