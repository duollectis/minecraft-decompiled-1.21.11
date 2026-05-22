package net.minecraft.server.dedicated.management.listener;

import net.minecraft.server.BannedIpEntry;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.OperatorEntry;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.rule.GameRule;

/**
 * Слушатель событий управления сервером: уведомления об изменениях конфигурации.
 */
public interface ManagementListener {

	void onPlayerJoined(ServerPlayerEntity player);

	void onPlayerLeft(ServerPlayerEntity player);

	void onServerStarted();

	void onServerStopping();

	void onServerSaving();

	void onServerSaved();

	void onServerActivity();

	void onOperatorAdded(OperatorEntry operator);

	void onOperatorRemoved(OperatorEntry operator);

	void onAllowlistAdded(PlayerConfigEntry player);

	void onAllowlistRemoved(PlayerConfigEntry player);

	void onIpBanAdded(BannedIpEntry entry);

	void onIpBanRemoved(String string);

	void onBanAdded(BannedPlayerEntry entry);

	void onBanRemoved(PlayerConfigEntry player);

	<T> void onGameRuleUpdated(GameRule<T> rule, T value);

	void onServerStatusHeartbeat();
}
