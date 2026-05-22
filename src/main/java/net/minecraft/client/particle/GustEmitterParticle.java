package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Невидимый эмиттер порывов ветра: каждые {@code interval+1} тиков порождает
 * три дочерних частицы {@link ParticleTypes#GUST} в случайных точках вокруг
 * своего центра. Используется для создания визуального эффекта порыва ветра
 * от атаки Breeze-моба.
 */
@Environment(EnvType.CLIENT)
public class GustEmitterParticle extends NoRenderParticle {

	private final double deviation;
	private final int interval;

	GustEmitterParticle(ClientWorld world, double x, double y, double z, double deviation, int maxAge, int interval) {
		super(world, x, y, z, 0.0, 0.0, 0.0);
		this.deviation = deviation;
		this.maxAge = maxAge;
		this.interval = interval;
	}

	@Override
	public void tick() {
		if (this.age % (this.interval + 1) == 0) {
			for (int spawnIndex = 0; spawnIndex < 3; spawnIndex++) {
				double spawnX = this.x + (this.random.nextDouble() - this.random.nextDouble()) * this.deviation;
				double spawnY = this.y + (this.random.nextDouble() - this.random.nextDouble()) * this.deviation;
				double spawnZ = this.z + (this.random.nextDouble() - this.random.nextDouble()) * this.deviation;
				this.world.addParticleClient(ParticleTypes.GUST, spawnX, spawnY, spawnZ, (float) this.age / this.maxAge, 0.0, 0.0);
			}
		}

		if (this.age++ == this.maxAge) {
			this.markDead();
		}
	}

	/**
	 * Фабрика для создания {@link GustEmitterParticle} с настраиваемыми параметрами
	 * разброса, времени жизни и интервала спавна дочерних частиц.
	 */
	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private final double deviation;
		private final int maxAge;
		private final int interval;

		public Factory(double deviation, int maxAge, int interval) {
			this.deviation = deviation;
			this.maxAge = maxAge;
			this.interval = interval;
		}

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
			return new GustEmitterParticle(world, x, y, z, this.deviation, this.maxAge, this.interval);
		}
	}
}
