package net.minecraft.client.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.TextureFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Запечатанный интерфейс, описывающий привязку uniform-ресурса к шейдерной программе.
 * Три варианта: UBO-блок ({@link UniformBuffer}), текстурный буфер ({@link TexelBuffer})
 * и обычный сэмплер ({@link Sampler}).
 */
@Environment(EnvType.CLIENT)
public sealed interface GlUniform extends AutoCloseable permits GlUniform.UniformBuffer, GlUniform.TexelBuffer, GlUniform.Sampler {

	@Override
	default void close() {
	}

	/**
	 * Привязка uniform buffer object (UBO) к индексу блока шейдера.
	 */
	@Environment(EnvType.CLIENT)
	public record UniformBuffer(int blockBinding) implements GlUniform {
	}

	/**
	 * Привязка текстурного буфера (TBO) к uniform-сэмплеру.
	 * При создании без явного {@code texture} автоматически генерирует новый GL-объект текстуры.
	 */
	@Environment(EnvType.CLIENT)
	public record TexelBuffer(int location, int samplerIndex, TextureFormat format, int texture) implements GlUniform {

		public TexelBuffer(int location, int samplerIndex, TextureFormat format) {
			this(location, samplerIndex, format, GlStateManager._genTexture());
		}

		@Override
		public void close() {
			GlStateManager._deleteTexture(texture);
		}
	}

	/**
	 * Привязка обычного 2D/кубической текстуры к uniform-сэмплеру по индексу текстурного юнита.
	 */
	@Environment(EnvType.CLIENT)
	public record Sampler(int location, int samplerIndex) implements GlUniform {
	}
}
