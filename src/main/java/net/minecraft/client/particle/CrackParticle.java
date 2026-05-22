package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.Atlases;
import net.minecraft.util.math.random.Random;

/**
 * Частица-осколок, отображающая текстуру предмета или блока.
 * Используется при разрушении блоков, броске предметов и других событиях,
 * когда нужно визуально «разлететься» кусочками текстуры.
 * UV-координаты сэмплируются из случайного квадранта 4×4 спрайта.
 */
@Environment(EnvType.CLIENT)
public class CrackParticle extends BillboardParticle {

	private static final float UV_GRID_SIZE = 4.0F;

	private final float sampleU;
	private final float sampleV;
	private final BillboardParticle.RenderType renderType;

	CrackParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Sprite sprite
	) {
		this(world, x, y, z, sprite);
		this.velocityX *= 0.1F;
		this.velocityY *= 0.1F;
		this.velocityZ *= 0.1F;
		this.velocityX += velocityX;
		this.velocityY += velocityY;
		this.velocityZ += velocityZ;
	}

	protected CrackParticle(ClientWorld world, double x, double y, double z, Sprite sprite) {
		super(world, x, y, z, 0.0, 0.0, 0.0, sprite);
		this.gravityStrength = 1.0F;
		this.scale /= 2.0F;
		this.sampleU = this.random.nextFloat() * 3.0F;
		this.sampleV = this.random.nextFloat() * 3.0F;
		this.renderType = sprite.getAtlasId().equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
				? BillboardParticle.RenderType.BLOCK_ATLAS_TRANSLUCENT
				: BillboardParticle.RenderType.ITEM_ATLAS_TRANSLUCENT;
	}

	@Override
	protected float getMinU() {
		return this.sprite.getFrameU((this.sampleU + 1.0F) / UV_GRID_SIZE);
	}

	@Override
	protected float getMaxU() {
		return this.sprite.getFrameU(this.sampleU / UV_GRID_SIZE);
	}

	@Override
	protected float getMinV() {
		return this.sprite.getFrameV(this.sampleV / UV_GRID_SIZE);
	}

	@Override
	protected float getMaxV() {
		return this.sprite.getFrameV((this.sampleV + 1.0F) / UV_GRID_SIZE);
	}

	@Override
	public BillboardParticle.RenderType getRenderType() {
		return renderType;
	}

	/**
	 * Базовая фабрика осколочных частиц, умеющая получать спрайт из {@link ItemStack}.
	 * Конкретные фабрики наследуются от неё и передают нужный предмет.
	 */
	@Environment(EnvType.CLIENT)
	public abstract static class Factory<T extends ParticleEffect> implements ParticleFactory<T> {

		private final ItemRenderState itemRenderState = new ItemRenderState();

		/**
		 * Возвращает спрайт для указанного предмета, используя модель рендера.
		 * Если спрайт не найден — возвращает «missing» спрайт из атласа предметов.
		 */
		protected Sprite getSprite(ItemStack stack, ClientWorld world, Random random) {
			MinecraftClient
					.getInstance()
					.getItemModelManager()
					.clearAndUpdate(itemRenderState, stack, ItemDisplayContext.GROUND, world, null, 0);
			Sprite sprite = itemRenderState.getParticleSprite(random);

			return sprite != null
					? sprite
					: MinecraftClient
							.getInstance()
							.getAtlasManager()
							.getAtlasTexture(Atlases.ITEMS)
							.getMissingSprite();
		}
	}

	@Environment(EnvType.CLIENT)
	public static class CobwebFactory extends CrackParticle.Factory<SimpleParticleType> {

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
			return new CrackParticle(world, x, y, z, getSprite(new ItemStack(Items.COBWEB), world, random));
		}
	}

	@Environment(EnvType.CLIENT)
	public static class ItemFactory extends CrackParticle.Factory<ItemStackParticleEffect> {

		@Override
		public Particle createParticle(
				ItemStackParticleEffect effect,
				ClientWorld world,
				double x,
				double y,
				double z,
				double velocityX,
				double velocityY,
				double velocityZ,
				Random random
		) {
			return new CrackParticle(
					world,
					x,
					y,
					z,
					velocityX,
					velocityY,
					velocityZ,
					getSprite(effect.getItemStack(), world, random)
			);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class SlimeballFactory extends CrackParticle.Factory<SimpleParticleType> {

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
			return new CrackParticle(world, x, y, z, getSprite(new ItemStack(Items.SLIME_BALL), world, random));
		}
	}

	@Environment(EnvType.CLIENT)
	public static class SnowballFactory extends CrackParticle.Factory<SimpleParticleType> {

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
			return new CrackParticle(world, x, y, z, getSprite(new ItemStack(Items.SNOWBALL), world, random));
		}
	}
}
