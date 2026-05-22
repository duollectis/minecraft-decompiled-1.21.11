package net.minecraft.client.gl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Скомпилированная и слинкованная шейдерная программа OpenGL.
 * Хранит маппинг имён uniform-переменных на их GL-привязки ({@link GlUniform}).
 */
@Environment(EnvType.CLIENT)
public class ShaderProgram implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	// GL_LINK_STATUS = 35714, GL_ACTIVE_UNIFORM_BLOCKS = 35382
	private static final int GL_LINK_STATUS = 35714;
	private static final int GL_ACTIVE_UNIFORM_BLOCKS = 35382;

	public static Set<String> predefinedUniforms = Sets.newHashSet("Projection", "Lighting", "Fog", "Globals");
	public static ShaderProgram INVALID = new ShaderProgram(-1, "invalid");

	private final Map<String, GlUniform> uniformsByName = new HashMap<>();
	private final int glRef;
	private final String debugLabel;

	private ShaderProgram(int glRef, String debugLabel) {
		this.glRef = glRef;
		this.debugLabel = debugLabel;
	}

	/**
	 * Создаёт и линкует шейдерную программу из вершинного и фрагментного шейдеров.
	 * Привязывает атрибуты вершинного формата по индексу до линковки.
	 */
	public static ShaderProgram create(
		CompiledShader vertexShader,
		CompiledShader fragmentShader,
		VertexFormat format,
		String name
	) throws ShaderLoader.LoadException {
		int programId = GlStateManager.glCreateProgram();

		if (programId <= 0) {
			throw new ShaderLoader.LoadException(
				"Could not create shader program (returned program ID " + programId + ")"
			);
		}

		int attributeIndex = 0;

		for (String attributeName : format.getElementAttributeNames()) {
			GlStateManager._glBindAttribLocation(programId, attributeIndex, attributeName);
			attributeIndex++;
		}

		GlStateManager.glAttachShader(programId, vertexShader.getHandle());
		GlStateManager.glAttachShader(programId, fragmentShader.getHandle());
		GlStateManager.glLinkProgram(programId);

		int linkStatus = GlStateManager.glGetProgrami(programId, GL_LINK_STATUS);
		String infoLog = GlStateManager.glGetProgramInfoLog(programId, 32768);

		if (linkStatus == 0 || infoLog.contains("Failed for unknown reason")) {
			throw new ShaderLoader.LoadException(
				"Error encountered when linking program containing VS " + vertexShader.getId()
					+ " and FS " + fragmentShader.getId() + ". Log output: " + infoLog
			);
		}

		if (!infoLog.isEmpty()) {
			LOGGER.info(
				"Info log when linking program containing VS {} and FS {}. Log output: {}",
				vertexShader.getId(), fragmentShader.getId(), infoLog
			);
		}

		return new ShaderProgram(programId, name);
	}

	/**
	 * Разрешает все uniform-привязки программы: UBO-блоки, texel-буферы и сэмплеры.
	 * Также обрабатывает предопределённые UBO (Projection, Lighting, Fog, Globals).
	 */
	public void set(List<RenderPipeline.UniformDescription> uniforms, List<String> samplers) {
		int uboBindingIndex = 0;
		int samplerIndex = 0;

		for (RenderPipeline.UniformDescription uniformDesc : uniforms) {
			String name = uniformDesc.name();

			GlUniform uniform = switch (uniformDesc.type()) {
				case UNIFORM_BUFFER -> {
					int blockIndex = GL31.glGetUniformBlockIndex(glRef, name);

					if (blockIndex == -1) {
						yield null;
					}

					int binding = uboBindingIndex++;
					GL31.glUniformBlockBinding(glRef, blockIndex, binding);
					yield new GlUniform.UniformBuffer(binding);
				}
				case TEXEL_BUFFER -> {
					int location = GlStateManager._glGetUniformLocation(glRef, name);

					if (location == -1) {
						LOGGER.warn(
							"{} shader program does not use utb {} defined in the pipeline. This might be a bug.",
							debugLabel, name
						);
						yield null;
					}

					int texelSamplerIndex = samplerIndex++;
					yield new GlUniform.TexelBuffer(
						location,
						texelSamplerIndex,
						Objects.requireNonNull(uniformDesc.textureFormat())
					);
				}
			};

			if (uniform != null) {
				uniformsByName.put(name, uniform);
			}
		}

		for (String samplerName : samplers) {
			int location = GlStateManager._glGetUniformLocation(glRef, samplerName);

			if (location == -1) {
				LOGGER.warn(
					"{} shader program does not use sampler {} defined in the pipeline. This might be a bug.",
					debugLabel, samplerName
				);
			} else {
				uniformsByName.put(samplerName, new GlUniform.Sampler(location, samplerIndex++));
			}
		}

		int activeBlockCount = GlStateManager.glGetProgrami(glRef, GL_ACTIVE_UNIFORM_BLOCKS);

		for (int blockIndex = 0; blockIndex < activeBlockCount; blockIndex++) {
			String blockName = GL31.glGetActiveUniformBlockName(glRef, blockIndex);

			if (uniformsByName.containsKey(blockName)) {
				continue;
			}

			if (!samplers.contains(blockName) && predefinedUniforms.contains(blockName)) {
				int binding = uboBindingIndex++;
				GL31.glUniformBlockBinding(glRef, blockIndex, binding);
				uniformsByName.put(blockName, new GlUniform.UniformBuffer(binding));
			} else {
				LOGGER.warn("Found unknown and unsupported uniform {} in {}", blockName, debugLabel);
			}
		}
	}

	@Override
	public void close() {
		uniformsByName.values().forEach(GlUniform::close);
		GlStateManager.glDeleteProgram(glRef);
	}

	public @Nullable GlUniform getUniform(String name) {
		RenderSystem.assertOnRenderThread();
		return uniformsByName.get(name);
	}

	@VisibleForTesting
	public int getGlRef() {
		return glRef;
	}

	@Override
	public String toString() {
		return debugLabel;
	}

	public String getDebugLabel() {
		return debugLabel;
	}

	public Map<String, GlUniform> getUniforms() {
		return uniformsByName;
	}
}
