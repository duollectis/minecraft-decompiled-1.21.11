package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code ColoredQuadGuiElementRenderState}.
 */
public record ColoredQuadGuiElementRenderState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2fc pose,
		int x0,
		int y0,
		int x1,
		int y1,
		int col1,
		int col2,
		@Nullable ScreenRect scissorArea,
		@Nullable ScreenRect bounds
) implements SimpleGuiElementRenderState {

	public ColoredQuadGuiElementRenderState(
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			Matrix3x2fc pose,
			int x0,
			int y0,
			int x1,
			int y1,
			int col1,
			int col2,
			@Nullable ScreenRect scissorArea
	) {
		this(
				pipeline,
				textureSetup,
				pose,
				x0,
				y0,
				x1,
				y1,
				col1,
				col2,
				scissorArea,
				createBounds(x0, y0, x1, y1, pose, scissorArea)
		);
	}

	@Override
	public void setupVertices(VertexConsumer vertices) {
		vertices.vertex(this.pose(), this.x0(), this.y0()).color(this.col1());
		vertices.vertex(this.pose(), this.x0(), this.y1()).color(this.col2());
		vertices.vertex(this.pose(), this.x1(), this.y1()).color(this.col2());
		vertices.vertex(this.pose(), this.x1(), this.y0()).color(this.col1());
	}

	private static @Nullable ScreenRect createBounds(
			int x0,
			int y0,
			int x1,
			int y1,
			Matrix3x2fc pose,
			@Nullable ScreenRect scissorArea
	) {
		ScreenRect screenRect = new ScreenRect(x0, y0, x1 - x0, y1 - y0).transformEachVertex(pose);
		return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
	}
}
