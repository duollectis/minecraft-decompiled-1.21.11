package net.minecraft.client.realms;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.QuickPlayLogger;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.resource.server.ServerResourcePackManager;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Objects;

@Environment(EnvType.CLIENT)
/**
 * {@code RealmsConnection}.
 */
public class RealmsConnection {

	static final Logger LOGGER = LogUtils.getLogger();
	final Screen onlineScreen;
	volatile boolean aborted;
	@Nullable ClientConnection connection;

	public RealmsConnection(Screen onlineScreen) {
		this.onlineScreen = onlineScreen;
	}

	/**
	 * Connect.
	 *
	 * @param server server
	 * @param address address
	 */
	public void connect(RealmsServer server, ServerAddress address) {
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		minecraftClient.loadBlockList();
		minecraftClient.getNarratorManager().narrateSystemImmediately(Text.translatable("mco.connect.success"));
		final String string = address.getAddress();
		final int i = address.getPort();
		(new Thread("Realms-connect-task") {
			@Override
			public void run() {
				InetSocketAddress inetSocketAddress = null;

				try {
					inetSocketAddress = new InetSocketAddress(string, i);
					if (RealmsConnection.this.aborted) {
						return;
					}

					RealmsConnection.this.connection = ClientConnection.connect(
							inetSocketAddress,
							NetworkingBackend.remote(minecraftClient.options.shouldUseNativeTransport()),
							minecraftClient.getDebugHud().getPacketSizeLog()
					);
					if (RealmsConnection.this.aborted) {
						return;
					}

					ClientLoginNetworkHandler clientLoginNetworkHandler = new ClientLoginNetworkHandler(
							RealmsConnection.this.connection,
							minecraftClient,
							server.createServerInfo(string),
							RealmsConnection.this.onlineScreen,
							false,
							null,
							status -> {},
							new ClientChunkLoadProgress(),
							null
					);
					if (server.isMinigame()) {
						clientLoginNetworkHandler.setMinigameName(server.minigameName);
					}

					if (RealmsConnection.this.aborted) {
						return;
					}

					RealmsConnection.this.connection.connect(string, i, clientLoginNetworkHandler);
					if (RealmsConnection.this.aborted) {
						return;
					}

					RealmsConnection.this.connection
							.send(new LoginHelloC2SPacket(
									minecraftClient.getSession().getUsername(),
									minecraftClient.getSession().getUuidOrNull()
							));
					minecraftClient.ensureAbuseReportContext(ReporterEnvironment.ofRealm(server));
					minecraftClient.getQuickPlayLogger()
					               .setWorld(
							               QuickPlayLogger.WorldType.REALMS,
							               String.valueOf(server.id),
							               Objects.requireNonNullElse(server.name, "unknown")
					               );
					minecraftClient
							.getServerResourcePackProvider()
							.init(RealmsConnection.this.connection, ServerResourcePackManager.AcceptanceStatus.ALLOWED);
				}
				catch (Exception var5) {
					minecraftClient.getServerResourcePackProvider().clear();
					if (RealmsConnection.this.aborted) {
						return;
					}

					RealmsConnection.LOGGER.error("Couldn't connect to world", var5);
					String stringx = var5.toString();
					if (inetSocketAddress != null) {
						String string2 = inetSocketAddress + ":" + i;
						stringx = stringx.replaceAll(string2, "");
					}

					DisconnectedScreen disconnectedScreen = new DisconnectedScreen(
							RealmsConnection.this.onlineScreen,
							Text.translatable("mco.connect.failed"),
							Text.translatable("disconnect.genericReason", stringx),
							ScreenTexts.BACK
					);
					minecraftClient.execute(() -> minecraftClient.setScreen(disconnectedScreen));
				}
			}
		}
		)
				.start();
	}

	/**
	 * Abort.
	 */
	public void abort() {
		this.aborted = true;
		if (this.connection != null && this.connection.isOpen()) {
			this.connection.disconnect(Text.translatable("disconnect.genericReason"));
			this.connection.handleDisconnection();
		}
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if (this.connection != null) {
			if (this.connection.isOpen()) {
				this.connection.tick();
			}
			else {
				this.connection.handleDisconnection();
			}
		}
	}
}
