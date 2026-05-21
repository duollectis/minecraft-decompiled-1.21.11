package net.minecraft.server.world;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;

/**
 * {@code PlayerChunkWatchingManager}.
 */
public final class PlayerChunkWatchingManager {

	private final Object2BooleanMap<ServerPlayerEntity> watchingPlayers = new Object2BooleanOpenHashMap();

	public Set<ServerPlayerEntity> getPlayersWatchingChunk() {
		return this.watchingPlayers.keySet();
	}

	/**
	 * Add.
	 *
	 * @param player player
	 * @param inactive inactive
	 */
	public void add(ServerPlayerEntity player, boolean inactive) {
		this.watchingPlayers.put(player, inactive);
	}

	/**
	 * Remove.
	 *
	 * @param player player
	 */
	public void remove(ServerPlayerEntity player) {
		this.watchingPlayers.removeBoolean(player);
	}

	/**
	 * Отключает watch.
	 *
	 * @param player player
	 */
	public void disableWatch(ServerPlayerEntity player) {
		this.watchingPlayers.replace(player, true);
	}

	/**
	 * Включает watch.
	 *
	 * @param player player
	 */
	public void enableWatch(ServerPlayerEntity player) {
		this.watchingPlayers.replace(player, false);
	}

	public boolean isWatchInactive(ServerPlayerEntity player) {
		return this.watchingPlayers.getOrDefault(player, true);
	}

	public boolean isWatchDisabled(ServerPlayerEntity player) {
		return this.watchingPlayers.getBoolean(player);
	}
}
