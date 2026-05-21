package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.telemetry.WorldSession;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Иммутабельный снимок состояния клиентского соединения при переходе в фазу игры.
 * <p>Передаётся из фазы конфигурации в фазу игры и содержит все данные,
 * необходимые для инициализации игрового мира на стороне клиента.
 *
 * @param chunkLoadProgress       прогресс загрузки чанков
 * @param localGameProfile        профиль локального игрока
 * @param worldSession            сессия телеметрии мира
 * @param receivedRegistries      реестры, полученные от сервера
 * @param enabledFeatures         набор включённых функций
 * @param serverBrand             идентификатор серверного программного обеспечения
 * @param serverInfo              информация о сервере из списка серверов
 * @param postDisconnectScreen    экран, показываемый после отключения
 * @param serverCookies           куки, полученные от сервера
 * @param chatState               сохранённое состояние чата
 * @param customReportDetails     дополнительные данные для отчётов об ошибках
 * @param serverLinks             ссылки, предоставленные сервером
 * @param seenPlayers             список игроков, которых видел клиент
 * @param seenInsecureChatWarning было ли показано предупреждение о небезопасном чате
 */
@Environment(EnvType.CLIENT)
public record ClientConnectionState(
		ClientChunkLoadProgress chunkLoadProgress,
		GameProfile localGameProfile,
		WorldSession worldSession,
		DynamicRegistryManager.Immutable receivedRegistries,
		FeatureSet enabledFeatures,
		@Nullable String serverBrand,
		@Nullable ServerInfo serverInfo,
		@Nullable Screen postDisconnectScreen,
		Map<Identifier, byte[]> serverCookies,
		ChatHud.@Nullable ChatState chatState,
		Map<String, String> customReportDetails,
		ServerLinks serverLinks,
		Map<UUID, PlayerListEntry> seenPlayers,
		boolean seenInsecureChatWarning
) {
}
