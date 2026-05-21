package net.minecraft.client.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code GlUniform}.
 */
public sealed interface GlUniform extends AutoCloseable permits GlUniform.UniformBuffer, GlUniform.TexelBuffer, GlUniform.Sampler {

	@Override
	default void close() {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Sampler}.
	 */
	public record Sampler(int location, int samplerIndex) implements GlUniform {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code TexelBuffer}.
	 */
	public record TexelBuffer(int location, int samplerIndex, TextureFormat format, int texture) implements GlUniform {

		public TexelBuffer(int location, int samplerIndex, TextureFormat format) {
			this(location, samplerIndex, format, GlStateManager._genTexture());
		}

		@Override
		public void close() {
			GlStateManager._deleteTexture(this.texture);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code UniformBuffer}.
	 */
	public record UniformBuffer(int blockBinding) implements GlUniform {
	}
}
