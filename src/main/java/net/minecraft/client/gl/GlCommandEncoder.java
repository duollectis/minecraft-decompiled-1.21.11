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

@Environment(EnvType.CLIENT)
/**
 * {@code GlCommandEncoder}.
 */
public class GlCommandEncoder implements CommandEncoder {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final GlBackend backend;
	private final int temporaryFb1;
	private final int temporaryFb2;
	private @Nullable RenderPipeline currentPipeline;
	private boolean renderPassOpen;
	private @Nullable ShaderProgram currentProgram;
	private @Nullable GlTimerQuery timerQuery;

	protected GlCommandEncoder(GlBackend backend) {
		this.backend = backend;
		this.temporaryFb1 = backend.getBufferManager().createFramebuffer();
		this.temporaryFb2 = backend.getBufferManager().createFramebuffer();
	}

	@Override
	public RenderPass createRenderPass(
			Supplier<String> supplier,
			GpuTextureView gpuTextureView,
			OptionalInt optionalInt
	) {
		return this.createRenderPass(supplier, gpuTextureView, optionalInt, null, OptionalDouble.empty());
	}

	@Override
	public RenderPass createRenderPass(
			Supplier<String> supplier,
			GpuTextureView gpuTextureView,
			OptionalInt optionalInt,
			@Nullable GpuTextureView gpuTextureView2,
			OptionalDouble optionalDouble
	) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}
		else {
			if (optionalDouble.isPresent() && gpuTextureView2 == null) {
				LOGGER.warn("Depth clear value was provided but no depth texture is being used");
			}

			if (gpuTextureView.isClosed()) {
				throw new IllegalStateException("Color texture is closed");
			}
			else if ((gpuTextureView.texture().usage() & 8) == 0) {
				throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
			}
			else if (gpuTextureView.texture().getDepthOrLayers() > 1) {
				throw new UnsupportedOperationException(
						"Textures with multiple depths or layers are not yet supported as an attachment");
			}
			else {
				if (gpuTextureView2 != null) {
					if (gpuTextureView2.isClosed()) {
						throw new IllegalStateException("Depth texture is closed");
					}

					if ((gpuTextureView2.texture().usage() & 8) == 0) {
						throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
					}

					if (gpuTextureView2.texture().getDepthOrLayers() > 1) {
						throw new UnsupportedOperationException(
								"Textures with multiple depths or layers are not yet supported as an attachment");
					}
				}

				this.renderPassOpen = true;
				this.backend.getDebugLabelManager().pushDebugGroup(supplier);
				int i = ((GlTextureView) gpuTextureView)
						.getOrCreateFramebuffer(
								this.backend.getBufferManager(),
								gpuTextureView2 == null ? null : gpuTextureView2.texture()
						);
				GlStateManager._glBindFramebuffer(36160, i);
				int j = 0;
				if (optionalInt.isPresent()) {
					int k = optionalInt.getAsInt();
					GL11.glClearColor(
							ColorHelper.getRedFloat(k),
							ColorHelper.getGreenFloat(k),
							ColorHelper.getBlueFloat(k),
							ColorHelper.getAlphaFloat(k)
					);
					j |= 16384;
				}

				if (gpuTextureView2 != null && optionalDouble.isPresent()) {
					GL11.glClearDepth(optionalDouble.getAsDouble());
					j |= 256;
				}

				if (j != 0) {
					GlStateManager._disableScissorTest();
					GlStateManager._depthMask(true);
					GlStateManager._colorMask(true, true, true, true);
					GlStateManager._clear(j);
				}

				GlStateManager._viewport(0, 0, gpuTextureView.getWidth(0), gpuTextureView.getHeight(0));
				this.currentPipeline = null;
				return new RenderPassImpl(this, gpuTextureView2 != null);
			}
		}
	}

	@Override
	public void clearColorTexture(GpuTexture gpuTexture, int i) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}
		else {
			this.validateColorAttachment(gpuTexture);
			this.backend
					.getBufferManager()
					.setupFramebuffer(this.temporaryFb2, ((GlTexture) gpuTexture).glId, 0, 0, 36160);
			GL11.glClearColor(
					ColorHelper.getRedFloat(i),
					ColorHelper.getGreenFloat(i),
					ColorHelper.getBlueFloat(i),
					ColorHelper.getAlphaFloat(i)
			);
			GlStateManager._disableScissorTest();
			GlStateManager._colorMask(true, true, true, true);
			GlStateManager._clear(16384);
			GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
			GlStateManager._glBindFramebuffer(36160, 0);
		}
	}

	@Override
	public void clearColorAndDepthTextures(GpuTexture gpuTexture, int i, GpuTexture gpuTexture2, double d) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}
		else {
			this.validateColorAttachment(gpuTexture);
			this.validateDepthAttachment(gpuTexture2);
			int j = ((GlTexture) gpuTexture).getOrCreateFramebuffer(this.backend.getBufferManager(), gpuTexture2);
			GlStateManager._glBindFramebuffer(36160, j);
			GlStateManager._disableScissorTest();
			GL11.glClearDepth(d);
			GL11.glClearColor(
					ColorHelper.getRedFloat(i),
					ColorHelper.getGreenFloat(i),
					ColorHelper.getBlueFloat(i),
					ColorHelper.getAlphaFloat(i)
			);
			GlStateManager._depthMask(true);
			GlStateManager._colorMask(true, true, true, true);
			GlStateManager._clear(16640);
			GlStateManager._glBindFramebuffer(36160, 0);
		}
	}

	@Override
	public void clearColorAndDepthTextures(
			GpuTexture gpuTexture,
			int i,
			GpuTexture gpuTexture2,
			double d,
			int j,
			int k,
			int l,
			int m
	) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}
		else {
			this.validateColorAttachment(gpuTexture);
			this.validateDepthAttachment(gpuTexture2);
			this.validate(gpuTexture, j, k, l, m);
			int n = ((GlTexture) gpuTexture).getOrCreateFramebuffer(this.backend.getBufferManager(), gpuTexture2);
			GlStateManager._glBindFramebuffer(36160, n);
			GlStateManager._scissorBox(j, k, l, m);
			GlStateManager._enableScissorTest();
			GL11.glClearDepth(d);
			GL11.glClearColor(
					ColorHelper.getRedFloat(i),
					ColorHelper.getGreenFloat(i),
					ColorHelper.getBlueFloat(i),
					ColorHelper.getAlphaFloat(i)
			);
			GlStateManager._depthMask(true);
			GlStateManager._colorMask(true, true, true, true);
			GlStateManager._clear(16640);
			GlStateManager._glBindFramebuffer(36160, 0);
		}
	}

	private void validate(GpuTexture texture, int regionX, int regionY, int regionWidth, int regionHeight) {
		if (regionX < 0 || regionX >= texture.getWidth(0)) {
			throw new IllegalArgumentException("regionX should not be outside of the texture");
		}
		else if (regionY < 0 || regionY >= texture.getHeight(0)) {
			throw new IllegalArgumentException("regionY should not be outside of the texture");
		}
		else if (regionWidth <= 0) {
			throw new IllegalArgumentException("regionWidth should be greater than 0");
		}
		else if (regionX + regionWidth > texture.getWidth(0)) {
			throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
		}
		else if (regionHeight <= 0) {
			throw new IllegalArgumentException("regionHeight should be greater than 0");
		}
		else if (regionY + regionHeight > texture.getHeight(0)) {
			throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
		}
	}

	@Override
	public void clearDepthTexture(GpuTexture gpuTexture, double d) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before creating a new one!");
		}
		else {
			this.validateDepthAttachment(gpuTexture);
			this.backend
					.getBufferManager()
					.setupFramebuffer(this.temporaryFb2, 0, ((GlTexture) gpuTexture).glId, 0, 36160);
			GL11.glDrawBuffer(0);
			GL11.glClearDepth(d);
			GlStateManager._depthMask(true);
			GlStateManager._disableScissorTest();
			GlStateManager._clear(256);
			GL11.glDrawBuffer(36064);
			GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
			GlStateManager._glBindFramebuffer(36160, 0);
		}
	}

	private void validateColorAttachment(GpuTexture texture) {
		if (!texture.getFormat().hasColorAspect()) {
			throw new IllegalStateException("Trying to clear a non-color texture as color");
		}
		else if (texture.isClosed()) {
			throw new IllegalStateException("Color texture is closed");
		}
		else if ((texture.usage() & 8) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
		}
		else if (texture.getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
					"Clearing a texture with multiple layers or depths is not yet supported");
		}
	}

	private void validateDepthAttachment(GpuTexture texture) {
		if (!texture.getFormat().hasDepthAspect()) {
			throw new IllegalStateException("Trying to clear a non-depth texture as depth");
		}
		else if (texture.isClosed()) {
			throw new IllegalStateException("Depth texture is closed");
		}
		else if ((texture.usage() & 8) == 0) {
			throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
		}
		else if (texture.getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
					"Clearing a texture with multiple layers or depths is not yet supported");
		}
	}

	@Override
	public void writeToBuffer(GpuBufferSlice gpuBufferSlice, ByteBuffer byteBuffer) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else {
			GlGpuBuffer glGpuBuffer = (GlGpuBuffer) gpuBufferSlice.buffer();
			if (glGpuBuffer.closed) {
				throw new IllegalStateException("Buffer already closed");
			}
			else if ((glGpuBuffer.usage() & 8) == 0) {
				throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
			}
			else {
				int i = byteBuffer.remaining();
				if (i > gpuBufferSlice.length()) {
					throw new IllegalArgumentException(
							"Cannot write more data than the slice allows (attempting to write " + i
									+ " bytes into a slice of length " + gpuBufferSlice.length() + ")"
					);
				}
				else if (gpuBufferSlice.length() + gpuBufferSlice.offset() > glGpuBuffer.size()) {
					throw new IllegalArgumentException(
							"Cannot write more data than this buffer can hold (attempting to write "
									+ i
									+ " bytes at offset "
									+ gpuBufferSlice.offset()
									+ " to "
									+ glGpuBuffer.size()
									+ " size buffer)"
					);
				}
				else {
					this.backend
							.getBufferManager()
							.setBufferSubData(glGpuBuffer.id, gpuBufferSlice.offset(), byteBuffer, glGpuBuffer.usage());
				}
			}
		}
	}

	@Override
	public GpuBuffer.MappedView mapBuffer(GpuBuffer gpuBuffer, boolean bl, boolean bl2) {
		return this.mapBuffer(gpuBuffer.slice(), bl, bl2);
	}

	@Override
	public GpuBuffer.MappedView mapBuffer(GpuBufferSlice gpuBufferSlice, boolean bl, boolean bl2) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else {
			GlGpuBuffer glGpuBuffer = (GlGpuBuffer) gpuBufferSlice.buffer();
			if (glGpuBuffer.closed) {
				throw new IllegalStateException("Buffer already closed");
			}
			else if (!bl && !bl2) {
				throw new IllegalArgumentException("At least read or write must be true");
			}
			else if (bl && (glGpuBuffer.usage() & 1) == 0) {
				throw new IllegalStateException("Buffer is not readable");
			}
			else if (bl2 && (glGpuBuffer.usage() & 2) == 0) {
				throw new IllegalStateException("Buffer is not writable");
			}
			else if (gpuBufferSlice.offset() + gpuBufferSlice.length() > glGpuBuffer.size()) {
				throw new IllegalArgumentException(
						"Cannot map more data than this buffer can hold (attempting to map "
								+ gpuBufferSlice.length()
								+ " bytes at offset "
								+ gpuBufferSlice.offset()
								+ " from "
								+ glGpuBuffer.size()
								+ " size buffer)"
				);
			}
			else {
				int i = 0;
				if (bl) {
					i |= 1;
				}

				if (bl2) {
					i |= 34;
				}

				return this.backend
						.getGpuBufferManager()
						.mapBufferRange(
								this.backend.getBufferManager(),
								glGpuBuffer,
								gpuBufferSlice.offset(),
								gpuBufferSlice.length(),
								i
						);
			}
		}
	}

	@Override
	public void copyToBuffer(GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else {
			GlGpuBuffer glGpuBuffer = (GlGpuBuffer) gpuBufferSlice.buffer();
			if (glGpuBuffer.closed) {
				throw new IllegalStateException("Source buffer already closed");
			}
			else if ((glGpuBuffer.usage() & 16) == 0) {
				throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
			}
			else {
				GlGpuBuffer glGpuBuffer2 = (GlGpuBuffer) gpuBufferSlice2.buffer();
				if (glGpuBuffer2.closed) {
					throw new IllegalStateException("Target buffer already closed");
				}
				else if ((glGpuBuffer2.usage() & 8) == 0) {
					throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
				}
				else if (gpuBufferSlice.length() != gpuBufferSlice2.length()) {
					throw new IllegalArgumentException(
							"Cannot copy from slice of size " + gpuBufferSlice.length() + " to slice of size "
									+ gpuBufferSlice2.length() + ", they must be equal"
					);
				}
				else if (gpuBufferSlice.offset() + gpuBufferSlice.length() > glGpuBuffer.size()) {
					throw new IllegalArgumentException(
							"Cannot copy more data than the source buffer holds (attempting to copy "
									+ gpuBufferSlice.length()
									+ " bytes at offset "
									+ gpuBufferSlice.offset()
									+ " from "
									+ glGpuBuffer.size()
									+ " size buffer)"
					);
				}
				else if (gpuBufferSlice2.offset() + gpuBufferSlice2.length() > glGpuBuffer2.size()) {
					throw new IllegalArgumentException(
							"Cannot copy more data than the target buffer can hold (attempting to copy "
									+ gpuBufferSlice2.length()
									+ " bytes at offset "
									+ gpuBufferSlice2.offset()
									+ " to "
									+ glGpuBuffer2.size()
									+ " size buffer)"
					);
				}
				else {
					this.backend
							.getBufferManager()
							.copyBufferSubData(
									glGpuBuffer.id,
									glGpuBuffer2.id,
									gpuBufferSlice.offset(),
									gpuBufferSlice2.offset(),
									gpuBufferSlice.length()
							);
				}
			}
		}
	}

	@Override
	public void writeToTexture(GpuTexture gpuTexture, NativeImage nativeImage) {
		int i = gpuTexture.getWidth(0);
		int j = gpuTexture.getHeight(0);
		if (nativeImage.getWidth() != i || nativeImage.getHeight() != j) {
			throw new IllegalArgumentException(
					"Cannot replace texture of size " + i + "x" + j + " with image of size " + nativeImage.getWidth()
							+ "x" + nativeImage.getHeight()
			);
		}
		else if (gpuTexture.isClosed()) {
			throw new IllegalStateException("Destination texture is closed");
		}
		else if ((gpuTexture.usage() & 1) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
		}
		else {
			this.writeToTexture(gpuTexture, nativeImage, 0, 0, 0, 0, i, j, 0, 0);
		}
	}

	@Override
	public void writeToTexture(
			GpuTexture gpuTexture,
			NativeImage nativeImage,
			int i,
			int j,
			int k,
			int l,
			int m,
			int n,
			int o,
			int p
	) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else if (i >= 0 && i < gpuTexture.getMipLevels()) {
			if (o + m > nativeImage.getWidth() || p + n > nativeImage.getHeight()) {
				throw new IllegalArgumentException(
						"Copy source ("
								+ nativeImage.getWidth()
								+ "x"
								+ nativeImage.getHeight()
								+ ") is not large enough to read a rectangle of "
								+ m
								+ "x"
								+ n
								+ " from "
								+ o
								+ "x"
								+ p
				);
			}
			else if (k + m > gpuTexture.getWidth(i) || l + n > gpuTexture.getHeight(i)) {
				throw new IllegalArgumentException(
						"Dest texture ("
								+ m
								+ "x"
								+ n
								+ ") is not large enough to write a rectangle of "
								+ m
								+ "x"
								+ n
								+ " at "
								+ k
								+ "x"
								+ l
								+ " (at mip level "
								+ i
								+ ")"
				);
			}
			else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Destination texture is closed");
			}
			else if ((gpuTexture.usage() & 1) == 0) {
				throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
			}
			else if (j >= gpuTexture.getDepthOrLayers()) {
				throw new UnsupportedOperationException(
						"Depth or layer is out of range, must be >= 0 and < " + gpuTexture.getDepthOrLayers());
			}
			else {
				int q;
				if ((gpuTexture.usage() & 16) != 0) {
					q = GlConst.CUBEMAP_TARGETS[j % 6];
					GL11.glBindTexture(34067, ((GlTexture) gpuTexture).glId);
				}
				else {
					q = 3553;
					GlStateManager._bindTexture(((GlTexture) gpuTexture).glId);
				}

				GlStateManager._pixelStore(3314, nativeImage.getWidth());
				GlStateManager._pixelStore(3316, o);
				GlStateManager._pixelStore(3315, p);
				GlStateManager._pixelStore(3317, nativeImage.getFormat().getChannelCount());
				GlStateManager._texSubImage2D(
						q,
						i,
						k,
						l,
						m,
						n,
						GlConst.toGl(nativeImage.getFormat()),
						5121,
						nativeImage.imageId()
				);
			}
		}
		else {
			throw new IllegalArgumentException(
					"Invalid mipLevel " + i + ", must be >= 0 and < " + gpuTexture.getMipLevels());
		}
	}

	@Override
	public void writeToTexture(
			GpuTexture gpuTexture,
			ByteBuffer byteBuffer,
			NativeImage.Format format,
			int i,
			int j,
			int k,
			int l,
			int m,
			int n
	) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else if (i >= 0 && i < gpuTexture.getMipLevels()) {
			if (m * n * format.getChannelCount() > byteBuffer.remaining()) {
				throw new IllegalArgumentException(
						"Copy would overrun the source buffer (remaining length of "
								+ byteBuffer.remaining()
								+ ", but copy is "
								+ m
								+ "x"
								+ n
								+ " of format "
								+ format
								+ ")"
				);
			}
			else if (k + m > gpuTexture.getWidth(i) || l + n > gpuTexture.getHeight(i)) {
				throw new IllegalArgumentException(
						"Dest texture ("
								+ gpuTexture.getWidth(i)
								+ "x"
								+ gpuTexture.getHeight(i)
								+ ") is not large enough to write a rectangle of "
								+ m
								+ "x"
								+ n
								+ " at "
								+ k
								+ "x"
								+ l
				);
			}
			else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Destination texture is closed");
			}
			else if ((gpuTexture.usage() & 1) == 0) {
				throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
			}
			else if (j >= gpuTexture.getDepthOrLayers()) {
				throw new UnsupportedOperationException(
						"Depth or layer is out of range, must be >= 0 and < " + gpuTexture.getDepthOrLayers());
			}
			else {
				int o;
				if ((gpuTexture.usage() & 16) != 0) {
					o = GlConst.CUBEMAP_TARGETS[j % 6];
					GL11.glBindTexture(34067, ((GlTexture) gpuTexture).glId);
				}
				else {
					o = 3553;
					GlStateManager._bindTexture(((GlTexture) gpuTexture).glId);
				}

				GlStateManager._pixelStore(3314, m);
				GlStateManager._pixelStore(3316, 0);
				GlStateManager._pixelStore(3315, 0);
				GlStateManager._pixelStore(3317, format.getChannelCount());
				GlStateManager._texSubImage2D(o, i, k, l, m, n, GlConst.toGl(format), 5121, byteBuffer);
			}
		}
		else {
			throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + gpuTexture.getMipLevels());
		}
	}

	@Override
	public void copyTextureToBuffer(GpuTexture gpuTexture, GpuBuffer gpuBuffer, long l, Runnable runnable, int i) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else {
			this.copyTextureToBuffer(
					gpuTexture,
					gpuBuffer,
					l,
					runnable,
					i,
					0,
					0,
					gpuTexture.getWidth(i),
					gpuTexture.getHeight(i)
			);
		}
	}

	@Override
	public void copyTextureToBuffer(
			GpuTexture gpuTexture,
			GpuBuffer gpuBuffer,
			long l,
			Runnable runnable,
			int i,
			int j,
			int k,
			int m,
			int n
	) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else if (i >= 0 && i < gpuTexture.getMipLevels()) {
			if (gpuTexture.getWidth(i) * gpuTexture.getHeight(i) * gpuTexture.getFormat().pixelSize() + l
					> gpuBuffer.size()) {
				throw new IllegalArgumentException(
						"Buffer of size "
								+ gpuBuffer.size()
								+ " is not large enough to hold "
								+ m
								+ "x"
								+ n
								+ " pixels ("
								+ gpuTexture.getFormat().pixelSize()
								+ " bytes each) starting from offset "
								+ l
				);
			}
			else if ((gpuTexture.usage() & 2) == 0) {
				throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
			}
			else if ((gpuBuffer.usage() & 8) == 0) {
				throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
			}
			else if (j + m > gpuTexture.getWidth(i) || k + n > gpuTexture.getHeight(i)) {
				throw new IllegalArgumentException(
						"Copy source texture ("
								+ gpuTexture.getWidth(i)
								+ "x"
								+ gpuTexture.getHeight(i)
								+ ") is not large enough to read a rectangle of "
								+ m
								+ "x"
								+ n
								+ " from "
								+ j
								+ ","
								+ k
				);
			}
			else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Source texture is closed");
			}
			else if (gpuBuffer.isClosed()) {
				throw new IllegalStateException("Destination buffer is closed");
			}
			else if (gpuTexture.getDepthOrLayers() > 1) {
				throw new UnsupportedOperationException(
						"Textures with multiple depths or layers are not yet supported for copying");
			}
			else {
				GlStateManager.clearGlErrors();
				this.backend
						.getBufferManager()
						.setupFramebuffer(this.temporaryFb1, ((GlTexture) gpuTexture).getGlId(), 0, i, 36008);
				GlStateManager._glBindBuffer(35051, ((GlGpuBuffer) gpuBuffer).id);
				GlStateManager._pixelStore(3330, m);
				GlStateManager._readPixels(
						j,
						k,
						m,
						n,
						GlConst.toGlExternalId(gpuTexture.getFormat()),
						GlConst.toGlType(gpuTexture.getFormat()),
						l
				);
				RenderSystem.queueFencedTask(runnable);
				GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, i);
				GlStateManager._glBindFramebuffer(36008, 0);
				GlStateManager._glBindBuffer(35051, 0);
				int o = GlStateManager._getError();
				if (o != 0) {
					throw new IllegalStateException(
							"Couldn't perform copyTobuffer for texture " + gpuTexture.getLabel() + ": GL error " + o);
				}
			}
		}
		else {
			throw new IllegalArgumentException(
					"Invalid mipLevel " + i + ", must be >= 0 and < " + gpuTexture.getMipLevels());
		}
	}

	@Override
	public void copyTextureToTexture(
			GpuTexture gpuTexture,
			GpuTexture gpuTexture2,
			int i,
			int j,
			int k,
			int l,
			int m,
			int n,
			int o
	) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else if (i >= 0 && i < gpuTexture.getMipLevels() && i < gpuTexture2.getMipLevels()) {
			if (j + n > gpuTexture2.getWidth(i) || k + o > gpuTexture2.getHeight(i)) {
				throw new IllegalArgumentException(
						"Dest texture ("
								+ gpuTexture2.getWidth(i)
								+ "x"
								+ gpuTexture2.getHeight(i)
								+ ") is not large enough to write a rectangle of "
								+ n
								+ "x"
								+ o
								+ " at "
								+ j
								+ "x"
								+ k
				);
			}
			else if (l + n > gpuTexture.getWidth(i) || m + o > gpuTexture.getHeight(i)) {
				throw new IllegalArgumentException(
						"Source texture ("
								+ gpuTexture.getWidth(i)
								+ "x"
								+ gpuTexture.getHeight(i)
								+ ") is not large enough to read a rectangle of "
								+ n
								+ "x"
								+ o
								+ " at "
								+ l
								+ "x"
								+ m
				);
			}
			else if (gpuTexture.isClosed()) {
				throw new IllegalStateException("Source texture is closed");
			}
			else if (gpuTexture2.isClosed()) {
				throw new IllegalStateException("Destination texture is closed");
			}
			else if ((gpuTexture.usage() & 2) == 0) {
				throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
			}
			else if ((gpuTexture2.usage() & 1) == 0) {
				throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
			}
			else if (gpuTexture.getDepthOrLayers() > 1) {
				throw new UnsupportedOperationException(
						"Textures with multiple depths or layers are not yet supported for copying");
			}
			else if (gpuTexture2.getDepthOrLayers() > 1) {
				throw new UnsupportedOperationException(
						"Textures with multiple depths or layers are not yet supported for copying");
			}
			else {
				GlStateManager.clearGlErrors();
				GlStateManager._disableScissorTest();
				boolean bl = gpuTexture.getFormat().hasDepthAspect();
				int p = ((GlTexture) gpuTexture).getGlId();
				int q = ((GlTexture) gpuTexture2).getGlId();
				this.backend.getBufferManager().setupFramebuffer(this.temporaryFb1, bl ? 0 : p, bl ? p : 0, 0, 0);
				this.backend.getBufferManager().setupFramebuffer(this.temporaryFb2, bl ? 0 : q, bl ? q : 0, 0, 0);
				this.backend
						.getBufferManager()
						.setupBlitFramebuffer(
								this.temporaryFb1,
								this.temporaryFb2,
								l,
								m,
								n,
								o,
								j,
								k,
								n,
								o,
								bl ? 256 : 16384,
								9728
						);
				int r = GlStateManager._getError();
				if (r != 0) {
					throw new IllegalStateException(
							"Couldn't perform copyToTexture for texture " + gpuTexture.getLabel() + " to "
									+ gpuTexture2.getLabel() + ": GL error " + r
					);
				}
			}
		}
		else {
			throw new IllegalArgumentException(
					"Invalid mipLevel " + i + ", must be >= 0 and < " + gpuTexture.getMipLevels() + " and < "
							+ gpuTexture2.getMipLevels()
			);
		}
	}

	@Override
	public void presentTexture(GpuTextureView gpuTextureView) {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else if (!gpuTextureView.texture().getFormat().hasColorAspect()) {
			throw new IllegalStateException("Cannot present a non-color texture!");
		}
		else if ((gpuTextureView.texture().usage() & 8) == 0) {
			throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT to presented to the screen");
		}
		else if (gpuTextureView.texture().getDepthOrLayers() > 1) {
			throw new UnsupportedOperationException(
					"Textures with multiple depths or layers are not yet supported for presentation");
		}
		else {
			GlStateManager._disableScissorTest();
			GlStateManager._viewport(0, 0, gpuTextureView.getWidth(0), gpuTextureView.getHeight(0));
			GlStateManager._depthMask(true);
			GlStateManager._colorMask(true, true, true, true);
			this.backend
					.getBufferManager()
					.setupFramebuffer(this.temporaryFb2, ((GlTexture) gpuTextureView.texture()).getGlId(), 0, 0, 0);
			this.backend
					.getBufferManager()
					.setupBlitFramebuffer(
							this.temporaryFb2,
							0,
							0,
							0,
							gpuTextureView.getWidth(0),
							gpuTextureView.getHeight(0),
							0,
							0,
							gpuTextureView.getWidth(0),
							gpuTextureView.getHeight(0),
							16384,
							9728
					);
		}
	}

	@Override
	public GpuFence createFence() {
		if (this.renderPassOpen) {
			throw new IllegalStateException("Close the existing render pass before performing additional commands");
		}
		else {
			return new GlGpuFence();
		}
	}

	protected <T> void drawObjectsWithRenderPass(
			RenderPassImpl pass,
			Collection<RenderPass.RenderObject<T>> objects,
			@Nullable GpuBuffer indexBuffer,
			VertexFormat.@Nullable IndexType indexType,
			Collection<String> validationSkippedUniforms,
			T object
	) {
		if (this.setupRenderPass(pass, validationSkippedUniforms)) {
			if (indexType == null) {
				indexType = VertexFormat.IndexType.SHORT;
			}

			for (RenderPass.RenderObject<T> renderObject : objects) {
				VertexFormat.IndexType
						indexType2 =
						renderObject.indexType() == null ? indexType : renderObject.indexType();
				pass.setIndexBuffer(
						renderObject.indexBuffer() == null ? indexBuffer : renderObject.indexBuffer(),
						indexType2
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

				BiConsumer<T, RenderPass.UniformUploader> biConsumer = renderObject.uniformUploaderConsumer();
				if (biConsumer != null) {
					biConsumer.accept(
							object, (name, gpuBufferSlice) -> {
								if (pass.pipeline
										.program()
										.getUniform(name) instanceof GlUniform.UniformBuffer(int i)) {
									GL32.glBindBufferRange(
											35345,
											i,
											((GlGpuBuffer) gpuBufferSlice.buffer()).id,
											gpuBufferSlice.offset(),
											gpuBufferSlice.length()
									);
								}
							}
					);
				}

				this.drawObjectWithRenderPass(
						pass,
						0,
						renderObject.firstIndex(),
						renderObject.indexCount(),
						indexType2,
						pass.pipeline,
						1
				);
			}
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
		if (this.setupRenderPass(pass, Collections.emptyList())) {
			if (RenderPassImpl.IS_DEVELOPMENT) {
				if (indexType != null) {
					if (pass.indexBuffer == null) {
						throw new IllegalStateException("Missing index buffer");
					}

					if (pass.indexBuffer.isClosed()) {
						throw new IllegalStateException("Index buffer has been closed!");
					}

					if ((pass.indexBuffer.usage() & 64) == 0) {
						throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
					}
				}

				CompiledShaderPipeline compiledShaderPipeline = pass.pipeline;
				if (pass.vertexBuffers[0] == null && compiledShaderPipeline != null && !compiledShaderPipeline
						.info()
						.getVertexFormat()
						.getElements()
						.isEmpty()) {
					throw new IllegalStateException(
							"Vertex format contains elements but vertex buffer at slot 0 is null");
				}

				if (pass.vertexBuffers[0] != null && pass.vertexBuffers[0].isClosed()) {
					throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
				}

				if (pass.vertexBuffers[0] != null && (pass.vertexBuffers[0].usage() & 32) == 0) {
					throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
				}
			}

			this.drawObjectWithRenderPass(pass, baseVertex, firstIndex, count, indexType, pass.pipeline, instanceCount);
		}
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
		this.backend
				.getVertexBufferManager()
				.setupBuffer(pipeline.info().getVertexFormat(), (GlGpuBuffer) pass.vertexBuffers[0]);
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

	private boolean setupRenderPass(RenderPassImpl pass, Collection<String> validationSkippedUniforms) {
		if (RenderPassImpl.IS_DEVELOPMENT) {
			if (pass.pipeline == null) {
				throw new IllegalStateException("Can't draw without a render pipeline");
			}

			if (pass.pipeline.program() == ShaderProgram.INVALID) {
				throw new IllegalStateException("Pipeline contains invalid shader program");
			}

			for (RenderPipeline.UniformDescription uniformDescription : pass.pipeline.info().getUniforms()) {
				GpuBufferSlice gpuBufferSlice = pass.simpleUniforms.get(uniformDescription.name());
				if (!validationSkippedUniforms.contains(uniformDescription.name())) {
					if (gpuBufferSlice == null) {
						throw new IllegalStateException("Missing uniform " + uniformDescription.name() + " (should be "
								+ uniformDescription.type() + ")");
					}

					if (uniformDescription.type() == UniformType.UNIFORM_BUFFER) {
						if (gpuBufferSlice.buffer().isClosed()) {
							throw new IllegalStateException(
									"Uniform buffer " + uniformDescription.name() + " is already closed");
						}

						if ((gpuBufferSlice.buffer().usage() & 128) == 0) {
							throw new IllegalStateException("Uniform buffer " + uniformDescription.name()
									+ " must have GpuBuffer.USAGE_UNIFORM");
						}
					}

					if (uniformDescription.type() == UniformType.TEXEL_BUFFER) {
						if (gpuBufferSlice.offset() != 0L || gpuBufferSlice.length() != gpuBufferSlice
								.buffer()
								.size()) {
							throw new IllegalStateException(
									"Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
						}

						if (uniformDescription.textureFormat() == null) {
							throw new IllegalStateException("Invalid uniform texel buffer " + uniformDescription.name()
									+ " (missing a texture format)");
						}
					}
				}
			}

			for (Entry<String, GlUniform> entry : pass.pipeline.program().getUniforms().entrySet()) {
				if (entry.getValue() instanceof GlUniform.Sampler) {
					String string = entry.getKey();
					RenderPassImpl.SamplerUniform samplerUniform = pass.samplerUniforms.get(string);
					if (samplerUniform == null) {
						throw new IllegalStateException("Missing sampler " + string);
					}

					GlTextureView glTextureView = samplerUniform.view();
					if (glTextureView.isClosed()) {
						throw new IllegalStateException(
								"Texture view " + string + " (" + glTextureView.texture().getLabel()
										+ ") has been closed!");
					}

					if ((glTextureView.texture().usage() & 4) == 0) {
						throw new IllegalStateException(
								"Texture view " + string + " (" + glTextureView.texture().getLabel()
										+ ") must have USAGE_TEXTURE_BINDING!");
					}

					if (samplerUniform.sampler().isClosed()) {
						throw new IllegalStateException(
								"Sampler for " + string + " (" + glTextureView.texture().getLabel()
										+ ") has been closed!");
					}
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
		this.setPipelineAndApplyState(renderPipeline);
		boolean bl = this.currentProgram != shaderProgram;
		if (bl) {
			GlStateManager._glUseProgram(shaderProgram.getGlRef());
			this.currentProgram = shaderProgram;
		}

		for (Entry<String, GlUniform> entry2 : shaderProgram.getUniforms().entrySet()) {
			String string2 = entry2.getKey();
			boolean bl2 = pass.setSimpleUniforms.contains(string2);
			switch ((GlUniform) entry2.getValue()) {
				case GlUniform.UniformBuffer(int var63):
					int var41 = var63;
					if (bl2) {
						GpuBufferSlice gpuBufferSlice2 = pass.simpleUniforms.get(string2);
						GL32.glBindBufferRange(
								35345,
								var41,
								((GlGpuBuffer) gpuBufferSlice2.buffer()).id,
								gpuBufferSlice2.offset(),
								gpuBufferSlice2.length()
						);
					}
					break;
				case GlUniform.TexelBuffer(int var43, int var44, TextureFormat var45, int var61):
					int var46 = var61;
					if (bl || bl2) {
						GlStateManager._glUniform1i(var43, var44);
					}

					GlStateManager._activeTexture(33984 + var44);
					GL11C.glBindTexture(35882, var46);
					if (bl2) {
						GpuBufferSlice gpuBufferSlice3 = pass.simpleUniforms.get(string2);
						GL31.glTexBuffer(
								35882,
								GlConst.toGlInternalId(var45),
								((GlGpuBuffer) gpuBufferSlice3.buffer()).id
						);
					}
					break;
				case GlUniform.Sampler(int samplerUniform2, int var53):
					int var48 = var53;
					RenderPassImpl.SamplerUniform samplerUniform2x = pass.samplerUniforms.get(string2);
					if (samplerUniform2x == null) {
						break;
					}

					GlTextureView glTextureView2 = samplerUniform2x.view();
					if (bl || bl2) {
						GlStateManager._glUniform1i(samplerUniform2, var48);
					}

					GlStateManager._activeTexture(33984 + var48);
					GlTexture glTexture = glTextureView2.texture();
					int o;
					if ((glTexture.usage() & 16) != 0) {
						o = 34067;
						GL11.glBindTexture(34067, glTexture.glId);
					}
					else {
						o = 3553;
						GlStateManager._bindTexture(glTexture.glId);
					}

					GL33C.glBindSampler(var48, samplerUniform2x.sampler().getSamplerId());
					GlStateManager._texParameter(o, 33084, glTextureView2.baseMipLevel());
					GlStateManager._texParameter(
							o,
							33085,
							glTextureView2.baseMipLevel() + glTextureView2.mipLevels() - 1
					);
					break;
				default:
					throw new MatchException(null, null);
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
		if (this.currentPipeline != pipeline) {
			this.currentPipeline = pipeline;
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
				BlendFunction blendFunction = pipeline.getBlendFunction().get();
				GlStateManager._blendFuncSeparate(
						GlConst.toGl(blendFunction.sourceColor()),
						GlConst.toGl(blendFunction.destColor()),
						GlConst.toGl(blendFunction.sourceAlpha()),
						GlConst.toGl(blendFunction.destAlpha())
				);
			}
			else {
				GlStateManager._disableBlend();
			}

			GlStateManager._polygonMode(1032, GlConst.toGl(pipeline.getPolygonMode()));
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
				case NONE:
					GlStateManager._disableColorLogicOp();
					break;
				case OR_REVERSE:
					GlStateManager._enableColorLogicOp();
					GlStateManager._logicOp(5387);
			}
		}
	}

	/**
	 * Закрывает pass.
	 */
	public void closePass() {
		this.renderPassOpen = false;
		GlStateManager._glBindFramebuffer(36160, 0);
		this.backend.getDebugLabelManager().popDebugGroup();
	}

	protected GlBackend getBackend() {
		return this.backend;
	}

	@Override
	public GpuQuery timerQueryBegin() {
		RenderSystem.assertOnRenderThread();
		if (this.timerQuery != null) {
			throw new IllegalStateException("A GL_TIME_ELAPSED query is already active");
		}
		else {
			int i = GL32C.glGenQueries();
			GL32C.glBeginQuery(35007, i);
			this.timerQuery = new GlTimerQuery(i);
			return this.timerQuery;
		}
	}

	@Override
	public void timerQueryEnd(GpuQuery gpuQuery) {
		RenderSystem.assertOnRenderThread();
		if (gpuQuery != this.timerQuery) {
			throw new IllegalStateException("Mismatched or duplicate GpuQuery when ending timerQuery");
		}
		else {
			GL32C.glEndQuery(35007);
			this.timerQuery = null;
		}
	}
}
