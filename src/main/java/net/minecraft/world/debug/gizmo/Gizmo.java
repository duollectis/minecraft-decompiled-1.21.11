package net.minecraft.world.debug.gizmo;

/**
 * Базовый интерфейс отладочного примитива (gizmo).
 * <p>
 * Каждый gizmo умеет отрисовывать себя через {@link GizmoDrawer},
 * учитывая текущую прозрачность (opacity), которая используется
 * для эффекта плавного затухания.
 */
public interface Gizmo {

	/**
	 * Отрисовывает примитив через переданный рисовальщик.
	 *
	 * @param consumer рисовальщик, принимающий геометрические примитивы
	 * @param opacity  прозрачность от {@code 0.0} (невидим) до {@code 1.0} (полностью непрозрачен)
	 */
	void draw(GizmoDrawer consumer, float opacity);
}
