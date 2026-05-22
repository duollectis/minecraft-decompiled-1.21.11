package net.minecraft.client.gl;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.path.PathUtil;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Загрузчик шейдеров и пайплайнов пост-обработки. Реализует {@link SinglePreparationResourceReloader}:
 * на этапе {@code prepare} читает исходники шейдеров и JSON пост-эффектов,
 * на этапе {@code apply} компилирует все зарегистрированные {@link RenderPipeline}.
 */
@Environment(EnvType.CLIENT)
public class ShaderLoader extends SinglePreparationResourceReloader<ShaderLoader.Definitions> implements AutoCloseable {

	static final Logger LOGGER = LogUtils.getLogger();
	public static final int MAX_SHADER_SOURCE_LENGTH = 32768;
	public static final String SHADERS_PATH = "shaders";
	private static final String INCLUDE_PATH = "shaders/include/";
	private static final ResourceFinder POST_EFFECT_FINDER = ResourceFinder.json("post_effect");

	final TextureManager textureManager;
	private final Consumer<Exception> onError;
	private ShaderLoader.Cache cache = new ShaderLoader.Cache(ShaderLoader.Definitions.EMPTY);
	final ProjectionMatrix2 projectionMatrix = new ProjectionMatrix2("post", 0.1F, 1000.0F, false);

	public ShaderLoader(TextureManager textureManager, Consumer<Exception> onError) {
		this.textureManager = textureManager;
		this.onError = onError;
	}

	@Override
	protected ShaderLoader.Definitions prepare(ResourceManager resourceManager, Profiler profiler) {
		ImmutableMap.Builder<ShaderLoader.ShaderSourceKey, String> sourcesBuilder = ImmutableMap.builder();
		Map<Identifier, Resource> allShaderResources = resourceManager.findResources("shaders", ShaderLoader::isShaderSource);

		for (Entry<Identifier, Resource> entry : allShaderResources.entrySet()) {
			Identifier resourceId = entry.getKey();
			ShaderType shaderType = ShaderType.byLocation(resourceId);

			if (shaderType != null) {
				loadShaderSource(resourceId, entry.getValue(), shaderType, allShaderResources, sourcesBuilder);
			}
		}

		ImmutableMap.Builder<Identifier, PostEffectPipeline> postChainsBuilder = ImmutableMap.builder();

		for (Entry<Identifier, Resource> entry : POST_EFFECT_FINDER.findResources(resourceManager).entrySet()) {
			loadPostEffect(entry.getKey(), entry.getValue(), postChainsBuilder);
		}

		return new ShaderLoader.Definitions(sourcesBuilder.build(), postChainsBuilder.build());
	}

	private static void loadShaderSource(
		Identifier id,
		Resource resource,
		ShaderType type,
		Map<Identifier, Resource> allResources,
		ImmutableMap.Builder<ShaderLoader.ShaderSourceKey, String> builder
	) {
		Identifier shaderId = type.idConverter().toResourceId(id);
		GlImportProcessor importProcessor = createImportProcessor(allResources, id);

		try (Reader reader = resource.getReader()) {
			String source = IOUtils.toString(reader);
			builder.put(
				new ShaderLoader.ShaderSourceKey(shaderId, type),
				String.join("", importProcessor.readSource(source))
			);
		} catch (IOException exception) {
			LOGGER.error("Failed to load shader source at {}", id, exception);
		}
	}

	private static GlImportProcessor createImportProcessor(Map<Identifier, Resource> allResources, Identifier id) {
		Identifier baseDir = id.withPath(PathUtil::getPosixFullPath);

		return new GlImportProcessor() {
			private final Set<Identifier> processed = new ObjectArraySet<>();

			@Override
			public @Nullable String loadImport(boolean inline, String name) {
				Identifier importId;

				try {
					importId = inline
						? baseDir.withPath(path -> PathUtil.normalizeToPosix(path + name))
						: Identifier.of(name).withPrefixedPath("shaders/include/");
				} catch (InvalidIdentifierException exception) {
					LOGGER.error("Malformed GLSL import {}: {}", name, exception.getMessage());
					return "#error " + exception.getMessage();
				}

				if (!processed.add(importId)) {
					return null;
				}

				try (Reader reader = allResources.get(importId).getReader()) {
					return IOUtils.toString(reader);
				} catch (IOException exception) {
					LOGGER.error("Could not open GLSL import {}: {}", importId, exception.getMessage());
					return "#error " + exception.getMessage();
				}
			}
		};
	}

	private static void loadPostEffect(
		Identifier id,
		Resource resource,
		ImmutableMap.Builder<Identifier, PostEffectPipeline> builder
	) {
		Identifier pipelineId = POST_EFFECT_FINDER.toResourceId(id);

		try (Reader reader = resource.getReader()) {
			JsonElement json = StrictJsonParser.parse(reader);
			builder.put(
				pipelineId,
				(PostEffectPipeline) PostEffectPipeline.CODEC
					.parse(JsonOps.INSTANCE, json)
					.getOrThrow(JsonSyntaxException::new)
			);
		} catch (JsonParseException | IOException exception) {
			LOGGER.error("Failed to parse post chain at {}", id, exception);
		}
	}

