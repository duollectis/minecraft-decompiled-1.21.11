package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.FramePass;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;

import java.util.*;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
/**
 * {@code PostEffectPass}.
 */
public class PostEffectPass implements AutoCloseable {

	private static final int SIZE = new Std140SizeCalculator().putVec2().get();
	private final String id;
	private final RenderPipeline pipeline;
	private final Identifier outputTargetId;
	private final Map<String, GpuBuffer> uniformBuffers = new HashMap<>();
	private final MappableRingBuffer samplerInfoBuffer;
	private final List<PostEffectPass.Sampler> samplers;

	public PostEffectPass(
			RenderPipeline pipeline,
			Identifier outputTargetId,
			Map<String, List<UniformValue>> uniforms,
			List<PostEffectPass.Sampler> samplers
	) {
		this.pipeline = pipeline;
		this.id = pipeline.getLocation().toString();
		this.outputTargetId = outputTargetId;
		this.samplers = samplers;

		for (Entry<String, List<UniformValue>> entry : uniforms.entrySet()) {
			List<UniformValue> list = entry.getValue();
			if (!list.isEmpty()) {
				Std140SizeCalculator std140SizeCalculator = new Std140SizeCalculator();

				for (UniformValue uniformValue : list) {
					uniformValue.addSize(std140SizeCalculator);
				}

				int i = std140SizeCalculator.get();
				MemoryStack memoryStack = MemoryStack.stackPush();

				try {
					Std140Builder std140Builder = Std140Builder.onStack(memoryStack, i);

					for (UniformValue uniformValue2 : list) {
						uniformValue2.write(std140Builder);
					}

					this.uniformBuffers.put(
							entry.getKey(),
							RenderSystem
									.getDevice()
									.createBuffer(() -> this.id + " / " + entry.getKey(), 128, std140Builder.get())
					);
				}
				catch (Throwable var15) {
					if (memoryStack != null) {
						try {
							memoryStack.close();
						}
						catch (Throwable var14) {
							var15.addSuppressed(var14);
						}
					}

					throw var15;
				}

				if (memoryStack != null) {
					memoryStack.close();
				}
			}
		}

		this.samplerInfoBuffer =
				new MappableRingBuffer(() -> this.id + " SamplerInfo", 130, (samplers.size() + 1) * SIZE);
	}

