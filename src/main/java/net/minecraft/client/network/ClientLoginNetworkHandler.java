package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.*;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.listener.ClientLoginPacketListener;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket;
import net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket;
import net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket;
import net.minecraft.network.packet.s2c.login.*;
import net.minecraft.network.state.ConfigurationStates;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerLinks;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Обработчик пакетов фазы логина на стороне клиента.
 * Управляет аутентификацией, шифрованием и переходом в фазу конфигурации.
 */
@Environment(EnvType.CLIENT)
public class ClientLoginNetworkHandler implements ClientLoginPacketListener {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final MinecraftClient client;
	private final @Nullable ServerInfo serverInfo;
	private final @Nullable Screen parentScreen;
	private final Consumer<Text> statusConsumer;
	private final ClientConnection connection;
	private final boolean newWorld;
	private final @Nullable Duration worldLoadTime;
	private @Nullable String minigameName;
	private final ClientChunkLoadProgress clientChunkLoadProgress;
	private final Map<Identifier, byte[]> serverCookies;
	private final boolean hasCookies;
	private final Map<UUID, PlayerListEntry> playersByUuid;
	private final boolean seenInsecureChatWarning;
	private final AtomicReference<ClientLoginNetworkHandler.State> state =
			new AtomicReference<>(ClientLoginNetworkHandler.State.CONNECTING);

	/**
	 * @param connection              сетевое соединение
	 * @param client                  клиент Minecraft
	 * @param serverInfo              информация о сервере или {@code null}
	 * @param parentScreen            экран для возврата при отключении
	 * @param newWorld                {@code true} если это новый мир
	 * @param worldLoadTime           время загрузки мира для телеметрии
	 * @param statusConsumer          колбэк для отображения статуса подключения
	 * @param clientChunkLoadProgress прогресс загрузки чанков
	 * @param cookieStorage           хранилище куки от предыдущего соединения или {@code null}
	 */
	public ClientLoginNetworkHandler(
			ClientConnection connection,
			MinecraftClient client,
			@Nullable ServerInfo serverInfo,
			@Nullable Screen parentScreen,
			boolean newWorld,
			@Nullable Duration worldLoadTime,
			Consumer<Text> statusConsumer,
			ClientChunkLoadProgress clientChunkLoadProgress,
			@Nullable CookieStorage cookieStorage
	) {
		this.connection = connection;
		this.client = client;
		this.serverInfo = serverInfo;
		this.parentScreen = parentScreen;
		this.statusConsumer = statusConsumer;
		this.newWorld = newWorld;
		this.worldLoadTime = worldLoadTime;
		this.clientChunkLoadProgress = clientChunkLoadProgress;
		serverCookies = cookieStorage != null ? new HashMap<>(cookieStorage.cookies()) : new HashMap<>();
		playersByUuid = cookieStorage != null ? cookieStorage.seenPlayers() : Map.of();
		seenInsecureChatWarning = cookieStorage != null && cookieStorage.seenInsecureChatWarning();
		hasCookies = cookieStorage != null;
	}

	@Override
	public void onHello(LoginHelloS2CPacket packet) {
		switchTo(ClientLoginNetworkHandler.State.AUTHORIZING);

		Cipher decryptionCipher;
		Cipher encryptionCipher;
		String serverId;
		LoginKeyC2SPacket keyPacket;

		try {
			SecretKey secretKey = NetworkEncryptionUtils.generateSecretKey();
			PublicKey publicKey = packet.getPublicKey();
			serverId = new BigInteger(
					NetworkEncryptionUtils.computeServerId(packet.getServerId(), publicKey, secretKey)
			).toString(16);
			decryptionCipher = NetworkEncryptionUtils.cipherFromKey(2, secretKey);
			encryptionCipher = NetworkEncryptionUtils.cipherFromKey(1, secretKey);
			keyPacket = new LoginKeyC2SPacket(secretKey, publicKey, packet.getNonce());
		}
		catch (Exception e) {
			throw new IllegalStateException("Protocol error", e);
		}

		if (packet.needsAuthentication() == false) {
			setupEncryption(keyPacket, decryptionCipher, encryptionCipher);
			return;
		}

		final Cipher finalDecryption = decryptionCipher;
		final Cipher finalEncryption = encryptionCipher;
		final LoginKeyC2SPacket finalKeyPacket = keyPacket;
		final String finalServerId = serverId;

		Util.getIoWorkerExecutor().execute(() -> {
			Text error = joinServerSession(finalServerId);

			if (error != null) {
				if (serverInfo == null || serverInfo.isLocal() == false) {
					connection.disconnect(error);
					return;
				}

				LOGGER.warn(error.getString());
			}

			setupEncryption(finalKeyPacket, finalDecryption, finalEncryption);
		});
	}

