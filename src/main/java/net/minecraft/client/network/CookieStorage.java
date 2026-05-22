package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

/**
 * Хранилище данных, переносимых между соединениями при трансфере сервера.
 * <p>Содержит куки, список виденных игроков и флаг предупреждения о небезопасном чате,
 * которые передаются в {@link ClientLoginNetworkHandler} при переходе на другой сервер.
 *
 * @param cookies                 куки, полученные от предыдущего сервера
 * @param seenPlayers             список игроков, которых видел клиент
 * @param seenInsecureChatWarning было ли показано предупреждение о небезопасном чате
 */
@Environment(EnvType.CLIENT)
public record CookieStorage(
		Map<Identifier, byte[]> cookies,
		Map<UUID, PlayerListEntry> seenPlayers,
		boolean seenInsecureChatWarning
) {
}
