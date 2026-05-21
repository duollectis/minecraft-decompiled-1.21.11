package net.minecraft.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.attribute.EnvironmentAttributeInterpolator;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import org.joml.*;

import java.lang.Math;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
/**
 * {@code SkyRendering}.
 */
public class SkyRendering implements AutoCloseable {

	private static final Identifier SUN_TEXTURE = Identifier.ofVanilla("sun");
	private static final Identifier END_FLASH_TEXTURE = Identifier.ofVanilla("end_flash");
	private static final Identifier END_SKY_TEXTURE = Identifier.ofVanilla("textures/environment/end_sky.png");
	private static final float SKY_DISC_RADIUS = 512.0F;
	private static final int SKY_DISC_VERTEX_COUNT = 10;
	private static final int STAR_COUNT = 1500;
	private static final float SUN_SCALE = 30.0F;
	private static final float SUN_HEIGHT = 100.0F;
	private static final float MOON_SCALE = 20.0F;
	private static final float MOON_HEIGHT = 100.0F;
	private static final int SUNRISE_SEGMENT_COUNT = 16;
	private static final int QUAD_INDEX_COUNT = 6;
	private static final float END_FLASH_HEIGHT = 100.0F;
	private static final float END_FLASH_SCALE = 60.0F;
	private final SpriteAtlasTexture celestialAtlasTexture;
	private final GpuBuffer starVertexBuffer;
	private final GpuBuffer topSkyVertexBuffer;
	private final GpuBuffer bottomSkyVertexBuffer;
	private final GpuBuffer endSkyVertexBuffer;
	private final GpuBuffer sunVertexBuffer;
	private final GpuBuffer moonPhaseVertexBuffer;
	private final GpuBuffer sunRiseVertexBuffer;
	private final GpuBuffer endFlashVertexBuffer;
	private final RenderSystem.ShapeIndexBuffer
			indexBuffer2 =
			RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
	private final AbstractTexture endSkyTexture;
	private int starIndexCount;

	public SkyRendering(TextureManager textureManager, AtlasManager atlasManager) {
		this.celestialAtlasTexture = atlasManager.getAtlasTexture(Atlases.CELESTIALS);
		this.starVertexBuffer = this.createStars();
		this.endSkyVertexBuffer = createEndSky();
		this.endSkyTexture = this.bindTexture(textureManager, END_SKY_TEXTURE);
		this.endFlashVertexBuffer = createEndFlash(this.celestialAtlasTexture);
		this.sunVertexBuffer = createSun(this.celestialAtlasTexture);
		this.moonPhaseVertexBuffer = createMoonPhases(this.celestialAtlasTexture);
		this.sunRiseVertexBuffer = this.createSunRise();

		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(10 * VertexFormats.POSITION.getVertexSize())
		) {
			BufferBuilder
					bufferBuilder =
					new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
			this.createSky(bufferBuilder, 16.0F);

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				this.topSkyVertexBuffer =
						RenderSystem
								.getDevice()
								.createBuffer(() -> "Top sky vertex buffer", 32, builtBuffer.getBuffer());
			}

