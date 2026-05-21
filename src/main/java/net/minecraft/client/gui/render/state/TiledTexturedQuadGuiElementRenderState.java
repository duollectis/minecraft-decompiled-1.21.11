package net.minecraft.client.gui.render.state;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code TiledTexturedQuadGuiElementRenderState}.
 */
public record TiledTexturedQuadGuiElementRenderState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2f pose,
		int tileWidth,
		int tileHeight,
		int x0,
		int y0,
		int x1,
		int y1,
		float u0,
		float u1,
		float v0,
		float v1,
		int color,
		@Nullable ScreenRect scissorArea,
		@Nullable ScreenRect bounds
) implements SimpleGuiElementRenderState {

	public TiledTexturedQuadGuiElementRenderState(
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			Matrix3x2f pose,
			int tileWidth,
			int tileHeight,
			int x0,
			int y0,
			int x1,
			int y1,
			float u0,
			float u1,
			float v0,
			float v1,
			int color,
			@Nullable ScreenRect scissorArea
	) {
		this(
				pipeline,
				textureSetup,
				pose,
				tileWidth,
				tileHeight,
				x0,
				y0,
				x1,
				y1,
				u0,
				u1,
				v0,
				v1,
				color,
				scissorArea,
				createBounds(x0, y0, x1, y1, pose, scissorArea)
		);
	}

	@Override
	public void setupVertices(VertexConsumer vertices) {
		int i = this.x1() - this.x0();
		int j = this.y1() - this.y0();

		for (int k = 0; k < i; k += this.tileWidth()) {
			int l = i - k;
			int m;
			float f;
			if (this.tileWidth() <= l) {
				m = this.tileWidth();
				f = this.u1();
			}
			else {
				m = l;
				f = MathHelper.lerp((float) l / this.tileWidth(), this.u0(), this.u1());
			}

			for (int n = 0; n < j; n += this.tileHeight()) {
				int o = j - n;
				int p;
				float g;
				if (this.tileHeight() <= o) {
					p = this.tileHeight();
					g = this.v1();
				}
				else {
					p = o;
					g = MathHelper.lerp((float) o / this.tileHeight(), this.v0(), this.v1());
				}

				int q = this.x0() + k;
				int r = this.x0() + k + m;
				int s = this.y0() + n;
				int t = this.y0() + n + p;
				vertices.vertex(this.pose(), q, s).texture(this.u0(), this.v0()).color(this.color());
				vertices.vertex(this.pose(), q, t).texture(this.u0(), g).color(this.color());
				vertices.vertex(this.pose(), r, t).texture(f, g).color(this.color());
				vertices.vertex(this.pose(), r, s).texture(f, this.v0()).color(this.color());
			}
		}
	}

	private static @Nullable ScreenRect createBounds(
			int x1,
			int y1,
			int x2,
			int y2,
			Matrix3x2f pose,
			@Nullable ScreenRect rect
	) {
		ScreenRect screenRect = new ScreenRect(x1, y1, x2 - x1, y2 - y1).transformEachVertex(pose);
		return rect != null ? rect.intersection(screenRect) : screenRect;
	}
}
