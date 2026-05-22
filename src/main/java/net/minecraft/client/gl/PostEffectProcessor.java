package net.minecraft.client.gl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Процессор пост-обработки: управляет набором {@link PostEffectPass}, внутренними
 * фреймбуферами и проекционной матрицей. Создаётся из {@link PostEffectPipeline} через
 * {@link #parseEffect}.
 */
@Environment(EnvType.CLIENT)
public class PostEffectProcessor implements AutoCloseable {

	public static final Identifier MAIN = Identifier.ofVanilla("main");

	private final List<PostEffectPass> passes;
	private final Map<Identifier, PostEffectPipeline.Targets> internalTargets;
	private final Set<Identifier> externalTargets;
	private final Map<Identifier, Framebuffer> framebuffers = new HashMap<>();
	private final ProjectionMatrix2 projectionMatrix;

	private PostEffectProcessor(
		List<PostEffectPass> passes,
		Map<Identifier, PostEffectPipeline.Targets> internalTargets,
		Set<Identifier> externalTargets,
		ProjectionMatrix2 projectionMatrix
	) {
		this.passes = passes;
		this.internalTargets = internalTargets;
		this.externalTargets = externalTargets;
		this.projectionMatrix = projectionMatrix;
	}

	/**
	 * Разбирает {@link PostEffectPipeline} и создаёт процессор.
	 * Проверяет, что все внешние цели доступны в текущем контексте рендеринга.
	 */
	public static PostEffectProcessor parseEffect(
		PostEffectPipeline pipeline,
		TextureManager textureManager,
		Set<Identifier> availableExternalTargets,
		Identifier id,
		ProjectionMatrix2 projectionMatrix
	) throws ShaderLoader.LoadException {
		Stream<Identifier> allTargets = pipeline.passes().stream().flatMap(PostEffectPipeline.Pass::streamTargets);
		Set<Identifier> referencedExternal = allTargets
			.filter(target -> !pipeline.internalTargets().containsKey(target))
			.collect(Collectors.toSet());
		Set<Identifier> missingTargets = Sets.difference(referencedExternal, availableExternalTargets);

		if (!missingTargets.isEmpty()) {
			throw new ShaderLoader.LoadException(
				"Referenced external targets are not available in this context: " + missingTargets
			);
		}

		ImmutableList.Builder<PostEffectPass> builder = ImmutableList.builder();

		for (int index = 0; index < pipeline.passes().size(); index++) {
			PostEffectPipeline.Pass pass = pipeline.passes().get(index);
			builder.add(parsePass(textureManager, pass, id.withSuffixedPath("/" + index)));
		}

		return new PostEffectProcessor(builder.build(), pipeline.internalTargets(), referencedExternal, projectionMatrix);
	}

	private static PostEffectPass parsePass(
		TextureManager textureManager,
		PostEffectPipeline.Pass pass,
		Identifier id
	) throws ShaderLoader.LoadException {
		RenderPipeline.Builder pipelineBuilder = RenderPipeline
			.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
			.withFragmentShader(pass.fragmentShaderId())
			.withVertexShader(pass.vertexShaderId())
			.withLocation(id);

		for (PostEffectPipeline.Input input : pass.inputs()) {
			pipelineBuilder.withSampler(input.samplerName() + "Sampler");
		}

		pipelineBuilder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);

		for (String uniformName : pass.uniforms().keySet()) {
			pipelineBuilder.withUniform(uniformName, UniformType.UNIFORM_BUFFER);
		}

		RenderPipeline renderPipeline = pipelineBuilder.build();
		List<PostEffectPass.Sampler> samplers = new ArrayList<>();

		for (PostEffectPipeline.Input input : pass.inputs()) {
			switch (input) {
				case PostEffectPipeline.TextureSampler(
					String samplerName, Identifier location, int width, int height, boolean bilinear
				) -> {
					AbstractTexture texture = textureManager.getTexture(
						location.withPath(name -> "textures/effect/" + name + ".png")
					);
					samplers.add(new PostEffectPass.TextureSampler(samplerName, texture, width, height, bilinear));
				}
				case PostEffectPipeline.TargetSampler(
					String samplerName, Identifier targetId, boolean depthBuffer, boolean bilinear
				) -> samplers.add(new PostEffectPass.TargetSampler(samplerName, targetId, depthBuffer, bilinear));
				default -> throw new MatchException(null, null);
			}
		}

		return new PostEffectPass(renderPipeline, pass.outputTarget(), pass.uniforms(), samplers);
	}

	public void render(
		FrameGraphBuilder builder,
		int textureWidth,
		int textureHeight,
		PostEffectProcessor.FramebufferSet framebufferSet
	) {
		GpuBufferSlice projectionSlice = projectionMatrix.set(textureWidth, textureHeight);
		Map<Identifier, Handle<Framebuffer>> handles = new HashMap<>(
			internalTargets.size() + externalTargets.size()
		);

		for (Identifier externalId : externalTargets) {
			handles.put(externalId, framebufferSet.getOrThrow(externalId));
		}

		for (Entry<Identifier, PostEffectPipeline.Targets> entry : internalTargets.entrySet()) {
			Identifier targetId = entry.getKey();
			PostEffectPipeline.Targets targets = entry.getValue();
			SimpleFramebufferFactory factory = new SimpleFramebufferFactory(
				targets.width().orElse(textureWidth),
				targets.height().orElse(textureHeight),
				true,
				targets.clearColor()
			);

			if (targets.persistent()) {
				Framebuffer framebuffer = createFramebuffer(targetId, factory);
				handles.put(targetId, builder.createObjectNode(targetId.toString(), framebuffer));
			} else {
				handles.put(targetId, builder.createResourceHandle(targetId.toString(), factory));
			}
		}

		for (PostEffectPass pass : passes) {
			pass.render(builder, handles, projectionSlice);
		}

		for (Identifier externalId : externalTargets) {
			framebufferSet.set(externalId, handles.get(externalId));
		}
	}

	/**
	 * @deprecated Используй {@link #render(FrameGraphBuilder, int, int, FramebufferSet)} напрямую.
	 */
	@Deprecated
	public void render(Framebuffer framebuffer, ObjectAllocator objectAllocator) {
		FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
		PostEffectProcessor.FramebufferSet framebufferSet = PostEffectProcessor.FramebufferSet.singleton(
			MAIN, frameGraphBuilder.createObjectNode("main", framebuffer)
		);
		render(frameGraphBuilder, framebuffer.textureWidth, framebuffer.textureHeight, framebufferSet);
		frameGraphBuilder.run(objectAllocator);
	}

	private Framebuffer createFramebuffer(Identifier id, SimpleFramebufferFactory factory) {
		Framebuffer existing = framebuffers.get(id);

		if (existing != null
			&& existing.textureWidth == factory.width()
			&& existing.textureHeight == factory.height()
		) {
			return existing;
		}

		if (existing != null) {
			existing.delete();
		}

		Framebuffer created = factory.create();
		factory.prepare(created);
		framebuffers.put(id, created);

		return created;
	}

	@Override
	public void close() {
		framebuffers.values().forEach(Framebuffer::delete);
		framebuffers.clear();

		for (PostEffectPass pass : passes) {
			pass.close();
		}
	}

	/**
	 * Набор фреймбуферов, доступных для пост-эффекта по идентификатору.
	 * Позволяет передавать внешние цели (например, главный экран) в процессор.
	 */
	@Environment(EnvType.CLIENT)
	public interface FramebufferSet {

		static PostEffectProcessor.FramebufferSet singleton(
			Identifier targetId,
			Handle<Framebuffer> initialFramebuffer
		) {
			return new PostEffectProcessor.FramebufferSet() {
				private Handle<Framebuffer> framebuffer = initialFramebuffer;

				@Override
				public void set(Identifier id, Handle<Framebuffer> newFramebuffer) {
					if (id.equals(targetId)) {
						framebuffer = newFramebuffer;
					} else {
						throw new IllegalArgumentException("No target with id " + id);
					}
				}

				@Override
				public @Nullable Handle<Framebuffer> get(Identifier id) {
					return id.equals(targetId) ? framebuffer : null;
				}
			};
		}

		void set(Identifier id, Handle<Framebuffer> framebuffer);

		@Nullable Handle<Framebuffer> get(Identifier id);

		default Handle<Framebuffer> getOrThrow(Identifier id) {
			Handle<Framebuffer> handle = get(id);

			if (handle == null) {
				throw new IllegalArgumentException("Missing target with id " + id);
			}

			return handle;
		}
	}
}
