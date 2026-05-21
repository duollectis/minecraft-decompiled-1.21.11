package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.MacWindowUtil;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
/**
 * {@code GlStateManager}.
 */
public class GlStateManager {

	private static final Plot PLOT_TEXTURES = TracyClient.createPlot("GPU Textures");
	private static int numTextures = 0;
	private static final Plot PLOT_BUFFERS = TracyClient.createPlot("GPU Buffers");
	private static int numBuffers = 0;
	private static final GlStateManager.BlendFuncState BLEND = new GlStateManager.BlendFuncState();
	private static final GlStateManager.DepthTestState DEPTH = new GlStateManager.DepthTestState();
	private static final GlStateManager.CullFaceState CULL = new GlStateManager.CullFaceState();
	private static final GlStateManager.PolygonOffsetState POLY_OFFSET = new GlStateManager.PolygonOffsetState();
	private static final GlStateManager.LogicOpState COLOR_LOGIC = new GlStateManager.LogicOpState();
	private static final GlStateManager.ScissorTestState SCISSOR = new GlStateManager.ScissorTestState();
	private static int activeTexture;
	private static final int TEXTURE_COUNT = 12;
	private static final GlStateManager.Texture2DState[] TEXTURES = IntStream.range(0, 12)
	                                                                         .mapToObj(index -> new GlStateManager.Texture2DState())
	                                                                         .toArray(GlStateManager.Texture2DState[]::new);
	private static final GlStateManager.ColorMask COLOR_MASK = new GlStateManager.ColorMask();
	private static int readFbo;
	private static int writeFbo;

	public static void _disableScissorTest() {
		RenderSystem.assertOnRenderThread();
		SCISSOR.capState.disable();
	}

	public static void _enableScissorTest() {
		RenderSystem.assertOnRenderThread();
		SCISSOR.capState.enable();
	}

	public static void _scissorBox(int x, int y, int width, int height) {
		RenderSystem.assertOnRenderThread();
		GL20.glScissor(x, y, width, height);
	}

	public static void _disableDepthTest() {
		RenderSystem.assertOnRenderThread();
		DEPTH.capState.disable();
	}

	public static void _enableDepthTest() {
		RenderSystem.assertOnRenderThread();
		DEPTH.capState.enable();
	}

	public static void _depthFunc(int func) {
		RenderSystem.assertOnRenderThread();
		if (func != DEPTH.func) {
			DEPTH.func = func;
			GL11.glDepthFunc(func);
		}
	}

	public static void _depthMask(boolean mask) {
		RenderSystem.assertOnRenderThread();
		if (mask != DEPTH.mask) {
			DEPTH.mask = mask;
			GL11.glDepthMask(mask);
		}
	}

	public static void _disableBlend() {
		RenderSystem.assertOnRenderThread();
		BLEND.capState.disable();
	}

	public static void _enableBlend() {
		RenderSystem.assertOnRenderThread();
		BLEND.capState.enable();
	}

	public static void _blendFuncSeparate(int srcFactorRGB, int dstFactorRgb, int srcFactorAlpha, int dstFactorAlpha) {
		RenderSystem.assertOnRenderThread();
		if (srcFactorRGB != BLEND.srcFactorRgb
				|| dstFactorRgb != BLEND.dstFactorRgb
				|| srcFactorAlpha != BLEND.srcFactorAlpha
				|| dstFactorAlpha != BLEND.dstFactorAlpha) {
			BLEND.srcFactorRgb = srcFactorRGB;
			BLEND.dstFactorRgb = dstFactorRgb;
			BLEND.srcFactorAlpha = srcFactorAlpha;
			BLEND.dstFactorAlpha = dstFactorAlpha;
			glBlendFuncSeparate(srcFactorRGB, dstFactorRgb, srcFactorAlpha, dstFactorAlpha);
		}
	}

	public static int glGetProgrami(int program, int pname) {
		RenderSystem.assertOnRenderThread();
		return GL20.glGetProgrami(program, pname);
	}

