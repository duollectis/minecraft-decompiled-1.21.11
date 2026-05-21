package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Пингует серверы из списка мультиплеера.
 * Устанавливает временное соединение для получения метаданных сервера
 * (описание, версия, количество игроков, иконка) и измерения задержки.
 */
@Environment(EnvType.CLIENT)
public class MultiplayerServerListPinger {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text CANNOT_CONNECT_TEXT = Text.translatable("multiplayer.status.cannot_connect")
	                                                    .withColor(-65536);

	private final List<ClientConnection> clientConnections = Collections.synchronizedList(Lists.newArrayList());

	/**
	 * Добавляет сервер в очередь пинга и начинает асинхронное подключение.
	 *
	 * @param entry        запись сервера для обновления
	 * @param saver        колбэк сохранения (вызывается при обновлении иконки)
	 * @param pingCallback колбэк завершения пинга
	 * @param backend      сетевой бэкенд для создания соединения
	 * @throws UnknownHostException если адрес сервера не удалось разрешить
	 */
	public void add(
			ServerInfo entry,
			Runnable saver,
			Runnable pingCallback,
			NetworkingBackend backend
	) throws UnknownHostException {
		ServerAddress serverAddress = ServerAddress.parse(entry.address);
		Optional<InetSocketAddress> resolved = AllowedAddressResolver.DEFAULT
				.resolve(serverAddress)
				.map(Address::getInetSocketAddress);

		if (resolved.isEmpty()) {
			showError(ConnectScreen.UNKNOWN_HOST_TEXT, entry);
			return;
		}

		InetSocketAddress socketAddress = resolved.get();
		ClientConnection
				connection =
				ClientConnection.connect(socketAddress, backend, (MultiValueDebugSampleLogImpl) null);
		clientConnections.add(connection);
		entry.label = Text.translatable("multiplayer.status.pinging");
		entry.playerListSummary = Collections.emptyList();

		ClientQueryPacketListener listener = new ClientQueryPacketListener() {
			private boolean sentQuery;
			private boolean received;
			private long startTime;

			@Override
			public void onResponse(QueryResponseS2CPacket packet) {
				if (received) {
					connection.disconnect(Text.translatable("multiplayer.status.unrequested"));
					return;
				}

				received = true;
				ServerMetadata metadata = packet.metadata();
				entry.label = metadata.description();

				metadata.version().ifPresentOrElse(
						version -> {
							entry.version = Text.literal(version.gameVersion());
							entry.protocolVersion = version.protocolVersion();
						},
						() -> {
							entry.version = Text.translatable("multiplayer.status.old");
							entry.protocolVersion = 0;
						}
				);

				metadata.players().ifPresentOrElse(
						players -> {
							entry.playerCountLabel = createPlayerCountText(players.online(), players.max());
							entry.players = players;

							if (players.sample().isEmpty()) {
								entry.playerListSummary = List.of();
								return;
							}

							List<Text> summary = new ArrayList<>(players.sample().size());
							for (PlayerConfigEntry playerEntry : players.sample()) {
								Text name = playerEntry.equals(MinecraftServer.ANONYMOUS_PLAYER_PROFILE)
								            ? Text.translatable("multiplayer.status.anonymous_player")
								            : Text.literal(playerEntry.name());
								summary.add(name);
							}

							if (players.sample().size() < players.online()) {
								summary.add(Text.translatable(
										"multiplayer.status.and_more",
										players.online() - players.sample().size()
								));
							}

							entry.playerListSummary = summary;
						},
						() -> entry.playerCountLabel = Text.translatable("multiplayer.status.unknown")
						                                   .formatted(Formatting.DARK_GRAY)
				);

				metadata.favicon().ifPresent(favicon -> {
					if (Arrays.equals(favicon.iconBytes(), entry.getFavicon()) == false) {
						entry.setFavicon(ServerInfo.validateFavicon(favicon.iconBytes()));
						saver.run();
					}
				});

				startTime = Util.getMeasuringTimeMs();
				connection.send(new QueryPingC2SPacket(startTime));
				sentQuery = true;
			}

			@Override
			public void onPingResult(PingResultS2CPacket packet) {
				entry.ping = Util.getMeasuringTimeMs() - startTime;
				connection.disconnect(Text.translatable("multiplayer.status.finished"));
				pingCallback.run();
			}

			@Override
			public void onDisconnected(DisconnectionInfo info) {
				if (sentQuery) {
					return;
				}

				showError(info.reason(), entry);
				ping(socketAddress, serverAddress, entry, backend);
			}

			@Override
			public boolean isConnectionOpen() {
				return connection.isOpen();
			}
		};

		try {
			connection.connect(serverAddress.getAddress(), serverAddress.getPort(), listener);
			connection.send(QueryRequestC2SPacket.INSTANCE);
		}
		catch (Throwable e) {
			LOGGER.error("Failed to ping server {}", serverAddress, e);
		}
	}

