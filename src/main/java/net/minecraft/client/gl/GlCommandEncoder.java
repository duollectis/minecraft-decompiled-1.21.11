package net.minecraft.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Реализация {@link CommandEncoder} для OpenGL.
 * Транслирует высокоуровневые команды рендеринга в вызовы OpenGL API.
 * Управляет состоянием активного render pass и текущего шейдерного конвейера.
 */
@Environment(EnvType.CLIENT)
public class GlCommandEncoder implements CommandEncoder {

	private static final Logger LOGGER = LogUtils.getLogger();

	// GL-константы для операций с фреймбуферами и текстурами
	private static final int GL_FRAMEBUFFER = 36160;
	private static final int GL_READ_FRAMEBUFFER = 36008;
	private static final int GL_COLOR_ATTACHMENT0 = 36064;
	private static final int GL_DEPTH_ATTACHMENT = 36096;
	private static final int GL_TEXTURE_2D = 3553;
	private static final int GL_TEXTURE_CUBE_MAP = 34067;
	private static final int GL_TEXTURE_BUFFER = 35882;
	private static final int GL_UNIFORM_BUFFER = 35345;
	private static final int GL_PIXEL_PACK_BUFFER = 35051;
	private static final int GL_UNSIGNED_BYTE = 5121;
	private static final int GL_NEAREST = 9728;
	private static final int GL_TEXTURE_BASE_LEVEL = 33084;
	private static final int GL_TEXTURE_MAX_LEVEL = 33085;
	private static final int GL_TEXTURE_UNIT_0 = 33984;
	private static final int GL_UNPACK_ROW_LENGTH = 3314;
	private static final int GL_UNPACK_SKIP_PIXELS = 3316;
	private static final int GL_UNPACK_SKIP_ROWS = 3315;
	private static final int GL_UNPACK_ALIGNMENT = 3317;
	private static final int GL_PACK_ROW_LENGTH = 3330;
	private static final int GL_COLOR_BUFFER_BIT = 16384;
	private static final int GL_DEPTH_BUFFER_BIT = 256;
	private static final int GL_COLOR_AND_DEPTH_BUFFER_BIT = 16640;
	private static final int GL_TIME_ELAPSED = 35007;
	private static final int GL_FRONT_AND_BACK = 1032;
	private static final int GL_OR_REVERSE = 5387;

	private final GlBackend backend;
	private final int temporaryFb1;
	private final int temporaryFb2;
	private @Nullable RenderPipeline currentPipeline;
	private boolean renderPassOpen;
	private @Nullable ShaderProgram currentProgram;
	private @Nullable GlTimerQuery timerQuery;

	protected GlCommandEncoder(GlBackend backend) {
		this.backend = backend;
		temporaryFb1 = backend.getBufferManager().createFramebuffer();
		temporaryFb2 = backend.getBufferManager().createFramebuffer();
	}

	@Override
	public RenderPass createRenderPass(
		Supplier<String> labelSupplier,
		GpuTextureView colorTexture,
		OptionalInt clearColor
	) {
		return createRenderPass(labelSupplier, colorTexture, clearColor, null, OptionalDouble.empty());
	}

