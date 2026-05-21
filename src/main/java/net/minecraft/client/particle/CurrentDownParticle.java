package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code CurrentDownParticle}.
 */
public class CurrentDownParticle extends BillboardParticle {

	private float accelerationAngle;

	CurrentDownParticle(ClientWorld clientWorld, double d, double e, double f, Sprite sprite) {
		super(clientWorld, d, e, f, sprite);
		this.maxAge = (int) (this.random.nextFloat() * 60.0F) + 30;
		this.collidesWithWorld = false;
		this.velocityX = 0.0;
		this.velocityY = -0.05;
		this.velocityZ = 0.0;
		this.setBoundingBoxSpacing(0.02F, 0.02F);
		this.scale = this.scale * (this.random.nextFloat() * 0.6F + 0.2F);
		this.gravityStrength = 0.002F;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		if (this.age++ >= this.maxAge) {
			this.markDead();
		}
		else {
			float f = 0.6F;
			this.velocityX = this.velocityX + 0.6F * MathHelper.cos(this.accelerationAngle);
			this.velocityZ = this.velocityZ + 0.6F * MathHelper.sin(this.accelerationAngle);
			this.velocityX *= 0.07;
			this.velocityZ *= 0.07;
			this.move(this.velocityX, this.velocityY, this.velocityZ);
			if (!this.world.getFluidState(BlockPos.ofFloored(this.x, this.y, this.z)).isIn(FluidTags.WATER)
					|| this.onGround) {
				this.markDead();
			}

			this.accelerationAngle += 0.08F;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public static class Factory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
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
			return new CurrentDownParticle(clientWorld, d, e, f, this.spriteProvider.getSprite(random));
		}
	}
}
