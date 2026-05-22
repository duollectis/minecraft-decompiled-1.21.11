package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.server.dedicated.management.listener.ManagementListener;

import java.io.File;
import java.util.Objects;

/**
 * Список операторов сервера.
 * При добавлении и удалении записей уведомляет {@link ManagementListener}.
 */
public class OperatorList extends ServerConfigList<PlayerConfigEntry, OperatorEntry> {

	public OperatorList(File file, ManagementListener managementListener) {
		super(file, managementListener);
	}

	@Override
	protected ServerConfigEntry<PlayerConfigEntry> fromJson(JsonObject json) {
		return new OperatorEntry(json);
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
	public boolean add(OperatorEntry entry) {
		if (!super.add(entry)) {
			return false;
		}

		if (entry.getKey() != null) {
			managementListener.onOperatorAdded(entry);
		}

		return true;
	}

	@Override
	public boolean remove(PlayerConfigEntry player) {
		OperatorEntry entry = get(player);

		if (!super.remove(player)) {
			return false;
		}

		if (entry != null) {
			managementListener.onOperatorRemoved(entry);
		}

		return true;
	}

	@Override
	public void clear() {
		for (OperatorEntry entry : values()) {
			if (entry.getKey() != null) {
				managementListener.onOperatorRemoved(entry);
			}
		}

		super.clear();
	}

	public boolean canBypassPlayerLimit(PlayerConfigEntry player) {
		OperatorEntry entry = get(player);
		return entry != null && entry.canBypassPlayerLimit();
	}

	@Override
	protected String toString(PlayerConfigEntry player) {
		return player.id().toString();
	}
}
