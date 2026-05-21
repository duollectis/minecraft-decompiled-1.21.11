package net.minecraft.entity;

import net.minecraft.util.Util;

import java.util.*;
import java.util.function.Consumer;

/**
 * {@code EntityCollisionHandler}.
 */
public interface EntityCollisionHandler {

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
	 * {@code Impl}.
	 */
	public static class Impl implements EntityCollisionHandler {

		private static final CollisionEvent[] ALL_EVENTS = CollisionEvent.values();
		private static final int INVALID_VERSION = -1;
		private final Set<CollisionEvent> activeEvents = EnumSet.noneOf(CollisionEvent.class);
		private final Map<CollisionEvent, List<Consumer<Entity>>>
				preCallbacks =
				Util.mapEnum(CollisionEvent.class, value -> new ArrayList<>());
		private final Map<CollisionEvent, List<Consumer<Entity>>>
				postCallbacks =
				Util.mapEnum(CollisionEvent.class, value -> new ArrayList<>());
		private final List<Consumer<Entity>> callbacks = new ArrayList<>();
		private int version = -1;

		/**
		 * Обновляет if necessary.
		 *
		 * @param version version
		 */
		public void updateIfNecessary(int version) {
			if (this.version != version) {
				this.version = version;
				this.update();
			}
		}

		/**
		 * Run callbacks.
		 *
		 * @param entity entity
		 */
		public void runCallbacks(Entity entity) {
			this.update();

			for (Consumer<Entity> consumer : this.callbacks) {
				if (!entity.isAlive()) {
					break;
				}

				consumer.accept(entity);
			}

			this.callbacks.clear();
			this.version = -1;
		}

		private void update() {
			for (CollisionEvent collisionEvent : ALL_EVENTS) {
				List<Consumer<Entity>> list = this.preCallbacks.get(collisionEvent);
				this.callbacks.addAll(list);
				list.clear();
				if (this.activeEvents.remove(collisionEvent)) {
					this.callbacks.add(collisionEvent.getAction());
				}

				List<Consumer<Entity>> list2 = this.postCallbacks.get(collisionEvent);
				this.callbacks.addAll(list2);
				list2.clear();
			}
		}

		@Override
		public void addEvent(CollisionEvent event) {
			this.activeEvents.add(event);
		}

		@Override
		public void addPreCallback(CollisionEvent event, Consumer<Entity> callback) {
			this.preCallbacks.get(event).add(callback);
		}

		@Override
		public void addPostCallback(CollisionEvent event, Consumer<Entity> callback) {
			this.postCallbacks.get(event).add(callback);
		}
	}
}
