package net.minecraft.nbt.scanner;

import net.minecraft.nbt.NbtType;

import java.util.List;

/**
 * Запрос на выборочное чтение поля из NBT-структуры.
 * <p>
 * Описывает путь к целевому полю через вложенные компаунды ({@code path}),
 * ожидаемый тип поля ({@code type}) и имя конечного ключа ({@code key}).
 * Используется в {@link SelectiveNbtCollector} и {@link ExclusiveNbtCollector}.
 *
 * @param path список ключей вложенных компаундов, ведущих к целевому полю
 * @param type ожидаемый тип целевого поля
 * @param key  имя целевого поля в конечном компаунде
 */
public record NbtScanQuery(List<String> path, NbtType<?> type, String key) {

	/** Запрос к полю в корневом компаунде (без вложенности). */
	public NbtScanQuery(NbtType<?> type, String key) {
		this(List.of(), type, key);
	}

	/** Запрос к полю в компаунде, вложенном на один уровень. */
	public NbtScanQuery(String path, NbtType<?> type, String key) {
		this(List.of(path), type, key);
	}

	/** Запрос к полю в компаунде, вложенном на два уровня. */
	public NbtScanQuery(String path1, String path2, NbtType<?> type, String key) {
		this(List.of(path1, path2), type, key);
	}
}
