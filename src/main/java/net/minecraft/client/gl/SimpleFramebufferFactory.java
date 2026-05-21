package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ClosableFactory;

@Environment(EnvType.CLIENT)
/**
 * {@code SimpleFramebufferFactory}.
 */
public record SimpleFramebufferFactory(
		int width,
		int height,
		boolean useDepth,
		int clearColor
) implements ClosableFactory<Framebuffer> {

	/**
	 * Create.
	 *
	 * @return Framebuffer — результат операции
	 */
	public Framebuffer create() {
		return new SimpleFramebuffer(null, this.width, this.height, this.useDepth);
	}

	/**
	 * Prepare.
	 *
	 * @param framebuffer framebuffer
	 */
	public void prepare(Framebuffer framebuffer) {
		if (this.useDepth) {
			RenderSystem.getDevice()
			            .createCommandEncoder()
			            .clearColorAndDepthTextures(
					            framebuffer.getColorAttachment(),
					            this.clearColor,
					            framebuffer.getDepthAttachment(),
					            1.0
			            );
		}
		else {
			RenderSystem
					.getDevice()
					.createCommandEncoder()
					.clearColorTexture(framebuffer.getColorAttachment(), this.clearColor);
		}
	}

	/**
	 * Close.
	 *
	 * @param framebuffer framebuffer
	 */
	public void close(Framebuffer framebuffer) {
		framebuffer.delete();
	}

	@Override
	public boolean equals(ClosableFactory<?> factory) {
		return !(factory instanceof SimpleFramebufferFactory simpleFramebufferFactory)
		       ? false
		       : this.width == simpleFramebufferFactory.width && this.height == simpleFramebufferFactory.height
		         && this.useDepth == simpleFramebufferFactory.useDepth;
	}
}
