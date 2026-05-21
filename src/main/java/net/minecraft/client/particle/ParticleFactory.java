package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ParticleFactory}.
 */
public interface ParticleFactory<T extends ParticleEffect> {

	@Nullable Particle createParticle(
			T parameters,
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Random random
	);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BlockLeakParticleFactory}.
	 */
	public interface BlockLeakParticleFactory<T extends ParticleEffect> {

		@Nullable BillboardParticle createParticle(
				T parameters,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		);
	}
}
