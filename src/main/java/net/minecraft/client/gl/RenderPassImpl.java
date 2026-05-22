package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.texture.GlTextureView;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Реализация {@link RenderPass} поверх {@link GlCommandEncoder}.
 * Хранит текущее состояние прохода: пайплайн, буферы, uniform-значения и scissor-регион.
 */
@Environment(EnvType.CLIENT)
public class RenderPassImpl implements RenderPass {

	protected static final int INITIAL_PASS_INDEX = 1;
	public static final boolean IS_DEVELOPMENT = SharedConstants.isDevelopment;

	private final GlCommandEncoder resourceManager;
	private final boolean hasDepth;
	private boolean closed;
	protected @Nullable CompiledShaderPipeline pipeline;
	protected final @Nullable GpuBuffer[] vertexBuffers = new GpuBuffer[1];
	protected @Nullable GpuBuffer indexBuffer;
	protected VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;
	private final ScissorState scissorState = new ScissorState();
	protected final HashMap<String, GpuBufferSlice> simpleUniforms = new HashMap<>();
	protected final HashMap<String, RenderPassImpl.SamplerUniform> samplerUniforms = new HashMap<>();
	protected final Set<String> setSimpleUniforms = new HashSet<>();
	protected int debugGroupPushCount;

	public RenderPassImpl(GlCommandEncoder resourceManager, boolean hasDepth) {
		this.resourceManager = resourceManager;
		this.hasDepth = hasDepth;
	}

	public boolean hasDepth() {
		return hasDepth;
	}

	@Override
	public void pushDebugGroup(Supplier<String> supplier) {
		if (closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}

		debugGroupPushCount++;
		resourceManager.getBackend().getDebugLabelManager().pushDebugGroup(supplier);
	}

	@Override
	public void popDebugGroup() {
		if (closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}

		if (debugGroupPushCount == 0) {
			throw new IllegalStateException("Can't pop more debug groups than was pushed!");
		}

		debugGroupPushCount--;
		resourceManager.getBackend().getDebugLabelManager().popDebugGroup();
	}

	@Override
	public void setPipeline(RenderPipeline renderPipeline) {
		if (pipeline == null || pipeline.info() != renderPipeline) {
			setSimpleUniforms.addAll(simpleUniforms.keySet());
			setSimpleUniforms.addAll(samplerUniforms.keySet());
		}

		pipeline = resourceManager.getBackend().compilePipelineCached(renderPipeline);
	}

	@Override
	public void bindTexture(String name, @Nullable GpuTextureView textureView, @Nullable GpuSampler sampler) {
		if (sampler == null) {
			samplerUniforms.remove(name);
		} else {
			samplerUniforms.put(name, new RenderPassImpl.SamplerUniform((GlTextureView) textureView, (GlSampler) sampler));
		}

		setSimpleUniforms.add(name);
	}

	@Override
	public void setUniform(String name, GpuBuffer buffer) {
		simpleUniforms.put(name, buffer.slice());
		setSimpleUniforms.add(name);
	}

	@Override
	public void setUniform(String name, GpuBufferSlice bufferSlice) {
		int alignment = resourceManager.getBackend().getUniformOffsetAlignment();

		if (bufferSlice.offset() % alignment > 0L) {
			throw new IllegalArgumentException("Uniform buffer offset must be aligned to " + alignment);
		}

		simpleUniforms.put(name, bufferSlice);
		setSimpleUniforms.add(name);
	}

	@Override
	public void enableScissor(int x, int y, int width, int height) {
		scissorState.enable(x, y, width, height);
	}

	@Override
	public void disableScissor() {
		scissorState.disable();
	}

	public boolean isScissorEnabled() {
		return scissorState.isEnabled();
	}

	public int getScissorX() {
		return scissorState.getX();
	}

	public int getScissorY() {
		return scissorState.getY();
	}

	public int getScissorWidth() {
		return scissorState.getWidth();
	}

	public int getScissorHeight() {
		return scissorState.getHeight();
	}

	@Override
	public void setVertexBuffer(int slot, GpuBuffer buffer) {
		if (slot < 0 || slot >= vertexBuffers.length) {
			throw new IllegalArgumentException("Vertex buffer slot is out of range: " + slot);
		}

		vertexBuffers[slot] = buffer;
	}

	@Override
	public void setIndexBuffer(@Nullable GpuBuffer buffer, VertexFormat.IndexType type) {
		indexBuffer = buffer;
		indexType = type;
	}

	@Override
	public void drawIndexed(int firstIndex, int baseVertex, int indexCount, int instanceCount) {
		if (closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}

		resourceManager.drawBoundObjectWithRenderPass(this, firstIndex, baseVertex, indexCount, indexType, instanceCount);
	}

	@Override
	public <T> void drawMultipleIndexed(
		Collection<RenderPass.RenderObject<T>> objects,
		@Nullable GpuBuffer indexBuffer,
		VertexFormat.@Nullable IndexType indexType,
		Collection<String> validationSkippedUniforms,
		T object
	) {
		if (closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}

		resourceManager.drawObjectsWithRenderPass(
			this, objects, indexBuffer, indexType, validationSkippedUniforms, object
		);
	}

	@Override
	public void draw(int firstVertex, int vertexCount) {
		if (closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}

		resourceManager.drawBoundObjectWithRenderPass(this, firstVertex, 0, vertexCount, null, 1);
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}

		if (debugGroupPushCount > 0) {
			throw new IllegalStateException("Render pass had debug groups left open!");
		}

		closed = true;
		resourceManager.closePass();
	}

	@Environment(EnvType.CLIENT)
	protected record SamplerUniform(GlTextureView view, GlSampler sampler) {
	}
}
