package net.minecraft.server;

import com.google.gson.JsonObject;
import net.minecraft.server.dedicated.management.listener.ManagementListener;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.net.SocketAddress;

/**
 * Список забаненных IP-адресов.
 * При добавлении и удалении записей уведомляет {@link ManagementListener}.
 */
public class BannedIpList extends ServerConfigList<String, BannedIpEntry> {

	public BannedIpList(File file, ManagementListener managementListener) {
		super(file, managementListener);
	}

	@Override
	protected ServerConfigEntry<String> fromJson(JsonObject json) {
		return new BannedIpEntry(json);
	}

	public boolean isBanned(SocketAddress address) {
		return contains(stringifyAddress(address));
	}

	public boolean isBanned(String ip) {
		return contains(ip);
	}

	public @Nullable BannedIpEntry get(SocketAddress address) {
		return get(stringifyAddress(address));
	}

	@Override
	public boolean add(BannedIpEntry entry) {
		if (!super.add(entry)) {
			return false;
		}

		if (entry.getKey() != null) {
			managementListener.onIpBanAdded(entry);
		}

		return true;
	}

	@Override
	public boolean remove(String ip) {
		if (!super.remove(ip)) {
			return false;
		}

		managementListener.onIpBanRemoved(ip);
		return true;
	}

	@Override
	public void clear() {
		for (BannedIpEntry entry : values()) {
			if (entry.getKey() != null) {
				managementListener.onIpBanRemoved(entry.getKey());
			}
		}

		super.clear();
	}

	/**
	 * Извлекает строковое представление IP из {@link SocketAddress}, отбрасывая порт и слэш-префикс.
	 */
	private String stringifyAddress(SocketAddress address) {
		String raw = address.toString();

		if (raw.contains("/")) {
			raw = raw.substring(raw.indexOf('/') + 1);
		}

		if (raw.contains(":")) {
			raw = raw.substring(0, raw.indexOf(':'));
		}

		return raw;
	}
}
