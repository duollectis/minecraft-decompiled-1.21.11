package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.dialog.DialogNetworkAccess;
import net.minecraft.client.gui.screen.multiplayer.CodeOfConductScreen;
import net.minecraft.client.resource.ClientDataPackManager;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ClientConfigurationPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.config.AcceptCodeOfConductC2SPacket;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.s2c.config.*;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;

/**
 * Обработчик пакетов фазы конфигурации на стороне клиента.
 * Принимает реестры, теги, фичи и датапаки от сервера,
 * затем переходит в фазу игры при получении {@link ReadyS2CPacket}.
 */
@Environment(EnvType.CLIENT)
public class ClientConfigurationNetworkHandler
		extends ClientCommonNetworkHandler
		implements ClientConfigurationPacketListener, TickablePacketListener {

	static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Причина отключения при отказе от кодекса поведения.
	 */
	public static final Text CODE_OF_CONDUCT_DISCONNECT_REASON =
			Text.translatable("multiplayer.disconnect.code_of_conduct");

	private final ClientChunkLoadProgress chunkLoadProgress;
	private final GameProfile profile;
	private FeatureSet enabledFeatures;
	private final DynamicRegistryManager.Immutable registryManager;
	private final ClientRegistries clientRegistries = new ClientRegistries();
	private @Nullable ClientDataPackManager dataPackManager;
	protected ChatHud.@Nullable ChatState chatState;
	private boolean receivedCodeOfConduct;

	/**
	 * @param minecraftClient  клиент Minecraft
	 * @param clientConnection сетевое соединение
	 * @param connectionState  снимок состояния соединения
	 */
	public ClientConfigurationNetworkHandler(
			MinecraftClient minecraftClient,
			ClientConnection clientConnection,
			ClientConnectionState connectionState
	) {
		super(minecraftClient, clientConnection, connectionState);
		chunkLoadProgress = connectionState.chunkLoadProgress();
		profile = connectionState.localGameProfile();
		registryManager = connectionState.receivedRegistries();
		enabledFeatures = connectionState.enabledFeatures();
		chatState = connectionState.chatState();
	}

	@Override
	public boolean isConnectionOpen() {
		return connection.isOpen();
	}

	@Override
	protected void onCustomPayload(CustomPayload payload) {
		LOGGER.warn("Unknown custom packet payload: {}", payload.getId().id());
	}

	@Override
	public void onDynamicRegistries(DynamicRegistriesS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		clientRegistries.putDynamicRegistry(packet.registry(), packet.entries());
	}

	@Override
	public void onSynchronizeTags(SynchronizeTagsS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());
		clientRegistries.putTags(packet.getGroups());
	}

	@Override
	public void onFeatures(FeaturesS2CPacket packet) {
		enabledFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(packet.features());
	}

	@Override
	public void onSelectKnownPacks(SelectKnownPacksS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		if (dataPackManager == null) {
			dataPackManager = new ClientDataPackManager();
		}

		List<VersionedIdentifier> commonPacks = dataPackManager.getCommonKnownPacks(packet.knownPacks());
		sendPacket(new SelectKnownPacksC2SPacket(commonPacks));
	}

	@Override
	public void onResetChat(ResetChatS2CPacket packet) {
		chatState = null;
	}

	@Override
	public void onCodeOfConduct(CodeOfConductS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		if (receivedCodeOfConduct) {
			throw new IllegalStateException("Server sent duplicate Code of Conduct");
		}

		receivedCodeOfConduct = true;
		String codeText = packet.codeOfConduct();

		if (serverInfo != null && serverInfo.hasAcceptedCodeOfConduct(codeText)) {
			sendPacket(AcceptCodeOfConductC2SPacket.INSTANCE);
			return;
		}

		Screen previousScreen = client.currentScreen;
		client.setScreen(new CodeOfConductScreen(
				serverInfo, previousScreen, codeText, acknowledged -> {
			if (acknowledged) {
				sendPacket(AcceptCodeOfConductC2SPacket.INSTANCE);
				client.setScreen(previousScreen);
			}
			else {
				createDialogNetworkAccess().disconnect(CODE_OF_CONDUCT_DISCONNECT_REASON);
			}
		}
		));
	}

	@Override
	public void onReady(ReadyS2CPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, client.getPacketApplyBatcher());

		DynamicRegistryManager.Immutable finalRegistries = openClientDataPack(
				factory -> clientRegistries.createRegistryManager(factory, registryManager, connection.isLocal())
		);

		connection.transitionInbound(
				PlayStateFactories.S2C.bind(RegistryByteBuf.makeFactory(finalRegistries)),
				new ClientPlayNetworkHandler(
						client,
						connection,
						new ClientConnectionState(
								chunkLoadProgress,
								profile,
								worldSession,
								finalRegistries,
								enabledFeatures,
								brand,
								serverInfo,
								postDisconnectScreen,
								serverCookies,
								chatState,
								customReportDetails,
								getServerLinks(),
								seenPlayers,
								seenInsecureChatWarning
						)
				)
		);

		connection.send(ReadyC2SPacket.INSTANCE);
		connection.transitionOutbound(
				PlayStateFactories.C2S.bind(
						RegistryByteBuf.makeFactory(finalRegistries),
						new PlayStateFactories.PacketCodecModifierContext() {
							@Override
							public boolean isInCreativeMode() {
								return true;
							}
						}
				)
		);
	}

	@Override
	public void tick() {
		sendQueuedPackets();
	}

	@Override
	public void onDisconnected(DisconnectionInfo info) {
		super.onDisconnected(info);
		client.onDisconnected();
	}

	@Override
	protected DialogNetworkAccess createDialogNetworkAccess() {
		return new ClientCommonNetworkHandler.CommonDialogNetworkAccess() {
			@Override
			public void runClickEventCommand(String command, @Nullable Screen afterActionScreen) {
				LOGGER.warn("Commands are not supported in configuration phase, trying to run '{}'", command);
			}
		};
	}

	private <T> T openClientDataPack(Function<ResourceFactory, T> opener) {
		if (dataPackManager == null) {
			return opener.apply(ResourceFactory.MISSING);
		}

		try (LifecycledResourceManager manager = dataPackManager.createResourceManager()) {
			return opener.apply(manager);
		}
	}
}
