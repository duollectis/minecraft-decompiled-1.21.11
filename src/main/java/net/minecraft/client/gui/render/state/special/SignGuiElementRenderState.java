package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.WoodType;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.model.Model;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code SignGuiElementRenderState}.
 */
public record SignGuiElementRenderState(
		Model.SinglePartModel signModel,
		WoodType woodType,
		int x1,
		int y1,
		int x2,
		int y2,
		float scale,
		@Nullable ScreenRect scissorArea,
		@Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {

	public SignGuiElementRenderState(
			Model.SinglePartModel part,
			WoodType woodType,
			int x1,
			int y1,
			int x2,
			int y2,
			float scale,
			@Nullable ScreenRect scissorArea
	) {
		this(
				part,
				woodType,
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
