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

/**
 * Состояние отдельного глифа или прямоугольника текста в GUI.
 * Хранит уже подготовленный {@link TextDrawable} и матрицу трансформации.
 * Пайплайн и текстура берутся непосредственно из {@code renderable},
 * что позволяет корректно обрабатывать разные шрифты и эффекты (тень, outline).
 *
 * <p>Метод {@link #bounds()} всегда возвращает {@code null}, так как глифы
 * не участвуют в системе слоёв GUI — их порядок определяется порядком текстовых элементов.
 */
@Environment(EnvType.CLIENT)
public record GlyphGuiElementRenderState(
		Matrix3x2fc pose,
		TextDrawable renderable,
		@Nullable ScreenRect scissorArea
) implements SimpleGuiElementRenderState {

	@Override
	public void setupVertices(VertexConsumer vertices) {
		renderable.render(new Matrix4f().mul(pose), vertices, 15728880, true);
	}

	@Override
	public RenderPipeline pipeline() {
		return renderable.getPipeline();
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.withLightmap(
				renderable.textureView(),
				RenderSystem.getSamplerCache().get(FilterMode.NEAREST)
		);
	}

	@Override
	public @Nullable ScreenRect bounds() {
		return null;
	}
}
