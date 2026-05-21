package net.minecraft.client.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
/**
 * {@code TextDrawable}.
 */
public interface TextDrawable {

	void render(Matrix4f matrix4f, VertexConsumer consumer, int light, boolean noDepth);

	RenderLayer getRenderLayer(TextRenderer.TextLayerType type);

	GpuTextureView textureView();

	RenderPipeline getPipeline();

	float getEffectiveMinX();

	float getEffectiveMinY();

	float getEffectiveMaxX();

	float getEffectiveMaxY();

	@Environment(EnvType.CLIENT)
	/**
	 * {@code DrawnGlyphRect}.
	 */
	public interface DrawnGlyphRect extends GlyphRect, TextDrawable {

		@Override
		default float getLeft() {
			return this.getEffectiveMinX();
		}

		@Override
		default float getTop() {
			return this.getEffectiveMinY();
		}

		@Override
		default float getRight() {
			return this.getEffectiveMaxX();
		}

		@Override
		default float getBottom() {
			return this.getEffectiveMaxY();
		}
	}
}