	public static void glAttachShader(int program, int shader) {
		RenderSystem.assertOnRenderThread();
		GL20.glAttachShader(program, shader);
	}

	public static void glDeleteShader(int shader) {
		RenderSystem.assertOnRenderThread();
		GL20.glDeleteShader(shader);
	}

	public static int glCreateShader(int type) {
		RenderSystem.assertOnRenderThread();
		return GL20.glCreateShader(type);
	}

	public static void glShaderSource(int shader, String source) {
		RenderSystem.assertOnRenderThread();
		byte[] bs = source.getBytes(StandardCharsets.UTF_8);
		ByteBuffer byteBuffer = MemoryUtil.memAlloc(bs.length + 1);
		byteBuffer.put(bs);
		byteBuffer.put((byte) 0);
		byteBuffer.flip();

		try {
			MemoryStack memoryStack = MemoryStack.stackPush();

			try {
				PointerBuffer pointerBuffer = memoryStack.mallocPointer(1);
				pointerBuffer.put(byteBuffer);
				GL20C.nglShaderSource(shader, 1, pointerBuffer.address0(), 0L);
			}
			catch (Throwable var12) {
				if (memoryStack != null) {
					try {
						memoryStack.close();
					}
					catch (Throwable var11) {
						var12.addSuppressed(var11);
					}
				}

				throw var12;
			}

			if (memoryStack != null) {
				memoryStack.close();
			}
		}
		finally {
			MemoryUtil.memFree(byteBuffer);
		}
	}

	public static void glCompileShader(int shader) {
		RenderSystem.assertOnRenderThread();
		GL20.glCompileShader(shader);
	}

	public static int glGetShaderi(int shader, int pname) {
		RenderSystem.assertOnRenderThread();
		return GL20.glGetShaderi(shader, pname);
	}

	public static void _glUseProgram(int program) {
		RenderSystem.assertOnRenderThread();
		GL20.glUseProgram(program);
	}

	public static int glCreateProgram() {
		RenderSystem.assertOnRenderThread();
		return GL20.glCreateProgram();
	}

	public static void glDeleteProgram(int program) {
		RenderSystem.assertOnRenderThread();
		GL20.glDeleteProgram(program);
	}

	public static void glLinkProgram(int program) {
		RenderSystem.assertOnRenderThread();
		GL20.glLinkProgram(program);
	}

	public static int _glGetUniformLocation(int program, CharSequence name) {
		RenderSystem.assertOnRenderThread();
		return GL20.glGetUniformLocation(program, name);
	}

	public static void _glUniform1i(int location, int value) {
		RenderSystem.assertOnRenderThread();
		GL20.glUniform1i(location, value);
	}

	public static void _glBindAttribLocation(int program, int index, CharSequence name) {
		RenderSystem.assertOnRenderThread();
		GL20.glBindAttribLocation(program, index, name);
	}

	public static void incrementTrackedBuffers() {
		numBuffers++;
		PLOT_BUFFERS.setValue(numBuffers);
	}

	public static int _glGenBuffers() {
		RenderSystem.assertOnRenderThread();
		incrementTrackedBuffers();
		return GL15.glGenBuffers();
	}

	public static int _glGenVertexArrays() {
		RenderSystem.assertOnRenderThread();
		return GL30.glGenVertexArrays();
	}

	public static void _glBindBuffer(int target, int buffer) {
		RenderSystem.assertOnRenderThread();
		GL15.glBindBuffer(target, buffer);
	}

	public static void _glBindVertexArray(int array) {
		RenderSystem.assertOnRenderThread();
		GL30.glBindVertexArray(array);
	}

	public static void _glBufferData(int target, ByteBuffer data, int usage) {
		RenderSystem.assertOnRenderThread();
		GL15.glBufferData(target, data, usage);
	}

	public static void _glBufferSubData(int target, long offset, ByteBuffer data) {
		RenderSystem.assertOnRenderThread();
		GL15.glBufferSubData(target, offset, data);
	}

