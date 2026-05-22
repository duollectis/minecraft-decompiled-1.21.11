package net.minecraft.util;

import net.minecraft.server.PlayerConfigEntry;

import java.util.Optional;
import java.util.UUID;

/**
 * Кэш для поиска записей конфигурации игроков по имени или UUID.
 * Используется для разрешения имён игроков в offline-режиме и при управлении белым списком.
 */
public interface NameToIdCache {

	void add(PlayerConfigEntry player);

	Optional<PlayerConfigEntry> findByName(String name);

	Optional<PlayerConfigEntry> getByUuid(UUID uuid);

	void setOfflineMode(boolean offlineMode);

	void save();
}
