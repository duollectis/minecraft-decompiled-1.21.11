package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.*;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.client.util.TextureAllocationException;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;

/**
 * Реализация {@link GpuDevice} на базе OpenGL.
 * Управляет компиляцией шейдеров, созданием текстур и буферов,
 * а также инициализацией всех вспомогательных менеджеров (VAO, UBO, debug labels).
 */
@Environment(EnvType.CLIENT)
public class GlBackend implements GpuDevice {

	private static final Logger LOGGER = LogUtils.getLogger();

	// GL-константы для запроса параметров устройства
	private static final int GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 35380;
	private static final int GL_TEXTURE_CUBE_MAP = 34067;
	private static final int GL_TEXTURE_2D = 3553;
	private static final int GL_TEXTURE_MAX_LEVEL = 33085;
	private static final int GL_TEXTURE_BASE_LEVEL = 33082;
	private static final int GL_TEXTURE_MIN_LOD = 33082;
	private static final int GL_TEXTURE_MAX_LOD = 33083;
	private static final int GL_TEXTURE_COMPARE_MODE = 34892;
	private static final int GL_MAX_TEXTURE_SIZE = 3379;
	private static final int GL_PROXY_TEXTURE_2D = 32868;
	private static final int GL_RGBA8 = 6408;
	private static final int GL_UNSIGNED_BYTE = 5121;
	private static final int GL_TEXTURE_WIDTH = 4096;
	private static final int GL_VERTEX_SHADER = 35633;
	private static final int GL_COMPILE_STATUS = 35713;
	private static final int GL_MAX_SHADER_LOG = 32768;
	private static final int GL_RENDERER = 7937;
	private static final int GL_VERSION = 7938;
	private static final int GL_VENDOR = 7936;
	private static final int GL_OUT_OF_MEMORY = 1285;
	private static final int GL_POINT_SPRITE = 34895;
	private static final int GL_VERTEX_PROGRAM_POINT_SIZE = 34370;
	private static final int GL_MAX_ANISOTROPY = 34047;

	protected static boolean allowGlArbVABinding = true;
	protected static boolean allowGlKhrDebug = true;
	protected static boolean allowExtDebugLabel = true;
	protected static boolean allowGlArbDebugOutput = true;
	protected static boolean allowGlArbDirectAccess = true;
	protected static boolean allowGlBufferStorage = true;

	private final CommandEncoder commandEncoder;
	private final @Nullable GlDebug glDebug;
	private final DebugLabelManager debugLabelManager;
	private final int maxTextureSize;
	private final BufferManager bufferManager;
	private final ShaderSourceGetter defaultShaderSourceGetter;
	private final Map<RenderPipeline, CompiledShaderPipeline> pipelineCompileCache = new IdentityHashMap<>();
	private final Map<ShaderKey, CompiledShader> shaderCompileCache = new HashMap<>();
	private final VertexBufferManager vertexBufferManager;
	private final GpuBufferManager gpuBufferManager;
	private final Set<String> usedGlCapabilities = new HashSet<>();
	private final int uniformOffsetAlignment;
	private final int maxSupportedAnisotropy;

	public GlBackend(
		long contextId,
		int debugVerbosity,
		boolean sync,
		ShaderSourceGetter defaultShaderSourceGetter,
		boolean renderDebugLabels
	) {
		GLFW.glfwMakeContextCurrent(contextId);
		GLCapabilities capabilities = GL.createCapabilities();
		int maxTexSize = determineMaxTextureSize();
		GLFW.glfwSetWindowSizeLimits(contextId, -1, -1, maxTexSize, maxTexSize);
		GpuDeviceInfo gpuDeviceInfo = GpuDeviceInfo.get(this);
		glDebug = GlDebug.enableDebug(debugVerbosity, sync, usedGlCapabilities);
		debugLabelManager = DebugLabelManager.create(capabilities, renderDebugLabels, usedGlCapabilities);
		vertexBufferManager = VertexBufferManager.create(capabilities, debugLabelManager, usedGlCapabilities);
		gpuBufferManager = GpuBufferManager.create(capabilities, usedGlCapabilities);
		bufferManager = BufferManager.create(capabilities, usedGlCapabilities, gpuDeviceInfo);
		maxTextureSize = maxTexSize;
		this.defaultShaderSourceGetter = defaultShaderSourceGetter;
		commandEncoder = new GlCommandEncoder(this);
		uniformOffsetAlignment = GL11.glGetInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
		GL11.glEnable(GL_POINT_SPRITE);
		GL11.glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);

