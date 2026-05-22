package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.*;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.profiler.Profilers;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Класс Server Common Network Handler.
 */
public abstract class ServerCommonNetworkHandler implements ServerCommonPacketListener {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int KEEP_ALIVE_INTERVAL = 15000;
	private static final int TRANSITION_TIMEOUT = 15000;
	private static final Text TIMEOUT_TEXT = Text.translatable("disconnect.timeout");
	static final Text
			UNEXPECTED_QUERY_RESPONSE_TEXT =
			Text.translatable("multiplayer.disconnect.unexpected_query_response");
	protected final MinecraftServer server;
	protected final ClientConnection connection;
	private final boolean transferred;
	private long lastKeepAliveTime;
	private boolean waitingForKeepAlive;
	private long keepAliveId;
	private long transitionStartTime;
	private boolean transitioning = false;
	private int latency;
	private volatile boolean flushDisabled = false;

	public ServerCommonNetworkHandler(
			MinecraftServer server,
			ClientConnection connection,
			ConnectedClientData clientData
	) {
		this.server = server;
		this.connection = connection;
		this.lastKeepAliveTime = Util.getMeasuringTimeMs();
		this.latency = clientData.latency();
		this.transferred = clientData.transferred();
	}

	private void markTransitionTime() {
		if (!this.transitioning) {
			this.transitionStartTime = Util.getMeasuringTimeMs();
			this.transitioning = true;
		}
	}

	@Override
	public void onDisconnected(DisconnectionInfo info) {
		if (this.isHost()) {
			LOGGER.info("Stopping singleplayer server as player logged out");
			this.server.stop(false);
		}
	}

	@Override
	public void onPacketException(Packet packet, Exception exception) throws CrashException {
		ServerCommonPacketListener.super.onPacketException(packet, exception);
		this.server.onPacketException(exception, packet.getPacketType());
	}

	@Override
	public void onKeepAlive(KeepAliveC2SPacket packet) {
		if (this.waitingForKeepAlive && packet.getId() == this.keepAliveId) {
			int i = (int) (Util.getMeasuringTimeMs() - this.lastKeepAliveTime);
			this.latency = (this.latency * 3 + i) / 4;
			this.waitingForKeepAlive = false;
		}
		else if (!this.isHost()) {
			this.disconnect(TIMEOUT_TEXT);
		}
	}

	@Override
	public void onPong(CommonPongC2SPacket packet) {
	}

	@Override
	public void onCustomPayload(CustomPayloadC2SPacket packet) {
	}

	@Override
	public void onCustomClickAction(CustomClickActionC2SPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, this.server.getPacketApplyBatcher());
		this.server.handleCustomClickAction(packet.id(), packet.payload());
	}

	@Override
	public void onResourcePackStatus(ResourcePackStatusC2SPacket packet) {
		NetworkThreadUtils.forceMainThread(packet, this, this.server.getPacketApplyBatcher());
		if (packet.status() == ResourcePackStatusC2SPacket.Status.DECLINED && this.server.requireResourcePack()) {
			LOGGER.info("Disconnecting {} due to resource pack {} rejection", this.getProfile().name(), packet.id());
			this.disconnect(Text.translatable("multiplayer.requiredTexturePrompt.disconnect"));
		}
	}

	@Override
	public void onCookieResponse(CookieResponseC2SPacket packet) {
		this.disconnect(UNEXPECTED_QUERY_RESPONSE_TEXT);
	}

	/**
	 * Base tick.
	 */
	protected void baseTick() {
		Profilers.get().push("keepAlive");
		long l = Util.getMeasuringTimeMs();
		if (!this.isHost() && l - this.lastKeepAliveTime >= KEEP_ALIVE_INTERVAL) {
			if (this.waitingForKeepAlive) {
				this.disconnect(TIMEOUT_TEXT);
			}
			else if (this.checkTransitionTimeout(l)) {
				this.waitingForKeepAlive = true;
				this.lastKeepAliveTime = l;
				this.keepAliveId = l;
				this.sendPacket(new KeepAliveS2CPacket(this.keepAliveId));
			}
		}

		Profilers.get().pop();
	}

	private boolean checkTransitionTimeout(long time) {
		if (this.transitioning) {
			if (time - this.transitionStartTime >= TRANSITION_TIMEOUT) {
				this.disconnect(TIMEOUT_TEXT);
			}

			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Отключает flush.
	 */
	public void disableFlush() {
		this.flushDisabled = true;
	}

	/**
	 * Включает flush.
	 */
	public void enableFlush() {
		this.flushDisabled = false;
		this.connection.flush();
	}

	/**
	 * Отправляет packet.
	 *
	 * @param packet packet
	 */
	public void sendPacket(Packet<?> packet) {
		this.send(packet, null);
	}

	/**
	 * Send.
	 *
	 * @param packet packet
	 * @param channelFutureListener channel future listener
	 */
	public void send(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener) {
		if (packet.transitionsNetworkState()) {
			this.markTransitionTime();
		}

		boolean bl = !this.flushDisabled || !this.server.isOnThread();

		try {
			this.connection.send(packet, channelFutureListener, bl);
		}
		catch (Throwable var7) {
			CrashReport crashReport = CrashReport.create(var7, "Sending packet");
			CrashReportSection crashReportSection = crashReport.addElement("Packet being sent");
			crashReportSection.add("Packet class", () -> packet.getClass().getCanonicalName());
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Disconnect.
	 *
	 * @param reason reason
	 */
	public void disconnect(Text reason) {
		this.disconnect(new DisconnectionInfo(reason));
	}

	/**
	 * Disconnect.
	 *
	 * @param disconnectionInfo disconnection info
	 */
	public void disconnect(DisconnectionInfo disconnectionInfo) {
		this.connection.send(
				new DisconnectS2CPacket(disconnectionInfo.reason()),
				PacketCallbacks.always(() -> this.connection.disconnect(disconnectionInfo))
		);
		this.connection.tryDisableAutoRead();
		this.server.submitAndJoin(this.connection::handleDisconnection);
	}

	protected boolean isHost() {
		return this.server.isHost(new PlayerConfigEntry(this.getProfile()));
	}

	protected abstract GameProfile getProfile();

	@Debug
	public GameProfile getDebugProfile() {
		return this.getProfile();
	}

	public int getLatency() {
		return this.latency;
	}

	/**
	 * Создаёт client data.
	 *
	 * @param syncedOptions synced options
	 *
	 * @return ConnectedClientData — результат операции
	 */
	protected ConnectedClientData createClientData(SyncedClientOptions syncedOptions) {
		return new ConnectedClientData(this.getProfile(), this.latency, syncedOptions, this.transferred);
	}
}
