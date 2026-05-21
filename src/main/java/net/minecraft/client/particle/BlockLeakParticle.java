package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
/**
 * {@code BlockLeakParticle}.
 */
public class BlockLeakParticle extends BillboardParticle {

	private final Fluid fluid;
	protected boolean obsidianTear;

	BlockLeakParticle(ClientWorld world, double x, double y, double z, Fluid fluid, Sprite sprite) {
		super(world, x, y, z, sprite);
		this.setBoundingBoxSpacing(0.01F, 0.01F);
		this.gravityStrength = 0.06F;
		this.fluid = fluid;
	}

	protected Fluid getFluid() {
		return this.fluid;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public int getBrightness(float tint) {
		return this.obsidianTear ? 240 : super.getBrightness(tint);
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		this.updateAge();
		if (!this.dead) {
			this.velocityY = this.velocityY - this.gravityStrength;
			this.move(this.velocityX, this.velocityY, this.velocityZ);
			this.updateVelocity();
			if (!this.dead) {
				this.velocityX *= 0.98F;
				this.velocityY *= 0.98F;
				this.velocityZ *= 0.98F;
				if (this.fluid != Fluids.EMPTY) {
					BlockPos blockPos = BlockPos.ofFloored(this.x, this.y, this.z);
					FluidState fluidState = this.world.getFluidState(blockPos);
					if (fluidState.getFluid() == this.fluid
							&& this.y < blockPos.getY() + fluidState.getHeight(this.world, blockPos)) {
						this.markDead();
					}
				}
			}
		}
	}

	protected void updateAge() {
		if (this.maxAge-- <= 0) {
			this.markDead();
		}
	}

	protected void updateVelocity() {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ContinuousFalling}.
	 */
	static class ContinuousFalling extends BlockLeakParticle.Falling {

		protected final ParticleEffect nextParticle;

		ContinuousFalling(
				ClientWorld world,
				double x,
				double y,
				double z,
				Fluid fluid,
				ParticleEffect particleEffect,
				Sprite sprite
		) {
			super(world, x, y, z, fluid, sprite);
			this.maxAge = (int) (64.0 / (this.random.nextFloat() * 0.8 + 0.2));
			this.nextParticle = particleEffect;
		}

		@Override
		protected void updateVelocity() {
			if (this.onGround) {
				this.markDead();
				this.world.addParticleClient(this.nextParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Dripping}.
	 */
	static class Dripping extends BlockLeakParticle {

		private final ParticleEffect nextParticle;

		Dripping(
				ClientWorld world,
				double x,
				double y,
				double z,
				Fluid fluid,
				ParticleEffect nextParticle,
				Sprite sprite
		) {
			super(world, x, y, z, fluid, sprite);
			this.nextParticle = nextParticle;
			this.gravityStrength *= 0.02F;
			this.maxAge = 40;
		}

		@Override
		protected void updateAge() {
			if (this.maxAge-- <= 0) {
				this.markDead();
				this.world.addParticleClient(
						this.nextParticle,
						this.x,
						this.y,
						this.z,
						this.velocityX,
						this.velocityY,
						this.velocityZ
				);
			}
		}

		@Override
		protected void updateVelocity() {
			this.velocityX *= 0.02;
			this.velocityY *= 0.02;
			this.velocityZ *= 0.02;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrippingDripstoneLavaFactory}.
	 */
	public static class DrippingDripstoneLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingDripstoneLavaFactory(SpriteProvider spriteProvider) {
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
			return new BlockLeakParticle.DrippingLava(
					clientWorld,
					d,
					e,
					f,
					Fluids.LAVA,
					ParticleTypes.FALLING_DRIPSTONE_LAVA,
					this.spriteProvider.getSprite(random)
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrippingDripstoneWaterFactory}.
	 */
	public static class DrippingDripstoneWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingDripstoneWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.Dripping(
					clientWorld,
					d,
					e,
					f,
					Fluids.WATER,
					ParticleTypes.FALLING_DRIPSTONE_WATER,
					this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.setColor(0.2F, 0.3F, 1.0F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrippingHoneyFactory}.
	 */
	public static class DrippingHoneyFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingHoneyFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle.Dripping dripping = new BlockLeakParticle.Dripping(
					clientWorld,
					d,
					e,
					f,
					Fluids.EMPTY,
					ParticleTypes.FALLING_HONEY,
					this.spriteProvider.getSprite(random)
			);
			dripping.gravityStrength *= 0.01F;
			dripping.maxAge = 100;
			dripping.setColor(0.622F, 0.508F, 0.082F);
			return dripping;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrippingLava}.
	 */
	static class DrippingLava extends BlockLeakParticle.Dripping {

		DrippingLava(
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				Fluid fluid,
				ParticleEffect particleEffect,
				Sprite sprite
		) {
			super(clientWorld, d, e, f, fluid, particleEffect, sprite);
		}

		@Override
		protected void updateAge() {
			this.red = 1.0F;
			this.green = 16.0F / (40 - this.maxAge + 16);
			this.blue = 4.0F / (40 - this.maxAge + 8);
			super.updateAge();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrippingLavaFactory}.
	 */
	public static class DrippingLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingLavaFactory(SpriteProvider spriteProvider) {
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
			return new BlockLeakParticle.DrippingLava(
					clientWorld,
					d,
					e,
					f,
					Fluids.LAVA,
					ParticleTypes.FALLING_LAVA,
					this.spriteProvider.getSprite(random)
			);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrippingObsidianTearFactory}.
	 */
	public static class DrippingObsidianTearFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingObsidianTearFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle.Dripping dripping = new BlockLeakParticle.Dripping(
					clientWorld,
					d,
					e,
					f,
					Fluids.EMPTY,
					ParticleTypes.FALLING_OBSIDIAN_TEAR,
					this.spriteProvider.getSprite(random)
			);
			dripping.obsidianTear = true;
			dripping.gravityStrength *= 0.01F;
			dripping.maxAge = 100;
			dripping.setColor(0.51171875F, 0.03125F, 0.890625F);
			return dripping;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrippingWaterFactory}.
	 */
	public static class DrippingWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.Dripping(
					clientWorld,
					d,
					e,
					f,
					Fluids.WATER,
					ParticleTypes.FALLING_WATER,
					this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.setColor(0.2F, 0.3F, 1.0F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DripstoneLavaDrip}.
	 */
	static class DripstoneLavaDrip extends BlockLeakParticle.ContinuousFalling {

		DripstoneLavaDrip(
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				Fluid fluid,
				ParticleEffect particleEffect,
				Sprite sprite
		) {
			super(clientWorld, d, e, f, fluid, particleEffect, sprite);
		}

		@Override
		protected void updateVelocity() {
			if (this.onGround) {
				this.markDead();
				this.world.addParticleClient(this.nextParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
				SoundEvent soundEvent = this.getFluid() == Fluids.LAVA
				                        ? SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA
				                        : SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_WATER;
				float f = MathHelper.nextBetween(this.random, 0.3F, 1.0F);
				this.world.playSoundClient(this.x, this.y, this.z, soundEvent, SoundCategory.BLOCKS, f, 1.0F, false);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Falling}.
	 */
	static class Falling extends BlockLeakParticle {

		Falling(ClientWorld clientWorld, double d, double e, double f, Fluid fluid, Sprite sprite) {
			super(clientWorld, d, e, f, fluid, sprite);
		}

		@Override
		protected void updateVelocity() {
			if (this.onGround) {
				this.markDead();
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingDripstoneLavaFactory}.
	 */
	public static class FallingDripstoneLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingDripstoneLavaFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.DripstoneLavaDrip(
					clientWorld, d, e, f, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.setColor(1.0F, 0.2857143F, 0.083333336F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingDripstoneWaterFactory}.
	 */
	public static class FallingDripstoneWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingDripstoneWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.DripstoneLavaDrip(
					clientWorld, d, e, f, Fluids.WATER, ParticleTypes.SPLASH, this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.setColor(0.2F, 0.3F, 1.0F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingHoney}.
	 */
	static class FallingHoney extends BlockLeakParticle.ContinuousFalling {

		FallingHoney(
				ClientWorld clientWorld,
				double d,
				double e,
				double f,
				Fluid fluid,
				ParticleEffect particleEffect,
				Sprite sprite
		) {
			super(clientWorld, d, e, f, fluid, particleEffect, sprite);
		}

		@Override
		protected void updateVelocity() {
			if (this.onGround) {
				this.markDead();
				this.world.addParticleClient(this.nextParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
				float f = MathHelper.nextBetween(this.random, 0.3F, 1.0F);
				this.world.playSoundClient(
						this.x,
						this.y,
						this.z,
						SoundEvents.BLOCK_BEEHIVE_DRIP,
						SoundCategory.BLOCKS,
						f,
						1.0F,
						false
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingHoneyFactory}.
	 */
	public static class FallingHoneyFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingHoneyFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.FallingHoney(
					clientWorld,
					d,
					e,
					f,
					Fluids.EMPTY,
					ParticleTypes.LANDING_HONEY,
					this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.gravityStrength = 0.01F;
			blockLeakParticle.setColor(0.582F, 0.448F, 0.082F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingLavaFactory}.
	 */
	public static class FallingLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingLavaFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.ContinuousFalling(
					clientWorld, d, e, f, Fluids.LAVA, ParticleTypes.LANDING_LAVA, this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.setColor(1.0F, 0.2857143F, 0.083333336F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingNectarFactory}.
	 */
	public static class FallingNectarFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingNectarFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle
					blockLeakParticle =
					new BlockLeakParticle.Falling(
							clientWorld,
							d,
							e,
							f,
							Fluids.EMPTY,
							this.spriteProvider.getSprite(random)
					);
			blockLeakParticle.maxAge = (int) (16.0 / (random.nextFloat() * 0.8 + 0.2));
			blockLeakParticle.gravityStrength = 0.007F;
			blockLeakParticle.setColor(0.92F, 0.782F, 0.72F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingObsidianTearFactory}.
	 */
	public static class FallingObsidianTearFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingObsidianTearFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.ContinuousFalling(
					clientWorld,
					d,
					e,
					f,
					Fluids.EMPTY,
					ParticleTypes.LANDING_OBSIDIAN_TEAR,
					this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.obsidianTear = true;
			blockLeakParticle.gravityStrength = 0.01F;
			blockLeakParticle.setColor(0.51171875F, 0.03125F, 0.890625F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingSporeBlossomFactory}.
	 */
	public static class FallingSporeBlossomFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingSporeBlossomFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle
					blockLeakParticle =
					new BlockLeakParticle.Falling(
							clientWorld,
							d,
							e,
							f,
							Fluids.EMPTY,
							this.spriteProvider.getSprite(random)
					);
			blockLeakParticle.maxAge = (int) (64.0F / MathHelper.nextBetween(blockLeakParticle.random, 0.1F, 0.9F));
			blockLeakParticle.gravityStrength = 0.005F;
			blockLeakParticle.setColor(0.32F, 0.5F, 0.22F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FallingWaterFactory}.
	 */
	public static class FallingWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle blockLeakParticle = new BlockLeakParticle.ContinuousFalling(
					clientWorld, d, e, f, Fluids.WATER, ParticleTypes.SPLASH, this.spriteProvider.getSprite(random)
			);
			blockLeakParticle.setColor(0.2F, 0.3F, 1.0F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Landing}.
	 */
	static class Landing extends BlockLeakParticle {

		Landing(ClientWorld clientWorld, double d, double e, double f, Fluid fluid, Sprite sprite) {
			super(clientWorld, d, e, f, fluid, sprite);
			this.maxAge = (int) (16.0 / (this.random.nextFloat() * 0.8 + 0.2));
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LandingHoneyFactory}.
	 */
	public static class LandingHoneyFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public LandingHoneyFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle
					blockLeakParticle =
					new BlockLeakParticle.Landing(
							clientWorld,
							d,
							e,
							f,
							Fluids.EMPTY,
							this.spriteProvider.getSprite(random)
					);
			blockLeakParticle.maxAge = (int) (128.0 / (random.nextFloat() * 0.8 + 0.2));
			blockLeakParticle.setColor(0.522F, 0.408F, 0.082F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LandingLavaFactory}.
	 */
	public static class LandingLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public LandingLavaFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle
					blockLeakParticle =
					new BlockLeakParticle.Landing(
							clientWorld,
							d,
							e,
							f,
							Fluids.LAVA,
							this.spriteProvider.getSprite(random)
					);
			blockLeakParticle.setColor(1.0F, 0.2857143F, 0.083333336F);
			return blockLeakParticle;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LandingObsidianTearFactory}.
	 */
	public static class LandingObsidianTearFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public LandingObsidianTearFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle
					blockLeakParticle =
					new BlockLeakParticle.Landing(
							clientWorld,
							d,
							e,
							f,
							Fluids.EMPTY,
							this.spriteProvider.getSprite(random)
					);
			blockLeakParticle.obsidianTear = true;
			blockLeakParticle.maxAge = (int) (28.0 / (random.nextFloat() * 0.8 + 0.2));
			blockLeakParticle.setColor(0.51171875F, 0.03125F, 0.890625F);
			return blockLeakParticle;
		}
	}
}
