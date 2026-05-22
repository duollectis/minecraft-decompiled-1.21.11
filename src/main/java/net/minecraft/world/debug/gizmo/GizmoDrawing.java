package net.minecraft.world.debug.gizmo;

import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Статический фасад для создания отладочных примитивов (gizmo) в текущем контексте.
 * <p>
 * Перед использованием необходимо установить активный {@link GizmoCollector} через
 * {@link #using(GizmoCollector)}. Все методы-фабрики делегируют в текущий коллектор,
 * хранящийся в {@link ThreadLocal}. Это позволяет безопасно использовать gizmo
 * в многопоточной среде без явной передачи коллектора через параметры.
 */
public class GizmoDrawing {

	/** Вертикальное смещение метки блока над поверхностью. */
	private static final double BLOCK_LABEL_Y_OFFSET = 1.3;

	/** Шаг вертикального смещения между строками метки блока. */
	private static final double BLOCK_LABEL_LINE_STEP = 0.2;

	/** Горизонтальное смещение метки блока (центрирование по X/Z). */
	private static final double BLOCK_LABEL_CENTER_OFFSET = 0.5;

	/** Базовое вертикальное смещение метки сущности над головой. */
	private static final double ENTITY_LABEL_Y_BASE = 2.4;

	/** Шаг вертикального смещения между строками метки сущности. */
	private static final double ENTITY_LABEL_LINE_STEP = 0.25;

	/** Горизонтальное выравнивание метки сущности (центрирование). */
	private static final float ENTITY_LABEL_ADJUST = 0.5F;

	static final ThreadLocal<@Nullable GizmoCollector> CURRENT_GIZMO_COLLECTOR = new ThreadLocal<>();

	private GizmoDrawing() {}

	/**
	 * Устанавливает активный коллектор для текущего потока и возвращает scope-объект.
	 * <p>
	 * Используется в блоке try-with-resources: при закрытии scope предыдущий коллектор
	 * автоматически восстанавливается.
	 *
	 * @param gizmoCollector коллектор, который будет принимать все созданные примитивы
	 * @return scope-объект для использования в try-with-resources
	 */
	public static CollectorScope using(GizmoCollector gizmoCollector) {
		CollectorScope scope = new CollectorScope();
		CURRENT_GIZMO_COLLECTOR.set(gizmoCollector);
		return scope;
	}

	/**
	 * Передаёт gizmo в текущий активный коллектор.
	 *
	 * @param gizmo примитив для регистрации
	 * @return конфигуратор видимости
	 * @throws IllegalStateException если коллектор не установлен в текущем потоке
	 */
	public static VisibilityConfigurable collect(Gizmo gizmo) {
		GizmoCollector collector = CURRENT_GIZMO_COLLECTOR.get();
		if (collector == null) {
			throw new IllegalStateException("Gizmos cannot be created here! No GizmoCollector has been registered.");
		}

		return collector.collect(gizmo);
	}

	public static VisibilityConfigurable box(Box box, DrawStyle style) {
		return box(box, style, false);
	}

	public static VisibilityConfigurable box(Box box, DrawStyle style, boolean coloredCornerStroke) {
		return collect(new BoxGizmo(box, style, coloredCornerStroke));
	}

	public static VisibilityConfigurable box(BlockPos blockPos, DrawStyle style) {
		return box(new Box(blockPos), style);
	}

	public static VisibilityConfigurable box(BlockPos blockPos, float expansion, DrawStyle style) {
		return box(new Box(blockPos).expand(expansion), style);
	}

	public static VisibilityConfigurable circle(Vec3d pos, float radius, DrawStyle style) {
		return collect(new CircleGizmo(pos, radius, style));
	}

	/**
	 * Создаёт отрезок с толщиной по умолчанию ({@link LineGizmo#LINE_WIDTH}).
	 *
	 * @param start начало отрезка
	 * @param end   конец отрезка
	 * @param color цвет в формате ARGB
	 */
	public static VisibilityConfigurable line(Vec3d start, Vec3d end, int color) {
		return collect(new LineGizmo(start, end, color, LineGizmo.LINE_WIDTH));
	}

	public static VisibilityConfigurable line(Vec3d start, Vec3d end, int color, float width) {
		return collect(new LineGizmo(start, end, color, width));
	}

	/**
	 * Создаёт стрелку с размером наконечника по умолчанию ({@link ArrowGizmo#ARROW_SIZE}).
	 *
	 * @param start начало стрелки
	 * @param end   конец стрелки (наконечник)
	 * @param color цвет в формате ARGB
	 */
	public static VisibilityConfigurable arrow(Vec3d start, Vec3d end, int color) {
		return collect(new ArrowGizmo(start, end, color, ArrowGizmo.ARROW_SIZE));
	}

	public static VisibilityConfigurable arrow(Vec3d start, Vec3d end, int color, float width) {
		return collect(new ArrowGizmo(start, end, color, width));
	}

	public static VisibilityConfigurable face(Vec3d nwd, Vec3d seu, Direction direction, DrawStyle style) {
		return collect(QuadGizmo.ofFace(nwd, seu, direction, style));
	}

	public static VisibilityConfigurable quad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, DrawStyle style) {
		return collect(new QuadGizmo(a, b, c, d, style));
	}

	public static VisibilityConfigurable point(Vec3d pos, int color, float size) {
		return collect(new PointGizmo(pos, color, size));
	}

	/**
	 * Создаёт текстовую метку над блоком с автоматическим игнорированием окклюзии.
	 * <p>
	 * Метка размещается на высоте {@value #BLOCK_LABEL_Y_OFFSET} над поверхностью блока,
	 * с вертикальным шагом {@value #BLOCK_LABEL_LINE_STEP} на единицу {@code yOffset}.
	 *
	 * @param text    отображаемый текст
	 * @param blockPos позиция блока
	 * @param yOffset  вертикальное смещение строки (0 = первая строка)
	 * @param color   цвет текста в формате ARGB
	 * @param scale   масштаб текста
	 */
	public static VisibilityConfigurable blockLabel(
			String text,
			BlockPos blockPos,
			int yOffset,
			int color,
			float scale
	) {
		Vec3d labelPos = Vec3d.add(
				blockPos,
				BLOCK_LABEL_CENTER_OFFSET,
				BLOCK_LABEL_Y_OFFSET + yOffset * BLOCK_LABEL_LINE_STEP,
				BLOCK_LABEL_CENTER_OFFSET
		);
		VisibilityConfigurable configurable = text(text, labelPos, TextGizmo.Style.left(color).scaled(scale));
		configurable.ignoreOcclusion();
		return configurable;
	}

	/**
	 * Создаёт текстовую метку над сущностью с автоматическим игнорированием окклюзии.
	 * <p>
	 * Метка размещается на высоте {@value #ENTITY_LABEL_Y_BASE} над позицией сущности,
	 * с вертикальным шагом {@value #ENTITY_LABEL_LINE_STEP} на единицу {@code yOffset}.
	 *
	 * @param entity  целевая сущность
	 * @param yOffset вертикальное смещение строки (0 = первая строка)
	 * @param text    отображаемый текст
	 * @param color   цвет текста в формате ARGB
	 * @param scale   масштаб текста
	 */
	public static VisibilityConfigurable entityLabel(Entity entity, int yOffset, String text, int color, float scale) {
		Vec3d labelPos = new Vec3d(
				entity.getBlockX() + BLOCK_LABEL_CENTER_OFFSET,
				entity.getY() + ENTITY_LABEL_Y_BASE + yOffset * ENTITY_LABEL_LINE_STEP,
				entity.getBlockZ() + BLOCK_LABEL_CENTER_OFFSET
		);
		VisibilityConfigurable configurable = text(
				text,
				labelPos,
				TextGizmo.Style.centered(color).scaled(scale).adjusted(ENTITY_LABEL_ADJUST)
		);
		configurable.ignoreOcclusion();
		return configurable;
	}

	public static VisibilityConfigurable text(String text, Vec3d pos, TextGizmo.Style style) {
		return collect(new TextGizmo(pos, text, style));
	}

	/**
	 * Scope-объект для управления временем жизни активного коллектора в текущем потоке.
	 * <p>
	 * При закрытии восстанавливает предыдущий коллектор, что позволяет безопасно
	 * вкладывать несколько scope-блоков друг в друга.
	 */
	public static class CollectorScope implements AutoCloseable {

		private final @Nullable GizmoCollector prevCollector = CURRENT_GIZMO_COLLECTOR.get();
		private boolean closed;

		CollectorScope() {
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}

			closed = true;
			CURRENT_GIZMO_COLLECTOR.set(prevCollector);
		}
	}
}