	/**
	 * Создаёт новый render pass с привязкой цветового и опционального глубинного вложений.
	 * Выполняет очистку буферов если переданы значения clearColor / clearDepth.
	 */
	@Override
	public RenderPass createRenderPass(
		Supplier<String> labelSupplier,
		GpuTextureView colorTexture,
		OptionalInt clearColor,
		@Nullable GpuTextureView depthTexture,
		OptionalDouble clearDepth
	) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}

		if (clearDepth.isPresent() && depthTexture == null) {
			LOGGER.warn("Depth clear value was provided but no depth texture is being used");
		}

		if (colorTexture.isClosed()) {
			throw new IllegalStateException("Color texture is closed");
		}

		if ((colorTexture.texture().usage() & 8) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
		}

		if (colorTexture.texture().getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
				"Textures with multiple depths or layers are not yet supported as an attachment"
			);
		}

		if (depthTexture != null) {
			if (depthTexture.isClosed()) {
				throw new IllegalStateException("Depth texture is closed");
			}

			if ((depthTexture.texture().usage() & 8) == 0) {
				throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
			}

			if (depthTexture.texture().getDepthOrLayers() > 1) {
				throw new UnsupportedOperationException(
					"Textures with multiple depths or layers are not yet supported as an attachment"
				);
			}
		}

		renderPassOpen = true;
		backend.getDebugLabelManager().pushDebugGroup(labelSupplier);

		int framebufferId = ((GlTextureView) colorTexture).getOrCreateFramebuffer(
			backend.getBufferManager(),
			depthTexture == null ? null : depthTexture.texture()
		);
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);

		int clearMask = 0;

		if (clearColor.isPresent()) {
			int color = clearColor.getAsInt();
			GL11.glClearColor(
				ColorHelper.getRedFloat(color),
				ColorHelper.getGreenFloat(color),
				ColorHelper.getBlueFloat(color),
				ColorHelper.getAlphaFloat(color)
			);
			clearMask |= GL_COLOR_BUFFER_BIT;
		}

		if (depthTexture != null && clearDepth.isPresent()) {
			GL11.glClearDepth(clearDepth.getAsDouble());
			clearMask |= GL_DEPTH_BUFFER_BIT;
		}

		if (clearMask != 0) {
			GlStateManager._disableScissorTest();
			GlStateManager._depthMask(true);
			GlStateManager._colorMask(true, true, true, true);
			GlStateManager._clear(clearMask);
		}

		GlStateManager._viewport(0, 0, colorTexture.getWidth(0), colorTexture.getHeight(0));
		currentPipeline = null;
		return new RenderPassImpl(this, depthTexture != null);
	}

	@Override
	public void clearColorTexture(GpuTexture texture, int color) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}

		validateColorAttachment(texture);
		backend.getBufferManager().setupFramebuffer(
			temporaryFb2, ((GlTexture) texture).glId, 0, 0, GL_FRAMEBUFFER
		);
		GL11.glClearColor(
			ColorHelper.getRedFloat(color),
			ColorHelper.getGreenFloat(color),
			ColorHelper.getBlueFloat(color),
			ColorHelper.getAlphaFloat(color)
		);
		GlStateManager._disableScissorTest();
		GlStateManager._colorMask(true, true, true, true);
		GlStateManager._clear(GL_COLOR_BUFFER_BIT);
		GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	@Override
	public void clearColorAndDepthTextures(GpuTexture colorTexture, int color, GpuTexture depthTexture, double depth) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}

		validateColorAttachment(colorTexture);
		validateDepthAttachment(depthTexture);
		int framebufferId = ((GlTexture) colorTexture).getOrCreateFramebuffer(
			backend.getBufferManager(), depthTexture
		);
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
		GlStateManager._disableScissorTest();
		GL11.glClearDepth(depth);
		GL11.glClearColor(
			ColorHelper.getRedFloat(color),
			ColorHelper.getGreenFloat(color),
			ColorHelper.getBlueFloat(color),
			ColorHelper.getAlphaFloat(color)
		);
		GlStateManager._depthMask(true);
		GlStateManager._colorMask(true, true, true, true);
		GlStateManager._clear(GL_COLOR_AND_DEPTH_BUFFER_BIT);
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	@Override
	public void clearColorAndDepthTextures(
		GpuTexture colorTexture,
		int color,
		GpuTexture depthTexture,
		double depth,
		int regionX,
		int regionY,
		int regionWidth,
		int regionHeight
	) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}

		validateColorAttachment(colorTexture);
		validateDepthAttachment(depthTexture);
		validateRegion(colorTexture, regionX, regionY, regionWidth, regionHeight);
		int framebufferId = ((GlTexture) colorTexture).getOrCreateFramebuffer(
			backend.getBufferManager(), depthTexture
		);
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
		GlStateManager._scissorBox(regionX, regionY, regionWidth, regionHeight);
		GlStateManager._enableScissorTest();
		GL11.glClearDepth(depth);
		GL11.glClearColor(
			ColorHelper.getRedFloat(color),
			ColorHelper.getGreenFloat(color),
			ColorHelper.getBlueFloat(color),
			ColorHelper.getAlphaFloat(color)
		);
		GlStateManager._depthMask(true);
		GlStateManager._colorMask(true, true, true, true);
		GlStateManager._clear(GL_COLOR_AND_DEPTH_BUFFER_BIT);
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	@Override
	public void clearDepthTexture(GpuTexture texture, double depth) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}

		validateDepthAttachment(texture);
		backend.getBufferManager().setupFramebuffer(
			temporaryFb2, 0, ((GlTexture) texture).glId, 0, GL_FRAMEBUFFER
		);
		GL11.glDrawBuffer(0);
		GL11.glClearDepth(depth);
		GlStateManager._depthMask(true);
		GlStateManager._disableScissorTest();
		GlStateManager._clear(GL_DEPTH_BUFFER_BIT);
		GL11.glDrawBuffer(GL_COLOR_ATTACHMENT0);
		GlStateManager._glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, 0, 0);
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	@Override
	public void writeToBuffer(GpuBufferSlice slice, ByteBuffer data) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		GlGpuBuffer glBuffer = (GlGpuBuffer) slice.buffer();

		if (glBuffer.closed) {
			throw new IllegalStateException("Buffer already closed");
		}

		if ((glBuffer.usage() & 8) == 0) {
			throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
		}

		int dataSize = data.remaining();

		if (dataSize > slice.length()) {
			throw new IllegalArgumentException(
				"Cannot write more data than the slice allows (attempting to write " + dataSize
					+ " bytes into a slice of length " + slice.length() + ")"
			);
		}

		if (slice.length() + slice.offset() > glBuffer.size()) {
			throw new IllegalArgumentException(
				"Cannot write more data than this buffer can hold (attempting to write "
					+ dataSize + " bytes at offset " + slice.offset()
					+ " to " + glBuffer.size() + " size buffer)"
			);
		}

		backend.getBufferManager().setBufferSubData(glBuffer.id, slice.offset(), data, glBuffer.usage());
	}

	@Override
	public GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write) {
		return mapBuffer(buffer.slice(), read, write);
	}

	@Override
	public GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		GlGpuBuffer glBuffer = (GlGpuBuffer) slice.buffer();

		if (glBuffer.closed) {
			throw new IllegalStateException("Buffer already closed");
		}

		if (!read && !write) {
			throw new IllegalArgumentException("At least read or write must be true");
		}

		if (read && (glBuffer.usage() & 1) == 0) {
			throw new IllegalStateException("Buffer is not readable");
		}

		if (write && (glBuffer.usage() & 2) == 0) {
			throw new IllegalStateException("Buffer is not writable");
		}

		if (slice.offset() + slice.length() > glBuffer.size()) {
			throw new IllegalArgumentException(
				"Cannot map more data than this buffer can hold (attempting to map "
					+ slice.length() + " bytes at offset " + slice.offset()
					+ " from " + glBuffer.size() + " size buffer)"
			);
		}

		int accessFlags = 0;

		if (read) {
			accessFlags |= 1;
		}

		if (write) {
			accessFlags |= 34;
		}

		return backend.getGpuBufferManager().mapBufferRange(
			backend.getBufferManager(),
			glBuffer,
			slice.offset(),
			slice.length(),
			accessFlags
		);
	}

	@Override
	public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice destination) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		GlGpuBuffer srcBuffer = (GlGpuBuffer) source.buffer();

		if (srcBuffer.closed) {
			throw new IllegalStateException("Source buffer already closed");
		}

		if ((srcBuffer.usage() & GpuBuffer.USAGE_COPY_SRC) == 0) {
			throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
		}

		GlGpuBuffer dstBuffer = (GlGpuBuffer) destination.buffer();

		if (dstBuffer.closed) {
			throw new IllegalStateException("Target buffer already closed");
		}

		if ((dstBuffer.usage() & 8) == 0) {
			throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
		}

		if (source.length() != destination.length()) {
			throw new IllegalArgumentException(
				"Cannot copy from slice of size " + source.length()
					+ " to slice of size " + destination.length() + ", they must be equal"
			);
		}

		if (source.offset() + source.length() > srcBuffer.size()) {
			throw new IllegalArgumentException(
				"Cannot copy more data than the source buffer holds (attempting to copy "
					+ source.length() + " bytes at offset " + source.offset()
					+ " from " + srcBuffer.size() + " size buffer)"
			);
		}

		if (destination.offset() + destination.length() > dstBuffer.size()) {
			throw new IllegalArgumentException(
				"Cannot copy more data than the target buffer can hold (attempting to copy "
					+ destination.length() + " bytes at offset " + destination.offset()
					+ " to " + dstBuffer.size() + " size buffer)"
			);
		}

		backend.getBufferManager().copyBufferSubData(
			srcBuffer.id,
			dstBuffer.id,
			source.offset(),
			destination.offset(),
			source.length()
		);
	}

	@Override
	public void writeToTexture(GpuTexture texture, NativeImage image) {
		int width = texture.getWidth(0);
		int height = texture.getHeight(0);

		if (image.getWidth() != width || image.getHeight() != height) {
			throw new IllegalArgumentException(
				"Cannot replace texture of size " + width + "x" + height
					+ " with image of size " + image.getWidth() + "x" + image.getHeight()
			);
		}

		if (texture.isClosed()) {
			throw new IllegalStateException("Destination texture is closed");
		}

		if ((texture.usage() & 1) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
		}

		writeToTexture(texture, image, 0, 0, 0, 0, width, height, 0, 0);
	}

	@Override
	public void writeToTexture(
		GpuTexture texture,
		NativeImage image,
		int mipLevel,
		int layer,
		int destX,
		int destY,
		int width,
		int height,
		int srcX,
		int srcY
	) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		if (mipLevel < 0 || mipLevel >= texture.getMipLevels()) {
			throw new IllegalArgumentException(
				"Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + texture.getMipLevels()
			);
		}

		if (srcX + width > image.getWidth() || srcY + height > image.getHeight()) {
			throw new IllegalArgumentException(
				"Copy source (" + image.getWidth() + "x" + image.getHeight()
					+ ") is not large enough to read a rectangle of "
					+ width + "x" + height + " from " + srcX + "x" + srcY
			);
		}

		if (destX + width > texture.getWidth(mipLevel) || destY + height > texture.getHeight(mipLevel)) {
			throw new IllegalArgumentException(
				"Dest texture (" + width + "x" + height
					+ ") is not large enough to write a rectangle of "
					+ width + "x" + height + " at " + destX + "x" + destY
					+ " (at mip level " + mipLevel + ")"
			);
		}

		if (texture.isClosed()) {
			throw new IllegalStateException("Destination texture is closed");
		}

		if ((texture.usage() & 1) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
		}

		if (layer >= texture.getDepthOrLayers()) {
			throw new UnsupportedOperationException(
				"Depth or layer is out of range, must be >= 0 and < " + texture.getDepthOrLayers()
			);
		}

		int target;

		if ((texture.usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) {
			target = GlConst.CUBEMAP_TARGETS[layer % 6];
			GL11.glBindTexture(GL_TEXTURE_CUBE_MAP, ((GlTexture) texture).glId);
		}
		else {
			target = GL_TEXTURE_2D;
			GlStateManager._bindTexture(((GlTexture) texture).glId);
		}

		GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, image.getWidth());
		GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, srcX);
		GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, srcY);
		GlStateManager._pixelStore(GL_UNPACK_ALIGNMENT, image.getFormat().getChannelCount());
		GlStateManager._texSubImage2D(
			target, mipLevel, destX, destY, width, height,
			GlConst.toGl(image.getFormat()), GL_UNSIGNED_BYTE, image.imageId()
		);
	}

	@Override
	public void writeToTexture(
		GpuTexture texture,
		ByteBuffer data,
		NativeImage.Format format,
		int mipLevel,
		int layer,
		int destX,
		int destY,
		int width,
		int height
	) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		if (mipLevel < 0 || mipLevel >= texture.getMipLevels()) {
			throw new IllegalArgumentException(
				"Invalid mipLevel, must be >= 0 and < " + texture.getMipLevels()
			);
		}

		if (width * height * format.getChannelCount() > data.remaining()) {
			throw new IllegalArgumentException(
				"Copy would overrun the source buffer (remaining length of "
					+ data.remaining() + ", but copy is "
					+ width + "x" + height + " of format " + format + ")"
			);
		}

		if (destX + width > texture.getWidth(mipLevel) || destY + height > texture.getHeight(mipLevel)) {
			throw new IllegalArgumentException(
				"Dest texture (" + texture.getWidth(mipLevel) + "x" + texture.getHeight(mipLevel)
					+ ") is not large enough to write a rectangle of "
					+ width + "x" + height + " at " + destX + "x" + destY
			);
		}

		if (texture.isClosed()) {
			throw new IllegalStateException("Destination texture is closed");
		}

		if ((texture.usage() & 1) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
		}

		if (layer >= texture.getDepthOrLayers()) {
			throw new UnsupportedOperationException(
				"Depth or layer is out of range, must be >= 0 and < " + texture.getDepthOrLayers()
			);
		}

		int target;

		if ((texture.usage() & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) {
			target = GlConst.CUBEMAP_TARGETS[layer % 6];
			GL11.glBindTexture(GL_TEXTURE_CUBE_MAP, ((GlTexture) texture).glId);
		}
		else {
			target = GL_TEXTURE_2D;
			GlStateManager._bindTexture(((GlTexture) texture).glId);
		}

		GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, width);
		GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
		GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
		GlStateManager._pixelStore(GL_UNPACK_ALIGNMENT, format.getChannelCount());
		GlStateManager._texSubImage2D(
			target, mipLevel, destX, destY, width, height,
			GlConst.toGl(format), GlConst.GL_UNSIGNED_BYTE, data
		);
	}

	@Override
	public void copyTextureToBuffer(GpuTexture texture, GpuBuffer buffer, long bufferOffset, Runnable callback, int mipLevel) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		copyTextureToBuffer(
			texture, buffer, bufferOffset, callback, mipLevel,
			0, 0, texture.getWidth(mipLevel), texture.getHeight(mipLevel)
		);
	}

	/**
	 * Копирует прямоугольную область текстуры в GPU-буфер через временный фреймбуфер.
	 * Использует {@code GL_READ_FRAMEBUFFER} и {@code glReadPixels} с PBO.
	 */
	@Override
	public void copyTextureToBuffer(
		GpuTexture texture,
		GpuBuffer buffer,
		long bufferOffset,
		Runnable callback,
		int mipLevel,
		int srcX,
		int srcY,
		int width,
		int height
	) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		if (mipLevel < 0 || mipLevel >= texture.getMipLevels()) {
			throw new IllegalArgumentException(
				"Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + texture.getMipLevels()
			);
		}

		if (texture.getWidth(mipLevel) * texture.getHeight(mipLevel) * texture.getFormat().pixelSize() + bufferOffset
			> buffer.size()
		) {
			throw new IllegalArgumentException(
				"Buffer of size " + buffer.size() + " is not large enough to hold "
					+ width + "x" + height + " pixels (" + texture.getFormat().pixelSize()
					+ " bytes each) starting from offset " + bufferOffset
			);
		}

		if ((texture.usage() & 2) == 0) {
			throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
		}

		if ((buffer.usage() & 8) == 0) {
			throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
		}

		if (srcX + width > texture.getWidth(mipLevel) || srcY + height > texture.getHeight(mipLevel)) {
			throw new IllegalArgumentException(
				"Copy source texture (" + texture.getWidth(mipLevel) + "x" + texture.getHeight(mipLevel)
					+ ") is not large enough to read a rectangle of "
					+ width + "x" + height + " from " + srcX + "," + srcY
			);
		}

		if (texture.isClosed()) {
			throw new IllegalStateException("Source texture is closed");
		}

		if (buffer.isClosed()) {
			throw new IllegalStateException("Destination buffer is closed");
		}

		if (texture.getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
				"Textures with multiple depths or layers are not yet supported for copying"
			);
		}

		GlStateManager.clearGlErrors();
		backend.getBufferManager().setupFramebuffer(
			temporaryFb1, ((GlTexture) texture).getGlId(), 0, mipLevel, GL_READ_FRAMEBUFFER
		);
		GlStateManager._glBindBuffer(GL_PIXEL_PACK_BUFFER, ((GlGpuBuffer) buffer).id);
		GlStateManager._pixelStore(GL_PACK_ROW_LENGTH, width);
		GlStateManager._readPixels(
			srcX, srcY, width, height,
			GlConst.toGlExternalId(texture.getFormat()),
			GlConst.toGlType(texture.getFormat()),
			bufferOffset
		);
		RenderSystem.queueFencedTask(callback);
		GlStateManager._glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, mipLevel);
		GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
		GlStateManager._glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

		int glError = GlStateManager._getError();

		if (glError != 0) {
			throw new IllegalStateException(
				"Couldn't perform copyTobuffer for texture " + texture.getLabel() + ": GL error " + glError
			);
		}
	}

	@Override
	public void copyTextureToTexture(
		GpuTexture source,
		GpuTexture destination,
		int mipLevel,
		int destX,
		int destY,
		int srcX,
		int srcY,
		int width,
		int height
	) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		if (mipLevel < 0 || mipLevel >= source.getMipLevels() || mipLevel >= destination.getMipLevels()) {
			throw new IllegalArgumentException(
				"Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + source.getMipLevels()
					+ " and < " + destination.getMipLevels()
			);
		}

		if (destX + width > destination.getWidth(mipLevel) || destY + height > destination.getHeight(mipLevel)) {
			throw new IllegalArgumentException(
				"Dest texture (" + destination.getWidth(mipLevel) + "x" + destination.getHeight(mipLevel)
					+ ") is not large enough to write a rectangle of "
					+ width + "x" + height + " at " + destX + "x" + destY
			);
		}

		if (srcX + width > source.getWidth(mipLevel) || srcY + height > source.getHeight(mipLevel)) {
			throw new IllegalArgumentException(
				"Source texture (" + source.getWidth(mipLevel) + "x" + source.getHeight(mipLevel)
					+ ") is not large enough to read a rectangle of "
					+ width + "x" + height + " at " + srcX + "x" + srcY
			);
		}

		if (source.isClosed()) {
			throw new IllegalStateException("Source texture is closed");
		}

		if (destination.isClosed()) {
			throw new IllegalStateException("Destination texture is closed");
		}

		if ((source.usage() & 2) == 0) {
			throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
		}

		if ((destination.usage() & 1) == 0) {
			throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
		}

		if (source.getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
				"Textures with multiple depths or layers are not yet supported for copying"
			);
		}

		if (destination.getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
				"Textures with multiple depths or layers are not yet supported for copying"
			);
		}

		GlStateManager.clearGlErrors();
		GlStateManager._disableScissorTest();

		boolean isDepth = source.getFormat().hasDepthAspect();
		int srcGlId = ((GlTexture) source).getGlId();
		int dstGlId = ((GlTexture) destination).getGlId();

		backend.getBufferManager().setupFramebuffer(
			temporaryFb1, isDepth ? 0 : srcGlId, isDepth ? srcGlId : 0, 0, 0
		);
		backend.getBufferManager().setupFramebuffer(
			temporaryFb2, isDepth ? 0 : dstGlId, isDepth ? dstGlId : 0, 0, 0
		);
		backend.getBufferManager().setupBlitFramebuffer(
			temporaryFb1, temporaryFb2,
			srcX, srcY, width, height,
			destX, destY, width, height,
			isDepth ? GL_DEPTH_BUFFER_BIT : GL_COLOR_BUFFER_BIT,
			GL_NEAREST
		);

		int glError = GlStateManager._getError();

		if (glError != 0) {
			throw new IllegalStateException(
				"Couldn't perform copyToTexture for texture " + source.getLabel()
					+ " to " + destination.getLabel() + ": GL error " + glError
			);
		}
	}

	@Override
	public void presentTexture(GpuTextureView textureView) {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		if (!textureView.texture().getFormat().hasColorAspect()) {
			throw new IllegalStateException("Cannot present a non-color texture!");
		}

		if ((textureView.texture().usage() & 8) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT to presented to the screen");
		}

		if (textureView.texture().getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
				"Textures with multiple depths or layers are not yet supported for presentation"
			);
		}

		GlStateManager._disableScissorTest();
		GlStateManager._viewport(0, 0, textureView.getWidth(0), textureView.getHeight(0));
		GlStateManager._depthMask(true);
		GlStateManager._colorMask(true, true, true, true);
		backend.getBufferManager().setupFramebuffer(
			temporaryFb2, ((GlTexture) textureView.texture()).getGlId(), 0, 0, 0
		);
		backend.getBufferManager().setupBlitFramebuffer(
			temporaryFb2, 0,
			0, 0, textureView.getWidth(0), textureView.getHeight(0),
			0, 0, textureView.getWidth(0), textureView.getHeight(0),
			GL_COLOR_BUFFER_BIT,
			GL_NEAREST
		);
	}

	@Override
	public GpuFence createFence() {
		if (renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}

		return new GlGpuFence();
	}

	protected <T> void drawObjectsWithRenderPass(
		RenderPassImpl pass,
		Collection<RenderPass.RenderObject<T>> objects,
		@Nullable GpuBuffer indexBuffer,
		VertexFormat.@Nullable IndexType indexType,
		Collection<String> validationSkippedUniforms,
		T object
	) {
		if (!setupRenderPass(pass, validationSkippedUniforms)) {
			return;
		}

		VertexFormat.IndexType effectiveIndexType =
			indexType == null ? VertexFormat.IndexType.SHORT : indexType;

		for (RenderPass.RenderObject<T> renderObject : objects) {
			VertexFormat.IndexType objectIndexType =
				renderObject.indexType() == null ? effectiveIndexType : renderObject.indexType();
			pass.setIndexBuffer(
				renderObject.indexBuffer() == null ? indexBuffer : renderObject.indexBuffer(),
				objectIndexType
			);
			pass.setVertexBuffer(renderObject.slot(), renderObject.vertexBuffer());

			if (RenderPassImpl.IS_DEVELOPMENT) {
				if (pass.indexBuffer == null) {
					throw new IllegalStateException("Missing index buffer");
				}

				if (pass.indexBuffer.isClosed()) {
					throw new IllegalStateException("Index buffer has been closed!");
				}

				if (pass.vertexBuffers[0] == null) {
					throw new IllegalStateException("Missing vertex buffer at slot 0");
				}

				if (pass.vertexBuffers[0].isClosed()) {
					throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
				}
			}

			BiConsumer<T, RenderPass.UniformUploader> uniformConsumer = renderObject.uniformUploaderConsumer();

			if (uniformConsumer != null) {
				uniformConsumer.accept(
					object, (name, gpuBufferSlice) -> {
						if (pass.pipeline.program().getUniform(name) instanceof GlUniform.UniformBuffer(int bindingIndex)) {
							GL32.glBindBufferRange(
								GL_UNIFORM_BUFFER,
								bindingIndex,
								((GlGpuBuffer) gpuBufferSlice.buffer()).id,
								gpuBufferSlice.offset(),
								gpuBufferSlice.length()
							);
						}
					}
				);
			}

			drawObjectWithRenderPass(
				pass, 0, renderObject.firstIndex(), renderObject.indexCount(),
				objectIndexType, pass.pipeline, 1
			);
		}
	}

	protected void drawBoundObjectWithRenderPass(
		RenderPassImpl pass,
		int baseVertex,
		int firstIndex,
		int count,
		VertexFormat.@Nullable IndexType indexType,
		int instanceCount
	) {
		if (!setupRenderPass(pass, Collections.emptyList())) {
			return;
		}

		if (RenderPassImpl.IS_DEVELOPMENT) {
			if (indexType != null) {
				if (pass.indexBuffer == null) {
					throw new IllegalStateException("Missing index buffer");
				}

				if (pass.indexBuffer.isClosed()) {
					throw new IllegalStateException("Index buffer has been closed!");
				}

				if ((pass.indexBuffer.usage() & GpuBuffer.USAGE_INDEX) == 0) {
					throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
				}
			}

			CompiledShaderPipeline compiledPipeline = pass.pipeline;

			if (pass.vertexBuffers[0] == null
				&& compiledPipeline != null
				&& !compiledPipeline.info().getVertexFormat().getElements().isEmpty()
			) {
				throw new IllegalStateException(
					"Vertex format contains elements but vertex buffer at slot 0 is null"
				);
			}

			if (pass.vertexBuffers[0] != null && pass.vertexBuffers[0].isClosed()) {
				throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
			}

			if (pass.vertexBuffers[0] != null && (pass.vertexBuffers[0].usage() & GpuBuffer.USAGE_VERTEX) == 0) {
				throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
			}
		}

		drawObjectWithRenderPass(pass, baseVertex, firstIndex, count, indexType, pass.pipeline, instanceCount);
	}

	private void drawObjectWithRenderPass(
		RenderPassImpl pass,
		int baseVertex,
		int firstIndex,
		int count,
		VertexFormat.@Nullable IndexType indexType,
		CompiledShaderPipeline pipeline,
		int instanceCount
	) {
		backend.getVertexBufferManager().setupBuffer(
			pipeline.info().getVertexFormat(), (GlGpuBuffer) pass.vertexBuffers[0]
		);

		if (indexType != null) {
			GlStateManager._glBindBuffer(34963, ((GlGpuBuffer) pass.indexBuffer).id);

			if (instanceCount > 1) {
				if (baseVertex > 0) {
					GL32.glDrawElementsInstancedBaseVertex(
						GlConst.toGl(pipeline.info().getVertexFormatMode()),
						count,
						GlConst.toGl(indexType),
						(long) firstIndex * indexType.size,
						instanceCount,
						baseVertex
					);
				}
				else {
					GL31.glDrawElementsInstanced(
						GlConst.toGl(pipeline.info().getVertexFormatMode()),
						count,
						GlConst.toGl(indexType),
						(long) firstIndex * indexType.size,
						instanceCount
					);
				}
			}
			else if (baseVertex > 0) {
				GL32.glDrawElementsBaseVertex(
					GlConst.toGl(pipeline.info().getVertexFormatMode()),
					count,
					GlConst.toGl(indexType),
					(long) firstIndex * indexType.size,
					baseVertex
				);
			}
			else {
				GlStateManager._drawElements(
					GlConst.toGl(pipeline.info().getVertexFormatMode()),
					count,
					GlConst.toGl(indexType),
					(long) firstIndex * indexType.size
				);
			}
		}
		else if (instanceCount > 1) {
			GL31.glDrawArraysInstanced(
				GlConst.toGl(pipeline.info().getVertexFormatMode()),
				baseVertex,
				count,
				instanceCount
			);
		}
		else {
			GlStateManager._drawArrays(GlConst.toGl(pipeline.info().getVertexFormatMode()), baseVertex, count);
		}
	}

	/**
		* Настраивает состояние OpenGL для выполнения draw-вызова: применяет конвейер, загружает юниформы,
		* привязывает текстуры и сэмплеры. Возвращает {@code false} если конвейер невалиден (в release-режиме).
		*/
	private boolean setupRenderPass(RenderPassImpl pass, Collection<String> validationSkippedUniforms) {
		if (RenderPassImpl.IS_DEVELOPMENT) {
			if (pass.pipeline == null) {
				throw new IllegalStateException("Can't draw without a render pipeline");
			}

			if (pass.pipeline.program() == ShaderProgram.INVALID) {
				throw new IllegalStateException("Pipeline contains invalid shader program");
			}

			for (RenderPipeline.UniformDescription uniform : pass.pipeline.info().getUniforms()) {
				GpuBufferSlice slice = pass.simpleUniforms.get(uniform.name());

				if (validationSkippedUniforms.contains(uniform.name())) {
					continue;
				}

				if (slice == null) {
					throw new IllegalStateException(
						"Missing uniform " + uniform.name() + " (should be " + uniform.type() + ")"
					);
				}

				if (uniform.type() == UniformType.UNIFORM_BUFFER) {
					if (slice.buffer().isClosed()) {
						throw new IllegalStateException("Uniform buffer " + uniform.name() + " is already closed");
					}

					if ((slice.buffer().usage() & GpuBuffer.USAGE_UNIFORM) == 0) {
						throw new IllegalStateException(
							"Uniform buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM"
						);
					}
				}

				if (uniform.type() == UniformType.TEXEL_BUFFER) {
					if (slice.offset() != 0L || slice.length() != slice.buffer().size()) {
						throw new IllegalStateException(
							"Uniform texel buffers do not support a slice of a buffer, must be entire buffer"
						);
					}

					if (uniform.textureFormat() == null) {
						throw new IllegalStateException(
							"Invalid uniform texel buffer " + uniform.name() + " (missing a texture format)"
						);
					}
				}
			}

			for (Entry<String, GlUniform> entry : pass.pipeline.program().getUniforms().entrySet()) {
				if (!(entry.getValue() instanceof GlUniform.Sampler)) {
					continue;
				}

				String samplerName = entry.getKey();
				RenderPassImpl.SamplerUniform samplerUniform = pass.samplerUniforms.get(samplerName);

				if (samplerUniform == null) {
					throw new IllegalStateException("Missing sampler " + samplerName);
				}

				GlTextureView textureView = samplerUniform.view();

				if (textureView.isClosed()) {
					throw new IllegalStateException(
						"Texture view " + samplerName + " (" + textureView.texture().getLabel() + ") has been closed!"
					);
				}

				if ((textureView.texture().usage() & 4) == 0) {
					throw new IllegalStateException(
						"Texture view " + samplerName + " (" + textureView.texture().getLabel()
							+ ") must have USAGE_TEXTURE_BINDING!"
					);
				}

				if (samplerUniform.sampler().isClosed()) {
					throw new IllegalStateException(
						"Sampler for " + samplerName + " (" + textureView.texture().getLabel() + ") has been closed!"
					);
				}
			}

			if (pass.pipeline.info().wantsDepthTexture() && !pass.hasDepth()) {
				LOGGER.warn(
					"Render pipeline {} wants a depth texture but none was provided - this is probably a bug",
					pass.pipeline.info().getLocation()
				);
			}
		}
		else if (pass.pipeline == null || pass.pipeline.program() == ShaderProgram.INVALID) {
			return false;
		}

		RenderPipeline renderPipeline = pass.pipeline.info();
		ShaderProgram shaderProgram = pass.pipeline.program();
		setPipelineAndApplyState(renderPipeline);

		boolean programChanged = currentProgram != shaderProgram;

		if (programChanged) {
			GlStateManager._glUseProgram(shaderProgram.getGlRef());
			currentProgram = shaderProgram;
		}

		for (Entry<String, GlUniform> entry : shaderProgram.getUniforms().entrySet()) {
			String uniformName = entry.getKey();
			boolean uniformChanged = pass.setSimpleUniforms.contains(uniformName);

			switch ((GlUniform) entry.getValue()) {
				case GlUniform.UniformBuffer(int bindingIndex) -> {
					if (uniformChanged) {
						GpuBufferSlice slice = pass.simpleUniforms.get(uniformName);
						GL32.glBindBufferRange(
							GL_UNIFORM_BUFFER,
							bindingIndex,
							((GlGpuBuffer) slice.buffer()).id,
							slice.offset(),
							slice.length()
						);
					}
				}
				case GlUniform.TexelBuffer(int location, int samplerIndex, TextureFormat format, int textureId) -> {
					if (programChanged || uniformChanged) {
						GlStateManager._glUniform1i(location, samplerIndex);
					}

					GlStateManager._activeTexture(GL_TEXTURE_UNIT_0 + samplerIndex);
					GL11C.glBindTexture(GL_TEXTURE_BUFFER, textureId);

					if (uniformChanged) {
						GpuBufferSlice slice = pass.simpleUniforms.get(uniformName);
						GL31.glTexBuffer(
							GL_TEXTURE_BUFFER,
							GlConst.toGlInternalId(format),
							((GlGpuBuffer) slice.buffer()).id
						);
					}
				}
				case GlUniform.Sampler(int location, int samplerIndex) -> {
					RenderPassImpl.SamplerUniform samplerUniform = pass.samplerUniforms.get(uniformName);

					if (samplerUniform == null) {
						break;
					}

					GlTextureView textureView = samplerUniform.view();

					if (programChanged || uniformChanged) {
						GlStateManager._glUniform1i(location, samplerIndex);
					}

					GlStateManager._activeTexture(GL_TEXTURE_UNIT_0 + samplerIndex);
					GlTexture glTexture = textureView.texture();
					int target;

					if ((glTexture.usage() & 16) != 0) {
						target = GL_TEXTURE_CUBE_MAP;
						GL11.glBindTexture(GL_TEXTURE_CUBE_MAP, glTexture.glId);
					}
					else {
						target = GL_TEXTURE_2D;
						GlStateManager._bindTexture(glTexture.glId);
					}

					GL33C.glBindSampler(samplerIndex, samplerUniform.sampler().getSamplerId());
					GlStateManager._texParameter(target, GL_TEXTURE_BASE_LEVEL, textureView.baseMipLevel());
					GlStateManager._texParameter(
						target,
						GL_TEXTURE_MAX_LEVEL,
						textureView.baseMipLevel() + textureView.mipLevels() - 1
					);
				}
				default -> throw new MatchException(null, null);
			}
		}

		pass.setSimpleUniforms.clear();

		if (pass.isScissorEnabled()) {
			GlStateManager._enableScissorTest();
			GlStateManager._scissorBox(
				pass.getScissorX(),
				pass.getScissorY(),
				pass.getScissorWidth(),
				pass.getScissorHeight()
			);
		}
		else {
			GlStateManager._disableScissorTest();
		}

		return true;
	}

	private void setPipelineAndApplyState(RenderPipeline pipeline) {
		if (currentPipeline == pipeline) {
			return;
		}

		currentPipeline = pipeline;

		if (pipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
			GlStateManager._enableDepthTest();
			GlStateManager._depthFunc(GlConst.toGl(pipeline.getDepthTestFunction()));
		}
		else {
			GlStateManager._disableDepthTest();
		}

		if (pipeline.isCull()) {
			GlStateManager._enableCull();
		}
		else {
			GlStateManager._disableCull();
		}

		if (pipeline.getBlendFunction().isPresent()) {
			GlStateManager._enableBlend();
			BlendFunction blend = pipeline.getBlendFunction().get();
			GlStateManager._blendFuncSeparate(
				GlConst.toGl(blend.sourceColor()),
				GlConst.toGl(blend.destColor()),
				GlConst.toGl(blend.sourceAlpha()),
				GlConst.toGl(blend.destAlpha())
			);
		}
		else {
			GlStateManager._disableBlend();
		}

		GlStateManager._polygonMode(GL_FRONT_AND_BACK, GlConst.toGl(pipeline.getPolygonMode()));
		GlStateManager._depthMask(pipeline.isWriteDepth());
		GlStateManager._colorMask(
			pipeline.isWriteColor(),
			pipeline.isWriteColor(),
			pipeline.isWriteColor(),
			pipeline.isWriteAlpha()
		);

		if (pipeline.getDepthBiasConstant() == 0.0F && pipeline.getDepthBiasScaleFactor() == 0.0F) {
			GlStateManager._disablePolygonOffset();
		}
		else {
			GlStateManager._polygonOffset(pipeline.getDepthBiasScaleFactor(), pipeline.getDepthBiasConstant());
			GlStateManager._enablePolygonOffset();
		}

		switch (pipeline.getColorLogic()) {
			case NONE -> GlStateManager._disableColorLogicOp();
			case OR_REVERSE -> {
				GlStateManager._enableColorLogicOp();
				GlStateManager._logicOp(GL_OR_REVERSE);
			}
		}
	}

	public void closePass() {
		renderPassOpen = false;
		GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
		backend.getDebugLabelManager().popDebugGroup();
	}

	protected GlBackend getBackend() {
		return backend;
	}

	@Override
	public GpuQuery timerQueryBegin() {
		RenderSystem.assertOnRenderThread();

		if (timerQuery != null) {
			throw new IllegalStateException("A GL_TIME_ELAPSED query is already active");
		}

		int queryId = GL32C.glGenQueries();
		GL32C.glBeginQuery(GL_TIME_ELAPSED, queryId);
		timerQuery = new GlTimerQuery(queryId);
		return timerQuery;
	}

	@Override
	public void timerQueryEnd(GpuQuery query) {
		RenderSystem.assertOnRenderThread();

		if (query != timerQuery) {
			throw new IllegalStateException("Mismatched or duplicate GpuQuery when ending timerQuery");
		}

		GL32C.glEndQuery(GL_TIME_ELAPSED);
		timerQuery = null;
	}

	private void validateRegion(GpuTexture texture, int regionX, int regionY, int regionWidth, int regionHeight) {
		if (regionX < 0 || regionX >= texture.getWidth(0)) {
			throw new IllegalArgumentException("regionX should not be outside of the texture");
		}

		if (regionY < 0 || regionY >= texture.getHeight(0)) {
			throw new IllegalArgumentException("regionY should not be outside of the texture");
		}

		if (regionWidth <= 0) {
			throw new IllegalArgumentException("regionWidth should be greater than 0");
		}

		if (regionX + regionWidth > texture.getWidth(0)) {
			throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
		}

		if (regionHeight <= 0) {
			throw new IllegalArgumentException("regionHeight should be greater than 0");
		}

		if (regionY + regionHeight > texture.getHeight(0)) {
			throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
		}
	}

	private void validateColorAttachment(GpuTexture texture) {
		if (!texture.getFormat().hasColorAspect()) {
			throw new IllegalStateException("Trying to clear a non-color texture as color");
		}

		if (texture.isClosed()) {
			throw new IllegalStateException("Color texture is closed");
		}

		if ((texture.usage() & 8) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
		}

		if (texture.getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
				"Clearing a texture with multiple layers or depths is not yet supported"
			);
		}
	}

	private void validateDepthAttachment(GpuTexture texture) {
		if (!texture.getFormat().hasDepthAspect()) {
			throw new IllegalStateException("Trying to clear a non-depth texture as depth");
		}

		if (texture.isClosed()) {
			throw new IllegalStateException("Depth texture is closed");
		}

		if ((texture.usage() & 8) == 0) {
			throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
		}

		if (texture.getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
				"Clearing a texture with multiple layers or depths is not yet supported"
			);
		}
	}
}
