package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code ExplosionEmitterParticle}.
 */
public class ExplosionEmitterParticle extends NoRenderParticle {

	ExplosionEmitterParticle(ClientWorld clientWorld, double d, double e, double f) {
		super(clientWorld, d, e, f, 0.0, 0.0, 0.0);
		this.maxAge = 8;
	}

	@Override
	public void tick() {
		for (int i = 0; i < 6; i++) {
			double d = this.x + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
			double e = this.y + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
			double f = this.z + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
			this.world.addParticleClient(ParticleTypes.EXPLOSION, d, e, f, (float) this.age / this.maxAge, 0.0, 0.0);
		}

		this.age++;
		if (this.age == this.maxAge) {
			this.markDead();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<SimpleParticleType> {

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
			return new ExplosionEmitterParticle(clientWorld, d, e, f);
		}
	}
}
