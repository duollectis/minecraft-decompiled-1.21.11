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

/**
 * Состояние тайлированного текстурированного прямоугольника в GUI.
 * Заполняет область повторяющимися тайлами размером {@code tileWidth × tileHeight}.
 * Крайние тайлы обрезаются по границе области с корректным пересчётом UV-координат
 * через линейную интерполяцию, чтобы избежать растяжения текстуры.
 */
@Environment(EnvType.CLIENT)
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

	/**
	 * Генерирует вершины для всех тайлов, заполняющих область.
	 * Для каждого тайла вычисляется фактический размер (полный или обрезанный)
	 * и соответствующие UV-координаты через {@link MathHelper#lerp}.
	 */
	@Override
	public void setupVertices(VertexConsumer vertices) {
		int totalWidth = x1() - x0();
		int totalHeight = y1() - y0();

		for (int tileX = 0; tileX < totalWidth; tileX += tileWidth()) {
			int remainingWidth = totalWidth - tileX;
			int segmentWidth;
			float segmentU;

			if (tileWidth() <= remainingWidth) {
				segmentWidth = tileWidth();
				segmentU = u1();
			} else {
				segmentWidth = remainingWidth;
				segmentU = MathHelper.lerp((float) remainingWidth / tileWidth(), u0(), u1());
			}

			for (int tileY = 0; tileY < totalHeight; tileY += tileHeight()) {
				int remainingHeight = totalHeight - tileY;
				int segmentHeight;
				float segmentV;

				if (tileHeight() <= remainingHeight) {
					segmentHeight = tileHeight();
					segmentV = v1();
				} else {
					segmentHeight = remainingHeight;
					segmentV = MathHelper.lerp((float) remainingHeight / tileHeight(), v0(), v1());
				}

				int left = x0() + tileX;
				int right = x0() + tileX + segmentWidth;
				int top = y0() + tileY;
				int bottom = y0() + tileY + segmentHeight;

				vertices.vertex(pose(), left, top).texture(u0(), v0()).color(color());
				vertices.vertex(pose(), left, bottom).texture(u0(), segmentV).color(color());
				vertices.vertex(pose(), right, bottom).texture(segmentU, segmentV).color(color());
				vertices.vertex(pose(), right, top).texture(segmentU, v0()).color(color());
			}
		}
	}

	private static @Nullable ScreenRect createBounds(
			int x0,
			int y0,
			int x1,
			int y1,
			Matrix3x2f pose,
			@Nullable ScreenRect scissorArea
	) {
		ScreenRect rect = new ScreenRect(x0, y0, x1 - x0, y1 - y0).transformEachVertex(pose);
		return scissorArea != null ? scissorArea.intersection(rect) : rect;
	}
}
