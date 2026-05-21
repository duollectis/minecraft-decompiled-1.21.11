package net.minecraft.server.dedicated.management.handler;

import net.minecraft.server.BannedIpEntry;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;

import java.util.Collection;

/**
 * {@code BanManagementHandler}.
 */
public interface BanManagementHandler {

	void addPlayer(BannedPlayerEntry entry, ManagementConnectionId remote);

	void removePlayer(PlayerConfigEntry entry, ManagementConnectionId remote);

	Collection<BannedPlayerEntry> getUserBanList();

	Collection<BannedIpEntry> getIpBanList();

	void addIpAddress(BannedIpEntry entry, ManagementConnectionId remote);

	void clearIpBanList(ManagementConnectionId remote);

	void removeIpAddress(String ipAddress, ManagementConnectionId remote);

	void clearBanList(ManagementConnectionId remote);
}
