package net.minecraft.client.gui;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.ProjectionMatrix3;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.CubemapTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Рендерит кубическую карту (skybox) для фона главного меню.
 * Использует 6 граней куба с перспективной проекцией 85°.
 */
@Environment(EnvType.CLIENT)
public class CubeMapRenderer implements AutoCloseable {

	private static final int FACES_COUNT = 6;
	private final GpuBuffer buffer;
	private final ProjectionMatrix3 projectionMatrix;
	private final Identifier id;

	public CubeMapRenderer(Identifier id) {
		this.id = id;
		projectionMatrix = new ProjectionMatrix3("cubemap", 0.05F, 10.0F);
		buffer = upload();
	}

	/**
	 * Отрисовывает кубическую карту с заданными углами поворота.
	 *
	 * @param client клиент Minecraft для доступа к окну и текстурам
	 * @param x угол наклона по оси X (pitch) в градусах
	 * @param y угол поворота по оси Y (yaw) в градусах
	 */
	public void draw(MinecraftClient client, float x, float y) {
		RenderSystem.setProjectionMatrix(
				projectionMatrix.set(
						client.getWindow().getFramebufferWidth(),
						client.getWindow().getFramebufferHeight(),
						85.0F
				), ProjectionType.PERSPECTIVE
		);
		RenderPipeline renderPipeline = RenderPipelines.POSITION_TEX_PANORAMA;
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		GpuTextureView colorView = framebuffer.getColorAttachmentView();
		GpuTextureView depthView = framebuffer.getDepthAttachmentView();
		RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
		GpuBuffer indexBuffer = shapeIndexBuffer.getIndexBuffer(36);
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.rotationX((float) Math.PI);
		modelViewStack.rotateX(x * (float) (Math.PI / 180.0));
		modelViewStack.rotateY(y * (float) (Math.PI / 180.0));
		GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms()
		                                             .write(
				                                             new Matrix4f(modelViewStack),
				                                             new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				                                             new Vector3f(),
				                                             new Matrix4f()
		                                             );
		modelViewStack.popMatrix();

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Cubemap",
				                                         colorView,
				                                         OptionalInt.empty(),
				                                         depthView,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setVertexBuffer(0, buffer);
			renderPass.setIndexBuffer(indexBuffer, shapeIndexBuffer.getIndexType());
			renderPass.setUniform("DynamicTransforms", dynamicUniforms);
			AbstractTexture cubeTexture = client.getTextureManager().getTexture(id);
			renderPass.bindTexture("Sampler0", cubeTexture.getGlTextureView(), cubeTexture.getSampler());
			renderPass.drawIndexed(0, 0, 36, 1);
		}
	}

	private static GpuBuffer upload() {
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(
				VertexFormats.POSITION.getVertexSize() * 4 * FACES_COUNT)
		) {
			BufferBuilder bufferBuilder = new BufferBuilder(
					bufferAllocator,
					VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION
			);
			bufferBuilder.vertex(-1.0F, -1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, 1.0F, 1.0F);
			bufferBuilder.vertex(1.0F, 1.0F, 1.0F);
			bufferBuilder.vertex(1.0F, -1.0F, 1.0F);
			bufferBuilder.vertex(1.0F, -1.0F, 1.0F);
			bufferBuilder.vertex(1.0F, 1.0F, 1.0F);
			bufferBuilder.vertex(1.0F, 1.0F, -1.0F);
			bufferBuilder.vertex(1.0F, -1.0F, -1.0F);
			bufferBuilder.vertex(1.0F, -1.0F, -1.0F);
			bufferBuilder.vertex(1.0F, 1.0F, -1.0F);
			bufferBuilder.vertex(-1.0F, 1.0F, -1.0F);
			bufferBuilder.vertex(-1.0F, -1.0F, -1.0F);
			bufferBuilder.vertex(-1.0F, -1.0F, -1.0F);
			bufferBuilder.vertex(-1.0F, 1.0F, -1.0F);
			bufferBuilder.vertex(-1.0F, 1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, -1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, -1.0F, -1.0F);
			bufferBuilder.vertex(-1.0F, -1.0F, 1.0F);
			bufferBuilder.vertex(1.0F, -1.0F, 1.0F);
			bufferBuilder.vertex(1.0F, -1.0F, -1.0F);
			bufferBuilder.vertex(-1.0F, 1.0F, 1.0F);
			bufferBuilder.vertex(-1.0F, 1.0F, -1.0F);
			bufferBuilder.vertex(1.0F, 1.0F, -1.0F);
			bufferBuilder.vertex(1.0F, 1.0F, 1.0F);

			try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
				return RenderSystem
						.getDevice()
						.createBuffer(() -> "Cube map vertex buffer", 32, builtBuffer.getBuffer());
			}
		}
	}

	public void registerTextures(TextureManager textureManager) {
		textureManager.registerTexture(id, new CubemapTexture(id));
	}

	@Override
	public void close() {
		buffer.close();
		projectionMatrix.close();
	}
}
