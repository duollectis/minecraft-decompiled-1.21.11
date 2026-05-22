package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Состояние скина игрока в GUI.
 * Хранит модель игрока, текстуру скина и параметры вращения для интерактивного
 * превью (например, на экране настройки внешнего вида).
 * {@code yPivot} задаёт нормализованную точку вращения по оси Y относительно высоты модели.
 */
@Environment(EnvType.CLIENT)
public record PlayerSkinGuiElementRenderState(
		PlayerEntityModel playerModel,
		Identifier texture,
		float xRotation,
		float yRotation,
		float yPivot,
		int x1,
		int y1,
		int x2,
		int y2,
		float scale,
		@Nullable ScreenRect scissorArea,
		@Nullable ScreenRect bounds
) implements SpecialGuiElementRenderState {

	public PlayerSkinGuiElementRenderState(
			PlayerEntityModel model,
			Identifier texture,
			float xRotation,
			float yRotation,
			float yPivot,
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
				xRotation,
				yRotation,
				yPivot,
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
