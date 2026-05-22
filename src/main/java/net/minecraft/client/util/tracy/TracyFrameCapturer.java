package net.minecraft.client.util.tracy;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.jtracy.TracyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class TracyFrameCapturer implements AutoCloseable {

	private static final int MAX_WIDTH = 320;
	private static final int MAX_HEIGHT = 180;
	private static final int ALIGNMENT_FACTOR = 4;
	private static final int BYTES_PER_PIXEL = 4;

	private int framebufferWidth;
	private int framebufferHeight;
	private int width;
	private int height;
	private GpuTexture texture;
	private GpuTextureView textureView;
	private GpuBuffer buffer;
	private int offset;
	private boolean captured;
	private Status status = Status.WAITING_FOR_CAPTURE;

	public TracyFrameCapturer() {
		width = MAX_WIDTH;
		height = MAX_HEIGHT;
		GpuDevice gpuDevice = RenderSystem.getDevice();
		texture = gpuDevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, width, height, 1, 1);
		textureView = gpuDevice.createTextureView(texture);
		buffer = gpuDevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, (long) width * height * BYTES_PER_PIXEL);
	}

	/**
	 * Пересчитывает размер capture-текстуры под новый размер фреймбуфера.
	 * Размеры выравниваются до кратных 4 для совместимости с GPU-форматами.
	 */
	private void resize(int newFramebufferWidth, int newFramebufferHeight) {
		float aspectRatio = (float) newFramebufferWidth / newFramebufferHeight;
		if (newFramebufferWidth > MAX_WIDTH) {
			newFramebufferWidth = MAX_WIDTH;
			newFramebufferHeight = (int) (MAX_WIDTH / aspectRatio);
		}

		if (newFramebufferHeight > MAX_HEIGHT) {
			newFramebufferWidth = (int) (MAX_HEIGHT * aspectRatio);
			newFramebufferHeight = MAX_HEIGHT;
		}

		newFramebufferWidth = newFramebufferWidth / ALIGNMENT_FACTOR * ALIGNMENT_FACTOR;
		newFramebufferHeight = newFramebufferHeight / ALIGNMENT_FACTOR * ALIGNMENT_FACTOR;

		if (width == newFramebufferWidth && height == newFramebufferHeight) {
			return;
		}

		width = newFramebufferWidth;
		height = newFramebufferHeight;
		GpuDevice gpuDevice = RenderSystem.getDevice();

		texture.close();
		texture = gpuDevice.createTexture("Tracy Frame Capture", 10, TextureFormat.RGBA8, width, height, 1, 1);
		textureView.close();
		textureView = gpuDevice.createTextureView(texture);
		buffer.close();
		buffer = gpuDevice.createBuffer(() -> "Tracy Frame Capture buffer", 9, (long) width * height * BYTES_PER_PIXEL);
	}

	public void capture(Framebuffer framebuffer) {
		if (status != Status.WAITING_FOR_CAPTURE || captured || framebuffer.getColorAttachment() == null) {
			return;
		}

		captured = true;
		if (framebuffer.textureWidth != framebufferWidth || framebuffer.textureHeight != framebufferHeight) {
			framebufferWidth = framebuffer.textureWidth;
			framebufferHeight = framebuffer.textureHeight;
			resize(framebufferWidth, framebufferHeight);
		}

		status = Status.WAITING_FOR_COPY;
		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

		try (RenderPass renderPass = RenderSystem.getDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "Tracy blit", textureView, OptionalInt.empty())
		) {
			renderPass.setPipeline(RenderPipelines.TRACY_BLIT);
			renderPass.bindTexture(
				"InSampler",
				framebuffer.getColorAttachmentView(),
				RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
			);
			renderPass.draw(0, 3);
		}

		commandEncoder.copyTextureToBuffer(
			texture,
			buffer,
			0L,
			() -> status = Status.WAITING_FOR_UPLOAD,
			0
		);
		offset = 0;
	}

	public void upload() {
		if (status != Status.WAITING_FOR_UPLOAD) {
			return;
		}

		status = Status.WAITING_FOR_CAPTURE;
		try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice()
			.createCommandEncoder()
			.mapBuffer(buffer, true, false)
		) {
			TracyClient.frameImage(mappedView.data(), width, height, offset, true);
		}
	}

	public void markFrame() {
		offset++;
		captured = false;
		TracyClient.markFrame();
	}

	@Override
	public void close() {
		texture.close();
		textureView.close();
		buffer.close();
	}

	@Environment(EnvType.CLIENT)
	enum Status {
		WAITING_FOR_CAPTURE,
		WAITING_FOR_COPY,
		WAITING_FOR_UPLOAD
	}
}
