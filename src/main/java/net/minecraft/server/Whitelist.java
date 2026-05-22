package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.server.dedicated.management.listener.ManagementListener;

import java.io.File;
import java.util.Objects;

/**
 * Белый список сервера: только игроки из этого списка могут подключиться,
 * если белый список включён. Уведомляет {@link ManagementListener} об изменениях.
 */
public class Whitelist extends ServerConfigList<PlayerConfigEntry, WhitelistEntry> {

	public Whitelist(File file, ManagementListener managementListener) {
		super(file, managementListener);
	}

	@Override
	protected ServerConfigEntry<PlayerConfigEntry> fromJson(JsonObject json) {
		return new WhitelistEntry(json);
	}

	public boolean isAllowed(PlayerConfigEntry playerConfigEntry) {
		return contains(playerConfigEntry);
	}

	public boolean add(WhitelistEntry whitelistEntry) {
		if (!super.add(whitelistEntry)) {
			return false;
		}

		if (whitelistEntry.getKey() != null) {
			managementListener.onAllowlistAdded(whitelistEntry.getKey());
		}

		return true;
	}

	public boolean remove(PlayerConfigEntry playerConfigEntry) {
		if (!super.remove(playerConfigEntry)) {
			return false;
		}

		managementListener.onAllowlistRemoved(playerConfigEntry);
		return true;
	}

	@Override
	public void clear() {
		for (WhitelistEntry entry : values()) {
			if (entry.getKey() != null) {
				managementListener.onAllowlistRemoved(entry.getKey());
			}
		}

		super.clear();
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
	protected String toString(PlayerConfigEntry playerConfigEntry) {
		return playerConfigEntry.id().toString();
	}
}
