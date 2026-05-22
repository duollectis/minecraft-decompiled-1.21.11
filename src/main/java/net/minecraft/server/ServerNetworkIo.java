package net.minecraft.server;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import net.minecraft.SharedConstants;
import net.minecraft.network.*;
import net.minecraft.network.handler.LegacyQueryHandler;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.server.network.LocalServerHandshakeNetworkHandler;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Управляет сетевым вводом-выводом сервера: привязывает TCP-порты, принимает входящие соединения
 * и тикает все активные {@link ClientConnection} на каждом игровом тике.
 * Поддерживает как удалённые (TCP/Epoll), так и локальные (in-process) соединения для интегрированного сервера.
 */
public class ServerNetworkIo {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int READ_TIMEOUT_SECONDS = 30;

	final MinecraftServer server;
	public volatile boolean active;
	private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
	final List<ClientConnection> connections = Collections.synchronizedList(Lists.newArrayList());

	public ServerNetworkIo(MinecraftServer server) {
		this.server = server;
		this.active = true;
	}

	/**
	 * Привязывает сервер к указанному адресу и порту через TCP (или Epoll на Linux).
	 * Настраивает полный pipeline: таймаут, legacy-запросы, кодеки и rate-limiting.
	 *
	 * @param address адрес для привязки, {@code null} означает все интерфейсы
	 * @param port    порт для прослушивания
	 * @throws IOException если привязка не удалась
	 */
	public void bind(@Nullable InetAddress address, int port) throws IOException {
		synchronized (channels) {
			NetworkingBackend backend = NetworkingBackend.remote(server.isUsingNativeTransport());

			channels.add(
				new ServerBootstrap()
					.channel(backend.getServerChannelClass())
					.childHandler(new ChannelInitializer<Channel>() {
						protected void initChannel(Channel channel) {
							try {
								channel.config().setOption(ChannelOption.TCP_NODELAY, true);
							} catch (ChannelException ignored) {
							}

							ChannelPipeline pipeline = channel.pipeline()
									.addLast("timeout", new ReadTimeoutHandler(READ_TIMEOUT_SECONDS));

							if (ServerNetworkIo.this.server.acceptsStatusQuery()) {
								pipeline.addLast("legacy_query", new LegacyQueryHandler(ServerNetworkIo.this.getServer()));
							}

							ClientConnection.addHandlers(pipeline, NetworkSide.SERVERBOUND, false, null);

							int rateLimit = ServerNetworkIo.this.server.getRateLimit();
							ClientConnection connection = rateLimit > 0
									? new RateLimitedConnection(rateLimit)
									: new ClientConnection(NetworkSide.SERVERBOUND);

							ServerNetworkIo.this.connections.add(connection);
							connection.addFlowControlHandler(pipeline);
							connection.setInitialPacketListener(new ServerHandshakeNetworkHandler(
									ServerNetworkIo.this.server,
									connection
							));
						}
					})
					.group(backend.getEventLoopGroup())
					.localAddress(address, port)
					.bind()
					.syncUninterruptibly()
			);
		}
	}

	/**
	 * Привязывает локальный (in-process) канал для интегрированного сервера.
	 * При наличии искусственной задержки ({@link SharedConstants#FAKE_LATENCY_MS}) добавляет
	 * {@link DelayingChannelInboundHandler} в pipeline.
	 *
	 * @return адрес привязанного локального канала
	 */
	public SocketAddress bindLocal() {
		ChannelFuture future;

		synchronized (channels) {
			NetworkingBackend localBackend = NetworkingBackend.local();

			future = new ServerBootstrap()
					.channel(localBackend.getServerChannelClass())
					.childHandler(new ChannelInitializer<Channel>() {
						protected void initChannel(Channel channel) {
							ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
							connection.setInitialPacketListener(new LocalServerHandshakeNetworkHandler(
									ServerNetworkIo.this.server,
									connection
							));
							ServerNetworkIo.this.connections.add(connection);

							ChannelPipeline pipeline = channel.pipeline();
							ClientConnection.addLocalValidator(pipeline, NetworkSide.SERVERBOUND);

							if (SharedConstants.FAKE_LATENCY_MS > 0) {
								pipeline.addLast("latency", new ServerNetworkIo.DelayingChannelInboundHandler(
										SharedConstants.FAKE_LATENCY_MS,
										SharedConstants.FAKE_JITTER_MS
								));
							}

							connection.addFlowControlHandler(pipeline);
						}
					})
					.group(localBackend.getEventLoopGroup())
					.localAddress(LocalAddress.ANY)
					.bind()
					.syncUninterruptibly();

			channels.add(future);
		}

		return future.channel().localAddress();
	}

	public void stop() {
		active = false;

		for (ChannelFuture channelFuture : channels) {
			try {
				channelFuture.channel().close().sync();
			} catch (InterruptedException exception) {
				LOGGER.error("Interrupted whilst closing channel");
			}
		}
	}

	/**
	 * Обходит все активные соединения: тикает открытые и обрабатывает отключения закрытых.
	 * При критической ошибке в локальном соединении бросает {@link CrashException}.
	 */
	public void tick() {
		synchronized (connections) {
			Iterator<ClientConnection> iterator = connections.iterator();

			while (iterator.hasNext()) {
				ClientConnection connection = iterator.next();
				if (connection.isChannelAbsent()) {
					continue;
				}

				if (connection.isOpen()) {
					try {
						connection.tick();
					} catch (Exception exception) {
						if (connection.isLocal()) {
							throw new CrashException(CrashReport.create(exception, "Ticking memory connection"));
						}

						LOGGER.warn(
								"Failed to handle packet for {}",
								connection.getAddressAsString(server.shouldLogIps()),
								exception
						);
						Text errorMessage = Text.literal("Internal server error");
						connection.send(
								new DisconnectS2CPacket(errorMessage),
								PacketCallbacks.always(() -> connection.disconnect(errorMessage))
						);
						connection.tryDisableAutoRead();
					}
				} else {
					iterator.remove();
					connection.handleDisconnection();
				}
			}
		}
	}

	public MinecraftServer getServer() {
		return server;
	}

	public List<ClientConnection> getConnections() {
		return connections;
	}

	/**
	 * Обработчик Netty-канала, искусственно задерживающий входящие пакеты на заданное время.
	 * Используется исключительно в режиме разработки для симуляции сетевой задержки
	 * (управляется через {@link SharedConstants#FAKE_LATENCY_MS}).
	 */
	static class DelayingChannelInboundHandler extends ChannelInboundHandlerAdapter {

		private static final Timer TIMER = new HashedWheelTimer();

		private final int baseDelay;
		private final int extraDelay;
		private final List<DelayingChannelInboundHandler.Packet> packets = Lists.newArrayList();

		public DelayingChannelInboundHandler(int baseDelay, int extraDelay) {
			this.baseDelay = baseDelay;
			this.extraDelay = extraDelay;
		}

		public void channelRead(ChannelHandlerContext context, Object message) {
			delay(context, message);
		}

		private void delay(ChannelHandlerContext context, Object message) {
			int delayMs = baseDelay + (int) (Math.random() * extraDelay);
			packets.add(new Packet(context, message));
			TIMER.newTimeout(this::forward, delayMs, TimeUnit.MILLISECONDS);
		}

		private void forward(Timeout timeout) {
			Packet packet = packets.remove(0);
			packet.context.fireChannelRead(packet.message);
		}

		static class Packet {

			public final ChannelHandlerContext context;
			public final Object message;

			public Packet(ChannelHandlerContext context, Object message) {
				this.context = context;
				this.message = message;
			}
		}
	}
}
