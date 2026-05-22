package net.minecraft.server.dedicated.management.handler;

import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.management.ManagementLogger;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;

import java.util.Collection;

/**
 * Класс Allowlist Management Handler Impl.
 */
public class AllowlistManagementHandlerImpl implements AllowlistManagementHandler {

	private final MinecraftDedicatedServer server;
	private final ManagementLogger logger;

	public AllowlistManagementHandlerImpl(MinecraftDedicatedServer server, ManagementLogger logger) {
		this.server = server;
		this.logger = logger;
	}

	@Override
	public Collection<WhitelistEntry> getAllowlist() {
		return this.server.getPlayerManager().getWhitelist().values();
	}

	@Override
	public boolean add(WhitelistEntry player, ManagementConnectionId remote) {
		this.logger.logAction(remote, "Add player '{}' to allowlist", player.getKey());
		return this.server.getPlayerManager().getWhitelist().add(player);
	}

	@Override
	public void clear(ManagementConnectionId remote) {
		this.logger.logAction(remote, "Clear allowlist");
		this.server.getPlayerManager().getWhitelist().clear();
	}

	@Override
	public void remove(PlayerConfigEntry player, ManagementConnectionId remote) {
		this.logger.logAction(remote, "Remove player '{}' from allowlist", player);
		this.server.getPlayerManager().getWhitelist().remove(player);
	}

	@Override
	public void kickUnlisted(ManagementConnectionId remote) {
		this.logger.logAction(remote, "Kick unlisted players");
		this.server.kickNonWhitelistedPlayers();
	}
}
