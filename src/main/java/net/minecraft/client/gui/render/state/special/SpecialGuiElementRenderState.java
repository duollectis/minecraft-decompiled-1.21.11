package net.minecraft.client.gui.render.state.special;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс состояния специального GUI-элемента, требующего off-screen рендеринга (PIP).
 * Определяет прямоугольную область в GUI-координатах, масштаб и матрицу трансформации.
 *
 * <p>Поле {@code pose} является разделяемым экземпляром единичной матрицы —
 * специальные элементы всегда рендерятся без дополнительной трансформации позиции,
 * поскольку их положение задаётся координатами {@code x1/y1/x2/y2}.
 */
@Environment(EnvType.CLIENT)
public interface SpecialGuiElementRenderState extends GuiElementRenderState {

	/** Единичная матрица трансформации, общая для всех специальных элементов. */
	Matrix3x2f pose = new Matrix3x2f();

	int x1();

	int x2();

	int y1();

	int y2();

	float scale();

	default Matrix3x2f pose() {
		return pose;
	}

	@Nullable ScreenRect scissorArea();

	/**
	 * Вычисляет ограничивающий прямоугольник специального элемента.
	 * Если задана область отсечения, возвращает пересечение с ней.
	 */
	static @Nullable ScreenRect createBounds(int x1, int y1, int x2, int y2, @Nullable ScreenRect scissorArea) {
		ScreenRect rect = new ScreenRect(x1, y1, x2 - x1, y2 - y1);
		return scissorArea != null ? scissorArea.intersection(rect) : rect;
	}
}
