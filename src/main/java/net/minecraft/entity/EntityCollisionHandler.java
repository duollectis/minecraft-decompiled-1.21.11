package net.minecraft.entity;

import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Обработчик событий столкновения сущности с блоками окружающей среды.
 * Управляет очередью событий и колбэков, которые выполняются в строгом порядке:
 * сначала пре-колбэки, затем само действие события, затем пост-колбэки.
 * Реализация {@link Impl} накапливает события за тик и применяет их атомарно.
 */
public interface EntityCollisionHandler {

	/**
	 * Заглушка-пустышка для сущностей, которым не нужна обработка столкновений.
	 */
	EntityCollisionHandler DUMMY = new EntityCollisionHandler() {
		@Override
		public void addEvent(CollisionEvent event) {
		}

		@Override
		public void addPreCallback(CollisionEvent event, Consumer<Entity> callback) {
		}

		@Override
		public void addPostCallback(CollisionEvent event, Consumer<Entity> callback) {
		}
	};

	void addEvent(CollisionEvent event);

	void addPreCallback(CollisionEvent event, Consumer<Entity> callback);

	void addPostCallback(CollisionEvent event, Consumer<Entity> callback);

	/**
	 * Полноценная реализация обработчика столкновений.
	 * Хранит активные события и колбэки, применяя их при вызове {@link #runCallbacks(Entity)}.
	 * Версионирование через {@link #updateIfNecessary(int)} позволяет избежать
	 * повторного построения очереди колбэков в рамках одного тика.
	 */
	class Impl implements EntityCollisionHandler {

		private static final CollisionEvent[] ALL_EVENTS = CollisionEvent.values();
		private static final int INVALID_VERSION = -1;

		private final Set<CollisionEvent> activeEvents = EnumSet.noneOf(CollisionEvent.class);
		private final Map<CollisionEvent, List<Consumer<Entity>>> preCallbacks =
			Util.mapEnum(CollisionEvent.class, value -> new ArrayList<>());
		private final Map<CollisionEvent, List<Consumer<Entity>>> postCallbacks =
			Util.mapEnum(CollisionEvent.class, value -> new ArrayList<>());
		private final List<Consumer<Entity>> callbacks = new ArrayList<>();
		private int version = INVALID_VERSION;

		/**
		 * Перестраивает очередь колбэков, если версия изменилась с момента последнего вызова.
		 * Используется для ленивого обновления при изменении состояния столкновений.
		 *
		 * @param currentVersion текущая версия тика
		 */
		public void updateIfNecessary(int currentVersion) {
			if (version != currentVersion) {
				version = currentVersion;
				update();
			}
		}

		/**
		 * Выполняет все накопленные колбэки для сущности, останавливаясь если она умерла.
		 * После выполнения очередь очищается и версия сбрасывается.
		 *
		 * @param entity сущность, для которой применяются колбэки
		 */
		public void runCallbacks(Entity entity) {
			update();

			for (Consumer<Entity> consumer : callbacks) {
				if (!entity.isAlive()) {
					break;
				}

				consumer.accept(entity);
			}

			callbacks.clear();
			version = INVALID_VERSION;
		}

		private void update() {
			for (CollisionEvent collisionEvent : ALL_EVENTS) {
				List<Consumer<Entity>> preList = preCallbacks.get(collisionEvent);
				callbacks.addAll(preList);
				preList.clear();

				if (activeEvents.remove(collisionEvent)) {
					callbacks.add(collisionEvent.getAction());
				}

				List<Consumer<Entity>> postList = postCallbacks.get(collisionEvent);
				callbacks.addAll(postList);
				postList.clear();
			}
		}

		@Override
		public void addEvent(CollisionEvent event) {
			activeEvents.add(event);
		}

		@Override
		public void addPreCallback(CollisionEvent event, Consumer<Entity> callback) {
			preCallbacks.get(event).add(callback);
		}

		@Override
		public void addPostCallback(CollisionEvent event, Consumer<Entity> callback) {
			postCallbacks.get(event).add(callback);
		}
	}
}
