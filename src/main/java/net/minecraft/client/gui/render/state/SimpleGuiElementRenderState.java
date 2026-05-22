package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс состояния простого GUI-элемента, отрисовываемого напрямую через вершинный буфер.
 * Определяет контракт для элементов, которые самостоятельно записывают свои вершины
 * и несут информацию о пайплайне, текстуре и области отсечения.
 */
@Environment(EnvType.CLIENT)
public interface SimpleGuiElementRenderState extends GuiElementRenderState {

	/**
	 * Записывает вершины элемента в переданный {@link VertexConsumer}.
	 */
	void setupVertices(VertexConsumer vertices);

	RenderPipeline pipeline();

	TextureSetup textureSetup();

	@Nullable ScreenRect scissorArea();
}
