package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.AbstractDustParticleEffect;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code AbstractDustParticle}.
 */
public class AbstractDustParticle<T extends AbstractDustParticleEffect> extends BillboardParticle {

	private final SpriteProvider spriteProvider;

	protected AbstractDustParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			T parameters,
			SpriteProvider spriteProvider
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider.getFirst());
		this.velocityMultiplier = 0.96F;
		this.ascending = true;
		this.spriteProvider = spriteProvider;
		this.velocityX *= 0.1F;
		this.velocityY *= 0.1F;
		this.velocityZ *= 0.1F;
		this.scale = this.scale * (0.75F * parameters.getScale());
		int i = (int) (8.0 / (this.random.nextDouble() * 0.8 + 0.2));
		this.maxAge = (int) Math.max(i * parameters.getScale(), 1.0F);
		this.updateSprite(spriteProvider);
	}

	/**
	 * Darken.
	 *
	 * @param colorComponent color component
	 * @param multiplier multiplier
	 *
	 * @return float — результат операции
	 */
	protected float darken(float colorComponent, float multiplier) {
		return (this.random.nextFloat() * 0.2F + 0.8F) * colorComponent * multiplier;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public float getSize(float tickProgress) {
		return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * 32.0F, 0.0F, 1.0F);
	}

	@Override
	public void tick() {
		super.tick();
		this.updateSprite(this.spriteProvider);
	}
}
