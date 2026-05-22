package net.minecraft.world.debug.gizmo;

/**
 * Коллектор отладочных примитивов (gizmo).
 * <p>
 * Принимает созданные gizmo и возвращает {@link VisibilityConfigurable}
 * для дальнейшей настройки их видимости. Предоставляет пустую реализацию
 * {@link #EMPTY} для контекстов, где отладочная отрисовка отключена.
 */
public interface GizmoCollector {

	/**
	 * Заглушка {@link VisibilityConfigurable}, игнорирующая все настройки.
	 * Используется в {@link #EMPTY}.
	 */
	VisibilityConfigurable NOOP_CONFIGURABLE = new VisibilityConfigurable() {
		@Override
		public VisibilityConfigurable ignoreOcclusion() {
			return this;
		}

		@Override
		public VisibilityConfigurable withLifespan(int lifespan) {
			return this;
		}

		@Override
		public VisibilityConfigurable fadeOut() {
			return this;
		}
	};

	/** Пустой коллектор — отбрасывает все переданные примитивы. */
	GizmoCollector EMPTY = gizmo -> NOOP_CONFIGURABLE;

	/**
	 * Принимает gizmo и возвращает конфигуратор его видимости.
	 *
	 * @param gizmo отладочный примитив
	 * @return конфигуратор видимости
	 */
	VisibilityConfigurable collect(Gizmo gizmo);
}