	/**
	 * Отображает ошибку подключения в записи сервера.
	 *
	 * @param error текст ошибки
	 * @param info  запись сервера
	 */
	void showError(Text error, ServerInfo info) {
		LOGGER.error("Can't ping {}: {}", info.address, error.getString());
		info.label = CANNOT_CONNECT_TEXT;
		info.playerCountLabel = ScreenTexts.EMPTY;
	}

	/**
	 * Пробует подключиться через устаревший протокол (pre-1.7) для получения версии.
	 *
	 * @param socketAddress разрешённый адрес сокета
	 * @param address       адрес сервера
	 * @param serverInfo    запись сервера для обновления
	 * @param backend       сетевой бэкенд
	 */
	void ping(
			InetSocketAddress socketAddress,
			ServerAddress address,
			ServerInfo serverInfo,
			NetworkingBackend backend
	) {
		new Bootstrap()
				.group(backend.getEventLoopGroup())
				.handler(new ChannelInitializer<Channel>() {
					@Override
					protected void initChannel(Channel channel) {
						try {
							channel.config().setOption(ChannelOption.TCP_NODELAY, true);
						}
						catch (ChannelException ignored) {
						}

						channel.pipeline().addLast(new ChannelHandler[]{
								new LegacyServerPinger(
										address, (protocolVersion, version, label, online, max) -> {
									serverInfo.setStatus(ServerInfo.Status.INCOMPATIBLE);
									serverInfo.version = Text.literal(version);
									serverInfo.label = Text.literal(label);
									serverInfo.playerCountLabel = createPlayerCountText(online, max);
									serverInfo.players = new ServerMetadata.Players(max, online, List.of());
								}
								)
						});
					}
				})
				.channel(backend.getChannelClass())
				.connect(socketAddress.getAddress(), socketAddress.getPort());
	}

	/**
	 * Создаёт форматированный текст с количеством игроков на сервере.
	 *
	 * @param current текущее количество игроков
	 * @param max     максимальное количество игроков
	 * @return форматированный текст
	 */
	public static Text createPlayerCountText(int current, int max) {
		Text currentText = Text.literal(Integer.toString(current)).formatted(Formatting.GRAY);
		Text maxText = Text.literal(Integer.toString(max)).formatted(Formatting.GRAY);
		return Text.translatable("multiplayer.status.player_count", currentText, maxText)
		           .formatted(Formatting.DARK_GRAY);
	}

	/**
	 * Обновляет все активные соединения пинга, удаляя завершённые.
	 */
	public void tick() {
		synchronized (clientConnections) {
			Iterator<ClientConnection> iterator = clientConnections.iterator();

			while (iterator.hasNext()) {
				ClientConnection connection = iterator.next();

				if (connection.isOpen()) {
					connection.tick();
				}
				else {
					iterator.remove();
					connection.handleDisconnection();
				}
			}
		}
	}

	/**
	 * Отменяет все активные соединения пинга.
	 */
	public void cancel() {
		synchronized (clientConnections) {
			Iterator<ClientConnection> iterator = clientConnections.iterator();

			while (iterator.hasNext()) {
				ClientConnection connection = iterator.next();

				if (connection.isOpen()) {
					iterator.remove();
					connection.disconnect(Text.translatable("multiplayer.status.cancelled"));
				}
			}
		}
	}
}
