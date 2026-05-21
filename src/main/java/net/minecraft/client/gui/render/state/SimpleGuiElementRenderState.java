package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code SimpleGuiElementRenderState}.
 */
public interface SimpleGuiElementRenderState extends GuiElementRenderState {

	void setupVertices(VertexConsumer vertices);

	RenderPipeline pipeline();

	TextureSetup textureSetup();

	@Nullable ScreenRect scissorArea();
}
