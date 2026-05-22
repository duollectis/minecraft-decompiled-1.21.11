package net.minecraft.server;

import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

/**
 * Базовая запись конфигурации сервера (бан, оператор, белый список).
 * Хранит ключ записи и предоставляет механизм сериализации в JSON.
 *
 * @param <T> тип ключа записи (например, {@link PlayerConfigEntry} или {@link String} для IP)
 */
public abstract class ServerConfigEntry<T> {

	private final @Nullable T key;

	public ServerConfigEntry(@Nullable T key) {
		this.key = key;
	}

	public @Nullable T getKey() {
		return key;
	}

	boolean isInvalid() {
		return false;
	}

	protected abstract void write(JsonObject json);
}
