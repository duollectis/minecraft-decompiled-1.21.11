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

@Environment(EnvType.CLIENT)
/**
 * {@code CubeMapRenderer}.
 */
public class CubeMapRenderer implements AutoCloseable {

	private static final int FACES_COUNT = 6;
	private final GpuBuffer buffer;
	private final ProjectionMatrix3 projectionMatrix;
	private final Identifier id;

	public CubeMapRenderer(Identifier id) {
		this.id = id;
		this.projectionMatrix = new ProjectionMatrix3("cubemap", 0.05F, 10.0F);
		this.buffer = upload();
	}

	public void draw(MinecraftClient client, float x, float y) {
		RenderSystem.setProjectionMatrix(
				this.projectionMatrix.set(
						client.getWindow().getFramebufferWidth(),
						client.getWindow().getFramebufferHeight(),
						85.0F
				), ProjectionType.PERSPECTIVE
		);
		RenderPipeline renderPipeline = RenderPipelines.POSITION_TEX_PANORAMA;
		Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
		GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
		GpuTextureView gpuTextureView2 = framebuffer.getDepthAttachmentView();
		RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
		GpuBuffer gpuBuffer = shapeIndexBuffer.getIndexBuffer(36);
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		matrix4fStack.pushMatrix();
		matrix4fStack.rotationX((float) Math.PI);
		matrix4fStack.rotateX(x * (float) (Math.PI / 180.0));
		matrix4fStack.rotateY(y * (float) (Math.PI / 180.0));
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            new Matrix4f(matrix4fStack),
				                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				                                            new Vector3f(),
				                                            new Matrix4f()
		                                            );
		matrix4fStack.popMatrix();

		try (RenderPass renderPass = RenderSystem.getDevice()
		                                         .createCommandEncoder()
		                                         .createRenderPass(
				                                         () -> "Cubemap",
				                                         gpuTextureView,
				                                         OptionalInt.empty(),
				                                         gpuTextureView2,
				                                         OptionalDouble.empty()
		                                         )
		) {
			renderPass.setPipeline(renderPipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setVertexBuffer(0, this.buffer);
			renderPass.setIndexBuffer(gpuBuffer, shapeIndexBuffer.getIndexType());
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			AbstractTexture abstractTexture = client.getTextureManager().getTexture(this.id);
			renderPass.bindTexture("Sampler0", abstractTexture.getGlTextureView(), abstractTexture.getSampler());
			renderPass.drawIndexed(0, 0, 36, 1);
		}
	}

	private static GpuBuffer upload() {
		GpuBuffer var3;
		try (BufferAllocator bufferAllocator = BufferAllocator.fixedSized(
				VertexFormats.POSITION.getVertexSize() * 4 * 6)
		) {
			BufferBuilder
					bufferBuilder =
					new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
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
				var3 =
						RenderSystem
								.getDevice()
								.createBuffer(() -> "Cube map vertex buffer", 32, builtBuffer.getBuffer());
			}
		}

		return var3;
	}

	public void registerTextures(TextureManager textureManager) {
		textureManager.registerTexture(this.id, (AbstractTexture) (new CubemapTexture(this.id)));
	}

	@Override
	public void close() {
		this.buffer.close();
		this.projectionMatrix.close();
	}
}
