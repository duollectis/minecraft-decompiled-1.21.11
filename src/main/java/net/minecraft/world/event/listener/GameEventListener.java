package net.minecraft.world.event.listener;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;

/**
 * Слушатель игровых событий, регистрируемый в секциях чанков.
 * Получает уведомления о событиях, произошедших в пределах {@link #getRange()} блоков.
 */
public interface GameEventListener {

	PositionSource getPositionSource();

	int getRange();

	/**
	 * Вызывается при получении игрового события в радиусе действия слушателя.
	 *
	 * @param emitterPos позиция источника события
	 * @return {@code true}, если событие было обработано
	 */
	boolean listen(ServerWorld world, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter, Vec3d emitterPos);

	default TriggerOrder getTriggerOrder() {
		return TriggerOrder.UNSPECIFIED;
	}

	/**
	 * Обёртка для объектов, содержащих слушателя игровых событий.
	 *
	 * @param <T> тип слушателя
	 */
	interface Holder<T extends GameEventListener> {

		T getEventListener();
	}

	/**
	 * Порядок срабатывания слушателя относительно других слушателей одного события.
	 */
	enum TriggerOrder {
		/** Порядок не определён — слушатель вызывается немедленно при обходе секций. */
		UNSPECIFIED,
		/** Слушатель вызывается после сортировки всех слушателей по расстоянию до источника. */
		BY_DISTANCE
	}
}
