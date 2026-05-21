package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public record CookieStorage(
		Map<Identifier, byte[]> cookies,
		Map<UUID, PlayerListEntry> seenPlayers,
		boolean seenInsecureChatWarning
) {
}
