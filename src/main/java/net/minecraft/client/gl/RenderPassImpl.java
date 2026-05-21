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

@Environment(EnvType.CLIENT)
/**
 * {@code RenderPassImpl}.
 */
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
		return this.hasDepth;
	}

	@Override
	public void pushDebugGroup(Supplier<String> supplier) {
		if (this.closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}
		else {
			this.debugGroupPushCount++;
			this.resourceManager.getBackend().getDebugLabelManager().pushDebugGroup(supplier);
		}
	}

	@Override
	public void popDebugGroup() {
		if (this.closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}
		else if (this.debugGroupPushCount == 0) {
			throw new IllegalStateException("Can't pop more debug groups than was pushed!");
		}
		else {
			this.debugGroupPushCount--;
			this.resourceManager.getBackend().getDebugLabelManager().popDebugGroup();
		}
	}

	@Override
	public void setPipeline(RenderPipeline renderPipeline) {
		if (this.pipeline == null || this.pipeline.info() != renderPipeline) {
			this.setSimpleUniforms.addAll(this.simpleUniforms.keySet());
			this.setSimpleUniforms.addAll(this.samplerUniforms.keySet());
		}

		this.pipeline = this.resourceManager.getBackend().compilePipelineCached(renderPipeline);
	}

	@Override
	public void bindTexture(String string, @Nullable GpuTextureView gpuTextureView, @Nullable GpuSampler gpuSampler) {
		if (gpuSampler == null) {
			this.samplerUniforms.remove(string);
		}
		else {
			this.samplerUniforms.put(
					string,
					new RenderPassImpl.SamplerUniform((GlTextureView) gpuTextureView, (GlSampler) gpuSampler)
			);
		}

		this.setSimpleUniforms.add(string);
	}

	@Override
	public void setUniform(String string, GpuBuffer gpuBuffer) {
		this.simpleUniforms.put(string, gpuBuffer.slice());
		this.setSimpleUniforms.add(string);
	}

	@Override
	public void setUniform(String string, GpuBufferSlice gpuBufferSlice) {
		int i = this.resourceManager.getBackend().getUniformOffsetAlignment();
		if (gpuBufferSlice.offset() % i > 0L) {
			throw new IllegalArgumentException("Uniform buffer offset must be aligned to " + i);
		}
		else {
			this.simpleUniforms.put(string, gpuBufferSlice);
			this.setSimpleUniforms.add(string);
		}
	}

	@Override
	public void enableScissor(int i, int j, int k, int l) {
		this.scissorState.enable(i, j, k, l);
	}

	@Override
	public void disableScissor() {
		this.scissorState.disable();
	}

	public boolean isScissorEnabled() {
		return this.scissorState.isEnabled();
	}

	public int getScissorX() {
		return this.scissorState.getX();
	}

	public int getScissorY() {
		return this.scissorState.getY();
	}

	public int getScissorWidth() {
		return this.scissorState.getWidth();
	}

	public int getScissorHeight() {
		return this.scissorState.getHeight();
	}

	@Override
	public void setVertexBuffer(int i, GpuBuffer gpuBuffer) {
		if (i >= 0 && i < 1) {
			this.vertexBuffers[i] = gpuBuffer;
		}
		else {
			throw new IllegalArgumentException("Vertex buffer slot is out of range: " + i);
		}
	}

	@Override
	public void setIndexBuffer(@Nullable GpuBuffer gpuBuffer, VertexFormat.IndexType indexType) {
		this.indexBuffer = gpuBuffer;
		this.indexType = indexType;
	}

	@Override
	public void drawIndexed(int i, int j, int k, int l) {
		if (this.closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}
		else {
			this.resourceManager.drawBoundObjectWithRenderPass(this, i, j, k, this.indexType, l);
		}
	}

	@Override
	public <T> void drawMultipleIndexed(
			Collection<RenderPass.RenderObject<T>> collection,
			@Nullable GpuBuffer gpuBuffer,
			VertexFormat.@Nullable IndexType indexType,
			Collection<String> collection2,
			T object
	) {
		if (this.closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}
		else {
			this.resourceManager.drawObjectsWithRenderPass(this, collection, gpuBuffer, indexType, collection2, object);
		}
	}

	@Override
	public void draw(int i, int j) {
		if (this.closed) {
			throw new IllegalStateException("Can't use a closed render pass");
		}
		else {
			this.resourceManager.drawBoundObjectWithRenderPass(this, i, 0, j, null, 1);
		}
	}

	@Override
	public void close() {
		if (!this.closed) {
			if (this.debugGroupPushCount > 0) {
				throw new IllegalStateException("Render pass had debug groups left open!");
			}

			this.closed = true;
			this.resourceManager.closePass();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SamplerUniform}.
	 */
	protected record SamplerUniform(GlTextureView view, GlSampler sampler) {
	}
}
