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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ScissorState;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code RenderLayer}.
 */
public class RenderLayer {

	private static final int DEFAULT_BUFFER_SIZE = 1048576;
	public static final int LARGE_BUFFER_SIZE = 4194304;
	public static final int MEDIUM_BUFFER_SIZE = 786432;
	public static final int SMALL_BUFFER_SIZE = 1536;
	private final RenderSetup renderSetup;
	private final Optional<RenderLayer> affectedOutline;
	protected final String name;

	private RenderLayer(String name, RenderSetup renderSetup) {
		this.name = name;
		this.renderSetup = renderSetup;
		this.affectedOutline = renderSetup.outlineMode == RenderSetup.OutlineMode.AFFECTS_OUTLINE
		                       ? renderSetup.textures
		                         .values()
		                         .stream()
		                         .findFirst()
		                         .map(texture -> RenderLayers.OUTLINE.apply(
				                         texture.location(),
				                         renderSetup.pipeline.isCull()
		                         ))
		                       : Optional.empty();
	}

	/**
	 * Of.
	 *
	 * @param name name
	 * @param renderSetup render setup
	 *
	 * @return RenderLayer — результат операции
	 */
	public static RenderLayer of(String name, RenderSetup renderSetup) {
		return new RenderLayer(name, renderSetup);
	}

	@Override
	public String toString() {
		return "RenderType[" + this.name + ":" + this.renderSetup + "]";
	}

	/**
	 * Draw.
	 *
	 * @param buffer buffer
	 */
	public void draw(BuiltBuffer buffer) {
		Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
		Consumer<Matrix4fStack> consumer = this.renderSetup.layeringTransform.getTransform();
		if (consumer != null) {
			matrix4fStack.pushMatrix();
			consumer.accept(matrix4fStack);
		}

		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
		                                            .write(
				                                            RenderSystem.getModelViewMatrix(),
				                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				                                            new Vector3f(),
				                                            this.renderSetup.textureTransform.getTransformSupplier()
		                                            );
		Map<String, RenderSetup.Texture> map = this.renderSetup.resolveTextures();
		BuiltBuffer var6 = buffer;

		try {
			GpuBuffer
					gpuBuffer =
					this.renderSetup.pipeline.getVertexFormat().uploadImmediateVertexBuffer(buffer.getBuffer());
			GpuBuffer gpuBuffer2;
			VertexFormat.IndexType indexType;
			if (buffer.getSortedBuffer() == null) {
				RenderSystem.ShapeIndexBuffer
						shapeIndexBuffer =
						RenderSystem.getSequentialBuffer(buffer.getDrawParameters().mode());
				gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(buffer.getDrawParameters().indexCount());
				indexType = shapeIndexBuffer.getIndexType();
			}
			else {
				gpuBuffer2 =
						this.renderSetup.pipeline
								.getVertexFormat()
								.uploadImmediateIndexBuffer(buffer.getSortedBuffer());
				indexType = buffer.getDrawParameters().indexType();
			}

			Framebuffer framebuffer = this.renderSetup.outputTarget.getFramebuffer();
			GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride != null
			                                ? RenderSystem.outputColorTextureOverride
			                                : framebuffer.getColorAttachmentView();
			GpuTextureView gpuTextureView2 = framebuffer.useDepthAttachment
			                                 ? (RenderSystem.outputDepthTextureOverride != null
			                                    ? RenderSystem.outputDepthTextureOverride
			                                    : framebuffer.getDepthAttachmentView()
			                                 )
			                                 : null;

			try (RenderPass renderPass = RenderSystem.getDevice()
			                                         .createCommandEncoder()
			                                         .createRenderPass(
					                                         () -> "Immediate draw for " + this.name,
					                                         gpuTextureView,
					                                         OptionalInt.empty(),
					                                         gpuTextureView2,
					                                         OptionalDouble.empty()
			                                         )
			) {
				renderPass.setPipeline(this.renderSetup.pipeline);
				ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
				if (scissorState.isEnabled()) {
					renderPass.enableScissor(
							scissorState.getX(),
							scissorState.getY(),
							scissorState.getWidth(),
							scissorState.getHeight()
					);
				}

				RenderSystem.bindDefaultUniforms(renderPass);
				renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
				renderPass.setVertexBuffer(0, gpuBuffer);

				for (Entry<String, RenderSetup.Texture> entry : map.entrySet()) {
					renderPass.bindTexture(entry.getKey(), entry.getValue().textureView(), entry.getValue().sampler());
				}

				renderPass.setIndexBuffer(gpuBuffer2, indexType);
				renderPass.drawIndexed(0, 0, buffer.getDrawParameters().indexCount(), 1);
			}
		}
		catch (Throwable var20) {
			if (buffer != null) {
				try {
					var6.close();
				}
				catch (Throwable var17) {
					var20.addSuppressed(var17);
				}
			}

			throw var20;
		}

		if (buffer != null) {
			buffer.close();
		}

		if (consumer != null) {
			matrix4fStack.popMatrix();
		}
	}

	public int getExpectedBufferSize() {
		return this.renderSetup.expectedBufferSize;
	}

	public VertexFormat getVertexFormat() {
		return this.renderSetup.pipeline.getVertexFormat();
	}

	public VertexFormat.DrawMode getDrawMode() {
		return this.renderSetup.pipeline.getVertexFormatMode();
	}

	public Optional<RenderLayer> getAffectedOutline() {
		return this.affectedOutline;
	}

	public boolean isOutline() {
		return this.renderSetup.outlineMode == RenderSetup.OutlineMode.IS_OUTLINE;
	}

	public RenderPipeline getRenderPipeline() {
		return this.renderSetup.pipeline;
	}

	public boolean hasCrumbling() {
		return this.renderSetup.hasCrumbling;
	}

	/**
	 * Are vertices not shared.
	 *
	 * @return boolean — результат операции
	 */
	public boolean areVerticesNotShared() {
		return !this.getDrawMode().shareVertices;
	}

	public boolean isTranslucent() {
		return this.renderSetup.translucent;
	}
}
