package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextDrawable;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code GlyphGuiElementRenderState}.
 */
public record GlyphGuiElementRenderState(
		Matrix3x2fc pose,
		TextDrawable renderable,
		@Nullable ScreenRect scissorArea
) implements SimpleGuiElementRenderState {

	@Override
	public void setupVertices(VertexConsumer vertices) {
		this.renderable.render(new Matrix4f().mul(this.pose), vertices, 15728880, true);
	}

	@Override
	public RenderPipeline pipeline() {
		return this.renderable.getPipeline();
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.withLightmap(
				this.renderable.textureView(),
				RenderSystem.getSamplerCache().get(FilterMode.NEAREST)
		);
	}

	@Override
	public @Nullable ScreenRect bounds() {
		return null;
	}
}