	public static void _glBufferData(int target, long size, int usage) {
		RenderSystem.assertOnRenderThread();
		GL15.glBufferData(target, size, usage);
	}

	public static @Nullable ByteBuffer _glMapBufferRange(int target, long offset, long range, int access) {
		RenderSystem.assertOnRenderThread();
		return GL30.glMapBufferRange(target, offset, range, access);
	}

	public static void _glUnmapBuffer(int target) {
		RenderSystem.assertOnRenderThread();
		GL15.glUnmapBuffer(target);
	}

	public static void _glDeleteBuffers(int buffer) {
		RenderSystem.assertOnRenderThread();
		numBuffers--;
		PLOT_BUFFERS.setValue(numBuffers);
		GL15.glDeleteBuffers(buffer);
	}

	public static void _glBindFramebuffer(int target, int framebuffer) {
		if ((target == 36008 || target == 36160) && readFbo != framebuffer) {
			GL30.glBindFramebuffer(36008, framebuffer);
			readFbo = framebuffer;
		}

		if ((target == 36009 || target == 36160) && writeFbo != framebuffer) {
			GL30.glBindFramebuffer(36009, framebuffer);
			writeFbo = framebuffer;
		}
	}

	public static int getFrameBuffer(int target) {
		if (target == 36008) {
			return readFbo;
		}
		else {
			return target == 36009 ? writeFbo : 0;
		}
	}

	public static void _glBlitFrameBuffer(
			int srcX0,
			int srcY0,
			int srcX1,
			int srcY1,
			int dstX0,
			int dstY0,
			int dstX1,
			int dstY1,
			int mask,
			int filter
	) {
		RenderSystem.assertOnRenderThread();
		GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
	}

	public static void _glDeleteFramebuffers(int framebuffer) {
		RenderSystem.assertOnRenderThread();
		GL30.glDeleteFramebuffers(framebuffer);
		if (readFbo == framebuffer) {
			readFbo = 0;
		}

		if (writeFbo == framebuffer) {
			writeFbo = 0;
		}
	}

	public static int glGenFramebuffers() {
		RenderSystem.assertOnRenderThread();
		return GL30.glGenFramebuffers();
	}

