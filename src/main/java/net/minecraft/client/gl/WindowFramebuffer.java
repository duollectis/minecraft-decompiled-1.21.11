package net.minecraft.client.gl;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.TextureAllocationException;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Основной фреймбуфер окна игры. При нехватке видеопамяти автоматически
 * откатывается к разрешению по умолчанию ({@value DEFAULT_WIDTH}x{@value DEFAULT_HEIGHT}).
 */
@Environment(EnvType.CLIENT)
public class WindowFramebuffer extends Framebuffer {

	public static final int DEFAULT_WIDTH = 854;
	public static final int DEFAULT_HEIGHT = 480;

	// Флаги использования текстуры: SAMPLED | COLOR_ATTACHMENT | COPY_SRC | COPY_DST
	private static final int TEXTURE_USAGE_FLAGS = 15;

	static final Size DEFAULT = new Size(DEFAULT_WIDTH, DEFAULT_HEIGHT);

	public WindowFramebuffer(int width, int height) {
		super("Main", true);
		init(width, height);
	}

	private void init(int width, int height) {
		Size size = findSuitableSize(width, height);

		if (colorAttachment == null || depthAttachment == null) {
			throw new IllegalStateException("Missing color and/or depth textures");
		}

		textureWidth = size.width;
		textureHeight = size.height;
	}

	private Size findSuitableSize(int width, int height) {
		RenderSystem.assertOnRenderThread();

		for (Size size : Size.findCompatible(width, height)) {
			if (colorAttachment != null) {
				colorAttachment.close();
				colorAttachment = null;
			}

			if (colorAttachmentView != null) {
				colorAttachmentView.close();
				colorAttachmentView = null;
			}

			if (depthAttachment != null) {
				depthAttachment.close();
				depthAttachment = null;
			}

			if (depthAttachmentView != null) {
				depthAttachmentView.close();
				depthAttachmentView = null;
			}

			colorAttachment = createColorAttachment(size);
			depthAttachment = createDepthAttachment(size);

			if (colorAttachment != null && depthAttachment != null) {
				colorAttachmentView = RenderSystem.getDevice().createTextureView(colorAttachment);
				depthAttachmentView = RenderSystem.getDevice().createTextureView(depthAttachment);
				return size;
			}
		}

		throw new RuntimeException(
			"Unrecoverable GL_OUT_OF_MEMORY ("
				+ (colorAttachment == null ? "missing color" : "have color")
				+ ", "
				+ (depthAttachment == null ? "missing depth" : "have depth")
				+ ")"
		);
	}

	private @Nullable GpuTexture createColorAttachment(Size size) {
		try {
			return RenderSystem.getDevice()
				.createTexture(
					() -> name + " / Color",
					TEXTURE_USAGE_FLAGS,
					TextureFormat.RGBA8,
					size.width,
					size.height,
					1,
					1
				);
		} catch (TextureAllocationException exception) {
			return null;
		}
	}

	private @Nullable GpuTexture createDepthAttachment(Size size) {
		try {
			return RenderSystem.getDevice()
				.createTexture(
					() -> name + " / Depth",
					TEXTURE_USAGE_FLAGS,
					TextureFormat.DEPTH32,
					size.width,
					size.height,
					1,
					1
				);
		} catch (TextureAllocationException exception) {
			return null;
		}
	}

	@Environment(EnvType.CLIENT)
	static class Size {

		public final int width;
		public final int height;

		Size(int width, int height) {
			this.width = width;
			this.height = height;
		}

		static List<Size> findCompatible(int width, int height) {
			RenderSystem.assertOnRenderThread();
			int maxSize = RenderSystem.getDevice().getMaxTextureSize();
			boolean fits = width > 0 && width <= maxSize && height > 0 && height <= maxSize;
			return fits
				? ImmutableList.of(new Size(width, height), DEFAULT)
				: ImmutableList.of(DEFAULT);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Size other = (Size) o;
			return width == other.width && height == other.height;
		}

		@Override
		public int hashCode() {
			return Objects.hash(width, height);
		}

		@Override
		public String toString() {
			return width + "x" + height;
		}
	}
}
