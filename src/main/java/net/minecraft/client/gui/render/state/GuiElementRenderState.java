package net.minecraft.client.gui.render.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import org.jspecify.annotations.Nullable;

/**
 * Базовый интерфейс состояния любого GUI-элемента.
 * Предоставляет ограничивающий прямоугольник элемента для системы слоёв GUI,
 * которая использует его для определения порядка отрисовки и пересечений.
 */
@Environment(EnvType.CLIENT)
public interface GuiElementRenderState {

	/**
	 * Возвращает ограничивающий прямоугольник элемента в экранных координатах.
	 * Может быть {@code null}, если элемент не имеет видимой области
	 * (например, пустой текст) — в этом случае элемент не участвует в системе слоёв.
	 */
	@Nullable ScreenRect bounds();
}
