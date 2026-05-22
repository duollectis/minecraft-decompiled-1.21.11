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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Один проход пост-обработки: привязывает входные сэмплеры, записывает uniform-данные
 * в кольцевой буфер и рисует полноэкранный треугольник в целевой фреймбуфер.
 */
@Environment(EnvType.CLIENT)
public class PostEffectPass implements AutoCloseable {

	// Размер одного vec2 в std140-раскладке (используется для SamplerInfo UBO)
	private static final int VEC2_STD140_SIZE = new Std140SizeCalculator().putVec2().get();

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
		id = pipeline.getLocation().toString();
		this.outputTargetId = outputTargetId;
		this.samplers = samplers;

		for (Entry<String, List<UniformValue>> entry : uniforms.entrySet()) {
			List<UniformValue> values = entry.getValue();

			if (values.isEmpty()) {
				continue;
			}

			Std140SizeCalculator sizeCalculator = new Std140SizeCalculator();

			for (UniformValue value : values) {
				value.addSize(sizeCalculator);
			}

			int bufferSize = sizeCalculator.get();

			try (MemoryStack stack = MemoryStack.stackPush()) {
				Std140Builder builder = Std140Builder.onStack(stack, bufferSize);

				for (UniformValue value : values) {
					value.write(builder);
				}

				uniformBuffers.put(
					entry.getKey(),
					RenderSystem.getDevice().createBuffer(() -> id + " / " + entry.getKey(), 128, builder.get())
				);
			}
		}

		samplerInfoBuffer = new MappableRingBuffer(
			() -> id + " SamplerInfo",
			130,
			(samplers.size() + 1) * VEC2_STD140_SIZE
		);
	}

	/**
	 * Регистрирует проход в {@link FrameGraphBuilder} и задаёт рендерер, который:
	 * обновляет SamplerInfo UBO размерами текстур, выполняет draw-call полноэкранного треугольника.
	 */
	public void render(FrameGraphBuilder builder, Map<Identifier, Handle<Framebuffer>> handles, GpuBufferSlice slice) {
		FramePass framePass = builder.createPass(id);

		for (PostEffectPass.Sampler sampler : samplers) {
			sampler.preRender(framePass, handles);
		}

		Handle<Framebuffer> outputHandle = handles.computeIfPresent(
			outputTargetId,
			(targetId, handle) -> framePass.transfer(handle)
		);

		if (outputHandle == null) {
			throw new IllegalStateException("Missing handle for target " + outputTargetId);
		}

		framePass.setRenderer(() -> {
			Framebuffer framebuffer = outputHandle.get();
			RenderSystem.backupProjectionMatrix();
			RenderSystem.setProjectionMatrix(slice, ProjectionType.ORTHOGRAPHIC);

			CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
			SamplerCache samplerCache = RenderSystem.getSamplerCache();

			List<PostEffectPass.Target> targets = samplers
				.stream()
				.map(sampler -> new PostEffectPass.Target(
					sampler.samplerName(),
					sampler.getTexture(handles),
					samplerCache.get(sampler.bilinear() ? FilterMode.LINEAR : FilterMode.NEAREST)
				))
				.toList();

			try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
				samplerInfoBuffer.getBlocking(),
				false,
				true
			)) {
				Std140Builder std140Builder = Std140Builder.intoBuffer(mappedView.data());
				std140Builder.putVec2(framebuffer.textureWidth, framebuffer.textureHeight);

				for (PostEffectPass.Target target : targets) {
					std140Builder.putVec2(target.view().getWidth(0), target.view().getHeight(0));
				}
			}

			try (RenderPass renderPass = commandEncoder.createRenderPass(
				() -> "Post pass " + id,
				framebuffer.getColorAttachmentView(),
				OptionalInt.empty(),
				framebuffer.useDepthAttachment ? framebuffer.getDepthAttachmentView() : null,
				OptionalDouble.empty()
			)) {
				renderPass.setPipeline(pipeline);
				RenderSystem.bindDefaultUniforms(renderPass);
				renderPass.setUniform("SamplerInfo", samplerInfoBuffer.getBlocking());

				for (Entry<String, GpuBuffer> entry : uniformBuffers.entrySet()) {
					renderPass.setUniform(entry.getKey(), entry.getValue());
				}

				for (PostEffectPass.Target target : targets) {
					renderPass.bindTexture(target.samplerName() + "Sampler", target.view(), target.sampler());
				}

				renderPass.draw(0, 3);
			}

			samplerInfoBuffer.rotate();
			RenderSystem.restoreProjectionMatrix();

			for (PostEffectPass.Sampler sampler : samplers) {
				sampler.postRender(handles);
			}
		});
	}

	@Override
	public void close() {
		for (GpuBuffer buffer : uniformBuffers.values()) {
			buffer.close();
		}

		samplerInfoBuffer.close();
	}

	@Environment(EnvType.CLIENT)
	public interface Sampler {

		void preRender(FramePass pass, Map<Identifier, Handle<Framebuffer>> internalTargets);

		default void postRender(Map<Identifier, Handle<Framebuffer>> internalTargets) {
		}

		GpuTextureView getTexture(Map<Identifier, Handle<Framebuffer>> internalTargets);

		String samplerName();

		boolean bilinear();
	}

	@Environment(EnvType.CLIENT)
	record Target(String samplerName, GpuTextureView view, GpuSampler sampler) {
	}

	/**
	 * Сэмплер, читающий цвет или глубину из внутреннего целевого фреймбуфера пост-эффекта.
	 */
	@Environment(EnvType.CLIENT)
	public record TargetSampler(
		String samplerName,
		Identifier targetId,
		boolean depthBuffer,
		boolean bilinear
	) implements PostEffectPass.Sampler {

		private Handle<Framebuffer> getTarget(Map<Identifier, Handle<Framebuffer>> internalTargets) {
			Handle<Framebuffer> handle = internalTargets.get(targetId);

			if (handle == null) {
				throw new IllegalStateException("Missing handle for target " + targetId);
			}

			return handle;
		}

		@Override
		public void preRender(FramePass pass, Map<Identifier, Handle<Framebuffer>> internalTargets) {
			pass.dependsOn(getTarget(internalTargets));
		}

		@Override
		public GpuTextureView getTexture(Map<Identifier, Handle<Framebuffer>> internalTargets) {
			Framebuffer framebuffer = getTarget(internalTargets).get();
			GpuTextureView view = depthBuffer
				? framebuffer.getDepthAttachmentView()
				: framebuffer.getColorAttachmentView();

			if (view == null) {
				throw new IllegalStateException(
					"Missing " + (depthBuffer ? "depth" : "color") + "texture for target " + targetId
				);
			}

			return view;
		}
	}

	/**
	 * Сэмплер, читающий данные из статической текстуры (не из фреймбуфера).
	 */
	@Environment(EnvType.CLIENT)
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
			return texture.getGlTextureView();
		}
	}
}
