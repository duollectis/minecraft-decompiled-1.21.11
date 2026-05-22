package net.minecraft.world.debug;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Хранилище отладочных данных, сгруппированных по типу подписки.
 * <p>
 * Предоставляет доступ к данным, привязанным к чанкам, блокам, сущностям
 * и временным событиям. Используется клиентской стороной для визуализации
 * отладочной информации, полученной с сервера.
 */
public interface DebugDataStore {

	/**
	 * Перебирает все данные, привязанные к чанкам, для заданного типа подписки.
	 *
	 * @param type   тип отладочной подписки
	 * @param action действие, вызываемое для каждой пары (позиция чанка, данные)
	 */
	<T> void forEachChunkData(DebugSubscriptionType<T> type, BiConsumer<ChunkPos, T> action);

	/**
	 * Возвращает данные, привязанные к конкретному чанку.
	 *
	 * @param type     тип отладочной подписки
	 * @param chunkPos позиция чанка
	 * @return данные или {@code null}, если для данного чанка ничего не зарегистрировано
	 */
	<T> @Nullable T getChunkData(DebugSubscriptionType<T> type, ChunkPos chunkPos);

	/**
	 * Перебирает все данные, привязанные к блокам, для заданного типа подписки.
	 *
	 * @param type   тип отладочной подписки
	 * @param action действие, вызываемое для каждой пары (позиция блока, данные)
	 */
	<T> void forEachBlockData(DebugSubscriptionType<T> type, BiConsumer<BlockPos, T> action);

	/**
	 * Возвращает данные, привязанные к конкретному блоку.
	 *
	 * @param type тип отладочной подписки
	 * @param pos  позиция блока
	 * @return данные или {@code null}, если для данного блока ничего не зарегистрировано
	 */
	<T> @Nullable T getBlockData(DebugSubscriptionType<T> type, BlockPos pos);

	/**
	 * Перебирает все данные, привязанные к сущностям, для заданного типа подписки.
	 *
	 * @param type   тип отладочной подписки
	 * @param action действие, вызываемое для каждой пары (сущность, данные)
	 */
	<T> void forEachEntityData(DebugSubscriptionType<T> type, BiConsumer<Entity, T> action);

	/**
	 * Возвращает данные, привязанные к конкретной сущности.
	 *
	 * @param type   тип отладочной подписки
	 * @param entity целевая сущность
	 * @return данные или {@code null}, если для данной сущности ничего не зарегистрировано
	 */
	<T> @Nullable T getEntityData(DebugSubscriptionType<T> type, Entity entity);

	/**
	 * Перебирает все временные события для заданного типа подписки.
	 *
	 * @param type   тип отладочной подписки
	 * @param action потребитель события с информацией о времени жизни
	 */
	<T> void forEachEvent(DebugSubscriptionType<T> type, DebugDataStore.EventConsumer<T> action);

	/**
	 * Потребитель временного отладочного события.
	 * <p>
	 * Помимо самого значения, получает информацию о времени жизни события,
	 * что позволяет реализовывать визуальное затухание или фильтрацию устаревших данных.
	 */
	@FunctionalInterface
	interface EventConsumer<T> {

		/**
		 * @param value         значение события
		 * @param remainingTime оставшееся время жизни события в тиках
		 * @param expiry        полное время жизни события в тиках
		 */
		void accept(T value, int remainingTime, int expiry);
	}
}