	@Override
	public void onSuccess(LoginSuccessS2CPacket packet) {
		switchTo(ClientLoginNetworkHandler.State.JOINING);
		GameProfile profile = packet.profile();

		connection.transitionInbound(
				ConfigurationStates.S2C,
				new ClientConfigurationNetworkHandler(
						client,
						connection,
						new ClientConnectionState(
								clientChunkLoadProgress,
								profile,
								client.getTelemetryManager().createWorldSession(newWorld, worldLoadTime, minigameName),
								ClientDynamicRegistryType
										.createCombinedDynamicRegistries()
										.getCombinedRegistryManager(),
								FeatureFlags.DEFAULT_ENABLED_FEATURES,
								null,
								serverInfo,
								parentScreen,
								serverCookies,
								null,
								Map.of(),
								ServerLinks.EMPTY,
								playersByUuid,
								false
						)
				)
		);

		connection.send(EnterConfigurationC2SPacket.INSTANCE);
		connection.transitionOutbound(ConfigurationStates.C2S);
		connection.send(new CustomPayloadC2SPacket(new BrandCustomPayload(ClientBrandRetriever.getClientModName())));
		connection.send(new ClientOptionsC2SPacket(client.options.getSyncedOptions()));
	}

	@Override
	public void onDisconnected(DisconnectionInfo info) {
		Text title = hasCookies ? ScreenTexts.CONNECT_FAILED_TRANSFER : ScreenTexts.CONNECT_FAILED;

		if (serverInfo != null && serverInfo.isRealm()) {
			client.setScreen(new DisconnectedScreen(parentScreen, title, info.reason(), ScreenTexts.BACK));
		}
		else {
			client.setScreen(new DisconnectedScreen(parentScreen, title, info));
		}
	}

	@Override
	public boolean isConnectionOpen() {
		return connection.isOpen();
	}

	@Override
	public void onDisconnect(LoginDisconnectS2CPacket packet) {
		connection.disconnect(packet.reason());
	}

	@Override
	public void onCompression(LoginCompressionS2CPacket packet) {
		if (connection.isLocal()) {
			return;
		}

		connection.setCompressionThreshold(packet.getCompressionThreshold(), false);
	}

	@Override
	public void onQueryRequest(LoginQueryRequestS2CPacket packet) {
		statusConsumer.accept(Text.translatable("connect.negotiating"));
		connection.send(new LoginQueryResponseC2SPacket(packet.queryId(), null));
	}

	/**
	 * Устанавливает имя мини-игры для телеметрии.
	 *
	 * @param minigameName название мини-игры или {@code null}
	 */
	public void setMinigameName(@Nullable String minigameName) {
		this.minigameName = minigameName;
	}

	@Override
	public void onCookieRequest(CookieRequestS2CPacket packet) {
		connection.send(new CookieResponseC2SPacket(packet.key(), serverCookies.get(packet.key())));
	}

	@Override
	public void addCustomCrashReportInfo(CrashReport report, CrashReportSection section) {
		section.add("Server type", () -> serverInfo != null ? serverInfo.getServerType().toString() : "<unknown>");
		section.add("Login phase", () -> state.get().toString());
		section.add("Is Local", () -> String.valueOf(connection.isLocal()));
	}

	private void switchTo(ClientLoginNetworkHandler.State newState) {
		ClientLoginNetworkHandler.State updated = state.updateAndGet(current -> {
			if (newState.prevStates.contains(current) == false) {
				throw new IllegalStateException(
						"Tried to switch to " + newState + " from " + current
								+ ", but expected one of " + newState.prevStates
				);
			}

			return newState;
		});
		statusConsumer.accept(updated.name);
	}

	private void setupEncryption(LoginKeyC2SPacket keyPacket, Cipher decryptionCipher, Cipher encryptionCipher) {
		switchTo(ClientLoginNetworkHandler.State.ENCRYPTING);
		connection.send(
				keyPacket,
				PacketCallbacks.always(() -> connection.setupEncryption(decryptionCipher, encryptionCipher))
		);
	}

	private @Nullable Text joinServerSession(String serverId) {
		try {
			client.getApiServices()
			      .sessionService()
			      .joinServer(client.getSession().getUuidOrNull(), client.getSession().getAccessToken(), serverId);
			return null;
		}
		catch (AuthenticationUnavailableException e) {
			return Text.translatable(
					"disconnect.loginFailedInfo",
					Text.translatable("disconnect.loginFailedInfo.serversUnavailable")
			);
		}
		catch (InvalidCredentialsException e) {
			return Text.translatable(
					"disconnect.loginFailedInfo",
					Text.translatable("disconnect.loginFailedInfo.invalidSession")
			);
		}
		catch (InsufficientPrivilegesException e) {
			return Text.translatable(
					"disconnect.loginFailedInfo",
					Text.translatable("disconnect.loginFailedInfo.insufficientPrivileges")
			);
		}
		catch (ForcedUsernameChangeException | UserBannedException e) {
			return Text.translatable(
					"disconnect.loginFailedInfo",
					Text.translatable("disconnect.loginFailedInfo.userBanned")
			);
		}
		catch (AuthenticationException e) {
			return Text.translatable("disconnect.loginFailedInfo", e.getMessage());
		}
	}

	/**
	 * Состояния процесса логина.
	 */
	@Environment(EnvType.CLIENT)
	enum State {
		CONNECTING(Text.translatable("connect.connecting"), Set.of()),
		AUTHORIZING(Text.translatable("connect.authorizing"), Set.of(CONNECTING)),
		ENCRYPTING(Text.translatable("connect.encrypting"), Set.of(AUTHORIZING)),
		JOINING(Text.translatable("connect.joining"), Set.of(ENCRYPTING, CONNECTING));

		final Text name;
		final Set<ClientLoginNetworkHandler.State> prevStates;

		State(final Text name, final Set<ClientLoginNetworkHandler.State> prevStates) {
			this.name = name;
			this.prevStates = prevStates;
		}
	}
}