			bufferBuilder =
					new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
			this.createSky(bufferBuilder, -16.0F);

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				this.bottomSkyVertexBuffer =
						RenderSystem
								.getDevice()
								.createBuffer(() -> "Bottom sky vertex buffer", 32, builtBuffer.getBuffer());
			}
		}
	}

	private AbstractTexture bindTexture(TextureManager textureManager, Identifier texture) {
		return textureManager.getTexture(texture);
	}

	private GpuBuffer createSunRise() {
		int i = 18;
		int j = VertexFormats.POSITION_COLOR.getVertexSize();

		GpuBuffer var16;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(18 * j)) {
			BufferBuilder
					bufferBuilder =
					new BufferBuilder(
							bufferAllocator,
							VertexFormat.DrawMode.TRIANGLE_FAN,
							VertexFormats.POSITION_COLOR
					);
			int k = ColorHelper.getWhite(1.0F);
			int l = ColorHelper.getWhite(0.0F);
			bufferBuilder.vertex(0.0F, 100.0F, 0.0F).color(k);

			for (int m = 0; m <= 16; m++) {
				float f = m * (float) (Math.PI * 2) / 16.0F;
				float g = MathHelper.sin(f);
				float h = MathHelper.cos(f);
				bufferBuilder.vertex(g * 120.0F, h * 120.0F, -h * 40.0F).color(l);
			}

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var16 = RenderSystem.getDevice().createBuffer(() -> "Sunrise/Sunset fan", 32, builtBuffer.getBuffer());
			}
		}

		return var16;
	}

	private static GpuBuffer createSun(SpriteAtlasTexture atlas) {
		return createQuadVertexBuffer("Sun quad", atlas.getSprite(SUN_TEXTURE));
	}

	private static GpuBuffer createEndFlash(SpriteAtlasTexture atlas) {
		return createQuadVertexBuffer("End flash quad", atlas.getSprite(END_FLASH_TEXTURE));
	}

	private static GpuBuffer createQuadVertexBuffer(String description, Sprite sprite) {
		VertexFormat vertexFormat = VertexFormats.POSITION_TEXTURE;

		GpuBuffer var6;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(4 * vertexFormat.getVertexSize())) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);
			bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(sprite.getMinU(), sprite.getMinV());
			bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(sprite.getMaxU(), sprite.getMinV());
			bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(sprite.getMaxU(), sprite.getMaxV());
			bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(sprite.getMinU(), sprite.getMaxV());

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var6 = RenderSystem.getDevice().createBuffer(() -> description, 32, builtBuffer.getBuffer());
			}
		}

		return var6;
	}

	private static GpuBuffer createMoonPhases(SpriteAtlasTexture atlas) {
		MoonPhase[] moonPhases = MoonPhase.values();
		VertexFormat vertexFormat = VertexFormats.POSITION_TEXTURE;

		GpuBuffer var15;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(
				moonPhases.length * 4 * vertexFormat.getVertexSize())
		) {
			BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, vertexFormat);

			for (MoonPhase moonPhase : moonPhases) {
				Sprite sprite = atlas.getSprite(Identifier.ofVanilla("moon/" + moonPhase.asString()));
				bufferBuilder.vertex(-1.0F, 0.0F, -1.0F).texture(sprite.getMaxU(), sprite.getMaxV());
				bufferBuilder.vertex(1.0F, 0.0F, -1.0F).texture(sprite.getMinU(), sprite.getMaxV());
				bufferBuilder.vertex(1.0F, 0.0F, 1.0F).texture(sprite.getMinU(), sprite.getMinV());
				bufferBuilder.vertex(-1.0F, 0.0F, 1.0F).texture(sprite.getMaxU(), sprite.getMinV());
			}

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var15 = RenderSystem.getDevice().createBuffer(() -> "Moon phases", 32, builtBuffer.getBuffer());
			}
		}

		return var15;
	}

	private GpuBuffer createStars() {
		Random random = Random.create(10842L);
		float f = 100.0F;

		GpuBuffer var19;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(
				VertexFormats.POSITION.getVertexSize() * 1500 * 4)
		) {
			BufferBuilder
					bufferBuilder =
					new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

			for (int i = 0; i < 1500; i++) {
				float g = random.nextFloat() * 2.0F - 1.0F;
				float h = random.nextFloat() * 2.0F - 1.0F;
				float j = random.nextFloat() * 2.0F - 1.0F;
				float k = 0.15F + random.nextFloat() * 0.1F;
				float l = MathHelper.magnitude(g, h, j);
				if (!(l <= 0.010000001F) && !(l >= 1.0F)) {
					Vector3f vector3f = new Vector3f(g, h, j).normalize(100.0F);
					float m = (float) (random.nextDouble() * (float) Math.PI * 2.0);
					Matrix3f
							matrix3f =
							new Matrix3f()
									.rotateTowards(new Vector3f(vector3f).negate(), new Vector3f(0.0F, 1.0F, 0.0F))
									.rotateZ(-m);
					bufferBuilder.vertex(new Vector3f(k, -k, 0.0F).mul(matrix3f).add(vector3f));
					bufferBuilder.vertex(new Vector3f(k, k, 0.0F).mul(matrix3f).add(vector3f));
					bufferBuilder.vertex(new Vector3f(-k, k, 0.0F).mul(matrix3f).add(vector3f));
					bufferBuilder.vertex(new Vector3f(-k, -k, 0.0F).mul(matrix3f).add(vector3f));
				}
			}

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				this.starIndexCount = builtBuffer.getDrawParameters().indexCount();
				var19 = RenderSystem.getDevice().createBuffer(() -> "Stars vertex buffer", 40, builtBuffer.getBuffer());
			}
		}

		return var19;
	}

	private void createSky(VertexConsumer vertexConsumer, float height) {
		float f = Math.signum(height) * 512.0F;
		vertexConsumer.vertex(0.0F, height, 0.0F);

		for (int i = -180; i <= 180; i += 45) {
			vertexConsumer.vertex(
					f * MathHelper.cos(i * (float) (Math.PI / 180.0)),
					height,
					512.0F * MathHelper.sin(i * (float) (Math.PI / 180.0))
			);
		}
	}

	private static GpuBuffer createEndSky() {
		GpuBuffer var10;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(
				24 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())
		) {
			BufferBuilder
					bufferBuilder =
					new BufferBuilder(
							bufferAllocator,
							VertexFormat.DrawMode.QUADS,
							VertexFormats.POSITION_TEXTURE_COLOR
					);

			for (int i = 0; i < 6; i++) {
				Matrix4f matrix4f = new Matrix4f();
				switch (i) {
					case 1:
						matrix4f.rotationX((float) (Math.PI / 2));
						break;
					case 2:
						matrix4f.rotationX((float) (-Math.PI / 2));
						break;
					case 3:
						matrix4f.rotationX((float) Math.PI);
						break;
					case 4:
						matrix4f.rotationZ((float) (Math.PI / 2));
						break;
					case 5:
						matrix4f.rotationZ((float) (-Math.PI / 2));
				}

				bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).texture(0.0F, 0.0F).color(-14145496);
				bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).texture(0.0F, 16.0F).color(-14145496);
				bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).texture(16.0F, 16.0F).color(-14145496);
				bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).texture(16.0F, 0.0F).color(-14145496);
			}

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				var10 =
						RenderSystem
								.getDevice()
								.createBuffer(() -> "End sky vertex buffer", 40, builtBuffer.getBuffer());
			}
		}

		return var10;
	}

	/**
	 * Отрисовывает top sky.
	 *
	 * @param i i
	 */
	public void renderTopSky(int i) {
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            RenderSystem.getModelViewMatrix(),
				                                            ColorHelper.toRgbaVector(i),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );
		GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
		GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Sky disc",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(RenderPipelines.POSITION_SKY);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.topSkyVertexBuffer);
			renderPass.draw(0, 10);
		}
	}

	/**
	 * Обновляет render state.
	 *
	 * @param world world
	 * @param tickProgress tick progress
	 * @param camera camera
	 * @param state state
	 */
	public void updateRenderState(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state) {
		state.skybox = world.getDimension().skybox();
		if (state.skybox != DimensionType.Skybox.NONE) {
			if (state.skybox == DimensionType.Skybox.END) {
				EndLightFlashManager endLightFlashManager = world.getEndLightFlashManager();
				if (endLightFlashManager != null) {
					state.endFlashIntensity = endLightFlashManager.getSkyFactor(tickProgress);
					state.endFlashPitch = endLightFlashManager.getPitch();
					state.endFlashYaw = endLightFlashManager.getYaw();
				}
			}
			else {
				EnvironmentAttributeInterpolator
						environmentAttributeInterpolator =
						camera.getEnvironmentAttributeInterpolator();
				state.sunAngle =
						environmentAttributeInterpolator.get(EnvironmentAttributes.SUN_ANGLE_VISUAL, tickProgress)
								* (float) (Math.PI / 180.0);
				state.moonAngle =
						environmentAttributeInterpolator.get(EnvironmentAttributes.MOON_ANGLE_VISUAL, tickProgress)
								* (float) (Math.PI / 180.0);
				state.starAngle =
						environmentAttributeInterpolator.get(EnvironmentAttributes.STAR_ANGLE_VISUAL, tickProgress)
								* (float) (Math.PI / 180.0);
				state.rainGradient = 1.0F - world.getRainGradient(tickProgress);
				state.starBrightness =
						environmentAttributeInterpolator.get(
								EnvironmentAttributes.STAR_BRIGHTNESS_VISUAL,
								tickProgress
						);
				state.sunriseAndSunsetColor =
						camera
								.getEnvironmentAttributeInterpolator()
								.get(EnvironmentAttributes.SUNRISE_SUNSET_COLOR_VISUAL, tickProgress);
				state.moonPhase =
						environmentAttributeInterpolator.get(EnvironmentAttributes.MOON_PHASE_VISUAL, tickProgress);
				state.skyColor =
						environmentAttributeInterpolator.get(EnvironmentAttributes.SKY_COLOR_VISUAL, tickProgress);
				state.shouldRenderSkyDark = this.isSkyDark(tickProgress, world);
			}
		}
	}

	private boolean isSkyDark(float tickProgress, ClientWorld world) {
		return MinecraftClient.getInstance().player.getCameraPosVec(tickProgress).y - world
				.getLevelProperties()
				.getSkyDarknessHeight(world) < 0.0;
	}

	/**
	 * Отрисовывает sky dark.
	 */
	public void renderSkyDark() {
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.translate(0.0F, 12.0F, 0.0F);
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            matrix4fStack,
				                                            new Vector4f(0.0F, 0.0F, 0.0F, 1.0F),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );
		GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
		GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Sky dark",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(RenderPipelines.POSITION_SKY);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.bottomSkyVertexBuffer);
			renderPass.draw(0, 10);
		}

		matrix4fStack.popMatrix();
	}

	public void renderCelestialBodies(
			MatrixStack matrices,
			float sunAngle,
			float moonAngle,
			float starAngle,
			MoonPhase moonPhase,
			float alpha,
			float starBrightness
	) {
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(sunAngle));
		this.renderSun(alpha, matrices);
		matrices.pop();
		matrices.push();
		matrices.multiply(RotationAxis.POSITIVE_X.rotation(moonAngle));
		this.renderMoon(moonPhase, alpha, matrices);
		matrices.pop();
		if (starBrightness > 0.0F) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_X.rotation(starAngle));
			this.renderStars(starBrightness, matrices);
			matrices.pop();
		}

		matrices.pop();
	}

	private void renderSun(float alpha, MatrixStack matrices) {
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.mul(matrices.peek().getPositionMatrix());
		matrix4fStack.translate(0.0F, 100.0F, 0.0F);
		matrix4fStack.scale(30.0F, 1.0F, 30.0F);
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            matrix4fStack,
				                                            new Vector4f(1.0F, 1.0F, 1.0F, alpha),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );
		GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
		GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();
		GpuBuffer gpuBuffer = this.indexBuffer2.getIndexBuffer(6);

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Sky sun",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_CELESTIAL);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.bindTexture(
					"Sampler0",
					this.celestialAtlasTexture.getGlTextureView(),
					this.celestialAtlasTexture.getSampler()
			);
			renderPass.setVertexBuffer(0, this.sunVertexBuffer);
			renderPass.setIndexBuffer(gpuBuffer, this.indexBuffer2.getIndexType());
			renderPass.drawIndexed(0, 0, 6, 1);
		}

		matrix4fStack.popMatrix();
	}

	private void renderMoon(MoonPhase moonPhase, float alpha, MatrixStack matrices) {
		int i = moonPhase.getIndex() * 4;
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.mul(matrices.peek().getPositionMatrix());
		matrix4fStack.translate(0.0F, 100.0F, 0.0F);
		matrix4fStack.scale(20.0F, 1.0F, 20.0F);
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            matrix4fStack,
				                                            new Vector4f(1.0F, 1.0F, 1.0F, alpha),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );
		GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
		GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();
		GpuBuffer gpuBuffer = this.indexBuffer2.getIndexBuffer(6);

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Sky moon",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_CELESTIAL);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.bindTexture(
					"Sampler0",
					this.celestialAtlasTexture.getGlTextureView(),
					this.celestialAtlasTexture.getSampler()
			);
			renderPass.setVertexBuffer(0, this.moonPhaseVertexBuffer);
			renderPass.setIndexBuffer(gpuBuffer, this.indexBuffer2.getIndexType());
			renderPass.drawIndexed(i, 0, 6, 1);
		}

		matrix4fStack.popMatrix();
	}

	private void renderStars(float brightness, MatrixStack matrices) {
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.mul(matrices.peek().getPositionMatrix());
		RenderPipeline renderPipeline = RenderPipelines.POSITION_STARS;
		GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
		GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();
		GpuBuffer gpuBuffer = this.indexBuffer2.getIndexBuffer(this.starIndexCount);
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            matrix4fStack,
				                                            new Vector4f(
						                                            brightness,
						                                            brightness,
						                                            brightness,
						                                            brightness
				                                            ),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Stars",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, this.starVertexBuffer);
			renderPass.setIndexBuffer(gpuBuffer, this.indexBuffer2.getIndexType());
			renderPass.drawIndexed(0, 0, this.starIndexCount, 1);
		}

		matrix4fStack.popMatrix();
	}

	/**
	 * Отрисовывает glowing sky.
	 *
	 * @param matrices matrices
	 * @param solarAngle solar angle
	 * @param color color
	 */
	public void renderGlowingSky(MatrixStack matrices, float solarAngle, int color) {
		float f = ColorHelper.getAlphaFloat(color);
		if (!(f <= 0.001F)) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
			float g = MathHelper.sin(solarAngle) < 0.0F ? 180.0F : 0.0F;
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g + 90.0F));
			Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
			matrix4fStack.pushMatrix();
			matrix4fStack.mul(matrices.peek().getPositionMatrix());
			matrix4fStack.scale(1.0F, 1.0F, f);
			GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
			                                            .write(
					                                            matrix4fStack,
					                                            ColorHelper.toRgbaVector(color),
					                                            new Vector3f(),
					                                            new Matrix4f()
			                                            );
			GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
			GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();

			try (RenderPass renderPass = RenderSystem.getDevice()
			                                         .createCommandEncoder()
			                                         .createRenderPass(
					                                         () -> "Sunrise sunset",
					                                         gpuTextureView,
					                                         OptionalInt.empty(),
					                                         gpuTextureView2,
					                                         OptionalDouble.empty()
			                                         )
			) {
				renderPass.setPipeline(RenderPipelines.POSITION_COLOR_SUNRISE_SUNSET);
				RenderSystem.bindDefaultUniforms(renderPass);
				renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
				renderPass.setVertexBuffer(0, this.sunRiseVertexBuffer);
				renderPass.draw(0, 18);
			}

			matrix4fStack.popMatrix();
			matrices.pop();
		}
	}

	/**
	 * Отрисовывает end sky.
	 */
	public void renderEndSky() {
		RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
		GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(36);
		GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
		GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            RenderSystem.getModelViewMatrix(),
				                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "End sky",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_END_SKY);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.bindTexture("Sampler0", this.endSkyTexture.getGlTextureView(), this.endSkyTexture.getSampler());
			renderPass.setVertexBuffer(0, this.endSkyVertexBuffer);
			renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());
			renderPass.drawIndexed(0, 0, 36, 1);
		}
	}

	/**
	 * Draw end light flash.
	 *
	 * @param matrices matrices
	 * @param intensity intensity
	 * @param pitch pitch
	 * @param yaw yaw
	 */
	public void drawEndLightFlash(MatrixStack matrices, float intensity, float pitch, float yaw) {
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - pitch));
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.mul(matrices.peek().getPositionMatrix());
		matrix4fStack.translate(0.0F, 100.0F, 0.0F);
		matrix4fStack.scale(60.0F, 1.0F, 60.0F);
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            matrix4fStack,
				                                            new Vector4f(intensity, intensity, intensity, intensity),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );
		GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
		GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();
		GpuBuffer gpuBuffer = this.indexBuffer2.getIndexBuffer(6);

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "End flash",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_CELESTIAL);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.bindTexture(
					"Sampler0",
					this.celestialAtlasTexture.getGlTextureView(),
					this.celestialAtlasTexture.getSampler()
			);
			renderPass.setVertexBuffer(0, this.endFlashVertexBuffer);
			renderPass.setIndexBuffer(gpuBuffer, this.indexBuffer2.getIndexType());
			renderPass.drawIndexed(0, 0, 6, 1);
		}

		matrix4fStack.popMatrix();
	}

	@Override
	public void close() {
		this.sunVertexBuffer.close();
		this.moonPhaseVertexBuffer.close();
		this.starVertexBuffer.close();
		this.topSkyVertexBuffer.close();
		this.bottomSkyVertexBuffer.close();
		this.endSkyVertexBuffer.close();
		this.sunRiseVertexBuffer.close();
		this.endFlashVertexBuffer.close();
	}
}
