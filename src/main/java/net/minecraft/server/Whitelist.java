package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.server.dedicated.management.listener.ManagementListener;

import java.io.File;
import java.util.Objects;

/**
 * {@code Whitelist}.
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
		return this.contains(playerConfigEntry);
	}

	/**
	 * Add.
	 *
	 * @param whitelistEntry whitelist entry
	 *
	 * @return boolean — результат операции
	 */
	public boolean add(WhitelistEntry whitelistEntry) {
		if (super.add(whitelistEntry)) {
			if (whitelistEntry.getKey() != null) {
				this.managementListener.onAllowlistAdded(whitelistEntry.getKey());
			}

			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Remove.
	 *
	 * @param playerConfigEntry player config entry
	 *
	 * @return boolean — результат операции
	 */
	public boolean remove(PlayerConfigEntry playerConfigEntry) {
		if (super.remove(playerConfigEntry)) {
			this.managementListener.onAllowlistRemoved(playerConfigEntry);
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void clear() {
		for (WhitelistEntry whitelistEntry : this.values()) {
			if (whitelistEntry.getKey() != null) {
				this.managementListener.onAllowlistRemoved(whitelistEntry.getKey());
			}
		}

		super.clear();
	}

	@Override
	public String[] getNames() {
		return this
				.values()
				.stream()
				.map(ServerConfigEntry::getKey)
				.filter(Objects::nonNull)
				.map(PlayerConfigEntry::name)
				.toArray(String[]::new);
	}

	/**
	 * To string.
	 *
	 * @param playerConfigEntry player config entry
	 *
	 * @return String — результат операции
	 */
	protected String toString(PlayerConfigEntry playerConfigEntry) {
		return playerConfigEntry.id().toString();
	}
}
