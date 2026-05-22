package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.server.dedicated.management.listener.ManagementListener;

import java.io.File;
import java.util.Objects;

/**
 * Список забаненных игроков.
 * При добавлении и удалении записей уведомляет {@link ManagementListener}.
 */
public class BannedPlayerList extends ServerConfigList<PlayerConfigEntry, BannedPlayerEntry> {

	public BannedPlayerList(File file, ManagementListener managementListener) {
		super(file, managementListener);
	}

	@Override
	protected ServerConfigEntry<PlayerConfigEntry> fromJson(JsonObject json) {
		return new BannedPlayerEntry(json);
	}

	@Override
	public boolean contains(PlayerConfigEntry player) {
		return super.contains(player);
	}

	@Override
	public String[] getNames() {
		return values()
			.stream()
			.map(ServerConfigEntry::getKey)
			.filter(Objects::nonNull)
			.map(PlayerConfigEntry::name)
			.toArray(String[]::new);
	}

	@Override
	public boolean add(BannedPlayerEntry entry) {
		if (!super.add(entry)) {
			return false;
		}

		if (entry.getKey() != null) {
			managementListener.onBanAdded(entry);
		}

		return true;
	}

	@Override
	public boolean remove(PlayerConfigEntry player) {
		if (!super.remove(player)) {
			return false;
		}

		managementListener.onBanRemoved(player);
		return true;
	}

	@Override
	public void clear() {
		for (BannedPlayerEntry entry : values()) {
			if (entry.getKey() != null) {
				managementListener.onBanRemoved(entry.getKey());
			}
		}

		super.clear();
	}

	@Override
	protected String toString(PlayerConfigEntry player) {
		return player.id().toString();
	}
}
