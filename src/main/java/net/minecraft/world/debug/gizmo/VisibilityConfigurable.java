package net.minecraft.world.debug.gizmo;

/**
 * Интерфейс настройки видимости отладочного примитива.
 * <p>
 * Позволяет задать параметры отображения gizmo в fluent-стиле:
 * игнорирование окклюзии, время жизни и эффект затухания.
 */
public interface VisibilityConfigurable {

	/**
	 * Отключает проверку окклюзии — примитив будет виден сквозь блоки.
	 *
	 * @return {@code this} для цепочки вызовов
	 */
	VisibilityConfigurable ignoreOcclusion();

	/**
	 * Задаёт время жизни примитива в миллисекундах.
	 *
	 * @param lifespan время жизни в мс
	 * @return {@code this} для цепочки вызовов
	 */
	VisibilityConfigurable withLifespan(int lifespan);

	/**
	 * Включает эффект плавного затухания к концу времени жизни.
	 *
	 * @return {@code this} для цепочки вызовов
	 */
	VisibilityConfigurable fadeOut();
}
