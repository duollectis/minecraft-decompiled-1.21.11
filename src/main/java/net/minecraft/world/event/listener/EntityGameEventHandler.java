package net.minecraft.world.event.listener;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Управляет регистрацией {@link GameEventListener} сущности в диспетчерах событий чанков.
 * <p>
 * При перемещении сущности автоматически переносит слушателя из старой секции чанка
 * в новую, обеспечивая корректное получение событий независимо от позиции сущности.
 *
 * @param <T> тип слушателя игровых событий
 */
public class EntityGameEventHandler<T extends GameEventListener> {

	private final T listener;
	private @Nullable ChunkSectionPos sectionPos;

	public EntityGameEventHandler(T listener) {
		this.listener = listener;
	}

	public T getListener() {
		return listener;
	}

	/**
	 * Вызывается движком при установке позиции сущности через колбэк.
	 * Делегирует в {@link #onEntitySetPos(ServerWorld)}.
	 */
	public void onEntitySetPosCallback(ServerWorld world) {
		onEntitySetPos(world);
	}

	/**
	 * Удаляет слушателя из диспетчера текущей секции чанка при удалении сущности из мира.
	 */
	public void onEntityRemoval(ServerWorld world) {
		updateDispatcher(world, sectionPos, dispatcher -> dispatcher.removeListener(listener));
	}

	/**
	 * Обновляет регистрацию слушателя при изменении позиции сущности.
	 * Если сущность перешла в другую секцию чанка — снимает регистрацию со старой
	 * и регистрирует в новой.
	 */
	public void onEntitySetPos(ServerWorld world) {
		listener.getPositionSource().getPos(world).map(ChunkSectionPos::from).ifPresent(newSectionPos -> {
			if (sectionPos == null || !sectionPos.equals(newSectionPos)) {
				updateDispatcher(world, sectionPos, dispatcher -> dispatcher.removeListener(listener));
				sectionPos = newSectionPos;
				updateDispatcher(world, sectionPos, dispatcher -> dispatcher.addListener(listener));
			}
		});
	}

	private static void updateDispatcher(
		WorldView world,
		@Nullable ChunkSectionPos sectionPos,
		Consumer<GameEventDispatcher> dispatcherConsumer
	) {
		if (sectionPos == null) {
			return;
		}

		Chunk chunk = world.getChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), ChunkStatus.FULL, false);
		if (chunk != null) {
			dispatcherConsumer.accept(chunk.getGameEventDispatcher(sectionPos.getSectionY()));
		}
	}
}
