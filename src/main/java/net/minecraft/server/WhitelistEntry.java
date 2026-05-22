package net.minecraft.server;

import com.google.gson.JsonObject;

/**
 * Запись белого списка игроков сервера.
 * Хранит профиль игрока ({@link PlayerConfigEntry}) и сериализует его в JSON.
 */
public class WhitelistEntry extends ServerConfigEntry<PlayerConfigEntry> {

	public WhitelistEntry(PlayerConfigEntry player) {
		super(player);
	}

	public WhitelistEntry(JsonObject json) {
		super(PlayerConfigEntry.read(json));
	}

	@Override
	protected void write(JsonObject json) {
		PlayerConfigEntry key = getKey();

		if (key != null) {
			key.write(json);
		}
	}
}