		if (capabilities.GL_EXT_texture_filter_anisotropic) {
			maxSupportedAnisotropy = MathHelper.floor(GL11.glGetFloat(GL_MAX_ANISOTROPY));
			usedGlCapabilities.add("GL_EXT_texture_filter_anisotropic");
		}
		else {
			maxSupportedAnisotropy = 1;
		}
	}

	public DebugLabelManager getDebugLabelManager() {
		return debugLabelManager;
	}

	@Override
	public CommandEncoder createCommandEncoder() {
		return commandEncoder;
	}

	@Override
	public int getMaxSupportedAnisotropy() {
		return maxSupportedAnisotropy;
	}

	@Override
	public GpuSampler createSampler(
		AddressMode addressModeU,
		AddressMode addressModeV,
		FilterMode minFilter,
		FilterMode magFilter,
		int maxAnisotropy,
		OptionalDouble lodBias
	) {
		if (maxAnisotropy < 1 || maxAnisotropy > maxSupportedAnisotropy) {
			throw new IllegalArgumentException(
				"maxAnisotropy out of range; must be >= 1 and <= " + getMaxSupportedAnisotropy()
					+ ", but was " + maxAnisotropy
			);
		}

		return new GlSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, lodBias);
	}

	@Override
	public GpuTexture createTexture(
		@Nullable Supplier<String> labelSupplier,
		@GpuTexture.Usage int usage,
		TextureFormat format,
		int width,
		int height,
		int depthOrLayers,
		int mipLevels
	) {
		String label = debugLabelManager.isUsable() && labelSupplier != null ? labelSupplier.get() : null;
		return createTexture(label, usage, format, width, height, depthOrLayers, mipLevels);
	}

	/**
	 * Создаёт OpenGL-текстуру с заданными параметрами.
	 * Поддерживает кубмапы (usage & 16) и обычные 2D-текстуры.
	 * Бросает {@link TextureAllocationException} при ошибке GL_OUT_OF_MEMORY.
	 */
	@Override
	public GpuTexture createTexture(
		@Nullable String label,
		@GpuTexture.Usage int usage,
		TextureFormat format,
		int width,
		int height,
		int depthOrLayers,
		int mipLevels
	) {
		if (mipLevels < 1) {
			throw new IllegalArgumentException("mipLevels must be at least 1");
		}

		if (depthOrLayers < 1) {
			throw new IllegalArgumentException("depthOrLayers must be at least 1");
		}

		boolean isCubemap = (usage & 16) != 0;

		if (isCubemap) {
			if (width != height) {
				throw new IllegalArgumentException(
					"Cubemap compatible textures must be square, but size is " + width + "x" + height
				);
			}

			if (depthOrLayers % 6 != 0) {
				throw new IllegalArgumentException(
					"Cubemap compatible textures must have a layer count with a multiple of 6, was " + depthOrLayers
				);
			}

			if (depthOrLayers > 6) {
				throw new UnsupportedOperationException("Array textures are not yet supported");
			}
		}
		else if (depthOrLayers > 1) {
			throw new UnsupportedOperationException("Array or 3D textures are not yet supported");
		}

		GlStateManager.clearGlErrors();
		int glId = GlStateManager._genTexture();

		if (label == null) {
			label = String.valueOf(glId);
		}

		int target;

		if (isCubemap) {
			GL11.glBindTexture(GL_TEXTURE_CUBE_MAP, glId);
			target = GL_TEXTURE_CUBE_MAP;
		}
		else {
			GlStateManager._bindTexture(glId);
			target = GL_TEXTURE_2D;
		}

		GlStateManager._texParameter(target, GL_TEXTURE_MAX_LEVEL, mipLevels - 1);
		GlStateManager._texParameter(target, GL_TEXTURE_MIN_LOD, 0);
		GlStateManager._texParameter(target, GL_TEXTURE_MAX_LOD, mipLevels - 1);

		if (format.hasDepthAspect()) {
			GlStateManager._texParameter(target, GL_TEXTURE_COMPARE_MODE, 0);
		}

		if (isCubemap) {
			for (int face : GlConst.CUBEMAP_TARGETS) {
				for (int mip = 0; mip < mipLevels; mip++) {
					GlStateManager._texImage2D(
						face, mip,
						GlConst.toGlInternalId(format),
						width >> mip, height >> mip,
						0,
						GlConst.toGlExternalId(format),
						GlConst.toGlType(format),
						null
					);
				}
			}
		}
		else {
			for (int mip = 0; mip < mipLevels; mip++) {
				GlStateManager._texImage2D(
					target, mip,
					GlConst.toGlInternalId(format),
					width >> mip, height >> mip,
					0,
					GlConst.toGlExternalId(format),
					GlConst.toGlType(format),
					null
				);
			}
		}

		int glError = GlStateManager._getError();

		if (glError == GL_OUT_OF_MEMORY) {
			throw new TextureAllocationException("Could not allocate texture of " + width + "x" + height + " for " + label);
		}

		if (glError != 0) {
			throw new IllegalStateException("OpenGL error " + glError);
		}

		GlTexture texture = new GlTexture(usage, label, format, width, height, depthOrLayers, mipLevels, glId);
		debugLabelManager.labelGlTexture(texture);
		return texture;
	}

	@Override
	public GpuTextureView createTextureView(GpuTexture texture) {
		return createTextureView(texture, 0, texture.getMipLevels());
	}

	@Override
	public GpuTextureView createTextureView(GpuTexture texture, int baseMip, int mipCount) {
		if (texture.isClosed()) {
			throw new IllegalArgumentException("Can't create texture view with closed texture");
		}

		if (baseMip < 0 || baseMip + mipCount > texture.getMipLevels()) {
			throw new IllegalArgumentException(
				mipCount + " mip levels starting from " + baseMip
					+ " would be out of range for texture with only " + texture.getMipLevels() + " mip levels"
			);
		}

		return new GlTextureView((GlTexture) texture, baseMip, mipCount);
	}

	@Override
	public GpuBuffer createBuffer(@Nullable Supplier<String> labelSupplier, @GpuBuffer.Usage int usage, long size) {
		if (size <= 0L) {
			throw new IllegalArgumentException("Buffer size must be greater than zero");
		}

		GlStateManager.clearGlErrors();
		GlGpuBuffer buffer = gpuBufferManager.createBuffer(bufferManager, labelSupplier, usage, size);
		int glError = GlStateManager._getError();

		if (glError == GL_OUT_OF_MEMORY) {
			throw new TextureAllocationException("Could not allocate buffer of " + size + " for " + labelSupplier);
		}

		if (glError != 0) {
			throw new IllegalStateException("OpenGL error " + glError);
		}

		debugLabelManager.labelGlGpuBuffer(buffer);
		return buffer;
	}

	@Override
	public GpuBuffer createBuffer(
		@Nullable Supplier<String> labelSupplier,
		@GpuBuffer.Usage int usage,
		ByteBuffer data
	) {
		if (!data.hasRemaining()) {
			throw new IllegalArgumentException("Buffer source must not be empty");
		}

		GlStateManager.clearGlErrors();
		long size = data.remaining();
		GlGpuBuffer buffer = gpuBufferManager.createBuffer(bufferManager, labelSupplier, usage, data);
		int glError = GlStateManager._getError();

		if (glError == GL_OUT_OF_MEMORY) {
			throw new TextureAllocationException("Could not allocate buffer of " + size + " for " + labelSupplier);
		}

		if (glError != 0) {
			throw new IllegalStateException("OpenGL error " + glError);
		}

		debugLabelManager.labelGlGpuBuffer(buffer);
		return buffer;
	}

	@Override
	public String getImplementationInformation() {
		return GLFW.glfwGetCurrentContext() == 0L
			? "NO CONTEXT"
			: GlStateManager._getString(GL_RENDERER) + " GL version "
				+ GlStateManager._getString(GL_VERSION) + ", "
				+ GlStateManager._getString(GL_VENDOR);
	}

	@Override
	public List<String> getLastDebugMessages() {
		return glDebug == null ? Collections.emptyList() : glDebug.collectDebugMessages();
	}

	@Override
	public boolean isDebuggingEnabled() {
		return glDebug != null;
	}

	@Override
	public String getRenderer() {
		return GlStateManager._getString(GL_RENDERER);
	}

	@Override
	public String getVendor() {
		return GlStateManager._getString(GL_VENDOR);
	}

	@Override
	public String getBackendName() {
		return "OpenGL";
	}

	@Override
	public String getVersion() {
		return GlStateManager._getString(GL_VERSION);
	}

	@Override
	public int getMaxTextureSize() {
		return maxTextureSize;
	}

	@Override
	public int getUniformOffsetAlignment() {
		return uniformOffsetAlignment;
	}

	@Override
	public void clearPipelineCache() {
		for (CompiledShaderPipeline pipeline : pipelineCompileCache.values()) {
			if (pipeline.program() != ShaderProgram.INVALID) {
				pipeline.program().close();
			}
		}

		pipelineCompileCache.clear();

		for (CompiledShader shader : shaderCompileCache.values()) {
			if (shader != CompiledShader.INVALID_SHADER) {
				shader.close();
			}
		}

		shaderCompileCache.clear();

		if (GlStateManager._getString(GL_RENDERER).contains("AMD")) {
			applyAmdCleanupHack();
		}
	}

	@Override
	public List<String> getEnabledExtensions() {
		return new ArrayList<>(usedGlCapabilities);
	}

	@Override
	public void close() {
		clearPipelineCache();
	}

	public BufferManager getBufferManager() {
		return bufferManager;
	}

	public VertexBufferManager getVertexBufferManager() {
		return vertexBufferManager;
	}

	public GpuBufferManager getGpuBufferManager() {
		return gpuBufferManager;
	}

	/**
	 * Компилирует или возвращает из кэша скомпилированный конвейер рендеринга.
	 * Использует {@link IdentityHashMap} для O(1) поиска по ссылке на объект.
	 */
	protected CompiledShaderPipeline compilePipelineCached(RenderPipeline pipeline) {
		return pipelineCompileCache.computeIfAbsent(
			pipeline,
			p -> compileRenderPipeline(p, defaultShaderSourceGetter)
		);
	}

	protected CompiledShader compileShader(
		Identifier id,
		ShaderType type,
		Defines defines,
		ShaderSourceGetter sourceGetter
	) {
		ShaderKey key = new ShaderKey(id, type, defines);
		return shaderCompileCache.computeIfAbsent(key, k -> compileShader(k, sourceGetter));
	}

	public CompiledShaderPipeline precompilePipeline(
		RenderPipeline renderPipeline,
		@Nullable ShaderSourceGetter shaderSourceGetter
	) {
		ShaderSourceGetter effectiveGetter =
			shaderSourceGetter == null ? defaultShaderSourceGetter : shaderSourceGetter;
		return pipelineCompileCache.computeIfAbsent(
			renderPipeline,
			p -> compileRenderPipeline(p, effectiveGetter)
		);
	}

	private CompiledShader compileShader(ShaderKey key, ShaderSourceGetter sourceGetter) {
		String source = sourceGetter.get(key.id, key.type);

		if (source == null) {
			LOGGER.error("Couldn't find source for {} shader ({})", key.type, key.id);
			return CompiledShader.INVALID_SHADER;
		}

		String sourceWithDefines = GlImportProcessor.addDefines(source, key.defines);
		int shaderId = GlStateManager.glCreateShader(GlConst.toGl(key.type));
		GlStateManager.glShaderSource(shaderId, sourceWithDefines);
		GlStateManager.glCompileShader(shaderId);

		if (GlStateManager.glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
			String log = StringUtils.trim(GlStateManager.glGetShaderInfoLog(shaderId, GL_MAX_SHADER_LOG));
			LOGGER.error("Couldn't compile {} shader ({}): {}", new Object[]{key.type.getName(), key.id, log});
			return CompiledShader.INVALID_SHADER;
		}

		CompiledShader compiled = new CompiledShader(shaderId, key.id, key.type);
		debugLabelManager.labelCompiledShader(compiled);
		return compiled;
	}

	private ShaderProgram compileProgram(RenderPipeline pipeline, ShaderSourceGetter sourceGetter) {
		CompiledShader vertexShader = compileShader(
			pipeline.getVertexShader(),
			ShaderType.VERTEX,
			pipeline.getShaderDefines(),
			sourceGetter
		);
		CompiledShader fragmentShader = compileShader(
			pipeline.getFragmentShader(),
			ShaderType.FRAGMENT,
			pipeline.getShaderDefines(),
			sourceGetter
		);

		if (vertexShader == CompiledShader.INVALID_SHADER) {
			LOGGER.error(
				"Couldn't compile pipeline {}: vertex shader {} was invalid",
				pipeline.getLocation(),
				pipeline.getVertexShader()
			);
			return ShaderProgram.INVALID;
		}

		if (fragmentShader == CompiledShader.INVALID_SHADER) {
			LOGGER.error(
				"Couldn't compile pipeline {}: fragment shader {} was invalid",
				pipeline.getLocation(),
				pipeline.getFragmentShader()
			);
			return ShaderProgram.INVALID;
		}

		try {
			ShaderProgram program = ShaderProgram.create(
				vertexShader,
				fragmentShader,
				pipeline.getVertexFormat(),
				pipeline.getLocation().toString()
			);
			program.set(pipeline.getUniforms(), pipeline.getSamplers());
			debugLabelManager.labelShaderProgram(program);
			return program;
		}
		catch (ShaderLoader.LoadException exception) {
			LOGGER.error("Couldn't compile program for pipeline {}: {}", pipeline.getLocation(), exception);
			return ShaderProgram.INVALID;
		}
	}

	private CompiledShaderPipeline compileRenderPipeline(RenderPipeline pipeline, ShaderSourceGetter sourceGetter) {
		return new CompiledShaderPipeline(pipeline, compileProgram(pipeline, sourceGetter));
	}

	/**
	 * Определяет максимальный поддерживаемый размер текстуры путём зондирования через proxy-текстуры.
	 * Начинает с max(32768, GL_MAX_TEXTURE_SIZE) и уменьшает вдвое до нахождения рабочего размера.
	 */
	private static int determineMaxTextureSize() {
		int reported = GlStateManager._getInteger(GL_MAX_TEXTURE_SIZE);

		for (int size = Math.max(32768, reported); size >= 1024; size >>= 1) {
			GlStateManager._texImage2D(GL_PROXY_TEXTURE_2D, 0, GL_RGBA8, size, size, 0, GL_RGBA8, GL_UNSIGNED_BYTE, null);
			int actualWidth = GlStateManager._getTexLevelParameter(GL_PROXY_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);

			if (actualWidth != 0) {
				return size;
			}
		}

		int fallback = Math.max(reported, 1024);
		LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", fallback);
		return fallback;
	}

	/**
	 * Хак для AMD: создаёт и сразу удаляет пустой шейдер и программу,
	 * чтобы принудить драйвер освободить ресурсы после очистки кэша.
	 */
	private static void applyAmdCleanupHack() {
		int shaderId = GlStateManager.glCreateShader(GL_VERTEX_SHADER);
		int programId = GlStateManager.glCreateProgram();
		GlStateManager.glAttachShader(programId, shaderId);
		GlStateManager.glDeleteShader(shaderId);
		GlStateManager.glDeleteProgram(programId);
	}

	/**
	 * Ключ кэша скомпилированных шейдеров: идентификатор ресурса, тип и набор директив.
	 */
	@Environment(EnvType.CLIENT)
	record ShaderKey(Identifier id, ShaderType type, Defines defines) {

		@Override
		public String toString() {
			String base = id + " (" + type + ")";
			return defines.isEmpty() ? base : base + " with " + defines;
		}
	}
}
