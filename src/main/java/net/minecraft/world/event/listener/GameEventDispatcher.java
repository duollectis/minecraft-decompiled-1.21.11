package net.minecraft.world.event.listener;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

/**
 * {@code GameEventDispatcher}.
 */
public interface GameEventDispatcher {

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
				GameEventDispatcher.DispatchCallback callback
		) {
			return false;
		}
	};

	boolean isEmpty();

	void addListener(GameEventListener listener);

	void removeListener(GameEventListener listener);

	boolean dispatch(
			RegistryEntry<GameEvent> event,
			Vec3d pos,
			GameEvent.Emitter emitter,
			GameEventDispatcher.DispatchCallback callback
	);

	@FunctionalInterface
	/**
	 * {@code DispatchCallback}.
	 */
	public interface DispatchCallback {

		void visit(GameEventListener listener, Vec3d listenerPos);
	}
}
