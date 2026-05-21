package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.server.dedicated.management.listener.ManagementListener;

import java.io.File;
import java.util.Objects;

/**
 * {@code OperatorList}.
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
		return this
				.values()
				.stream()
				.map(ServerConfigEntry::getKey)
				.filter(Objects::nonNull)
				.map(PlayerConfigEntry::name)
				.toArray(String[]::new);
	}

	public boolean add(OperatorEntry operatorEntry) {
		if (super.add(operatorEntry)) {
			if (operatorEntry.getKey() != null) {
				this.managementListener.onOperatorAdded(operatorEntry);
			}

			return true;
		}
		else {
			return false;
		}
	}

	public boolean remove(PlayerConfigEntry playerConfigEntry) {
		OperatorEntry operatorEntry = this.get(playerConfigEntry);
		if (super.remove(playerConfigEntry)) {
			if (operatorEntry != null) {
				this.managementListener.onOperatorRemoved(operatorEntry);
			}

			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void clear() {
		for (OperatorEntry operatorEntry : this.values()) {
			if (operatorEntry.getKey() != null) {
				this.managementListener.onOperatorRemoved(operatorEntry);
			}
		}

		super.clear();
	}

	public boolean canBypassPlayerLimit(PlayerConfigEntry playerConfigEntry) {
		OperatorEntry operatorEntry = this.get(playerConfigEntry);
		return operatorEntry != null ? operatorEntry.canBypassPlayerLimit() : false;
	}

	protected String toString(PlayerConfigEntry playerConfigEntry) {
		return playerConfigEntry.id().toString();
	}
}