	public static void _glFramebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
		RenderSystem.assertOnRenderThread();
		GL30.glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
	}

	public static void glBlendFuncSeparate(int srcFactorRgb, int dstFactorRgb, int srcFactorAlpha, int dstFactorAlpha) {
		RenderSystem.assertOnRenderThread();
		GL14.glBlendFuncSeparate(srcFactorRgb, dstFactorRgb, srcFactorAlpha, dstFactorAlpha);
	}

	public static String glGetShaderInfoLog(int shader, int maxLength) {
		RenderSystem.assertOnRenderThread();
		return GL20.glGetShaderInfoLog(shader, maxLength);
	}

	public static String glGetProgramInfoLog(int program, int maxLength) {
		RenderSystem.assertOnRenderThread();
		return GL20.glGetProgramInfoLog(program, maxLength);
	}

	public static void _enableCull() {
		RenderSystem.assertOnRenderThread();
		CULL.capState.enable();
	}

	public static void _disableCull() {
		RenderSystem.assertOnRenderThread();
		CULL.capState.disable();
	}

	public static void _polygonMode(int face, int mode) {
		RenderSystem.assertOnRenderThread();
		GL11.glPolygonMode(face, mode);
	}

	public static void _enablePolygonOffset() {
		RenderSystem.assertOnRenderThread();
		POLY_OFFSET.capFill.enable();
	}

	public static void _disablePolygonOffset() {
		RenderSystem.assertOnRenderThread();
		POLY_OFFSET.capFill.disable();
	}

	public static void _polygonOffset(float factor, float units) {
		RenderSystem.assertOnRenderThread();
		if (factor != POLY_OFFSET.factor || units != POLY_OFFSET.units) {
			POLY_OFFSET.factor = factor;
			POLY_OFFSET.units = units;
			GL11.glPolygonOffset(factor, units);
		}
	}

	public static void _enableColorLogicOp() {
		RenderSystem.assertOnRenderThread();
		COLOR_LOGIC.capState.enable();
	}

	public static void _disableColorLogicOp() {
		RenderSystem.assertOnRenderThread();
		COLOR_LOGIC.capState.disable();
	}

	public static void _logicOp(int op) {
		RenderSystem.assertOnRenderThread();
		if (op != COLOR_LOGIC.op) {
			COLOR_LOGIC.op = op;
			GL11.glLogicOp(op);
		}
	}

	public static void _activeTexture(int texture) {
		RenderSystem.assertOnRenderThread();
		if (activeTexture != texture - 33984) {
			activeTexture = texture - 33984;
			GL13.glActiveTexture(texture);
		}
	}

	public static void _texParameter(int target, int pname, int param) {
		RenderSystem.assertOnRenderThread();
		GL11.glTexParameteri(target, pname, param);
	}

	public static int _getTexLevelParameter(int target, int level, int pname) {
		return GL11.glGetTexLevelParameteri(target, level, pname);
	}

	public static int _genTexture() {
		RenderSystem.assertOnRenderThread();
		numTextures++;
		PLOT_TEXTURES.setValue(numTextures);
		return GL11.glGenTextures();
	}

	public static void _deleteTexture(int texture) {
		RenderSystem.assertOnRenderThread();
		GL11.glDeleteTextures(texture);

		for (GlStateManager.Texture2DState texture2DState : TEXTURES) {
			if (texture2DState.boundTexture == texture) {
				texture2DState.boundTexture = -1;
			}
		}

		numTextures--;
		PLOT_TEXTURES.setValue(numTextures);
	}

	public static void _bindTexture(int texture) {
		RenderSystem.assertOnRenderThread();
		if (texture != TEXTURES[activeTexture].boundTexture) {
			TEXTURES[activeTexture].boundTexture = texture;
			GL11.glBindTexture(3553, texture);
		}
	}

	public static void _texImage2D(
			int target,
			int level,
			int internalFormat,
			int width,
			int height,
			int border,
			int format,
			int type,
			@Nullable ByteBuffer byteBuffer
	) {
		RenderSystem.assertOnRenderThread();
		GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, type, byteBuffer);
	}

	public static void _texSubImage2D(
			int target,
			int level,
			int offsetX,
			int offsetY,
			int width,
			int height,
			int format,
			int type,
			long pixels
	) {
		RenderSystem.assertOnRenderThread();
		GL11.glTexSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
	}

	public static void _texSubImage2D(
			int target,
			int level,
			int offsetX,
			int offsetY,
			int width,
			int height,
			int format,
			int type,
			ByteBuffer byteBuffer
	) {
		RenderSystem.assertOnRenderThread();
		GL11.glTexSubImage2D(target, level, offsetX, offsetY, width, height, format, type, byteBuffer);
	}

	public static void _viewport(int x, int y, int width, int height) {
		GL11.glViewport(x, y, width, height);
	}

	public static void _colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		RenderSystem.assertOnRenderThread();
		if (red != COLOR_MASK.red || green != COLOR_MASK.green || blue != COLOR_MASK.blue
				|| alpha != COLOR_MASK.alpha) {
			COLOR_MASK.red = red;
			COLOR_MASK.green = green;
			COLOR_MASK.blue = blue;
			COLOR_MASK.alpha = alpha;
			GL11.glColorMask(red, green, blue, alpha);
		}
	}

	public static void _clear(int mask) {
		RenderSystem.assertOnRenderThread();
		GL11.glClear(mask);
		if (MacWindowUtil.IS_MAC) {
			_getError();
		}
	}

	public static void _vertexAttribPointer(
			int index,
			int size,
			int type,
			boolean normalized,
			int stride,
			long pointer
	) {
		RenderSystem.assertOnRenderThread();
		GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
	}

	public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
		RenderSystem.assertOnRenderThread();
		GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
	}

	public static void _enableVertexAttribArray(int index) {
		RenderSystem.assertOnRenderThread();
		GL20.glEnableVertexAttribArray(index);
	}

	public static void _drawElements(int mode, int type, int count, long indices) {
		RenderSystem.assertOnRenderThread();
		GL11.glDrawElements(mode, type, count, indices);
	}

	public static void _drawArrays(int mode, int first, int count) {
		RenderSystem.assertOnRenderThread();
		GL11.glDrawArrays(mode, first, count);
	}

	public static void _pixelStore(int pname, int param) {
		RenderSystem.assertOnRenderThread();
		GL11.glPixelStorei(pname, param);
	}

	public static void _readPixels(int x, int y, int width, int height, int format, int type, long pixels) {
		RenderSystem.assertOnRenderThread();
		GL11.glReadPixels(x, y, width, height, format, type, pixels);
	}

	public static int _getError() {
		RenderSystem.assertOnRenderThread();
		return GL11.glGetError();
	}

	public static void clearGlErrors() {
		RenderSystem.assertOnRenderThread();

		while (GL11.glGetError() != 0) {
		}
	}

	public static String _getString(int name) {
		RenderSystem.assertOnRenderThread();
		return GL11.glGetString(name);
	}

	public static int _getInteger(int pname) {
		RenderSystem.assertOnRenderThread();
		return GL11.glGetInteger(pname);
	}

	public static long _glFenceSync(int condition, int flags) {
		RenderSystem.assertOnRenderThread();
		return GL32.glFenceSync(condition, flags);
	}

	public static int _glClientWaitSync(long sync, int flags, long timeout) {
		RenderSystem.assertOnRenderThread();
		return GL32.glClientWaitSync(sync, flags, timeout);
	}

	public static void _glDeleteSync(long sync) {
		RenderSystem.assertOnRenderThread();
		GL32.glDeleteSync(sync);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BlendFuncState}.
	 */
	static class BlendFuncState {

		public final GlStateManager.CapabilityTracker capState = new GlStateManager.CapabilityTracker(3042);
		public int srcFactorRgb = 1;
		public int dstFactorRgb = 0;
		public int srcFactorAlpha = 1;
		public int dstFactorAlpha = 0;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CapabilityTracker}.
	 */
	static class CapabilityTracker {

		private final int cap;
		private boolean state;

		public CapabilityTracker(int cap) {
			this.cap = cap;
		}

		public void disable() {
			this.setState(false);
		}

		public void enable() {
			this.setState(true);
		}

		public void setState(boolean state) {
			RenderSystem.assertOnRenderThread();
			if (state != this.state) {
				this.state = state;
				if (state) {
					GL11.glEnable(this.cap);
				}
				else {
					GL11.glDisable(this.cap);
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ColorMask}.
	 */
	static class ColorMask {

		public boolean red = true;
		public boolean green = true;
		public boolean blue = true;
		public boolean alpha = true;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CullFaceState}.
	 */
	static class CullFaceState {

		public final GlStateManager.CapabilityTracker capState = new GlStateManager.CapabilityTracker(2884);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DepthTestState}.
	 */
	static class DepthTestState {

		public final GlStateManager.CapabilityTracker capState = new GlStateManager.CapabilityTracker(2929);
		public boolean mask = true;
		public int func = 513;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LogicOpState}.
	 */
	static class LogicOpState {

		public final GlStateManager.CapabilityTracker capState = new GlStateManager.CapabilityTracker(3058);
		public int op = 5379;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code PolygonOffsetState}.
	 */
	static class PolygonOffsetState {

		public final GlStateManager.CapabilityTracker capFill = new GlStateManager.CapabilityTracker(32823);
		public float factor;
		public float units;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ScissorTestState}.
	 */
	static class ScissorTestState {

		public final GlStateManager.CapabilityTracker capState = new GlStateManager.CapabilityTracker(3089);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Texture2DState}.
	 */
	static class Texture2DState {

		public int boundTexture;
	}
}