	public void render(FrameGraphBuilder builder, Map<Identifier, Handle<Framebuffer>> handles, GpuBufferSlice slice) {
		FramePass framePass = builder.createPass(this.id);

		for (PostEffectPass.Sampler sampler : this.samplers) {
			sampler.preRender(framePass, handles);
		}

		Handle<Framebuffer>
				handle =
				handles.computeIfPresent(
						this.outputTargetId,
						(id, handlex) -> framePass.transfer((Handle<Framebuffer>) handlex)
				);
		if (handle == null) {
			throw new IllegalStateException("Missing handle for target " + this.outputTargetId);
		}
		else {
			framePass.setRenderer(
					() -> {
						Framebuffer framebuffer = handle.get();
						RenderSystem.backupProjectionMatrix();
						RenderSystem.setProjectionMatrix(slice, ProjectionType.ORTHOGRAPHIC);
						CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
						SamplerCache samplerCache = RenderSystem.getSamplerCache();
						List<PostEffectPass.Target> list = this.samplers
								.stream()
								.map(
										samplerxx -> new PostEffectPass.Target(
												samplerxx.samplerName(),
												samplerxx.getTexture(handles),
												samplerCache.get(
														samplerxx.bilinear() ? FilterMode.LINEAR : FilterMode.NEAREST)
										)
								)
								.toList();

						try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
								this.samplerInfoBuffer.getBlocking(),
								false,
								true
						)
						) {
							Std140Builder std140Builder = Std140Builder.intoBuffer(mappedView.data());
							std140Builder.putVec2(framebuffer.textureWidth, framebuffer.textureHeight);

							for (PostEffectPass.Target target : list) {
								std140Builder.putVec2(target.view.getWidth(0), target.view.getHeight(0));
							}
						}

						try (RenderPass renderPass = commandEncoder.createRenderPass(
								() -> "Post pass " + this.id,
								framebuffer.getColorAttachmentView(),
								OptionalInt.empty(),
								framebuffer.useDepthAttachment ? framebuffer.getDepthAttachmentView() : null,
								OptionalDouble.empty()
						)
						) {
							renderPass.setPipeline(this.pipeline);
							RenderSystem.bindDefaultUniforms(renderPass);
							renderPass.setUniform("SamplerInfo", this.samplerInfoBuffer.getBlocking());

							for (Entry<String, GpuBuffer> entry : this.uniformBuffers.entrySet()) {
								renderPass.setUniform(entry.getKey(), entry.getValue());
							}

							for (PostEffectPass.Target target2 : list) {
								renderPass.bindTexture(
										target2.samplerName() + "Sampler",
										target2.view(),
										target2.sampler()
								);
							}

							renderPass.draw(0, 3);
						}

						this.samplerInfoBuffer.rotate();
						RenderSystem.restoreProjectionMatrix();

						for (PostEffectPass.Sampler samplerx : this.samplers) {
							samplerx.postRender(handles);
						}
					}
			);
		}
	}

	@Override
	public void close() {
		for (GpuBuffer gpuBuffer : this.uniformBuffers.values()) {
			gpuBuffer.close();
		}

		this.samplerInfoBuffer.close();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Sampler}.
	 */
	public interface Sampler {

		void preRender(FramePass pass, Map<Identifier, Handle<Framebuffer>> internalTargets);

		default void postRender(Map<Identifier, Handle<Framebuffer>> internalTargets) {
		}

		GpuTextureView getTexture(Map<Identifier, Handle<Framebuffer>> internalTargets);

		String samplerName();

		boolean bilinear();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Target}.
	 */
	record Target(String samplerName, GpuTextureView view, GpuSampler sampler) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TargetSampler}.
	 */
	public record TargetSampler(
			String samplerName,
			Identifier targetId,
			boolean depthBuffer,
			boolean bilinear
	) implements PostEffectPass.Sampler {

		private Handle<Framebuffer> getTarget(Map<Identifier, Handle<Framebuffer>> internalTargets) {
			Handle<Framebuffer> handle = internalTargets.get(this.targetId);
			if (handle == null) {
				throw new IllegalStateException("Missing handle for target " + this.targetId);
			}
			else {
				return handle;
			}
		}

		@Override
		public void preRender(FramePass pass, Map<Identifier, Handle<Framebuffer>> internalTargets) {
			pass.dependsOn(this.getTarget(internalTargets));
		}

		@Override
		public GpuTextureView getTexture(Map<Identifier, Handle<Framebuffer>> internalTargets) {
			Handle<Framebuffer> handle = this.getTarget(internalTargets);
			Framebuffer framebuffer = handle.get();
			GpuTextureView
					gpuTextureView =
					this.depthBuffer ? framebuffer.getDepthAttachmentView() : framebuffer.getColorAttachmentView();
			if (gpuTextureView == null) {
				throw new IllegalStateException(
						"Missing " + (this.depthBuffer ? "depth" : "color") + "texture for target " + this.targetId);
			}
			else {
				return gpuTextureView;
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TextureSampler}.
	 */
	public record TextureSampler(
			String samplerName,
			AbstractTexture texture,
			int width,
			int height,
			boolean bilinear
	) implements PostEffectPass.Sampler {

		@Override
		public void preRender(FramePass pass, Map<Identifier, Handle<Framebuffer>> internalTargets) {
		}

		@Override
		public GpuTextureView getTexture(Map<Identifier, Handle<Framebuffer>> internalTargets) {
			return this.texture.getGlTextureView();
		}
	}
}
