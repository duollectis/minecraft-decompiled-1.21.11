package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

/**
 * Абстрактный фреймбуфер с цветовым и опциональным глубинным вложениями.
 * Управляет жизненным циклом GPU-текстур: создаёт их в {@link #initFbo} и освобождает в {@link #delete}.
 */
@Environment(EnvType.CLIENT)
public abstract class Framebuffer {

	private static int index = 0;

	public int textureWidth;
	public int textureHeight;
	public final boolean useDepthAttachment;
	protected final String name;
	protected @Nullable GpuTexture colorAttachment;
	protected @Nullable GpuTextureView colorAttachmentView;
	protected @Nullable GpuTexture depthAttachment;
	protected @Nullable GpuTextureView depthAttachmentView;

	public Framebuffer(@Nullable String name, boolean useDepthAttachment) {
		this.name = name == null ? "FBO " + index++ : name;
		this.useDepthAttachment = useDepthAttachment;
	}

	public void resize(int width, int height) {
		RenderSystem.assertOnRenderThread();
		delete();
		initFbo(width, height);
	}

	public void delete() {
		RenderSystem.assertOnRenderThread();

		if (depthAttachment != null) {
			depthAttachment.close();
			depthAttachment = null;
		}

		if (depthAttachmentView != null) {
			depthAttachmentView.close();
			depthAttachmentView = null;
		}

		if (colorAttachment != null) {
			colorAttachment.close();
			colorAttachment = null;
		}

		if (colorAttachmentView != null) {
			colorAttachmentView.close();
			colorAttachmentView = null;
		}
	}

	/**
	 * Копирует содержимое буфера глубины из {@code source} в текущий фреймбуфер.
	 * Оба фреймбуфера обязаны иметь глубинное вложение.
	 */
	public void copyDepthFrom(Framebuffer source) {
		RenderSystem.assertOnRenderThread();

		if (depthAttachment == null) {
			throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
		}

		if (source.depthAttachment == null) {
			throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
		}

		RenderSystem.getDevice()
			.createCommandEncoder()
			.copyTextureToTexture(
				source.depthAttachment,
				depthAttachment,
				0,
				0,
				0,
				0,
				0,
				textureWidth,
				textureHeight
			);
	}

	/**
	 * Инициализирует GPU-текстуры фреймбуфера заданного размера.
	 * Размер должен быть в диапазоне [1, maxTextureSize].
	 * Флаг {@code usage = 15} означает COPY_DST | COPY_SRC | TEXTURE_BINDING | RENDER_ATTACHMENT.
	 */
	public void initFbo(int width, int height) {
		RenderSystem.assertOnRenderThread();
		GpuDevice gpuDevice = RenderSystem.getDevice();
		int maxSize = gpuDevice.getMaxTextureSize();

		if (width <= 0 || width > maxSize || height <= 0 || height > maxSize) {
			throw new IllegalArgumentException(
				"Window " + width + "x" + height + " size out of bounds (max. size: " + maxSize + ")"
			);
		}

		textureWidth = width;
		textureHeight = height;

		if (useDepthAttachment) {
			depthAttachment = gpuDevice.createTexture(
				() -> name + " / Depth",
				15,
				TextureFormat.DEPTH32,
				width,
				height,
				1,
				1
			);
			depthAttachmentView = gpuDevice.createTextureView(depthAttachment);
		}

		colorAttachment = gpuDevice.createTexture(
			() -> name + " / Color",
			15,
			TextureFormat.RGBA8,
			width,
			height,
			1,
			1
		);
		colorAttachmentView = gpuDevice.createTextureView(colorAttachment);
	}

	public void blitToScreen() {
		if (colorAttachment == null) {
			throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
		}

		RenderSystem.getDevice().createCommandEncoder().presentTexture(colorAttachmentView);
	}

	public void drawBlit(GpuTextureView texture) {
		RenderSystem.assertOnRenderThread();

		try (RenderPass renderPass = RenderSystem
			.getDevice()
			.createCommandEncoder()
			.createRenderPass(() -> "Blit render target", texture, OptionalInt.empty())
		) {
			renderPass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.bindTexture(
				"InSampler",
				colorAttachmentView,
				RenderSystem.getSamplerCache().get(FilterMode.NEAREST)
			);
			renderPass.draw(0, 3);
		}
	}

	public @Nullable GpuTexture getColorAttachment() {
		return colorAttachment;
	}

	public @Nullable GpuTextureView getColorAttachmentView() {
		return colorAttachmentView;
	}

	public @Nullable GpuTexture getDepthAttachment() {
		return depthAttachment;
	}

	public @Nullable GpuTextureView getDepthAttachmentView() {
		return depthAttachmentView;
	}
}
