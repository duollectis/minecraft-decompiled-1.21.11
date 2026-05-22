package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.DynamicUniforms;
import net.minecraft.client.gl.GlBackend;
import net.minecraft.client.gl.SamplerCache;
import net.minecraft.client.gl.ScissorState;
import net.minecraft.client.gl.ShaderSourceGetter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import net.minecraft.util.TimeSupplier;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.DeobfuscateClass;
import net.minecraft.util.collection.ArrayListDeque;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

/**
 * Центральный фасад системы рендеринга.
 *
 * <p>Управляет жизненным циклом GPU-устройства, матрицами проекции и модели,
 * индексными буферами для стандартных примитивов, а также очередью задач,
 * ожидающих завершения GPU-операций через {@link GpuFence}.
 *
 * <p>Большинство методов требуют вызова из потока рендеринга —
 * нарушение этого условия приводит к {@link IllegalStateException}.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class RenderSystem {

	static final Logger LOGGER = LogUtils.getLogger();
	public static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
	public static final int PROJECTION_MATRIX_UBO_SIZE = new Std140SizeCalculator().putMat4f().get();
	private static @Nullable Thread renderThread;
	private static @Nullable GpuDevice DEVICE;
	private static double lastDrawTime = Double.MIN_VALUE;
	private static final RenderSystem.ShapeIndexBuffer sharedSequential =
		new RenderSystem.ShapeIndexBuffer(1, 1, IntConsumer::accept);
	private static final RenderSystem.ShapeIndexBuffer sharedSequentialQuad = new RenderSystem.ShapeIndexBuffer(
		4, 6, (indexConsumer, firstVertexIndex) -> {
			indexConsumer.accept(firstVertexIndex);
			indexConsumer.accept(firstVertexIndex + 1);
			indexConsumer.accept(firstVertexIndex + 2);
			indexConsumer.accept(firstVertexIndex + 2);
			indexConsumer.accept(firstVertexIndex + 3);
			indexConsumer.accept(firstVertexIndex);
		}
	);
	private static final RenderSystem.ShapeIndexBuffer sharedSequentialLines = new RenderSystem.ShapeIndexBuffer(
		4, 6, (indexConsumer, firstVertexIndex) -> {
			indexConsumer.accept(firstVertexIndex);
			indexConsumer.accept(firstVertexIndex + 1);
			indexConsumer.accept(firstVertexIndex + 2);
			indexConsumer.accept(firstVertexIndex + 3);
			indexConsumer.accept(firstVertexIndex + 2);
			indexConsumer.accept(firstVertexIndex + 1);
		}
	);
	private static ProjectionType projectionType = ProjectionType.PERSPECTIVE;
	private static ProjectionType savedProjectionType = ProjectionType.PERSPECTIVE;
	private static final Matrix4fStack modelViewStack = new Matrix4fStack(16);
	private static @Nullable GpuBufferSlice shaderFog = null;
	private static @Nullable GpuBufferSlice shaderLightDirections;
	private static @Nullable GpuBufferSlice projectionMatrixBuffer;
	private static @Nullable GpuBufferSlice savedProjectionMatrixBuffer;
	private static String apiDescription = "Unknown";
	private static final AtomicLong pollEventsWaitStart = new AtomicLong();
	private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);
	private static final ArrayListDeque<RenderSystem.Task> PENDING_FENCES = new ArrayListDeque<>();
	public static @Nullable GpuTextureView outputColorTextureOverride;
	public static @Nullable GpuTextureView outputDepthTextureOverride;
	private static @Nullable GpuBuffer globalSettingsUniform;
	private static @Nullable DynamicUniforms dynamicUniforms;
	private static final ScissorState scissorStateForRenderTypeDraws = new ScissorState();
	private static SamplerCache samplerCache = new SamplerCache();

	public static SamplerCache getSamplerCache() {
		return samplerCache;
	}

	public static void initRenderThread() {
		if (renderThread != null) {
			throw new IllegalStateException("Could not initialize render thread");
		}

		renderThread = Thread.currentThread();
	}

	public static boolean isOnRenderThread() {
		return Thread.currentThread() == renderThread;
	}

	public static void assertOnRenderThread() {
		if (!isOnRenderThread()) {
			throw constructThreadException();
		}
	}

	private static IllegalStateException constructThreadException() {
		return new IllegalStateException("Rendersystem called from wrong thread");
	}

	private static void pollEvents() {
		pollEventsWaitStart.set(Util.getMeasuringTimeMs());
		pollingEvents.set(true);
		GLFW.glfwPollEvents();
		pollingEvents.set(false);
	}

	public static boolean isFrozenAtPollEvents() {
		return pollingEvents.get() && Util.getMeasuringTimeMs() - pollEventsWaitStart.get() > 200L;
	}

	public static void flipFrame(Window window, @Nullable TracyFrameCapturer capturer) {
		pollEvents();
		Tessellator.getInstance().clear();
		GLFW.glfwSwapBuffers(window.getHandle());
		if (capturer != null) {
			capturer.markFrame();
		}

		dynamicUniforms.clear();
		MinecraftClient.getInstance().worldRenderer.rotate();
		pollEvents();
	}

	/**
	 * Ограничивает FPS, блокируя поток рендеринга до наступления следующего кадра.
	 * Использует {@code glfwWaitEventsTimeout} вместо busy-wait для экономии CPU.
	 *
	 * @param fps целевое количество кадров в секунду
	 */
	public static void limitDisplayFPS(int fps) {
		double targetTime = lastDrawTime + 1.0 / fps;
		double currentTime = GLFW.glfwGetTime();

		while (currentTime < targetTime) {
			GLFW.glfwWaitEventsTimeout(targetTime - currentTime);
			currentTime = GLFW.glfwGetTime();
		}

		lastDrawTime = currentTime;
	}

	public static void setShaderFog(GpuBufferSlice shaderFog) {
		RenderSystem.shaderFog = shaderFog;
	}

	public static @Nullable GpuBufferSlice getShaderFog() {
		return shaderFog;
	}

	public static void setShaderLights(GpuBufferSlice shaderLightDirections) {
		RenderSystem.shaderLightDirections = shaderLightDirections;
	}

	public static @Nullable GpuBufferSlice getShaderLights() {
		return shaderLightDirections;
	}

	public static void enableScissorForRenderTypeDraws(int x, int y, int width, int height) {
		scissorStateForRenderTypeDraws.enable(x, y, width, height);
	}

	public static void disableScissorForRenderTypeDraws() {
		scissorStateForRenderTypeDraws.disable();
	}

	public static ScissorState getScissorStateForRenderTypeDraws() {
		return scissorStateForRenderTypeDraws;
	}

	public static String getBackendDescription() {
		return String.format(Locale.ROOT, "LWJGL version %s", GLX._getLWJGLVersion());
	}

	public static String getApiDescription() {
		return apiDescription;
	}

	public static TimeSupplier.Nanoseconds initBackendSystem() {
		return GLX._initGlfw()::getAsLong;
	}

	public static void initRenderer(
			long windowHandle,
			int debugVerbosity,
			boolean sync,
			ShaderSourceGetter shaderSourceGetter,
			boolean renderDebugLabels
	) {
		DEVICE = new GlBackend(windowHandle, debugVerbosity, sync, shaderSourceGetter, renderDebugLabels);
		apiDescription = getDevice().getImplementationInformation();
		dynamicUniforms = new DynamicUniforms();
		samplerCache.init();
	}

	public static void setErrorCallback(GLFWErrorCallbackI callback) {
		GLX._setGlfwErrorCallback(callback);
	}

	public static void setupDefaultState() {
		modelViewStack.clear();
	}

	public static void setProjectionMatrix(GpuBufferSlice projectionMatrixBuffer, ProjectionType projectionType) {
		assertOnRenderThread();
		RenderSystem.projectionMatrixBuffer = projectionMatrixBuffer;
		RenderSystem.projectionType = projectionType;
	}

	public static void backupProjectionMatrix() {
		assertOnRenderThread();
		savedProjectionMatrixBuffer = projectionMatrixBuffer;
		savedProjectionType = projectionType;
	}

	public static void restoreProjectionMatrix() {
		assertOnRenderThread();
		projectionMatrixBuffer = savedProjectionMatrixBuffer;
		projectionType = savedProjectionType;
	}

	public static @Nullable GpuBufferSlice getProjectionMatrixBuffer() {
		assertOnRenderThread();
		return projectionMatrixBuffer;
	}

	public static Matrix4f getModelViewMatrix() {
		assertOnRenderThread();
		return modelViewStack;
	}

	public static Matrix4fStack getModelViewStack() {
		assertOnRenderThread();
		return modelViewStack;
	}

	public static RenderSystem.ShapeIndexBuffer getSequentialBuffer(VertexFormat.DrawMode drawMode) {
		assertOnRenderThread();

		return switch (drawMode) {
			case QUADS -> sharedSequentialQuad;
			case LINES -> sharedSequentialLines;
			default -> sharedSequential;
		};
	}

	public static void setGlobalSettingsUniform(GpuBuffer globalSettingsUniform) {
		RenderSystem.globalSettingsUniform = globalSettingsUniform;
	}

	public static @Nullable GpuBuffer getGlobalSettingsUniform() {
		return globalSettingsUniform;
	}

	public static ProjectionType getProjectionType() {
		assertOnRenderThread();
		return projectionType;
	}

	public static void queueFencedTask(Runnable task) {
		PENDING_FENCES.addLast(new RenderSystem.Task(task, getDevice().createCommandEncoder().createFence()));
	}

	/**
	 * Выполняет все задачи из очереди, чей GPU-fence уже сигнализирован.
	 * Останавливается при первой незавершённой задаче, сохраняя порядок выполнения.
	 */
	public static void executePendingTasks() {
		for (RenderSystem.Task task = PENDING_FENCES.peekFirst(); task != null; task = PENDING_FENCES.peekFirst()) {
			if (!task.fence().awaitCompletion(0L)) {
				return;
			}

			try {
				task.callback().run();
			}
			finally {
				task.fence().close();
			}

			PENDING_FENCES.removeFirst();
		}
	}

	public static GpuDevice getDevice() {
		if (DEVICE == null) {
			throw new IllegalStateException("Can't getDevice() before it was initialized");
		}

		return DEVICE;
	}

	public static @Nullable GpuDevice tryGetDevice() {
		return DEVICE;
	}

	public static DynamicUniforms getDynamicUniforms() {
		if (dynamicUniforms == null) {
			throw new IllegalStateException("Can't getDynamicUniforms() before device was initialized");
		}

		return dynamicUniforms;
	}

	public static void bindDefaultUniforms(RenderPass pass) {
		GpuBufferSlice projection = getProjectionMatrixBuffer();
		if (projection != null) {
			pass.setUniform("Projection", projection);
		}

		GpuBufferSlice fog = getShaderFog();
		if (fog != null) {
			pass.setUniform("Fog", fog);
		}

		GpuBuffer globals = getGlobalSettingsUniform();
		if (globals != null) {
			pass.setUniform("Globals", globals);
		}

		GpuBufferSlice lighting = getShaderLights();
		if (lighting != null) {
			pass.setUniform("Lighting", lighting);
		}
	}

	/**
	 * Буфер индексов для стандартных примитивов (квады, линии и т.д.).
	 * Автоматически расширяется при нехватке места через {@link #grow(int)}.
	 */
	@Environment(EnvType.CLIENT)
	public static final class ShapeIndexBuffer {

		private final int vertexCountInShape;
		private final int vertexCountInTriangulated;
		private final RenderSystem.ShapeIndexBuffer.Triangulator triangulator;
		private @Nullable GpuBuffer indexBuffer;
		private VertexFormat.IndexType indexType = VertexFormat.IndexType.SHORT;
		private int size;

		ShapeIndexBuffer(
				int vertexCountInShape,
				int vertexCountInTriangulated,
				RenderSystem.ShapeIndexBuffer.Triangulator triangulator
		) {
			this.vertexCountInShape = vertexCountInShape;
			this.vertexCountInTriangulated = vertexCountInTriangulated;
			this.triangulator = triangulator;
		}

		public boolean isLargeEnough(int requiredSize) {
			return requiredSize <= size;
		}

		public GpuBuffer getIndexBuffer(int requiredSize) {
			grow(requiredSize);
			return indexBuffer;
		}

		private void grow(int requiredSize) {
			if (isLargeEnough(requiredSize)) {
				return;
			}

			requiredSize = MathHelper.roundUpToMultiple(requiredSize * 2, vertexCountInTriangulated);
			RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", size, requiredSize);

			int shapeCount = requiredSize / vertexCountInTriangulated;
			int vertexCount = shapeCount * vertexCountInShape;
			VertexFormat.IndexType newIndexType = VertexFormat.IndexType.smallestFor(vertexCount);
			int bufferBytes = MathHelper.roundUpToMultiple(requiredSize * newIndexType.size, 4);
			ByteBuffer byteBuffer = MemoryUtil.memAlloc(bufferBytes);

			try {
				indexType = newIndexType;
				it.unimi.dsi.fastutil.ints.IntConsumer intConsumer = getIndexConsumer(byteBuffer);

				for (int offset = 0; offset < requiredSize; offset += vertexCountInTriangulated) {
					triangulator.accept(
						intConsumer,
						offset * vertexCountInShape / vertexCountInTriangulated
					);
				}

				byteBuffer.flip();
				if (indexBuffer != null) {
					indexBuffer.close();
				}

				indexBuffer = RenderSystem.getDevice()
					.createBuffer(() -> "Auto Storage index buffer", 64, byteBuffer);
			}
			finally {
				MemoryUtil.memFree(byteBuffer);
			}

			size = requiredSize;
		}

		private it.unimi.dsi.fastutil.ints.IntConsumer getIndexConsumer(ByteBuffer buffer) {
			return switch (indexType) {
				case SHORT -> index -> buffer.putShort((short) index);
				case INT -> buffer::putInt;
			};
		}

		public VertexFormat.IndexType getIndexType() {
			return indexType;
		}

		@Environment(EnvType.CLIENT)
		interface Triangulator {

			void accept(it.unimi.dsi.fastutil.ints.IntConsumer indexConsumer, int firstVertexIndex);
		}
	}

	@Environment(EnvType.CLIENT)
	record Task(Runnable callback, GpuFence fence) {
	}
}
