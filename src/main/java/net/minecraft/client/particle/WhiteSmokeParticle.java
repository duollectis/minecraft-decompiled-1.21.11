package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Частица белого дыма (White Smoke), поднимающаяся вверх с постепенным угасанием.
 * Используется для визуализации дыма от костров и других источников в холодных биомах.
 * Цвет — светло-лавандовый (#BAB1C2), движение — медленное восходящее.
 */
@Environment(EnvType.CLIENT)
public class WhiteSmokeParticle extends AscendingParticle {

	// 0xBAB1C2 — светло-лавандовый цвет дыма (R=186, G=177, B=194)
	private static final float SMOKE_RED = 0.7294118F;
	private static final float SMOKE_GREEN = 0.69411767F;
	private static final float SMOKE_BLUE = 0.7607843F;
	private static final float HORIZONTAL_SPREAD = 0.1F;
	private static final float ALPHA_START = 0.3F;
	private static final int MAX_AGE = 8;
	private static final float SLOW_FACTOR = -0.1F;

	protected WhiteSmokeParticle(
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
				HORIZONTAL_SPREAD,
				HORIZONTAL_SPREAD,
				velocityX,
				velocityY,
				velocityZ,
				scaleMultiplier,
				spriteProvider,
				ALPHA_START,
				MAX_AGE,
				SLOW_FACTOR,
				true
		);
		this.red = SMOKE_RED;
		this.green = SMOKE_GREEN;
		this.blue = SMOKE_BLUE;
	}

	@Environment(EnvType.CLIENT)
	public static class Factory implements ParticleFactory<SimpleParticleType> {

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
			return new WhiteSmokeParticle(world, x, y, z, velocityX, velocityY, velocityZ, 1.0F, spriteProvider);
		}
	}
}
