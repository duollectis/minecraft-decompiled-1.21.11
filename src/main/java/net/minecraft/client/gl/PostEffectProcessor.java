package net.minecraft.client.gl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
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

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code PostEffectProcessor}.
 */
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

	public static PostEffectProcessor parseEffect(
			PostEffectPipeline pipeline,
			TextureManager textureManager,
			Set<Identifier> availableExternalTargets,
			Identifier id,
			ProjectionMatrix2 projectionMatrix
	) throws ShaderLoader.LoadException {
		Stream<Identifier> stream = pipeline.passes().stream().flatMap(PostEffectPipeline.Pass::streamTargets);
		Set<Identifier>
				set =
				stream.filter(target -> !pipeline.internalTargets().containsKey(target)).collect(Collectors.toSet());
		Set<Identifier> set2 = Sets.difference(set, availableExternalTargets);
		if (!set2.isEmpty()) {
			throw new ShaderLoader.LoadException(
					"Referenced external targets are not available in this context: " + set2);
		}
		else {
			Builder<PostEffectPass> builder = ImmutableList.builder();

			for (int i = 0; i < pipeline.passes().size(); i++) {
				PostEffectPipeline.Pass pass = pipeline.passes().get(i);
				builder.add(parsePass(textureManager, pass, id.withSuffixedPath("/" + i)));
			}

			return new PostEffectProcessor(builder.build(), pipeline.internalTargets(), set, projectionMatrix);
		}
	}

	private static PostEffectPass parsePass(TextureManager textureManager, PostEffectPipeline.Pass pass, Identifier id)
	throws ShaderLoader.LoadException {
		RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
		                                               .withFragmentShader(pass.fragmentShaderId())
		                                               .withVertexShader(pass.vertexShaderId())
		                                               .withLocation(id);

		for (PostEffectPipeline.Input input : pass.inputs()) {
			builder.withSampler(input.samplerName() + "Sampler");
		}

		builder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);

		for (String string : pass.uniforms().keySet()) {
			builder.withUniform(string, UniformType.UNIFORM_BUFFER);
		}

		RenderPipeline renderPipeline = builder.build();
		List<PostEffectPass.Sampler> list = new ArrayList<>();

		for (PostEffectPipeline.Input input2 : pass.inputs()) {
			switch (input2) {
				case PostEffectPipeline.TextureSampler(String var35, Identifier var36, int var37, int var38, boolean var39):
					AbstractTexture
							abstractTexture =
							textureManager.getTexture(var36.withPath(name -> "textures/effect/" + name + ".png"));
					list.add(new PostEffectPass.TextureSampler(var35, abstractTexture, var37, var38, var39));
					break;
				case PostEffectPipeline.TargetSampler(String var21, Identifier var41, boolean var42, boolean var43):
					list.add(new PostEffectPass.TargetSampler(var21, var41, var42, var43));
					break;
				default:
					throw new MatchException(null, null);
			}
		}

		return new PostEffectPass(renderPipeline, pass.outputTarget(), pass.uniforms(), list);
	}

	public void render(
			FrameGraphBuilder builder,
			int textureWidth,
			int textureHeight,
			PostEffectProcessor.FramebufferSet framebufferSet
	) {
		GpuBufferSlice gpuBufferSlice = this.projectionMatrix.set(textureWidth, textureHeight);
		Map<Identifier, Handle<Framebuffer>>
				map =
				new HashMap<>(this.internalTargets.size() + this.externalTargets.size());

		for (Identifier identifier : this.externalTargets) {
			map.put(identifier, framebufferSet.getOrThrow(identifier));
		}

		for (Entry<Identifier, PostEffectPipeline.Targets> entry : this.internalTargets.entrySet()) {
			Identifier identifier2 = entry.getKey();
			PostEffectPipeline.Targets targets = entry.getValue();
			SimpleFramebufferFactory simpleFramebufferFactory = new SimpleFramebufferFactory(
					targets.width().orElse(textureWidth),
					targets.height().orElse(textureHeight),
					true,
					targets.clearColor()
			);
			if (targets.persistent()) {
				Framebuffer framebuffer = this.createFramebuffer(identifier2, simpleFramebufferFactory);
				map.put(identifier2, builder.createObjectNode(identifier2.toString(), framebuffer));
			}
			else {
				map.put(identifier2, builder.createResourceHandle(identifier2.toString(), simpleFramebufferFactory));
			}
		}

		for (PostEffectPass postEffectPass : this.passes) {
			postEffectPass.render(builder, map, gpuBufferSlice);
		}

		for (Identifier identifier : this.externalTargets) {
			framebufferSet.set(identifier, map.get(identifier));
		}
	}

	@Deprecated
	/**
	 * Render.
	 *
	 * @param framebuffer framebuffer
	 * @param objectAllocator object allocator
	 */
	public void render(Framebuffer framebuffer, ObjectAllocator objectAllocator) {
		FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
		PostEffectProcessor.FramebufferSet framebufferSet = PostEffectProcessor.FramebufferSet.singleton(
				MAIN, frameGraphBuilder.createObjectNode("main", framebuffer)
		);
		this.render(frameGraphBuilder, framebuffer.textureWidth, framebuffer.textureHeight, framebufferSet);
		frameGraphBuilder.run(objectAllocator);
	}

	private Framebuffer createFramebuffer(Identifier id, SimpleFramebufferFactory factory) {
		Framebuffer framebuffer = this.framebuffers.get(id);
		if (framebuffer == null || framebuffer.textureWidth != factory.width()
				|| framebuffer.textureHeight != factory.height()) {
			if (framebuffer != null) {
				framebuffer.delete();
			}

			framebuffer = factory.create();
			factory.prepare(framebuffer);
			this.framebuffers.put(id, framebuffer);
		}

		return framebuffer;
	}

	@Override
	public void close() {
		this.framebuffers.values().forEach(Framebuffer::delete);
		this.framebuffers.clear();

		for (PostEffectPass postEffectPass : this.passes) {
			postEffectPass.close();
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FramebufferSet}.
	 */
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
						this.framebuffer = newFramebuffer;
					}
					else {
						throw new IllegalArgumentException("No target with id " + id);
					}
				}

				@Override
				public @Nullable Handle<Framebuffer> get(Identifier id) {
					return id.equals(id) ? this.framebuffer : null;
				}
			};
		}

		void set(Identifier id, Handle<Framebuffer> framebuffer);

		@Nullable Handle<Framebuffer> get(Identifier id);

		default Handle<Framebuffer> getOrThrow(Identifier id) {
			Handle<Framebuffer> handle = this.get(id);
			if (handle == null) {
				throw new IllegalArgumentException("Missing target with id " + id);
			}
			else {
				return handle;
			}
		}
	}
}
