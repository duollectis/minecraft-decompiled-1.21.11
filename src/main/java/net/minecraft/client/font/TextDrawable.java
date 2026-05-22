package net.minecraft.client.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Отрисовываемый элемент текста. Содержит геометрию глифа и ссылку на текстуру/пайплайн рендеринга.
 */
@Environment(EnvType.CLIENT)
public interface TextDrawable {

	void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth);

	RenderLayer getRenderLayer(TextRenderer.TextLayerType type);

	GpuTextureView textureView();

	RenderPipeline getPipeline();

	float getEffectiveMinX();

	float getEffectiveMinY();

	float getEffectiveMaxX();

	float getEffectiveMaxY();

	/**
	 * Запечённый прямоугольник глифа, реализующий одновременно {@link GlyphRect} и {@link TextDrawable}.
	 * Делегирует координаты прямоугольника к эффективным границам глифа.
	 */
	@Environment(EnvType.CLIENT)
	interface DrawnGlyphRect extends GlyphRect, TextDrawable {

		@Override
		default float getLeft() {
			return getEffectiveMinX();
		}

		@Override
		default float getTop() {
			return getEffectiveMinY();
		}

		@Override
		default float getRight() {
			return getEffectiveMaxX();
		}

		@Override
		default float getBottom() {
			return getEffectiveMaxY();
		}
	}
}
