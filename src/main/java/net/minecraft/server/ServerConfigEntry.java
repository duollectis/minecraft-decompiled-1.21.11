package net.minecraft.server;

import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;

/**
 * {@code ServerConfigEntry}.
 */
public abstract class ServerConfigEntry<T> {

	private final @Nullable T key;

	public ServerConfigEntry(@Nullable T key) {
		this.key = key;
	}

	public @Nullable T getKey() {
		return this.key;
	}

	boolean isInvalid() {
		return false;
	}

	protected abstract void write(JsonObject json);
}
