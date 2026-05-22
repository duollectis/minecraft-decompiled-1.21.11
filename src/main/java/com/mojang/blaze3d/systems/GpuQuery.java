package com.mojang.blaze3d.systems;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

import java.util.OptionalLong;

/**
 * GPU-запрос для измерения времени выполнения команд на видеокарте.
 * Результат доступен асинхронно — после завершения GPU-операций.
 * Обязательно закрывать после использования.
 */
@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface GpuQuery extends AutoCloseable {

	/**
	 * Возвращает результат запроса, если GPU уже завершил измерение.
	 *
	 * @return время в наносекундах, или пустой {@link OptionalLong} если результат ещё не готов
	 */
	OptionalLong getValue();

	@Override
	void close();
}
