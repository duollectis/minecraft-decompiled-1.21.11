package net.minecraft.util;

import net.minecraft.server.PlayerConfigEntry;

import java.util.Optional;
import java.util.UUID;

/**
 * {@code NameToIdCache}.
 */
public interface NameToIdCache {

	void add(PlayerConfigEntry player);

	Optional<PlayerConfigEntry> findByName(String name);

	Optional<PlayerConfigEntry> getByUuid(UUID uuid);

	void setOfflineMode(boolean offlineMode);

	void save();
}
