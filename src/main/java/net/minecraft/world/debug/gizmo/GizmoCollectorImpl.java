package net.minecraft.world.debug.gizmo;

import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Стандартная реализация {@link GizmoCollector} с поддержкой времени жизни примитивов.
 * <p>
 * Хранит два списка: {@code gizmos} — активные примитивы текущего кадра,
 * и {@code pendingGizmos} — примитивы, добавленные извне через {@link #add}.
 * При вызове {@link #extractGizmos()} оба списка объединяются, устаревшие
 * примитивы удаляются, а список ожидающих очищается.
 */
public class GizmoCollectorImpl implements GizmoCollector {

	private final List<Entry> gizmos = new ArrayList<>();
	private final List<Entry> pendingGizmos = new ArrayList<>();

	@Override
	public VisibilityConfigurable collect(Gizmo gizmo) {
		Entry entry = new Entry(gizmo);
		gizmos.add(entry);
		return entry;
	}

	/**
	 * Возвращает снимок всех активных примитивов (текущих и ожидающих),
	 * удаляя из основного списка те, чьё время жизни истекло.
	 *
	 * @return список всех примитивов для отрисовки в текущем кадре
	 */
	public List<Entry> extractGizmos() {
		List<Entry> snapshot = new ArrayList<>(gizmos);
		snapshot.addAll(pendingGizmos);

		long now = Util.getMeasuringTimeMs();
		gizmos.removeIf(entry -> entry.getRemovalTime() < now);
		pendingGizmos.clear();

		return snapshot;
	}

	public List<Entry> getGizmos() {
		return gizmos;
	}

	/**
	 * Добавляет внешние примитивы в список ожидающих.
	 * Они будут включены в следующий вызов {@link #extractGizmos()}.
	 *
	 * @param incoming коллекция примитивов для добавления
	 */
	public void add(Collection<Entry> incoming) {
		pendingGizmos.addAll(incoming);
	}

	/**
	 * Запись об одном отладочном примитиве с настройками видимости.
	 * <p>
	 * Хранит сам gizmo, флаг игнорирования окклюзии, временные метки
	 * создания и удаления, а также флаг плавного затухания.
	 */
	public static class Entry implements VisibilityConfigurable {

		private final Gizmo gizmo;
		private boolean ignoreOcclusion;
		private long creationTime;
		private long removalTime;
		private boolean fadeOut;

		Entry(Gizmo gizmo) {
			this.gizmo = gizmo;
		}

		@Override
		public VisibilityConfigurable ignoreOcclusion() {
			ignoreOcclusion = true;
			return this;
		}

		@Override
		public VisibilityConfigurable withLifespan(int lifespan) {
			creationTime = Util.getMeasuringTimeMs();
			removalTime = creationTime + lifespan;
			return this;
		}

		@Override
		public VisibilityConfigurable fadeOut() {
			fadeOut = true;
			return this;
		}

		/**
		 * Вычисляет текущую прозрачность примитива.
		 * <p>
		 * При включённом затухании возвращает линейно убывающее значение
		 * от {@code 1.0} в момент создания до {@code 0.0} в момент удаления.
		 *
		 * @param time текущее время в мс (от {@link Util#getMeasuringTimeMs()})
		 * @return прозрачность в диапазоне [{@code 0.0}, {@code 1.0}]
		 */
		public float getOpacity(long time) {
			if (!fadeOut) {
				return 1.0F;
			}

			long totalLifespan = removalTime - creationTime;
			long elapsed = time - creationTime;
			return 1.0F - MathHelper.clamp((float) elapsed / (float) totalLifespan, 0.0F, 1.0F);
		}

		public boolean ignoresOcclusion() {
			return ignoreOcclusion;
		}

		public long getRemovalTime() {
			return removalTime;
		}

		public Gizmo getGizmo() {
			return gizmo;
		}
	}
}
