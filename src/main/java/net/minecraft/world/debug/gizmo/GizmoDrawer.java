package net.minecraft.world.debug.gizmo;

import net.minecraft.util.math.Vec3d;

/**
 * Низкоуровневый интерфейс отрисовки геометрических примитивов отладочных gizmo.
 * <p>
 * Реализация отвечает за передачу геометрии в рендер-буфер клиента.
 * Все методы принимают цвет в формате ARGB.
 */
public interface GizmoDrawer {

	/**
	 * Добавляет точку.
	 *
	 * @param pos   позиция точки в мировых координатах
	 * @param color цвет в формате ARGB
	 * @param size  размер точки в пикселях
	 */
	void addPoint(Vec3d pos, int color, float size);

	/**
	 * Добавляет отрезок.
	 *
	 * @param start начало отрезка
	 * @param end   конец отрезка
	 * @param color цвет в формате ARGB
	 * @param width толщина линии в пикселях
	 */
	void addLine(Vec3d start, Vec3d end, int color, float width);

	/**
	 * Добавляет заполненный многоугольник.
	 *
	 * @param vertices массив вершин многоугольника; последний элемент должен совпадать с первым для замкнутого контура
	 * @param color    цвет заливки в формате ARGB
	 */
	void addPolygon(Vec3d[] vertices, int color);

	/**
	 * Добавляет четырёхугольник (quad) из четырёх вершин.
	 *
	 * @param a     первая вершина
	 * @param b     вторая вершина
	 * @param c     третья вершина
	 * @param d     четвёртая вершина
	 * @param color цвет в формате ARGB
	 */
	void addQuad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color);

	/**
	 * Добавляет текстовую метку.
	 *
	 * @param pos   позиция текста в мировых координатах
	 * @param text  отображаемый текст
	 * @param style стиль отображения текста
	 */
	void addText(Vec3d pos, String text, TextGizmo.Style style);
}
