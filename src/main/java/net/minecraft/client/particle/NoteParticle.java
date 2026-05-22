package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Частица музыкальной ноты, появляющаяся над нотным блоком. Цвет определяется
 * через HSB-подобное кодирование: параметр {@code pitch} (0.0–1.0) задаёт
 * оттенок через синусоидальные компоненты RGB. Частица плавно появляется
 * в течение 6 тиков жизни.
 */
@Environment(EnvType.CLIENT)
public class NoteParticle extends BillboardParticle {

	private static final float COLOR_AMPLITUDE = 0.65F;
	private static final float COLOR_OFFSET = 0.35F;
	private static final float GREEN_PHASE_SHIFT = 0.33333334F;
	private static final float BLUE_PHASE_SHIFT = 0.6666667F;
	private static final float SCALE_MULTIPLIER = 1.5F;
	private static final int LIFETIME = 6;

	NoteParticle(ClientWorld world, double x, double y, double z, double pitch, Sprite sprite) {
		super(world, x, y, z, 0.0, 0.0, 0.0, sprite);
		this.velocityMultiplier = 0.66F;
		this.ascending = true;
		this.velocityX *= 0.01F;
		this.velocityY *= 0.01F;
		this.velocityZ *= 0.01F;
		this.velocityY += 0.2;
		this.red = Math.max(0.0F, MathHelper.sin(((float) pitch) * (float) (Math.PI * 2)) * COLOR_AMPLITUDE + COLOR_OFFSET);
		this.green = Math.max(0.0F, MathHelper.sin(((float) pitch + GREEN_PHASE_SHIFT) * (float) (Math.PI * 2)) * COLOR_AMPLITUDE + COLOR_OFFSET);
		this.blue = Math.max(0.0F, MathHelper.sin(((float) pitch + BLUE_PHASE_SHIFT) * (float) (Math.PI * 2)) * COLOR_AMPLITUDE + COLOR_OFFSET);
		this.scale *= SCALE_MULTIPLIER;
		this.maxAge = LIFETIME;
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
				double pitch,
				double velocityY,
				double velocityZ,
				Random random
		) {
			return new NoteParticle(world, x, y, z, pitch, this.spriteProvider.getSprite(random));
		}
	}
}