	private static boolean isShaderSource(Identifier id) {
		return ShaderType.byLocation(id) != null || id.getPath().endsWith(".glsl");
	}

	/**
	 * Компилирует все зарегистрированные пайплайны. При ошибке компиляции хотя бы одного —
	 * очищает кэш и бросает {@link RuntimeException} со списком проблемных шейдеров.
	 */
	@Override
	protected void apply(ShaderLoader.Definitions definitions, ResourceManager resourceManager, Profiler profiler) {
		ShaderLoader.Cache newCache = new ShaderLoader.Cache(definitions);
		Set<RenderPipeline> pipelines = new HashSet<>(RenderPipelines.getAll());
		List<Identifier> failedPipelines = new ArrayList<>();
		GpuDevice gpuDevice = RenderSystem.getDevice();
		gpuDevice.clearPipelineCache();

		for (RenderPipeline pipeline : pipelines) {
			CompiledRenderPipeline compiled = gpuDevice.precompilePipeline(pipeline, newCache::getSource);

			if (!compiled.isValid()) {
				failedPipelines.add(pipeline.getLocation());
			}
		}

		if (!failedPipelines.isEmpty()) {
			gpuDevice.clearPipelineCache();
			throw new RuntimeException(
				"Failed to load required shader programs:\n" + failedPipelines
					.stream()
					.map(pipelineId -> " - " + pipelineId)
					.collect(Collectors.joining("\n"))
			);
		}

		cache.close();
		cache = newCache;
	}

	@Override
	public String getName() {
		return "Shader Loader";
	}

	private void handleError(Exception exception) {
		if (cache.errorHandled) {
			return;
		}

		onError.accept(exception);
		cache.errorHandled = true;
	}

	public @Nullable PostEffectProcessor loadPostEffect(Identifier id, Set<Identifier> availableExternalTargets) {
		try {
			return cache.getOrLoadProcessor(id, availableExternalTargets);
		} catch (ShaderLoader.LoadException exception) {
			LOGGER.error("Failed to load post chain: {}", id, exception);
			cache.postEffectProcessors.put(id, Optional.empty());
			handleError(exception);
			return null;
		}
	}

	@Override
	public void close() {
		cache.close();
		projectionMatrix.close();
	}

	public @Nullable String getSource(Identifier id, ShaderType type) {
		return cache.getSource(id, type);
	}

	@Environment(EnvType.CLIENT)
	class Cache implements AutoCloseable {

		private final ShaderLoader.Definitions definitions;
		final Map<Identifier, Optional<PostEffectProcessor>> postEffectProcessors = new HashMap<>();
		boolean errorHandled;

		Cache(ShaderLoader.Definitions definitions) {
			this.definitions = definitions;
		}

		public @Nullable PostEffectProcessor getOrLoadProcessor(
			Identifier id,
			Set<Identifier> availableExternalTargets
		) throws ShaderLoader.LoadException {
			Optional<PostEffectProcessor> cached = postEffectProcessors.get(id);

			if (cached != null) {
				return cached.orElse(null);
			}

			PostEffectProcessor processor = loadProcessor(id, availableExternalTargets);
			postEffectProcessors.put(id, Optional.of(processor));

			return processor;
		}

		private PostEffectProcessor loadProcessor(
			Identifier id,
			Set<Identifier> availableExternalTargets
		) throws ShaderLoader.LoadException {
			PostEffectPipeline pipeline = definitions.postChains.get(id);

			if (pipeline == null) {
				throw new ShaderLoader.LoadException("Could not find post chain with id: " + id);
			}

			return PostEffectProcessor.parseEffect(
				pipeline,
				ShaderLoader.this.textureManager,
				availableExternalTargets,
				id,
				ShaderLoader.this.projectionMatrix
			);
		}

		@Override
		public void close() {
			postEffectProcessors.values().forEach(processor -> processor.ifPresent(PostEffectProcessor::close));
			postEffectProcessors.clear();
		}

		public @Nullable String getSource(Identifier id, ShaderType type) {
			return definitions.shaderSources.get(new ShaderLoader.ShaderSourceKey(id, type));
		}
	}

	@Environment(EnvType.CLIENT)
	public record Definitions(
		Map<ShaderLoader.ShaderSourceKey, String> shaderSources,
		Map<Identifier, PostEffectPipeline> postChains
	) {

		public static final ShaderLoader.Definitions EMPTY = new ShaderLoader.Definitions(Map.of(), Map.of());
	}

	@Environment(EnvType.CLIENT)
	public static class LoadException extends Exception {

		public LoadException(String message) {
			super(message);
		}
	}

	@Environment(EnvType.CLIENT)
	record ShaderSourceKey(Identifier id, ShaderType type) {

		@Override
		public String toString() {
			return id + " (" + type + ")";
		}
	}
}
