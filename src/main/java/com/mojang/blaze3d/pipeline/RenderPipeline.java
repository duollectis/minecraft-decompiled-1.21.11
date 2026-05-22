package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderPipeline;
import net.minecraft.SharedConstants;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Описание конвейера рендеринга: шейдеры, формат вершин, состояния смешивания,
 * глубины, отсечения и прочие параметры GPU-пайплайна.
 *
 * <p>Создаётся через {@link Builder} и является иммутабельным после построения.
 * Для создания переиспользуемых фрагментов конфигурации используйте {@link Snippet}.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class RenderPipeline implements FabricRenderPipeline {

	private final Identifier location;
	private final Identifier vertexShader;
	private final Identifier fragmentShader;
	private final Defines shaderDefines;
	private final List<String> samplers;
	private final List<RenderPipeline.UniformDescription> uniforms;
	private final DepthTestFunction depthTestFunction;
	private final PolygonMode polygonMode;
	private final boolean cull;
	private final LogicOp colorLogic;
	private final Optional<BlendFunction> blendFunction;
	private final boolean writeColor;
	private final boolean writeAlpha;
	private final boolean writeDepth;
	private final VertexFormat vertexFormat;
	private final VertexFormat.DrawMode vertexFormatMode;
	private final float depthBiasScaleFactor;
	private final float depthBiasConstant;
	private final int sortKey;
	private static int sortKeySeed;

	protected RenderPipeline(
			Identifier location,
			Identifier vertexShader,
			Identifier fragmentShader,
			Defines shaderDefines,
			List<String> samplers,
			List<RenderPipeline.UniformDescription> uniforms,
			Optional<BlendFunction> blendFunction,
			DepthTestFunction depthTestFunction,
			PolygonMode polygonMode,
			boolean cull,
			boolean writeColor,
			boolean writeAlpha,
			boolean writeDepth,
			LogicOp colorLogic,
			VertexFormat vertexFormat,
			VertexFormat.DrawMode vertexFormatMode,
			float depthBiasScaleFactor,
			float depthBiasConstant,
			int sortKey
	) {
		this.location = location;
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.shaderDefines = shaderDefines;
		this.samplers = samplers;
		this.uniforms = uniforms;
		this.depthTestFunction = depthTestFunction;
		this.polygonMode = polygonMode;
		this.cull = cull;
		this.blendFunction = blendFunction;
		this.writeColor = writeColor;
		this.writeAlpha = writeAlpha;
		this.writeDepth = writeDepth;
		this.colorLogic = colorLogic;
		this.vertexFormat = vertexFormat;
		this.vertexFormatMode = vertexFormatMode;
		this.depthBiasScaleFactor = depthBiasScaleFactor;
		this.depthBiasConstant = depthBiasConstant;
		this.sortKey = sortKey;
	}

	/**
	 * Возвращает ключ сортировки пайплайна.
	 * В режиме отладки {@link SharedConstants#SHUFFLE_UI_RENDERING_ORDER} ключ
	 * рандомизируется при каждом кадре для выявления зависимостей от порядка рендеринга.
	 */
	public int getSortKey() {
		return SharedConstants.SHUFFLE_UI_RENDERING_ORDER
			? super.hashCode() * (sortKeySeed + 1)
			: sortKey;
	}

	public static void updateSortKeySeed() {
		sortKeySeed = Math.round(100000.0F * (float) Math.random());
	}

	@Override
	public String toString() {
		return location.toString();
	}

	public DepthTestFunction getDepthTestFunction() {
		return depthTestFunction;
	}

	public PolygonMode getPolygonMode() {
		return polygonMode;
	}

	public boolean isCull() {
		return cull;
	}

	public LogicOp getColorLogic() {
		return colorLogic;
	}

	public Optional<BlendFunction> getBlendFunction() {
		return blendFunction;
	}

	public boolean isWriteColor() {
		return writeColor;
	}

	public boolean isWriteAlpha() {
		return writeAlpha;
	}

	public boolean isWriteDepth() {
		return writeDepth;
	}

	public float getDepthBiasScaleFactor() {
		return depthBiasScaleFactor;
	}

	public float getDepthBiasConstant() {
		return depthBiasConstant;
	}

	public Identifier getLocation() {
		return location;
	}

	public VertexFormat getVertexFormat() {
		return vertexFormat;
	}

	public VertexFormat.DrawMode getVertexFormatMode() {
		return vertexFormatMode;
	}

	public Identifier getVertexShader() {
		return vertexShader;
	}

	public Identifier getFragmentShader() {
		return fragmentShader;
	}

	public Defines getShaderDefines() {
		return shaderDefines;
	}

	public List<String> getSamplers() {
		return samplers;
	}

	public List<RenderPipeline.UniformDescription> getUniforms() {
		return uniforms;
	}

	public boolean wantsDepthTexture() {
		return depthTestFunction != DepthTestFunction.NO_DEPTH_TEST
			|| depthBiasConstant != 0.0F
			|| depthBiasScaleFactor != 0.0F
			|| writeDepth;
	}

	/**
	 * Создаёт новый {@link Builder}, предварительно применив все переданные сниппеты.
	 *
	 * @param snippets фрагменты конфигурации для предзаполнения билдера
	 */
	public static RenderPipeline.Builder builder(RenderPipeline.Snippet... snippets) {
		RenderPipeline.Builder builder = new RenderPipeline.Builder();

		for (RenderPipeline.Snippet snippet : snippets) {
			builder.withSnippet(snippet);
		}

		return builder;
	}

	/**
	 * Строитель конвейера рендеринга.
	 *
	 * <p>Все параметры опциональны — при отсутствии используются значения по умолчанию
	 * (LEQUAL depth test, FILL polygon mode, culling включён, запись цвета/глубины включена).
	 * Обязательны: {@code location}, {@code vertexShader}, {@code fragmentShader},
	 * {@code vertexFormat} и {@code vertexFormatMode}.
	 */
	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	public static class Builder implements net.fabricmc.fabric.api.client.rendering.v1.FabricRenderPipeline.Builder {

		private static int nextPipelineSortKey;
		private Optional<Identifier> location = Optional.empty();
		private Optional<Identifier> fragmentShader = Optional.empty();
		private Optional<Identifier> vertexShader = Optional.empty();
		private Optional<Defines.Builder> definesBuilder = Optional.empty();
		private Optional<List<String>> samplers = Optional.empty();
		private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
		private Optional<DepthTestFunction> depthTestFunction = Optional.empty();
		private Optional<PolygonMode> polygonMode = Optional.empty();
		private Optional<Boolean> cull = Optional.empty();
		private Optional<Boolean> writeColor = Optional.empty();
		private Optional<Boolean> writeAlpha = Optional.empty();
		private Optional<Boolean> writeDepth = Optional.empty();
		private Optional<LogicOp> colorLogic = Optional.empty();
		private Optional<BlendFunction> blendFunction = Optional.empty();
		private Optional<VertexFormat> vertexFormat = Optional.empty();
		private Optional<VertexFormat.DrawMode> vertexFormatMode = Optional.empty();
		private float depthBiasScaleFactor;
		private float depthBiasConstant;

		Builder() {
		}

		public RenderPipeline.Builder withLocation(String location) {
			this.location = Optional.of(Identifier.ofVanilla(location));
			return this;
		}

		public RenderPipeline.Builder withLocation(Identifier location) {
			this.location = Optional.of(location);
			return this;
		}

		public RenderPipeline.Builder withFragmentShader(String fragmentShader) {
			this.fragmentShader = Optional.of(Identifier.ofVanilla(fragmentShader));
			return this;
		}

		public RenderPipeline.Builder withFragmentShader(Identifier fragmentShader) {
			this.fragmentShader = Optional.of(fragmentShader);
			return this;
		}

		public RenderPipeline.Builder withVertexShader(String vertexShader) {
			this.vertexShader = Optional.of(Identifier.ofVanilla(vertexShader));
			return this;
		}

		public RenderPipeline.Builder withVertexShader(Identifier vertexShader) {
			this.vertexShader = Optional.of(vertexShader);
			return this;
		}

		public RenderPipeline.Builder withShaderDefine(String flag) {
			if (definesBuilder.isEmpty()) {
				definesBuilder = Optional.of(Defines.builder());
			}

			definesBuilder.get().flag(flag);
			return this;
		}

		public RenderPipeline.Builder withShaderDefine(String name, int value) {
			if (definesBuilder.isEmpty()) {
				definesBuilder = Optional.of(Defines.builder());
			}

			definesBuilder.get().define(name, value);
			return this;
		}

		public RenderPipeline.Builder withShaderDefine(String name, float value) {
			if (definesBuilder.isEmpty()) {
				definesBuilder = Optional.of(Defines.builder());
			}

			definesBuilder.get().define(name, value);
			return this;
		}

		public RenderPipeline.Builder withSampler(String sampler) {
			if (samplers.isEmpty()) {
				samplers = Optional.of(new ArrayList<>());
			}

			samplers.get().add(sampler);
			return this;
		}

		public RenderPipeline.Builder withUniform(String name, UniformType type) {
			if (uniforms.isEmpty()) {
				uniforms = Optional.of(new ArrayList<>());
			}

			if (type == UniformType.TEXEL_BUFFER) {
				throw new IllegalArgumentException("Cannot use texel buffer without specifying texture format");
			}

			uniforms.get().add(new RenderPipeline.UniformDescription(name, type));
			return this;
		}

		public RenderPipeline.Builder withUniform(String name, UniformType type, TextureFormat format) {
			if (uniforms.isEmpty()) {
				uniforms = Optional.of(new ArrayList<>());
			}

			if (type != UniformType.TEXEL_BUFFER) {
				throw new IllegalArgumentException("Only texel buffer can specify texture format");
			}

			uniforms.get().add(new RenderPipeline.UniformDescription(name, format));
			return this;
		}

		public RenderPipeline.Builder withDepthTestFunction(DepthTestFunction depthTestFunction) {
			this.depthTestFunction = Optional.of(depthTestFunction);
			return this;
		}

		public RenderPipeline.Builder withPolygonMode(PolygonMode polygonMode) {
			this.polygonMode = Optional.of(polygonMode);
			return this;
		}

		public RenderPipeline.Builder withCull(boolean cull) {
			this.cull = Optional.of(cull);
			return this;
		}

		public RenderPipeline.Builder withBlend(BlendFunction blendFunction) {
			this.blendFunction = Optional.of(blendFunction);
			return this;
		}

		public RenderPipeline.Builder withoutBlend() {
			blendFunction = Optional.empty();
			return this;
		}

		public RenderPipeline.Builder withColorWrite(boolean writeColor) {
			this.writeColor = Optional.of(writeColor);
			writeAlpha = Optional.of(writeColor);
			return this;
		}

		public RenderPipeline.Builder withColorWrite(boolean writeColor, boolean writeAlpha) {
			this.writeColor = Optional.of(writeColor);
			this.writeAlpha = Optional.of(writeAlpha);
			return this;
		}

		public RenderPipeline.Builder withDepthWrite(boolean writeDepth) {
			this.writeDepth = Optional.of(writeDepth);
			return this;
		}

		@Deprecated
		public RenderPipeline.Builder withColorLogic(LogicOp colorLogic) {
			this.colorLogic = Optional.of(colorLogic);
			return this;
		}

		public RenderPipeline.Builder withVertexFormat(
				VertexFormat vertexFormat,
				VertexFormat.DrawMode vertexFormatMode
		) {
			this.vertexFormat = Optional.of(vertexFormat);
			this.vertexFormatMode = Optional.of(vertexFormatMode);
			return this;
		}

		public RenderPipeline.Builder withDepthBias(float depthBiasScaleFactor, float depthBiasConstant) {
			this.depthBiasScaleFactor = depthBiasScaleFactor;
			this.depthBiasConstant = depthBiasConstant;
			return this;
		}

		void withSnippet(RenderPipeline.Snippet snippet) {
			if (snippet.vertexShader().isPresent()) {
				vertexShader = snippet.vertexShader();
			}

			if (snippet.fragmentShader().isPresent()) {
				fragmentShader = snippet.fragmentShader();
			}

			if (snippet.shaderDefines().isPresent()) {
				if (definesBuilder.isEmpty()) {
					definesBuilder = Optional.of(Defines.builder());
				}

				Defines defines = snippet.shaderDefines().get();

				for (Entry<String, String> entry : defines.values().entrySet()) {
					definesBuilder.get().define(entry.getKey(), entry.getValue());
				}

				for (String flag : defines.flags()) {
					definesBuilder.get().flag(flag);
				}
			}

			snippet.samplers().ifPresent(snippetSamplers -> {
				if (samplers.isPresent()) {
					samplers.get().addAll(snippetSamplers);
				}
				else {
					samplers = Optional.of(new ArrayList<>(snippetSamplers));
				}
			});

			snippet.uniforms().ifPresent(snippetUniforms -> {
				if (uniforms.isPresent()) {
					uniforms.get().addAll(snippetUniforms);
				}
				else {
					uniforms = Optional.of(new ArrayList<>(snippetUniforms));
				}
			});

			if (snippet.depthTestFunction().isPresent()) {
				depthTestFunction = snippet.depthTestFunction();
			}

			if (snippet.cull().isPresent()) {
				cull = snippet.cull();
			}

			if (snippet.writeColor().isPresent()) {
				writeColor = snippet.writeColor();
			}

			if (snippet.writeAlpha().isPresent()) {
				writeAlpha = snippet.writeAlpha();
			}

			if (snippet.writeDepth().isPresent()) {
				writeDepth = snippet.writeDepth();
			}

			if (snippet.colorLogic().isPresent()) {
				colorLogic = snippet.colorLogic();
			}

			if (snippet.blendFunction().isPresent()) {
				blendFunction = snippet.blendFunction();
			}

			if (snippet.vertexFormat().isPresent()) {
				vertexFormat = snippet.vertexFormat();
			}

			if (snippet.vertexFormatMode().isPresent()) {
				vertexFormatMode = snippet.vertexFormatMode();
			}
		}

		public RenderPipeline.Snippet buildSnippet() {
			return new RenderPipeline.Snippet(
				vertexShader,
				fragmentShader,
				definesBuilder.map(Defines.Builder::build),
				samplers.map(Collections::unmodifiableList),
				uniforms.map(Collections::unmodifiableList),
				blendFunction,
				depthTestFunction,
				polygonMode,
				cull,
				writeColor,
				writeAlpha,
				writeDepth,
				colorLogic,
				vertexFormat,
				vertexFormatMode
			);
		}

		/**
		 * Строит финальный {@link RenderPipeline}.
		 *
		 * @throws IllegalStateException если не заданы обязательные параметры
		 */
		public RenderPipeline build() {
			if (location.isEmpty()) {
				throw new IllegalStateException("Missing location");
			}

			if (vertexShader.isEmpty()) {
				throw new IllegalStateException("Missing vertex shader");
			}

			if (fragmentShader.isEmpty()) {
				throw new IllegalStateException("Missing fragment shader");
			}

			if (vertexFormat.isEmpty()) {
				throw new IllegalStateException("Missing vertex buffer format");
			}

			if (vertexFormatMode.isEmpty()) {
				throw new IllegalStateException("Missing vertex mode");
			}

			return new RenderPipeline(
				location.get(),
				vertexShader.get(),
				fragmentShader.get(),
				definesBuilder.orElse(Defines.builder()).build(),
				List.copyOf(samplers.orElse(new ArrayList<>())),
				uniforms.orElse(Collections.emptyList()),
				blendFunction,
				depthTestFunction.orElse(DepthTestFunction.LEQUAL_DEPTH_TEST),
				polygonMode.orElse(PolygonMode.FILL),
				cull.orElse(true),
				writeColor.orElse(true),
				writeAlpha.orElse(true),
				writeDepth.orElse(true),
				colorLogic.orElse(LogicOp.NONE),
				vertexFormat.get(),
				vertexFormatMode.get(),
				depthBiasScaleFactor,
				depthBiasConstant,
				nextPipelineSortKey++
			);
		}
	}

	/** Переиспользуемый фрагмент конфигурации пайплайна для применения через {@link Builder#withSnippet}. */
	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	public record Snippet(
			Optional<Identifier> vertexShader,
			Optional<Identifier> fragmentShader,
			Optional<Defines> shaderDefines,
			Optional<List<String>> samplers,
			Optional<List<RenderPipeline.UniformDescription>> uniforms,
			Optional<BlendFunction> blendFunction,
			Optional<DepthTestFunction> depthTestFunction,
			Optional<PolygonMode> polygonMode,
			Optional<Boolean> cull,
			Optional<Boolean> writeColor,
			Optional<Boolean> writeAlpha,
			Optional<Boolean> writeDepth,
			Optional<LogicOp> colorLogic,
			Optional<VertexFormat> vertexFormat,
			Optional<VertexFormat.DrawMode> vertexFormatMode
	) implements net.fabricmc.fabric.api.client.rendering.v1.FabricRenderPipeline.Snippet {
	}

	/** Описание uniform-переменной шейдера: имя, тип и опциональный формат текстуры для texel-буферов. */
	@Environment(EnvType.CLIENT)
	@DeobfuscateClass
	public record UniformDescription(String name, UniformType type, @Nullable TextureFormat textureFormat) {

		public UniformDescription(String name, UniformType type) {
			this(name, type, null);
			if (type == UniformType.TEXEL_BUFFER) {
				throw new IllegalArgumentException("Texel buffer needs a texture format");
			}
		}

		public UniformDescription(String name, TextureFormat format) {
			this(name, UniformType.TEXEL_BUFFER, format);
		}
	}
}
