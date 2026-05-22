package net.minecraft.world.updater;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;

import java.util.function.Supplier;

/**
 * Функциональный интерфейс для обновления NBT-данных чанка при миграции формата сохранения.
 * Применяется в {@link WorldUpdater} для поочерёдной обработки всех чанков мира.
 */
@FunctionalInterface
public interface ChunkUpdater {

	/**
	 * Фабрика, создающая «сквозной» обновлятор, возвращающий NBT без изменений.
	 * Используется для измерений, не требующих конвертации данных.
	 */
	Supplier<ChunkUpdater> PASSTHROUGH_FACTORY = () -> nbt -> nbt;

	/**
	 * Применяет необходимые исправления к NBT-данным чанка.
	 *
	 * @param chunkNbt исходные NBT-данные чанка
	 * @return обновлённые NBT-данные чанка
	 */
	NbtCompound applyFix(NbtCompound chunkNbt);

	/**
	 * Вызывается после успешной обработки чанка.
	 * Используется для обновления индексов состояния обновления.
	 *
	 * @param chunkPos позиция обработанного чанка
	 */
	default void markChunkDone(ChunkPos chunkPos) {
	}

	/**
	 * @return целевая версия данных, до которой обновляет этот апдейтор,
	 *         или {@code -1} если версия не ограничена
	 */
	default int targetDataVersion() {
		return -1;
	}
}
