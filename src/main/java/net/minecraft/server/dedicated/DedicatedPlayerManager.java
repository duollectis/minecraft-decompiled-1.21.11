package net.minecraft.server.dedicated;

import com.mojang.logging.LogUtils;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.world.PlayerSaveHandler;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Менеджер игроков выделенного сервера: управление белым списком, операторами и ограничениями подключения.
 */
public class DedicatedPlayerManager extends PlayerManager {

	private static final Logger LOGGER = LogUtils.getLogger();

	public DedicatedPlayerManager(
			MinecraftDedicatedServer minecraftDedicatedServer,
			CombinedDynamicRegistries<ServerDynamicRegistryType> tracker,
			PlayerSaveHandler saveHandler
	) {
		super(minecraftDedicatedServer, tracker, saveHandler, minecraftDedicatedServer.getManagementListener());
		this.setViewDistance(minecraftDedicatedServer.getViewDistance());
		this.setSimulationDistance(minecraftDedicatedServer.getSimulationDistance());
		this.loadUserBanList();
		this.saveUserBanList();
		this.loadIpBanList();
		this.saveIpBanList();
		this.loadOpList();
		this.loadWhitelist();
		this.saveOpList();
		if (!this.getWhitelist().getFile().exists()) {
			this.saveWhitelist();
		}
	}

	@Override
	public void reloadWhitelist() {
		this.loadWhitelist();
	}

	private void saveIpBanList() {
		try {
			this.getIpBanList().save();
		}
		catch (IOException var2) {
			LOGGER.warn("Failed to save ip banlist: ", var2);
		}
	}

	private void saveUserBanList() {
		try {
			this.getUserBanList().save();
		}
		catch (IOException var2) {
			LOGGER.warn("Failed to save user banlist: ", var2);
		}
	}

	private void loadIpBanList() {
		try {
			this.getIpBanList().load();
		}
		catch (IOException var2) {
			LOGGER.warn("Failed to load ip banlist: ", var2);
		}
	}

	private void loadUserBanList() {
		try {
			this.getUserBanList().load();
		}
		catch (IOException var2) {
			LOGGER.warn("Failed to load user banlist: ", var2);
		}
	}

	private void loadOpList() {
		try {
			this.getOpList().load();
		}
		catch (Exception var2) {
			LOGGER.warn("Failed to load operators list: ", var2);
		}
	}

	private void saveOpList() {
		try {
			this.getOpList().save();
		}
		catch (Exception var2) {
			LOGGER.warn("Failed to save operators list: ", var2);
		}
	}

	private void loadWhitelist() {
		try {
			this.getWhitelist().load();
		}
		catch (Exception var2) {
			LOGGER.warn("Failed to load white-list: ", var2);
		}
	}

	private void saveWhitelist() {
		try {
			this.getWhitelist().save();
		}
		catch (Exception var2) {
			LOGGER.warn("Failed to save white-list: ", var2);
		}
	}

	@Override
	public boolean isWhitelisted(PlayerConfigEntry player) {
		return !this.isWhitelistEnabled() || this.isOperator(player) || this.getWhitelist().isAllowed(player);
	}

	public MinecraftDedicatedServer getServer() {
		return (MinecraftDedicatedServer) super.getServer();
	}

	@Override
	public boolean canBypassPlayerLimit(PlayerConfigEntry configEntry) {
		return this.getOpList().canBypassPlayerLimit(configEntry);
	}
}
