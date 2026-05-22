package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.ClosableFactory;

/**
 * Фабрика для создания и управления жизненным циклом {@link SimpleFramebuffer}.
 * Реализует {@link ClosableFactory}, что позволяет использовать её в контексте
 * управляемых ресурсов пост-эффектов.
 */
@Environment(EnvType.CLIENT)
public record SimpleFramebufferFactory(
	int width,
	int height,
	boolean useDepth,
	int clearColor
) implements ClosableFactory<Framebuffer> {

	public Framebuffer create() {
		return new SimpleFramebuffer(null, width, height, useDepth);
	}

	public void prepare(Framebuffer framebuffer) {
		if (useDepth) {
			RenderSystem.getDevice()
				.createCommandEncoder()
				.clearColorAndDepthTextures(
					framebuffer.getColorAttachment(),
					clearColor,
					framebuffer.getDepthAttachment(),
					1.0
				);
		} else {
			RenderSystem.getDevice()
				.createCommandEncoder()
				.clearColorTexture(framebuffer.getColorAttachment(), clearColor);
		}
	}

	public void close(Framebuffer framebuffer) {
		framebuffer.delete();
	}

	public boolean equals(ClosableFactory<?> factory) {
		return factory instanceof SimpleFramebufferFactory other
			&& width == other.width
			&& height == other.height
			&& useDepth == other.useDepth;
	}
}
