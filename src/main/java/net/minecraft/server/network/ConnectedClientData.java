package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;

/**
 * Класс Connected Client Data.
 */
public record ConnectedClientData(
		GameProfile gameProfile,
		int latency,
		SyncedClientOptions syncedOptions,
		boolean transferred
) {

	/**
	 * Создаёт default.
	 *
	 * @param profile profile
	 * @param bl bl
	 *
	 * @return ConnectedClientData — результат операции
	 */
	public static ConnectedClientData createDefault(GameProfile profile, boolean bl) {
		return new ConnectedClientData(profile, 0, SyncedClientOptions.createDefault(), bl);
	}
}
