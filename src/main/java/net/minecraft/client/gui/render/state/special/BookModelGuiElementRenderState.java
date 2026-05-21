package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code BookModelGuiElementRenderState}.
 */
public record BookModelGuiElementRenderState(
		BookModel bookModel,
		Identifier texture,
		float open,
		float flip,
		int x1,
		int y1,
		int x2,
		int y2,
		float scale,
		@Nullable ScreenRect scissorArea,
		@Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {

	public BookModelGuiElementRenderState(
			BookModel model,
			Identifier texture,
			float open,
			float flip,
			int x1,
			int y1,
			int x2,
			int y2,
			float scale,
			@Nullable ScreenRect scissorArea
	) {
		this(
				model,
				texture,
				open,
				flip,
				x1,
				y1,
				x2,
				y2,
				scale,
				scissorArea,
				SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea)
		);
	}
}
