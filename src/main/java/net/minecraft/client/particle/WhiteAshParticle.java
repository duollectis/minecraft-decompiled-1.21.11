package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица белого пепла (White Ash), медленно оседающая вниз.
 * Используется в биомах с горящей растительностью (Basalt Deltas и др.).
 * Цвет — светло-серый (#BAC0C2), движение — медленное нисходящее с лёгким дрейфом.
 */
@Environment(EnvType.CLIENT)
public class WhiteAshParticle extends AscendingParticle {

	// 0xBAC0C2 — светло-серый цвет пепла
	private static final int ASH_COLOR = 12235202;
	private static final float HORIZONTAL_SPREAD = 0.1F;
	private static final float VERTICAL_SPREAD = -0.1F;
	private static final float SLOW_FACTOR = 0.0125F;
	private static final int MAX_AGE = 20;

	protected WhiteAshParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			float scaleMultiplier,
			SpriteProvider spriteProvider
	) {
		super(
				world,
				x,
				y,
				z,
				HORIZONTAL_SPREAD,
				VERTICAL_SPREAD,
				HORIZONTAL_SPREAD,
				velocityX,
				velocityY,
				velocityZ,
				scaleMultiplier,
				spriteProvider,
				0.0F,
				MAX_AGE,
				SLOW_FACTOR,
				false
		);
		this.red = ColorHelper.getRed(ASH_COLOR) / 255.0F;
		this.green = ColorHelper.getGreen(ASH_COLOR) / 255.0F;
		this.blue = ColorHelper.getBlue(ASH_COLOR) / 255.0F;
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private static final double DRIFT_SCALE = 1.9;
		private static final double DRIFT_VERTICAL_SCALE = 0.5;
		private static final double DRIFT_FACTOR = 0.1;

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
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
			double driftX = random.nextFloat() * -DRIFT_SCALE * random.nextFloat() * DRIFT_FACTOR;
			double driftY = random.nextFloat() * -DRIFT_VERTICAL_SCALE * random.nextFloat() * DRIFT_FACTOR * 5.0;
			double driftZ = random.nextFloat() * -DRIFT_SCALE * random.nextFloat() * DRIFT_FACTOR;
			return new WhiteAshParticle(world, x, y, z, driftX, driftY, driftZ, 1.0F, spriteProvider);
		}
	}
}
