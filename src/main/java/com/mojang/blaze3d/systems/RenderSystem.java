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
import net.minecraft.client.gl.*;
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

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code RenderSystem}.
 */
public class RenderSystem {

	static final Logger LOGGER = LogUtils.getLogger();
	public static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
	public static final int PROJECTION_MATRIX_UBO_SIZE = new Std140SizeCalculator().putMat4f().get();
	private static @Nullable Thread renderThread;
	private static @Nullable GpuDevice DEVICE;
	private static double lastDrawTime = Double.MIN_VALUE;
	private static final RenderSystem.ShapeIndexBuffer
			sharedSequential =
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
		else {
			renderThread = Thread.currentThread();
		}
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

	public static void limitDisplayFPS(int fps) {
		double d = lastDrawTime + 1.0 / fps;

		double e;
		for (e = GLFW.glfwGetTime(); e < d; e = GLFW.glfwGetTime()) {
			GLFW.glfwWaitEventsTimeout(d - e);
		}

		lastDrawTime = e;
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

	public static void enableScissorForRenderTypeDraws(int i, int j, int k, int l) {
		scissorStateForRenderTypeDraws.enable(i, j, k, l);
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

	public static void executePendingTasks() {
		for (RenderSystem.Task task = PENDING_FENCES.peekFirst(); task != null; task = PENDING_FENCES.peekFirst()) {
			if (!task.fence.awaitCompletion(0L)) {
				return;
			}

			try {
				task.callback.run();
			}
			finally {
				task.fence.close();
			}

			PENDING_FENCES.removeFirst();
		}
	}

	public static GpuDevice getDevice() {
		if (DEVICE == null) {
			throw new IllegalStateException("Can't getDevice() before it was initialized");
		}
		else {
			return DEVICE;
		}
	}

	public static @Nullable GpuDevice tryGetDevice() {
		return DEVICE;
	}

	public static DynamicUniforms getDynamicUniforms() {
		if (dynamicUniforms == null) {
			throw new IllegalStateException("Can't getDynamicUniforms() before device was initialized");
		}
		else {
			return dynamicUniforms;
		}
	}

	public static void bindDefaultUniforms(RenderPass pass) {
		GpuBufferSlice gpuBufferSlice = getProjectionMatrixBuffer();
		if (gpuBufferSlice != null) {
			pass.setUniform("Projection", gpuBufferSlice);
		}

		GpuBufferSlice gpuBufferSlice2 = getShaderFog();
		if (gpuBufferSlice2 != null) {
			pass.setUniform("Fog", gpuBufferSlice2);
		}

		GpuBuffer gpuBuffer = getGlobalSettingsUniform();
		if (gpuBuffer != null) {
			pass.setUniform("Globals", gpuBuffer);
		}

		GpuBufferSlice gpuBufferSlice3 = getShaderLights();
		if (gpuBufferSlice3 != null) {
			pass.setUniform("Lighting", gpuBufferSlice3);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ShapeIndexBuffer}.
	 */
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
			return requiredSize <= this.size;
		}

		public GpuBuffer getIndexBuffer(int requiredSize) {
			this.grow(requiredSize);
			return this.indexBuffer;
		}

		private void grow(int requiredSize) {
			if (!this.isLargeEnough(requiredSize)) {
				requiredSize = MathHelper.roundUpToMultiple(requiredSize * 2, this.vertexCountInTriangulated);
				RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.size, requiredSize);
				int i = requiredSize / this.vertexCountInTriangulated;
				int j = i * this.vertexCountInShape;
				VertexFormat.IndexType indexType = VertexFormat.IndexType.smallestFor(j);
				int k = MathHelper.roundUpToMultiple(requiredSize * indexType.size, 4);
				ByteBuffer byteBuffer = MemoryUtil.memAlloc(k);

				try {
					this.indexType = indexType;
					it.unimi.dsi.fastutil.ints.IntConsumer intConsumer = this.getIndexConsumer(byteBuffer);

					for (int l = 0; l < requiredSize; l += this.vertexCountInTriangulated) {
						this.triangulator.accept(
								intConsumer,
								l * this.vertexCountInShape / this.vertexCountInTriangulated
						);
					}

					byteBuffer.flip();
					if (this.indexBuffer != null) {
						this.indexBuffer.close();
					}

					this.indexBuffer =
							RenderSystem.getDevice().createBuffer(() -> "Auto Storage index buffer", 64, byteBuffer);
				}
				finally {
					MemoryUtil.memFree(byteBuffer);
				}

				this.size = requiredSize;
			}
		}

		private it.unimi.dsi.fastutil.ints.IntConsumer getIndexConsumer(ByteBuffer indexBuffer) {
			switch (this.indexType) {
				case SHORT:
					return index -> indexBuffer.putShort((short) index);
				case INT:
				default:
					return indexBuffer::putInt;
			}
		}

		public VertexFormat.IndexType getIndexType() {
			return this.indexType;
		}

		@Environment(EnvType.CLIENT)
		/**
		 * {@code Triangulator}.
		 */
		interface Triangulator {

			void accept(it.unimi.dsi.fastutil.ints.IntConsumer indexConsumer, int firstVertexIndex);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Task}.
	 */
	record Task(Runnable callback, GpuFence fence) {
	}
}
