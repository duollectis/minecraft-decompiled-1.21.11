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

import java.util.*;

@Environment(EnvType.CLIENT)
/**
 * {@code ShaderProgram}.
 */
public class ShaderProgram implements AutoCloseable {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static Set<String>
			predefinedUniforms =
			Sets.newHashSet(new String[]{"Projection", "Lighting", "Fog", "Globals"});
	public static ShaderProgram INVALID = new ShaderProgram(-1, "invalid");
	private final Map<String, GlUniform> uniformsByName = new HashMap<>();
	private final int glRef;
	private final String debugLabel;

	private ShaderProgram(int glRef, String debugLabel) {
		this.glRef = glRef;
		this.debugLabel = debugLabel;
	}

	public static ShaderProgram create(
			CompiledShader vertexShader,
			CompiledShader fragmentShader,
			VertexFormat format,
			String name
	) throws ShaderLoader.LoadException {
		int i = GlStateManager.glCreateProgram();
		if (i <= 0) {
			throw new ShaderLoader.LoadException("Could not create shader program (returned program ID " + i + ")");
		}
		else {
			int j = 0;

			for (String string : format.getElementAttributeNames()) {
				GlStateManager._glBindAttribLocation(i, j, string);
				j++;
			}

			GlStateManager.glAttachShader(i, vertexShader.getHandle());
			GlStateManager.glAttachShader(i, fragmentShader.getHandle());
			GlStateManager.glLinkProgram(i);
			int k = GlStateManager.glGetProgrami(i, 35714);
			String string = GlStateManager.glGetProgramInfoLog(i, 32768);
			if (k != 0 && !string.contains("Failed for unknown reason")) {
				if (!string.isEmpty()) {
					LOGGER.info(
							"Info log when linking program containing VS {} and FS {}. Log output: {}",
							new Object[]{vertexShader.getId(), fragmentShader.getId(), string}
					);
				}

				return new ShaderProgram(i, name);
			}
			else {
				throw new ShaderLoader.LoadException(
						"Error encountered when linking program containing VS " + vertexShader.getId() + " and FS "
								+ fragmentShader.getId() + ". Log output: " + string
				);
			}
		}
	}

	public void set(List<RenderPipeline.UniformDescription> uniforms, List<String> samplers) {
		int i = 0;
		int j = 0;

		for (RenderPipeline.UniformDescription uniformDescription : uniforms) {
			String string = uniformDescription.name();

			Object var10000_1 = switch (uniformDescription.type()) {
				case UNIFORM_BUFFER -> {
					int k = GL31.glGetUniformBlockIndex(this.glRef, string);
					if (k == -1) {
						yield null;
					}
					else {
						int l = i++;
						GL31.glUniformBlockBinding(this.glRef, k, l);
						yield new GlUniform.UniformBuffer(l);
					}
				}
				case TEXEL_BUFFER -> {
					int k = GlStateManager._glGetUniformLocation(this.glRef, string);
					if (k == -1) {
						LOGGER.warn(
								"{} shader program does not use utb {} defined in the pipeline. This might be a bug.",
								this.debugLabel,
								string
						);
						yield null;
					}
					else {
						int l = j++;
						yield new GlUniform.TexelBuffer(
								k,
								l,
								Objects.requireNonNull(uniformDescription.textureFormat())
						);
					}
				}
			};

			GlUniform glUniform = (GlUniform) var10000_1;
			if (glUniform != null) {
				this.uniformsByName.put(string, glUniform);
			}
		}

		for (String string2 : samplers) {
			int m = GlStateManager._glGetUniformLocation(this.glRef, string2);
			if (m == -1) {
				LOGGER.warn(
						"{} shader program does not use sampler {} defined in the pipeline. This might be a bug.",
						this.debugLabel,
						string2
				);
			}
			else {
				int n = j++;
				this.uniformsByName.put(string2, new GlUniform.Sampler(m, n));
			}
		}

		int o = GlStateManager.glGetProgrami(this.glRef, 35382);

		for (int p = 0; p < o; p++) {
			String string = GL31.glGetActiveUniformBlockName(this.glRef, p);
			if (!this.uniformsByName.containsKey(string)) {
				if (!samplers.contains(string) && predefinedUniforms.contains(string)) {
					int n = i++;
					GL31.glUniformBlockBinding(this.glRef, p, n);
					this.uniformsByName.put(string, new GlUniform.UniformBuffer(n));
				}
				else {
					LOGGER.warn("Found unknown and unsupported uniform {} in {}", string, this.debugLabel);
				}
			}
		}
	}

	@Override
	public void close() {
		this.uniformsByName.values().forEach(GlUniform::close);
		GlStateManager.glDeleteProgram(this.glRef);
	}

	public @Nullable GlUniform getUniform(String name) {
		RenderSystem.assertOnRenderThread();
		return this.uniformsByName.get(name);
	}

	@VisibleForTesting
	public int getGlRef() {
		return this.glRef;
	}

	@Override
	public String toString() {
		return this.debugLabel;
	}

	public String getDebugLabel() {
		return this.debugLabel;
	}

	public Map<String, GlUniform> getUniforms() {
		return this.uniformsByName;
	}
}
