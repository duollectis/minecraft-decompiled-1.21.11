package net.minecraft.world.event.listener;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.data.GameEventDebugData;
import net.minecraft.world.event.GameEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Менеджер рассылки игровых событий на уровне серверного мира.
 * <p>
 * Определяет все секции чанков в радиусе события, собирает слушателей
 * с порядком {@link GameEventListener.TriggerOrder#BY_DISTANCE} в список
 * и обрабатывает их отсортированными по расстоянию. Слушатели с порядком
 * {@link GameEventListener.TriggerOrder#UNSPECIFIED} вызываются немедленно.
 */
public class GameEventDispatchManager {

	private final ServerWorld world;

	public GameEventDispatchManager(ServerWorld world) {
		this.world = world;
	}

	/**
	 * Рассылает игровое событие всем слушателям в радиусе действия.
	 * <p>
	 * Итерирует по всем секциям чанков в кубе со стороной {@code 2 * notificationRadius},
	 * центрированном на позиции источника события.
	 *
	 * @param event      зарегистрированное игровое событие
	 * @param emitterPos позиция источника события в мире
	 * @param emitter    описание источника (сущность и/или блок)
	 */
	public void dispatch(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {
		int radius = event.value().notificationRadius();
		BlockPos emitterBlockPos = BlockPos.ofFloored(emitterPos);
		int minSectionX = ChunkSectionPos.getSectionCoord(emitterBlockPos.getX() - radius);
		int minSectionY = ChunkSectionPos.getSectionCoord(emitterBlockPos.getY() - radius);
		int minSectionZ = ChunkSectionPos.getSectionCoord(emitterBlockPos.getZ() - radius);
		int maxSectionX = ChunkSectionPos.getSectionCoord(emitterBlockPos.getX() + radius);
		int maxSectionY = ChunkSectionPos.getSectionCoord(emitterBlockPos.getY() + radius);
		int maxSectionZ = ChunkSectionPos.getSectionCoord(emitterBlockPos.getZ() + radius);
		List<GameEvent.Message> byDistanceMessages = new ArrayList<>();

		GameEventDispatcher.DispatchCallback dispatchCallback = (listener, listenerPos) -> {
			if (listener.getTriggerOrder() == GameEventListener.TriggerOrder.BY_DISTANCE) {
				byDistanceMessages.add(new GameEvent.Message(event, emitterPos, emitter, listener, listenerPos));
			} else {
				listener.listen(world, event, emitter, emitterPos);
			}
		};

		boolean anyDispatched = false;

		for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
			for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
				Chunk chunk = world.getChunkManager().getWorldChunk(sectionX, sectionZ);
				if (chunk == null) {
					continue;
				}

				for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
					anyDispatched |= chunk.getGameEventDispatcher(sectionY)
						.dispatch(event, emitterPos, emitter, dispatchCallback);
				}
			}
		}

		if (!byDistanceMessages.isEmpty()) {
			dispatchListenersByDistance(byDistanceMessages);
		}

		if (anyDispatched) {
			world.getSubscriptionTracker()
				.sendEventDebugData(
					BlockPos.ofFloored(emitterPos),
					DebugSubscriptionTypes.GAME_EVENTS,
					new GameEventDebugData(event, emitterPos)
				);
		}
	}

	private void dispatchListenersByDistance(List<GameEvent.Message> messages) {
		Collections.sort(messages);

		for (GameEvent.Message message : messages) {
			message.getListener().listen(world, message.getEvent(), message.getEmitter(), message.getEmitterPos());
		}
	}
}
