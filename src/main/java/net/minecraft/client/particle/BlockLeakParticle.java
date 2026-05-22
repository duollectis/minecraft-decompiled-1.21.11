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

/**
 * Базовый класс для всех частиц утечки жидкости из блоков: капли воды, лавы, мёда,
 * слёз обсидиана и нектара. Иерархия включает три фазы жизни частицы:
 * <ul>
 *   <li>{@link Dripping} — медленно висит под блоком, накапливая каплю;</li>
 *   <li>{@link Falling} / {@link ContinuousFalling} — падает вниз после отрыва;</li>
 *   <li>{@link Landing} — приземляется и быстро исчезает.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class BlockLeakParticle extends BillboardParticle {

	private static final float OBSIDIAN_TEAR_BRIGHTNESS = 240.0F / 255.0F;
	private static final int OBSIDIAN_TEAR_BRIGHTNESS_PACKED = 240;
	private static final float VELOCITY_DAMPING = 0.98F;
	private static final float GRAVITY = 0.06F;

	private final Fluid fluid;
	protected boolean obsidianTear;

	BlockLeakParticle(ClientWorld world, double x, double y, double z, Fluid fluid, Sprite sprite) {
		super(world, x, y, z, sprite);
		setBoundingBoxSpacing(0.01F, 0.01F);
		this.gravityStrength = GRAVITY;
		this.fluid = fluid;
	}

	protected Fluid getFluid() {
		return fluid;
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
	}

	@Override
	public int getBrightness(float tint) {
		return obsidianTear ? OBSIDIAN_TEAR_BRIGHTNESS_PACKED : super.getBrightness(tint);
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		updateAge();

		if (this.dead) {
			return;
		}

		this.velocityY -= this.gravityStrength;
		move(this.velocityX, this.velocityY, this.velocityZ);
		updateVelocity();

		if (this.dead) {
			return;
		}

		this.velocityX *= VELOCITY_DAMPING;
		this.velocityY *= VELOCITY_DAMPING;
		this.velocityZ *= VELOCITY_DAMPING;

		if (fluid == Fluids.EMPTY) {
			return;
		}

		BlockPos blockPos = BlockPos.ofFloored(this.x, this.y, this.z);
		FluidState fluidState = this.world.getFluidState(blockPos);

		if (fluidState.getFluid() == fluid
				&& this.y < blockPos.getY() + fluidState.getHeight(this.world, blockPos)) {
			markDead();
		}
	}

	protected void updateAge() {
		if (this.maxAge-- <= 0) {
			markDead();
		}
	}

	protected void updateVelocity() {
	}

	// ─── Внутренние классы фаз жизни частицы ───────────────────────────────────

	/**
	 * Падающая частица, которая при касании земли порождает следующую фазу (например, Landing).
	 */
	@Environment(EnvType.CLIENT)
	static class ContinuousFalling extends BlockLeakParticle.Falling {

		protected final ParticleEffect nextParticle;

		ContinuousFalling(
				ClientWorld world,
				double x,
				double y,
				double z,
				Fluid fluid,
				ParticleEffect nextParticle,
				Sprite sprite
		) {
			super(world, x, y, z, fluid, sprite);
			this.maxAge = (int) (64.0 / (this.random.nextFloat() * 0.8 + 0.2));
			this.nextParticle = nextParticle;
		}

		@Override
		protected void updateVelocity() {
			if (this.onGround) {
				markDead();
				this.world.addParticleClient(nextParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
			}
		}
	}

	/**
	 * Фаза «капля висит под блоком»: очень медленно движется, по истечении maxAge
	 * переходит в следующую фазу, порождая частицу {@code nextParticle}.
	 */
	@Environment(EnvType.CLIENT)
	static class Dripping extends BlockLeakParticle {

		private static final int MAX_AGE = 40;
		private static final float VELOCITY_SCALE = 0.02F;

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
			this.maxAge = MAX_AGE;
		}

		@Override
		protected void updateAge() {
			if (this.maxAge-- > 0) {
				return;
			}

			markDead();
			this.world.addParticleClient(
					nextParticle,
					this.x,
					this.y,
					this.z,
					this.velocityX,
					this.velocityY,
					this.velocityZ
			);
		}

		@Override
		protected void updateVelocity() {
			this.velocityX *= VELOCITY_SCALE;
			this.velocityY *= VELOCITY_SCALE;
			this.velocityZ *= VELOCITY_SCALE;
		}
	}

	/** Фаза лавовой капли: цвет плавно меняется от оранжевого к красному по мере старения. */
	@Environment(EnvType.CLIENT)
	static class DrippingLava extends BlockLeakParticle.Dripping {

		private static final int BASE_AGE = 40;

		DrippingLava(
				ClientWorld world,
				double x,
				double y,
				double z,
				Fluid fluid,
				ParticleEffect nextParticle,
				Sprite sprite
		) {
			super(world, x, y, z, fluid, nextParticle, sprite);
		}

		@Override
		protected void updateAge() {
			this.red = 1.0F;
			this.green = 16.0F / (BASE_AGE - this.maxAge + 16);
			this.blue = 4.0F / (BASE_AGE - this.maxAge + 8);
			super.updateAge();
		}
	}

	/** Простая падающая частица: умирает при касании земли. */
	@Environment(EnvType.CLIENT)
	static class Falling extends BlockLeakParticle {

		Falling(ClientWorld world, double x, double y, double z, Fluid fluid, Sprite sprite) {
			super(world, x, y, z, fluid, sprite);
		}

		@Override
		protected void updateVelocity() {
			if (this.onGround) {
				markDead();
			}
		}
	}

	/**
	 * Падающая лавовая капля из сталактита: при касании земли воспроизводит звук
	 * капающей лавы или воды в зависимости от типа жидкости.
	 */
	@Environment(EnvType.CLIENT)
	static class DripstoneLavaDrip extends BlockLeakParticle.ContinuousFalling {

		DripstoneLavaDrip(
				ClientWorld world,
				double x,
				double y,
				double z,
				Fluid fluid,
				ParticleEffect nextParticle,
				Sprite sprite
		) {
			super(world, x, y, z, fluid, nextParticle, sprite);
		}

		@Override
		protected void updateVelocity() {
			if (!this.onGround) {
				return;
			}

			markDead();
			this.world.addParticleClient(nextParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
			SoundEvent sound = getFluid() == Fluids.LAVA
					? SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA
					: SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_WATER;
			float volume = MathHelper.nextBetween(this.random, 0.3F, 1.0F);
			this.world.playSoundClient(this.x, this.y, this.z, sound, SoundCategory.BLOCKS, volume, 1.0F, false);
		}
	}

	/** Падающая медовая капля: при касании земли воспроизводит звук улья. */
	@Environment(EnvType.CLIENT)
	static class FallingHoney extends BlockLeakParticle.ContinuousFalling {

		FallingHoney(
				ClientWorld world,
				double x,
				double y,
				double z,
				Fluid fluid,
				ParticleEffect nextParticle,
				Sprite sprite
		) {
			super(world, x, y, z, fluid, nextParticle, sprite);
		}

		@Override
		protected void updateVelocity() {
			if (!this.onGround) {
				return;
			}

			markDead();
			this.world.addParticleClient(nextParticle, this.x, this.y, this.z, 0.0, 0.0, 0.0);
			float volume = MathHelper.nextBetween(this.random, 0.3F, 1.0F);
			this.world.playSoundClient(
					this.x,
					this.y,
					this.z,
					SoundEvents.BLOCK_BEEHIVE_DRIP,
					SoundCategory.BLOCKS,
					volume,
					1.0F,
					false
			);
		}
	}

	/** Фаза приземления: короткое время жизни, быстро исчезает. */
	@Environment(EnvType.CLIENT)
	static class Landing extends BlockLeakParticle {

		Landing(ClientWorld world, double x, double y, double z, Fluid fluid, Sprite sprite) {
			super(world, x, y, z, fluid, sprite);
			this.maxAge = (int) (16.0 / (this.random.nextFloat() * 0.8 + 0.2));
		}
	}

	// ─── Фабрики ────────────────────────────────────────────────────────────────

	@Environment(EnvType.CLIENT)
	public static class DrippingDripstoneLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingDripstoneLavaFactory(SpriteProvider spriteProvider) {
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
			return new BlockLeakParticle.DrippingLava(
					world,
					x,
					y,
					z,
					Fluids.LAVA,
					ParticleTypes.FALLING_DRIPSTONE_LAVA,
					spriteProvider.getSprite(random)
			);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class DrippingDripstoneWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingDripstoneWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.Dripping(
					world,
					x,
					y,
					z,
					Fluids.WATER,
					ParticleTypes.FALLING_DRIPSTONE_WATER,
					spriteProvider.getSprite(random)
			);
			particle.setColor(0.2F, 0.3F, 1.0F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class DrippingHoneyFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingHoneyFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle.Dripping dripping = new BlockLeakParticle.Dripping(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					ParticleTypes.FALLING_HONEY,
					spriteProvider.getSprite(random)
			);
			dripping.gravityStrength *= 0.01F;
			dripping.maxAge = 100;
			dripping.setColor(0.622F, 0.508F, 0.082F);
			return dripping;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class DrippingLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingLavaFactory(SpriteProvider spriteProvider) {
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
			return new BlockLeakParticle.DrippingLava(
					world,
					x,
					y,
					z,
					Fluids.LAVA,
					ParticleTypes.FALLING_LAVA,
					spriteProvider.getSprite(random)
			);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class DrippingObsidianTearFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingObsidianTearFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle.Dripping dripping = new BlockLeakParticle.Dripping(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					ParticleTypes.FALLING_OBSIDIAN_TEAR,
					spriteProvider.getSprite(random)
			);
			dripping.obsidianTear = true;
			dripping.gravityStrength *= 0.01F;
			dripping.maxAge = 100;
			dripping.setColor(0.51171875F, 0.03125F, 0.890625F);
			return dripping;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class DrippingWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public DrippingWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.Dripping(
					world,
					x,
					y,
					z,
					Fluids.WATER,
					ParticleTypes.FALLING_WATER,
					spriteProvider.getSprite(random)
			);
			particle.setColor(0.2F, 0.3F, 1.0F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingDripstoneLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingDripstoneLavaFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.DripstoneLavaDrip(
					world, x, y, z, Fluids.LAVA, ParticleTypes.LANDING_LAVA, spriteProvider.getSprite(random)
			);
			particle.setColor(1.0F, 0.2857143F, 0.083333336F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingDripstoneWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingDripstoneWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.DripstoneLavaDrip(
					world, x, y, z, Fluids.WATER, ParticleTypes.SPLASH, spriteProvider.getSprite(random)
			);
			particle.setColor(0.2F, 0.3F, 1.0F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingHoneyFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingHoneyFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.FallingHoney(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					ParticleTypes.LANDING_HONEY,
					spriteProvider.getSprite(random)
			);
			particle.gravityStrength = 0.01F;
			particle.setColor(0.582F, 0.448F, 0.082F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingLavaFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.ContinuousFalling(
					world, x, y, z, Fluids.LAVA, ParticleTypes.LANDING_LAVA, spriteProvider.getSprite(random)
			);
			particle.setColor(1.0F, 0.2857143F, 0.083333336F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingNectarFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingNectarFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.Falling(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					spriteProvider.getSprite(random)
			);
			particle.maxAge = (int) (16.0 / (random.nextFloat() * 0.8 + 0.2));
			particle.gravityStrength = 0.007F;
			particle.setColor(0.92F, 0.782F, 0.72F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingObsidianTearFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingObsidianTearFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.ContinuousFalling(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					ParticleTypes.LANDING_OBSIDIAN_TEAR,
					spriteProvider.getSprite(random)
			);
			particle.obsidianTear = true;
			particle.gravityStrength = 0.01F;
			particle.setColor(0.51171875F, 0.03125F, 0.890625F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingSporeBlossomFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingSporeBlossomFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.Falling(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					spriteProvider.getSprite(random)
			);
			particle.maxAge = (int) (64.0F / MathHelper.nextBetween(particle.random, 0.1F, 0.9F));
			particle.gravityStrength = 0.005F;
			particle.setColor(0.32F, 0.5F, 0.22F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class FallingWaterFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public FallingWaterFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.ContinuousFalling(
					world, x, y, z, Fluids.WATER, ParticleTypes.SPLASH, spriteProvider.getSprite(random)
			);
			particle.setColor(0.2F, 0.3F, 1.0F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class LandingHoneyFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public LandingHoneyFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.Landing(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					spriteProvider.getSprite(random)
			);
			particle.maxAge = (int) (128.0 / (random.nextFloat() * 0.8 + 0.2));
			particle.setColor(0.522F, 0.408F, 0.082F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class LandingLavaFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public LandingLavaFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.Landing(
					world,
					x,
					y,
					z,
					Fluids.LAVA,
					spriteProvider.getSprite(random)
			);
			particle.setColor(1.0F, 0.2857143F, 0.083333336F);
			return particle;
		}
	}

	@Environment(EnvType.CLIENT)
	public static class LandingObsidianTearFactory implements ParticleFactory<SimpleParticleType> {

		private final SpriteProvider spriteProvider;

		public LandingObsidianTearFactory(SpriteProvider spriteProvider) {
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
			BlockLeakParticle particle = new BlockLeakParticle.Landing(
					world,
					x,
					y,
					z,
					Fluids.EMPTY,
					spriteProvider.getSprite(random)
			);
			particle.obsidianTear = true;
			particle.maxAge = (int) (28.0 / (random.nextFloat() * 0.8 + 0.2));
			particle.setColor(0.51171875F, 0.03125F, 0.890625F);
			return particle;
		}
	}
}
