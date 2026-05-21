package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code SculkChargePopParticle}.
 */
public class SculkChargePopParticle extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	SculkChargePopParticle(
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
		this.scale(1.0F);
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
	public record Factory(SpriteProvider spriteProvider) implements ParticleFactory<SimpleParticleType> {

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
			SculkChargePopParticle
					sculkChargePopParticle =
					new SculkChargePopParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			sculkChargePopParticle.setAlpha(1.0F);
			sculkChargePopParticle.setVelocity(g, h, i);
			sculkChargePopParticle.setMaxAge(random.nextInt(4) + 6);
			return sculkChargePopParticle;
		}
	}
}
