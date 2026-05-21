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

@Environment(EnvType.CLIENT)
/**
 * {@code Framebuffer}.
 */
public abstract class Framebuffer {

	private static int index = 0;
	public int textureWidth;
	public int textureHeight;
	protected final String name;
	public final boolean useDepthAttachment;
	protected @Nullable GpuTexture colorAttachment;
	protected @Nullable GpuTextureView colorAttachmentView;
	protected @Nullable GpuTexture depthAttachment;
	protected @Nullable GpuTextureView depthAttachmentView;

	public Framebuffer(@Nullable String name, boolean useDepthAttachment) {
		this.name = name == null ? "FBO " + index++ : name;
		this.useDepthAttachment = useDepthAttachment;
	}

	/**
	 * Resize.
	 *
	 * @param width width
	 * @param height height
	 */
	public void resize(int width, int height) {
		RenderSystem.assertOnRenderThread();
		this.delete();
		this.initFbo(width, height);
	}

	/**
	 * Delete.
	 */
	public void delete() {
		RenderSystem.assertOnRenderThread();
		if (this.depthAttachment != null) {
			this.depthAttachment.close();
			this.depthAttachment = null;
		}

		if (this.depthAttachmentView != null) {
			this.depthAttachmentView.close();
			this.depthAttachmentView = null;
		}

		if (this.colorAttachment != null) {
			this.colorAttachment.close();
			this.colorAttachment = null;
		}

		if (this.colorAttachmentView != null) {
			this.colorAttachmentView.close();
			this.colorAttachmentView = null;
		}
	}

	/**
	 * Создаёт копию depth from.
	 *
	 * @param framebuffer framebuffer
	 */
	public void copyDepthFrom(Framebuffer framebuffer) {
		RenderSystem.assertOnRenderThread();
		if (this.depthAttachment == null) {
			throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
		}
		else if (framebuffer.depthAttachment == null) {
			throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
		}
		else {
			RenderSystem.getDevice()
			            .createCommandEncoder()
			            .copyTextureToTexture(
					            framebuffer.depthAttachment,
					            this.depthAttachment,
					            0,
					            0,
					            0,
					            0,
					            0,
					            this.textureWidth,
					            this.textureHeight
			            );
		}
	}

	/**
	 * Инициализирует fbo.
	 *
	 * @param width width
	 * @param height height
	 */
	public void initFbo(int width, int height) {
		RenderSystem.assertOnRenderThread();
		GpuDevice gpuDevice = RenderSystem.getDevice();
		int i = gpuDevice.getMaxTextureSize();
		if (width > 0 && width <= i && height > 0 && height <= i) {
			this.textureWidth = width;
			this.textureHeight = height;
			if (this.useDepthAttachment) {
				this.depthAttachment =
						gpuDevice.createTexture(
								() -> this.name + " / Depth",
								15,
								TextureFormat.DEPTH32,
								width,
								height,
								1,
								1
						);
				this.depthAttachmentView = gpuDevice.createTextureView(this.depthAttachment);
			}

			this.colorAttachment =
					gpuDevice.createTexture(() -> this.name + " / Color", 15, TextureFormat.RGBA8, width, height, 1, 1);
			this.colorAttachmentView = gpuDevice.createTextureView(this.colorAttachment);
		}
		else {
			throw new IllegalArgumentException(
					"Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
		}
	}

	/**
	 * Blit to screen.
	 */
	public void blitToScreen() {
		if (this.colorAttachment == null) {
			throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
		}
		else {
			RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorAttachmentView);
		}
	}

	/**
	 * Draw blit.
	 *
	 * @param texture texture
	 */
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
					this.colorAttachmentView,
					RenderSystem.getSamplerCache().get(FilterMode.NEAREST)
			);
			renderPass.draw(0, 3);
		}
	}

	public @Nullable GpuTexture getColorAttachment() {
		return this.colorAttachment;
	}

	public @Nullable GpuTextureView getColorAttachmentView() {
		return this.colorAttachmentView;
	}

	public @Nullable GpuTexture getDepthAttachment() {
		return this.depthAttachment;
	}

	public @Nullable GpuTextureView getDepthAttachmentView() {
		return this.depthAttachmentView;
	}
}
