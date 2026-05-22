package net.minecraft.world.event.listener;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.entity.Entity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.GameEventListenerDebugData;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.EntityPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Стандартная реализация {@link GameEventDispatcher} для секции чанка.
 * <p>
 * Поддерживает безопасное добавление и удаление слушателей во время итерации:
 * изменения буферизуются в {@code toAdd} / {@code toRemove} и применяются
 * после завершения текущего цикла рассылки.
 */
public class SimpleGameEventDispatcher implements GameEventDispatcher {

	private final List<GameEventListener> listeners = Lists.newArrayList();
	private final Set<GameEventListener> toRemove = Sets.newHashSet();
	private final List<GameEventListener> toAdd = Lists.newArrayList();
	private boolean dispatching;
	private final ServerWorld world;
	private final int ySectionCoord;
	private final DisposalCallback disposalCallback;

	public SimpleGameEventDispatcher(
		ServerWorld world,
		int ySectionCoord,
		DisposalCallback disposalCallback
	) {
		this.world = world;
		this.ySectionCoord = ySectionCoord;
		this.disposalCallback = disposalCallback;
	}

	@Override
	public boolean isEmpty() {
		return listeners.isEmpty();
	}

	@Override
	public void addListener(GameEventListener listener) {
		if (dispatching) {
			toAdd.add(listener);
		} else {
			listeners.add(listener);
		}

		sendDebugData(world, listener);
	}

	@Override
	public void removeListener(GameEventListener listener) {
		if (dispatching) {
			toRemove.add(listener);
		} else {
			listeners.remove(listener);
		}

		if (listeners.isEmpty()) {
			disposalCallback.apply(ySectionCoord);
		}
	}

	/**
	 * Рассылает событие всем слушателям секции, находящимся в радиусе действия.
	 * Во время итерации изменения списка буферизуются и применяются после её завершения.
	 *
	 * @return {@code true}, если хотя бы один слушатель получил событие
	 */
	@Override
	public boolean dispatch(
		RegistryEntry<GameEvent> event,
		Vec3d pos,
		GameEvent.Emitter emitter,
		GameEventDispatcher.DispatchCallback callback
	) {
		dispatching = true;
		boolean anyDispatched = false;

		try {
			Iterator<GameEventListener> iterator = listeners.iterator();

			while (iterator.hasNext()) {
				GameEventListener listener = iterator.next();

				if (toRemove.remove(listener)) {
					iterator.remove();
					continue;
				}

				Optional<Vec3d> listenerPos = dispatchTo(world, pos, listener);
				if (listenerPos.isPresent()) {
					callback.visit(listener, listenerPos.get());
					anyDispatched = true;
				}
			}
		} finally {
			dispatching = false;
		}

		if (!toAdd.isEmpty()) {
			listeners.addAll(toAdd);
			toAdd.clear();
		}

		if (!toRemove.isEmpty()) {
			listeners.removeAll(toRemove);
			toRemove.clear();
		}

		return anyDispatched;
	}

	/**
	 * Проверяет, находится ли слушатель в радиусе действия события.
	 *
	 * @param emitterPos позиция источника события
	 * @param listener   проверяемый слушатель
	 * @return позиция слушателя, если он в радиусе; иначе {@link Optional#empty()}
	 */
	private static Optional<Vec3d> dispatchTo(ServerWorld world, Vec3d emitterPos, GameEventListener listener) {
		Optional<Vec3d> listenerPosOptional = listener.getPositionSource().getPos(world);
		if (listenerPosOptional.isEmpty()) {
			return Optional.empty();
		}

		Vec3d listenerPos = listenerPosOptional.get();
		double squaredDistance = BlockPos.ofFloored(listenerPos).getSquaredDistance(BlockPos.ofFloored(emitterPos));
		int squaredRange = listener.getRange() * listener.getRange();
		return squaredDistance > squaredRange ? Optional.empty() : listenerPosOptional;
	}

	private static void sendDebugData(ServerWorld world, GameEventListener listener) {
		if (!world.getSubscriptionTracker().isSubscribed(DebugSubscriptionTypes.GAME_EVENT_LISTENERS)) {
			return;
		}

		GameEventListenerDebugData debugData = new GameEventListenerDebugData(listener.getRange());
		PositionSource positionSource = listener.getPositionSource();

		if (positionSource instanceof BlockPositionSource blockPositionSource) {
			world.getSubscriptionTracker()
				.sendBlockDebugData(
					blockPositionSource.pos(),
					DebugSubscriptionTypes.GAME_EVENT_LISTENERS,
					debugData
				);
		} else if (positionSource instanceof EntityPositionSource entityPositionSource) {
			Entity entity = world.getEntity(entityPositionSource.getUuid());
			if (entity != null) {
				world.getSubscriptionTracker()
					.sendEntityDebugData(
						entity,
						DebugSubscriptionTypes.GAME_EVENT_LISTENERS,
						debugData
					);
			}
		}
	}

	/**
	 * Колбэк, вызываемый при опустошении списка слушателей секции.
	 * Позволяет чанку освободить диспетчер для данной Y-секции.
	 */
	@FunctionalInterface
	public interface DisposalCallback {

		void apply(int ySectionCoord);
	}
}
