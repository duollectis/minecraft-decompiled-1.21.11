package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Vector3f;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
/**
 * {@code LightmapTextureManager}.
 */
public class LightmapTextureManager implements AutoCloseable {

	public static final int MAX_LIGHT_COORDINATE = 15728880;
	public static final int MAX_SKY_LIGHT_COORDINATE = 15728640;
	public static final int MAX_BLOCK_LIGHT_COORDINATE = 240;
	private static final int BLOCK_LIGHT_STEP = 16;
	private static final int UBO_SIZE = new Std140SizeCalculator()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.putFloat()
			.putVec3()
			.putVec3()
			.get();
	private final GpuTexture glTexture;
	private final GpuTextureView glTextureView;
	private boolean dirty;
	private float flickerIntensity;
	private final GameRenderer renderer;
	private final MinecraftClient client;
	private final MappableRingBuffer buffer;
	private final Random random = Random.create();

	public LightmapTextureManager(GameRenderer gameRenderer, MinecraftClient client) {
		this.renderer = gameRenderer;
		this.client = client;
		GpuDevice gpuDevice = RenderSystem.getDevice();
		this.glTexture = gpuDevice.createTexture("Light Texture", 12, TextureFormat.RGBA8, BLOCK_LIGHT_STEP, BLOCK_LIGHT_STEP, 1, 1);
		this.glTextureView = gpuDevice.createTextureView(this.glTexture);
		gpuDevice.createCommandEncoder().clearColorTexture(this.glTexture, -1);
		this.buffer = new MappableRingBuffer(() -> "Lightmap UBO", 130, UBO_SIZE);
	}

	public GpuTextureView getGlTextureView() {
		return this.glTextureView;
	}

	@Override
	public void close() {
		this.glTexture.close();
		this.glTextureView.close();
		this.buffer.close();
	}

	/**
	 * Tick.
	 */
	public void tick() {
		this.flickerIntensity = this.flickerIntensity
				+ (this.random.nextFloat() - this.random.nextFloat()) * this.random.nextFloat()
				* this.random.nextFloat() * 0.1F;
		this.flickerIntensity *= 0.9F;
		this.dirty = true;
	}

	private float getDarkness(LivingEntity entity, float factor, float tickProgress) {
		float f = 0.45F * factor;
		return Math.max(0.0F, MathHelper.cos((entity.age - tickProgress) * (float) Math.PI * 0.025F) * f);
	}

	/**
	 * Update.
	 *
	 * @param tickProgress tick progress
	 */
	public void update(float tickProgress) {
		if (this.dirty) {
			this.dirty = false;
			Profiler profiler = Profilers.get();
			profiler.push("lightTex");
			ClientWorld clientWorld = this.client.world;
			if (clientWorld != null) {
				Camera camera = this.client.gameRenderer.getCamera();
				int
						i =
						camera
								.getEnvironmentAttributeInterpolator()
								.get(EnvironmentAttributes.SKY_LIGHT_COLOR_VISUAL, tickProgress);
				float f = clientWorld.getDimension().ambientLight();
				float
						g =
						camera
								.getEnvironmentAttributeInterpolator()
								.get(EnvironmentAttributes.SKY_LIGHT_FACTOR_VISUAL, tickProgress);
				EndLightFlashManager endLightFlashManager = clientWorld.getEndLightFlashManager();
				Vector3f vector3f;
				if (endLightFlashManager != null) {
					vector3f = new Vector3f(0.99F, 1.12F, 1.0F);
					if (!this.client.options.getHideLightningFlashes().getValue()) {
						float h = endLightFlashManager.getSkyFactor(tickProgress);
						if (this.client.inGameHud.getBossBarHud().shouldThickenFog()) {
							g += h / 3.0F;
						}
						else {
							g += h;
						}
					}
				}
				else {
					vector3f = new Vector3f(1.0F, 1.0F, 1.0F);
				}

				float h = this.client.options.getDarknessEffectScale().getValue().floatValue();
				float j = this.client.player.getEffectFadeFactor(StatusEffects.DARKNESS, tickProgress) * h;
				float k = this.getDarkness(this.client.player, j, tickProgress) * h;
				float l = this.client.player.getUnderwaterVisibility();
				float m;
				if (this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
					m = GameRenderer.getNightVisionStrength(this.client.player, tickProgress);
				}
				else if (l > 0.0F && this.client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
					m = l;
				}
				else {
					m = 0.0F;
				}

				float n = this.flickerIntensity + 1.5F;
				float o = this.client.options.getGamma().getValue().floatValue();
				CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

				try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
						this.buffer.getBlocking(),
						false,
						true
				)
				) {
					Std140Builder.intoBuffer(mappedView.data())
					             .putFloat(f)
					             .putFloat(g)
					             .putFloat(n)
					             .putFloat(m)
					             .putFloat(k)
					             .putFloat(this.renderer.getSkyDarkness(tickProgress))
					             .putFloat(Math.max(0.0F, o - j))
					             .putVec3(ColorHelper.toRgbVector(i))
					             .putVec3(vector3f);
				}

				try (RenderPass renderPass = commandEncoder.createRenderPass(
						() -> "Update light",
						this.glTextureView,
						OptionalInt.empty()
				)
				) {
					renderPass.setPipeline(RenderPipelines.BILT_SCREEN_LIGHTMAP);
					RenderSystem.bindDefaultUniforms(renderPass);
					renderPass.setUniform("LightmapInfo", this.buffer.getBlocking());
					renderPass.draw(0, 3);
				}

				this.buffer.rotate();
				profiler.pop();
			}
		}
	}

	public static float getBrightness(DimensionType type, int lightLevel) {
		return getBrightness(type.ambientLight(), lightLevel);
	}

	public static float getBrightness(float ambientLight, int lightLevel) {
		float f = lightLevel / 15.0F;
		float g = f / (4.0F - 3.0F * f);
		return MathHelper.lerp(ambientLight, g, 1.0F);
	}

	/**
	 * Pack.
	 *
	 * @param block block
	 * @param sky sky
	 *
	 * @return int — результат операции
	 */
	public static int pack(int block, int sky) {
		return block << 4 | sky << 20;
	}

	public static int getBlockLightCoordinates(int light) {
		return light >>> 4 & 15;
	}

	public static int getSkyLightCoordinates(int light) {
		return light >>> 20 & 15;
	}

	/**
	 * Применяет emission.
	 *
	 * @param light light
	 * @param lightEmission light emission
	 *
	 * @return int — результат операции
	 */
	public static int applyEmission(int light, int lightEmission) {
		if (lightEmission == 0) {
			return light;
		}
		else {
			int i = Math.max(getSkyLightCoordinates(light), lightEmission);
			int j = Math.max(getBlockLightCoordinates(light), lightEmission);
			return pack(j, i);
		}
	}
}
