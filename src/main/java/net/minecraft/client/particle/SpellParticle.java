package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.EffectParticleEffect;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code SpellParticle}.
 */
public class SpellParticle extends BillboardParticle {

	private static final Random RANDOM = Random.create();
	private final SpriteProvider spriteProvider;
	private float defaultAlpha = 1.0F;

	SpellParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			SpriteProvider spriteProvider
	) {
		super(
				world,
				x,
				y,
				z,
				0.5 - RANDOM.nextDouble(),
				velocityY,
				0.5 - RANDOM.nextDouble(),
				spriteProvider.getFirst()
		);
		this.velocityMultiplier = 0.96F;
		this.gravityStrength = -0.1F;
		this.ascending = true;
		this.spriteProvider = spriteProvider;
		this.velocityY *= 0.2F;
		if (velocityX == 0.0 && velocityZ == 0.0) {
			this.velocityX *= 0.1F;
			this.velocityZ *= 0.1F;
		}

		this.scale *= 0.75F;
		this.maxAge = (int) (8.0 / (this.random.nextFloat() * 0.8 + 0.2));
		this.collidesWithWorld = false;
		this.updateSprite(spriteProvider);
		if (this.isInvisible()) {
			this.setAlpha(0.0F);
		}
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(this.spriteProvider);
		if (this.isInvisible()) {
			this.alpha = 0.0F;
		}
		else {
			this.alpha = MathHelper.lerp(0.05F, this.alpha, this.defaultAlpha);
		}
	}

	@Override
	protected void setAlpha(float alpha) {
		super.setAlpha(alpha);
		this.defaultAlpha = alpha;
	}

	private boolean isInvisible() {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		ClientPlayerEntity clientPlayerEntity = minecraftClient.player;
		return clientPlayerEntity != null
				&& clientPlayerEntity.getEyePos().squaredDistanceTo(this.x, this.y, this.z) <= 9.0
				&& minecraftClient.options.getPerspective().isFirstPerson()
				&& clientPlayerEntity.isUsingSpyglass();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DefaultFactory}.
	 */
	public static class DefaultFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DefaultFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

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
			return new SpellParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code EntityFactory}.
	 */
	public static class EntityFactory implements ParticleFactory<TintedParticleEffect> {

		private final SpriteProvider spriteProvider;

		public EntityFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				TintedParticleEffect tintedParticleEffect,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			SpellParticle spellParticle = new SpellParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			spellParticle.setColor(
					tintedParticleEffect.getRed(),
					tintedParticleEffect.getGreen(),
					tintedParticleEffect.getBlue()
			);
			spellParticle.setAlpha(tintedParticleEffect.getAlpha());
			return spellParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code InstantFactory}.
	 */
	public static class InstantFactory implements ParticleFactory<EffectParticleEffect> {

		private final SpriteProvider spriteProvider;

		public InstantFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(
				EffectParticleEffect effectParticleEffect,
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				double g,
				double h,
				double i,
				Random random
		) {
			SpellParticle spellParticle = new SpellParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			spellParticle.setColor(
					effectParticleEffect.getRed(),
					effectParticleEffect.getGreen(),
					effectParticleEffect.getBlue()
			);
			spellParticle.move(effectParticleEffect.getPower());
			return spellParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WitchFactory}.
	 */
	public static class WitchFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public WitchFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

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
			SpellParticle spellParticle = new SpellParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
			float j = random.nextFloat() * 0.5F + 0.35F;
			spellParticle.setColor(1.0F * j, 0.0F * j, 1.0F * j);
			return spellParticle;
		}
	}
}
