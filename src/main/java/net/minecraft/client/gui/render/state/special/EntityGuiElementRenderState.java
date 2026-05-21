package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
/**
 * {@code EntityGuiElementRenderState}.
 */
public record EntityGuiElementRenderState(
		EntityRenderState renderState,
		Vector3f translation,
		Quaternionf rotation,
		@Nullable Quaternionf overrideCameraAngle,
		int x1,
		int y1,
		int x2,
		int y2,
		float scale,
		@Nullable ScreenRect scissorArea,
		@Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {

	public EntityGuiElementRenderState(
			EntityRenderState renderState,
			Vector3f translation,
			Quaternionf rotation,
			@Nullable Quaternionf overrideCameraAngle,
			int x1,
			int y1,
			int x2,
			int y2,
			float scale,
			@Nullable ScreenRect scissorArea
	) {
		this(
				renderState,
				translation,
				rotation,
				overrideCameraAngle,
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
