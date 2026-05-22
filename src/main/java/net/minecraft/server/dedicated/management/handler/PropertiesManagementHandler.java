package net.minecraft.server.dedicated.management.handler;

import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;

/**
 * Класс Properties Management Handler.
 */
public interface PropertiesManagementHandler {

	boolean getAutosave();

	boolean setAutosave(boolean autosaveEnabled, ManagementConnectionId remote);

	Difficulty getDifficulty();

	Difficulty setDifficulty(Difficulty difficulty, ManagementConnectionId remote);

	boolean getEnforceAllowlist();

	boolean setEnforceAllowlist(boolean enforceAllowlist, ManagementConnectionId remote);

	boolean getUseAllowlist();

	boolean setUseAllowlist(boolean useAllowlist, ManagementConnectionId remote);

	int getMaxPlayers();

	int setMaxPlayers(int maxPlayers, ManagementConnectionId remote);

	int getPauseWhenEmptySeconds();

	int setPauseWhenEmptySeconds(int pauseWhenEmptySeconds, ManagementConnectionId remote);

	int getPlayerIdleTimeout();

	int setPlayerIdleTimeout(int playerIdleTimeout, ManagementConnectionId remote);

	boolean getAllowFlight();

	boolean setAllowFlight(boolean allowFlight, ManagementConnectionId remote);

	int getSpawnProtectionRadius();

	int setSpawnProtectionRadius(int spawnProtectionRadius, ManagementConnectionId remote);

	String getMotd();

	String setMotd(String motd, ManagementConnectionId remote);

	boolean getForceGameMode();

	boolean setForceGameMode(boolean forceGameMode, ManagementConnectionId remote);

	GameMode getGameMode();

	GameMode setGameMode(GameMode gameMode, ManagementConnectionId remote);

	int getViewDistance();

	int setViewDistance(int viewDistance, ManagementConnectionId remote);

	int getSimulationDistance();

	int setSimulationDistance(int simulationDistance, ManagementConnectionId remote);

	boolean getAcceptTransfers();

	boolean setAcceptTransfers(boolean acceptTransfers, ManagementConnectionId remote);

	int getStatusHeartbeatInterval();

	int setStatusHeartbeatInterval(int statusHeartbeatInterval, ManagementConnectionId remote);

	LeveledPermissionPredicate getOperatorUserPermissionLevel();

	LeveledPermissionPredicate setOperatorUserPermissionLevel(
			LeveledPermissionPredicate operatorUserPermissionLevel,
			ManagementConnectionId remote
	);

	boolean getHideOnlinePlayers();

	boolean setHideOnlinePlayers(boolean hideOnlinePlayers, ManagementConnectionId remote);

	boolean getStatusReplies();

	boolean setStatusReplies(boolean repliesToStatus, ManagementConnectionId remote);

	int getEntityBroadcastRange();

	int setEntityBroadcastRange(int entityBroadcastRange, ManagementConnectionId remote);
}
