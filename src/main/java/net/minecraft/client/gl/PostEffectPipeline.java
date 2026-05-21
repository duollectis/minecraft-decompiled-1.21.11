package net.minecraft.client.gl;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code PostEffectPipeline}.
 */
public record PostEffectPipeline(
		Map<Identifier, PostEffectPipeline.Targets> internalTargets,
		List<PostEffectPipeline.Pass> passes
) {

	public static final Codec<PostEffectPipeline> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codec.unboundedMap(Identifier.CODEC, PostEffectPipeline.Targets.CODEC)
					                         .optionalFieldOf("targets", Map.of())
					                         .forGetter(PostEffectPipeline::internalTargets),
					                    PostEffectPipeline.Pass.CODEC
							                    .listOf()
							                    .optionalFieldOf("passes", List.of())
							                    .forGetter(PostEffectPipeline::passes)
			                    )
			                    .apply(instance, PostEffectPipeline::new)
	);

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Input}.
	 */
	public sealed interface Input permits PostEffectPipeline.TextureSampler, PostEffectPipeline.TargetSampler {

		Codec<PostEffectPipeline.Input>
				CODEC =
				Codec.xor(PostEffectPipeline.TextureSampler.CODEC, PostEffectPipeline.TargetSampler.CODEC)
				     .xmap(
						     either -> (PostEffectPipeline.Input) either.map(Function.identity(), Function.identity()),
						     sampler -> {
							     return switch (sampler) {
								     case PostEffectPipeline.TextureSampler textureSampler ->
										     Either.left(textureSampler);
								     case PostEffectPipeline.TargetSampler targetSampler -> Either.right(targetSampler);
								     default -> throw new MatchException(null, null);
							     };
						     }
				     );

		String samplerName();

		Set<Identifier> getTargetId();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Pass}.
	 */
	public record Pass(
			Identifier vertexShaderId,
			Identifier fragmentShaderId,
			List<PostEffectPipeline.Input> inputs,
			Identifier outputTarget,
			Map<String, List<UniformValue>> uniforms
	) {

		private static final Codec<List<PostEffectPipeline.Input>>
				INPUTS_CODEC =
				PostEffectPipeline.Input.CODEC.listOf().validate(inputs -> {
					Set<String> set = new ObjectArraySet(inputs.size());

					for (PostEffectPipeline.Input input : inputs) {
						if (!set.add(input.samplerName())) {
							return DataResult.error(() -> "Encountered repeated sampler name: " + input.samplerName());
						}
					}

					return DataResult.success(inputs);
				});
		private static final Codec<Map<String, List<UniformValue>>>
				UNIFORMS_CODEC =
				Codec.unboundedMap(Codec.STRING, UniformValue.CODEC.listOf());
		public static final Codec<PostEffectPipeline.Pass> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Identifier.CODEC.fieldOf("vertex_shader").forGetter(PostEffectPipeline.Pass::vertexShaderId),
						                    Identifier.CODEC
								                    .fieldOf("fragment_shader")
								                    .forGetter(PostEffectPipeline.Pass::fragmentShaderId),
						                    INPUTS_CODEC.optionalFieldOf("inputs", List.of()).forGetter(PostEffectPipeline.Pass::inputs),
						                    Identifier.CODEC.fieldOf("output").forGetter(PostEffectPipeline.Pass::outputTarget),
						                    UNIFORMS_CODEC
								                    .optionalFieldOf("uniforms", Map.of())
								                    .forGetter(PostEffectPipeline.Pass::uniforms)
				                    )
				                    .apply(instance, PostEffectPipeline.Pass::new)
		);

		public Stream<Identifier> streamTargets() {
			Stream<Identifier> stream = this.inputs.stream().flatMap(input -> input.getTargetId().stream());
			return Stream.concat(stream, Stream.of(this.outputTarget));
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TargetSampler}.
	 */
	public record TargetSampler(
			String samplerName,
			Identifier targetId,
			boolean useDepthBuffer,
			boolean bilinear
	) implements PostEffectPipeline.Input {

		public static final Codec<PostEffectPipeline.TargetSampler> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("sampler_name").forGetter(PostEffectPipeline.TargetSampler::samplerName),
						                    Identifier.CODEC.fieldOf("target").forGetter(PostEffectPipeline.TargetSampler::targetId),
						                    Codec.BOOL
								                    .optionalFieldOf("use_depth_buffer", false)
								                    .forGetter(PostEffectPipeline.TargetSampler::useDepthBuffer),
						                    Codec.BOOL
								                    .optionalFieldOf("bilinear", false)
								                    .forGetter(PostEffectPipeline.TargetSampler::bilinear)
				                    )
				                    .apply(instance, PostEffectPipeline.TargetSampler::new)
		);

		@Override
		public Set<Identifier> getTargetId() {
			return Set.of(this.targetId);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Targets}.
	 */
	public record Targets(Optional<Integer> width, Optional<Integer> height, boolean persistent, int clearColor) {

		public static final Codec<PostEffectPipeline.Targets> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codecs.POSITIVE_INT.optionalFieldOf("width").forGetter(PostEffectPipeline.Targets::width),
						                    Codecs.POSITIVE_INT.optionalFieldOf("height").forGetter(PostEffectPipeline.Targets::height),
						                    Codec.BOOL
								                    .optionalFieldOf("persistent", false)
								                    .forGetter(PostEffectPipeline.Targets::persistent),
						                    Codecs.ARGB.optionalFieldOf("clear_color", 0).forGetter(PostEffectPipeline.Targets::clearColor)
				                    )
				                    .apply(instance, PostEffectPipeline.Targets::new)
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TextureSampler}.
	 */
	public record TextureSampler(
			String samplerName,
			Identifier location,
			int width,
			int height,
			boolean bilinear
	) implements PostEffectPipeline.Input {

		public static final Codec<PostEffectPipeline.TextureSampler> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("sampler_name").forGetter(PostEffectPipeline.TextureSampler::samplerName),
						                    Identifier.CODEC.fieldOf("location").forGetter(PostEffectPipeline.TextureSampler::location),
						                    Codecs.POSITIVE_INT.fieldOf("width").forGetter(PostEffectPipeline.TextureSampler::width),
						                    Codecs.POSITIVE_INT.fieldOf("height").forGetter(PostEffectPipeline.TextureSampler::height),
						                    Codec.BOOL
								                    .optionalFieldOf("bilinear", false)
								                    .forGetter(PostEffectPipeline.TextureSampler::bilinear)
				                    )
				                    .apply(instance, PostEffectPipeline.TextureSampler::new)
		);

		@Override
		public Set<Identifier> getTargetId() {
			return Set.of();
		}
	}
}
