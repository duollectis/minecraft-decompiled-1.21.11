package net.minecraft.world.event.listener;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

/**
 * Диспетчер игровых событий для одной секции чанка.
 * Хранит список слушателей и рассылает им события в пределах радиуса действия.
 */
public interface GameEventDispatcher {

	/** Пустая реализация-заглушка для секций без слушателей. */
	GameEventDispatcher EMPTY = new GameEventDispatcher() {
		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public void addListener(GameEventListener listener) {
		}

		@Override
		public void removeListener(GameEventListener listener) {
		}

		@Override
		public boolean dispatch(
			RegistryEntry<GameEvent> event,
			Vec3d pos,
			GameEvent.Emitter emitter,
			DispatchCallback callback
		) {
			return false;
		}
	};

	boolean isEmpty();

	void addListener(GameEventListener listener);

	void removeListener(GameEventListener listener);

	/**
	 * Рассылает событие всем слушателям в радиусе действия.
	 *
	 * @param callback вызывается для каждого слушателя, находящегося в радиусе
	 * @return {@code true}, если хотя бы один слушатель получил событие
	 */
	boolean dispatch(
		RegistryEntry<GameEvent> event,
		Vec3d pos,
		GameEvent.Emitter emitter,
		DispatchCallback callback
	);

	/**
	 * Колбэк, вызываемый для каждого слушателя, находящегося в радиусе события.
	 */
	@FunctionalInterface
	interface DispatchCallback {

		void visit(GameEventListener listener, Vec3d listenerPos);
	}
}
