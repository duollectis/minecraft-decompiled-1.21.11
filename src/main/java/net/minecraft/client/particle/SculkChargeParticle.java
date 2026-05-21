package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SculkChargeParticleEffect;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code SculkChargeParticle}.
 */
public class SculkChargeParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	SculkChargeParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider.getFirst());
		this.velocityMultiplier = 0.96F;
		this.spriteProvider = spriteProvider;
		this.scale(1.5F);
		this.collidesWithWorld = false;
		this.updateSprite(spriteProvider);
	}

	@Override
	public int getBrightness(float tint) {
		return 240;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(this.spriteProvider);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public record Factory(SpriteProvider spriteProvider) implements ParticleFactory<SculkChargeParticleEffect> {

		public Particle createParticle(
				SculkChargeParticleEffect sculkChargeParticleEffect,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			SculkChargeParticle
					sculkChargeParticle =
					new SculkChargeParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			sculkChargeParticle.setAlpha(1.0F);
			sculkChargeParticle.setVelocity(g, h, i);
			sculkChargeParticle.lastZRotation = sculkChargeParticleEffect.roll();
			sculkChargeParticle.zRotation = sculkChargeParticleEffect.roll();
			sculkChargeParticle.setMaxAge(random.nextInt(12) + 8);
			return sculkChargeParticle;
		}
	}
}
