package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Невидимая частица-эмиттер взрыва. За 8 тиков своей жизни каждый тик
 * порождает 6 дочерних частиц {@link ParticleTypes#EXPLOSION}, разлетающихся
 * в случайных направлениях в радиусе 4 блоков. Сама не рендерится.
 */
@Environment(EnvType.CLIENT)
public class ExplosionEmitterParticle extends NoRenderParticle {

	private static final int MAX_AGE = 8;
	private static final int PARTICLES_PER_TICK = 6;
	private static final double SPREAD_RADIUS = 4.0;

	ExplosionEmitterParticle(ClientWorld world, double x, double y, double z) {
		super(world, x, y, z, 0.0, 0.0, 0.0);
		this.maxAge = MAX_AGE;
	}

	@Override
	public void tick() {
		for (int spawnIndex = 0; spawnIndex < PARTICLES_PER_TICK; spawnIndex++) {
			double spawnX = this.x + (this.random.nextDouble() - this.random.nextDouble()) * SPREAD_RADIUS;
			double spawnY = this.y + (this.random.nextDouble() - this.random.nextDouble()) * SPREAD_RADIUS;
			double spawnZ = this.z + (this.random.nextDouble() - this.random.nextDouble()) * SPREAD_RADIUS;
			this.world.addParticleClient(
					ParticleTypes.EXPLOSION,
					spawnX,
					spawnY,
					spawnZ,
					(float) this.age / this.maxAge,
					0.0,
					0.0
			);
		}

		this.age++;

		if (this.age == this.maxAge) {
			this.markDead();
		}
	}

	/**
	 * Фабрика для создания эмиттера взрыва.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		@Override
		public Particle createParticle(
				SimpleParticleType type,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			return new ExplosionEmitterParticle(world, x, y, z);
		}
	}
}
