package net.minecraft.client.particle;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
/**
 * {@code BillboardParticle}.
 */
public abstract class BillboardParticle extends Particle {

	protected float scale;
	protected float red = 1.0F;
	protected float green = 1.0F;
	protected float blue = 1.0F;
	protected float alpha = 1.0F;
	protected float zRotation;
	protected float lastZRotation;
	protected Sprite sprite;

	protected BillboardParticle(ClientWorld world, double x, double y, double z, Sprite sprite) {
		super(world, x, y, z);
		this.sprite = sprite;
		this.scale = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
	}

	protected BillboardParticle(
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Sprite sprite
	) {
		super(world, x, y, z, velocityX, velocityY, velocityZ);
		this.sprite = sprite;
		this.scale = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
	}

	public BillboardParticle.Rotator getRotator() {
		return BillboardParticle.Rotator.ALL_AXIS;
	}

	/**
	 * Render.
	 *
	 * @param submittable submittable
	 * @param camera camera
	 * @param tickProgress tick progress
	 */
	public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
		Quaternionf quaternionf = new Quaternionf();
		this.getRotator().setRotation(quaternionf, camera, tickProgress);
		if (this.zRotation != 0.0F) {
			quaternionf.rotateZ(MathHelper.lerp(tickProgress, this.lastZRotation, this.zRotation));
		}

		this.render(submittable, camera, quaternionf, tickProgress);
	}

	protected void render(
			BillboardParticleSubmittable submittable,
			Camera camera,
			Quaternionf rotation,
			float tickProgress
	) {
		Vec3d vec3d = camera.getCameraPos();
		float f = (float) (MathHelper.lerp((double) tickProgress, this.lastX, this.x) - vec3d.getX());
		float g = (float) (MathHelper.lerp((double) tickProgress, this.lastY, this.y) - vec3d.getY());
		float h = (float) (MathHelper.lerp((double) tickProgress, this.lastZ, this.z) - vec3d.getZ());
		this.renderVertex(submittable, rotation, f, g, h, tickProgress);
	}

	protected void renderVertex(
			BillboardParticleSubmittable submittable,
			Quaternionf rotation,
			float x,
			float y,
			float z,
			float tickProgress
	) {
		submittable.render(
				this.getRenderType(),
				x,
				y,
				z,
				rotation.x,
				rotation.y,
				rotation.z,
				rotation.w,
				this.getSize(tickProgress),
				this.getMinU(),
				this.getMaxU(),
				this.getMinV(),
				this.getMaxV(),
				ColorHelper.fromFloats(this.alpha, this.red, this.green, this.blue),
				this.getBrightness(tickProgress)
		);
	}

	public float getSize(float tickProgress) {
		return this.scale;
	}

	@Override
	public Particle scale(float scale) {
		this.scale *= scale;
		return super.scale(scale);
	}

	@Override
	public ParticleTextureSheet textureSheet() {
		return ParticleTextureSheet.SINGLE_QUADS;
	}

	/**
	 * Обновляет sprite.
	 *
	 * @param spriteProvider sprite provider
	 */
	public void updateSprite(SpriteProvider spriteProvider) {
		if (!this.dead) {
			this.setSprite(spriteProvider.getSprite(this.age, this.maxAge));
		}
	}

	protected void setSprite(Sprite sprite) {
		this.sprite = sprite;
	}

	protected float getMinU() {
		return this.sprite.getMinU();
	}

	protected float getMaxU() {
		return this.sprite.getMaxU();
	}

	protected float getMinV() {
		return this.sprite.getMinV();
	}

	protected float getMaxV() {
		return this.sprite.getMaxV();
	}

	protected abstract BillboardParticle.RenderType getRenderType();

	public void setColor(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	protected void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ ", Pos ("
				+ this.x
				+ ","
				+ this.y
				+ ","
				+ this.z
				+ "), RGBA ("
				+ this.red
				+ ","
				+ this.green
				+ ","
				+ this.blue
				+ ","
				+ this.alpha
				+ "), Age "
				+ this.age;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RenderType}.
	 */
	public record RenderType(boolean translucent, Identifier textureAtlasLocation, RenderPipeline pipeline) {

		public static final BillboardParticle.RenderType BLOCK_ATLAS_TRANSLUCENT = new BillboardParticle.RenderType(
				true, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, RenderPipelines.TRANSLUCENT_PARTICLE
		);
		public static final BillboardParticle.RenderType ITEM_ATLAS_TRANSLUCENT = new BillboardParticle.RenderType(
				true, SpriteAtlasTexture.ITEMS_ATLAS_TEXTURE, RenderPipelines.TRANSLUCENT_PARTICLE
		);
		public static final BillboardParticle.RenderType PARTICLE_ATLAS_OPAQUE = new BillboardParticle.RenderType(
				false, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE, RenderPipelines.OPAQUE_PARTICLE
		);
		public static final BillboardParticle.RenderType PARTICLE_ATLAS_TRANSLUCENT = new BillboardParticle.RenderType(
				true, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE, RenderPipelines.TRANSLUCENT_PARTICLE
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Rotator}.
	 */
	public interface Rotator {

		BillboardParticle.Rotator ALL_AXIS = (quaternion, camera, tickProgress) -> quaternion.set(camera.getRotation());

		BillboardParticle.Rotator
				Y_AND_W_ONLY =
				(quaternion, camera, tickProgress) -> quaternion.set(
						0.0F,
						camera.getRotation().y,
						0.0F,
						camera.getRotation().w
				);

		void setRotation(Quaternionf quaternion, Camera camera, float tickProgress);
	}
}
