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
 * {@code ServerNetworkIo}.
 */
public class ServerNetworkIo {

	private static final Logger LOGGER = LogUtils.getLogger();
	final MinecraftServer server;
	public volatile boolean active;
	private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
	final List<ClientConnection> connections = Collections.synchronizedList(Lists.newArrayList());

	public ServerNetworkIo(MinecraftServer server) {
		this.server = server;
		this.active = true;
	}

	/**
	 * Bind.
	 *
	 * @param address address
	 * @param port port
	 */
	public void bind(@Nullable InetAddress address, int port) throws IOException {
		synchronized (this.channels) {
			NetworkingBackend networkingBackend = NetworkingBackend.remote(this.server.isUsingNativeTransport());
			this.channels
					.add(
							((ServerBootstrap) ((ServerBootstrap) new ServerBootstrap().channel(networkingBackend.getServerChannelClass()))
									.childHandler(new ChannelInitializer<Channel>() {
										/**
										 * Инициализирует channel.
										 *
										 * @param channel channel
										 */
										protected void initChannel(Channel channel) {
											try {
												channel.config().setOption(ChannelOption.TCP_NODELAY, true);
											}
											catch (ChannelException var5) {
											}

											ChannelPipeline
													channelPipeline =
													channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
											if (ServerNetworkIo.this.server.acceptsStatusQuery()) {
												channelPipeline.addLast(
														"legacy_query",
														new LegacyQueryHandler(ServerNetworkIo.this.getServer())
												);
											}

											ClientConnection.addHandlers(
													channelPipeline,
													NetworkSide.SERVERBOUND,
													false,
													null
											);
											int i = ServerNetworkIo.this.server.getRateLimit();
											ClientConnection clientConnection = (ClientConnection) (i > 0
											                                                        ? new RateLimitedConnection(
													i)
											                                                        : new ClientConnection(
													                                                        NetworkSide.SERVERBOUND)
											);
											ServerNetworkIo.this.connections.add(clientConnection);
											clientConnection.addFlowControlHandler(channelPipeline);
											clientConnection.setInitialPacketListener(new ServerHandshakeNetworkHandler(
													ServerNetworkIo.this.server,
													clientConnection
											));
										}
									})
									.group(networkingBackend.getEventLoopGroup())
									.localAddress(address, port)
							)
									.bind()
									.syncUninterruptibly()
					);
		}
	}

	/**
	 * Bind local.
	 *
	 * @return SocketAddress — результат операции
	 */
	public SocketAddress bindLocal() {
		ChannelFuture channelFuture;
		synchronized (this.channels) {
			channelFuture =
					((ServerBootstrap) ((ServerBootstrap) new ServerBootstrap().channel(NetworkingBackend
							.local()
							.getServerChannelClass())
					)
							.childHandler(
									new ChannelInitializer<Channel>() {
										/**
										 * Инициализирует channel.
										 *
										 * @param channel channel
										 */
										protected void initChannel(Channel channel) {
											ClientConnection
													clientConnection =
													new ClientConnection(NetworkSide.SERVERBOUND);
											clientConnection.setInitialPacketListener(new LocalServerHandshakeNetworkHandler(
													ServerNetworkIo.this.server,
													clientConnection
											));
											ServerNetworkIo.this.connections.add(clientConnection);
											ChannelPipeline channelPipeline = channel.pipeline();
											ClientConnection.addLocalValidator(
													channelPipeline,
													NetworkSide.SERVERBOUND
											);
											if (SharedConstants.FAKE_LATENCY_MS > 0) {
												channelPipeline.addLast(
														"latency",
														new ServerNetworkIo.DelayingChannelInboundHandler(
																SharedConstants.FAKE_LATENCY_MS,
																SharedConstants.FAKE_JITTER_MS
														)
												);
											}

											clientConnection.addFlowControlHandler(channelPipeline);
										}
									}
							)
							.group(NetworkingBackend.local().getEventLoopGroup())
							.localAddress(LocalAddress.ANY)
					)
							.bind()
							.syncUninterruptibly();
			this.channels.add(channelFuture);
		}

		return channelFuture.channel().localAddress();
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.active = false;

		for (ChannelFuture channelFuture : this.channels) {
			try {
				channelFuture.channel().close().sync();
			}
			catch (InterruptedException var4) {
				LOGGER.error("Interrupted whilst closing channel");
			}
		}
	}

	/**
	 * Tick.
	 */
	public void tick() {
		synchronized (this.connections) {
			Iterator<ClientConnection> iterator = this.connections.iterator();

			while (iterator.hasNext()) {
				ClientConnection clientConnection = iterator.next();
				if (!clientConnection.isChannelAbsent()) {
					if (clientConnection.isOpen()) {
						try {
							clientConnection.tick();
						}
						catch (Exception var7) {
							if (clientConnection.isLocal()) {
								throw new CrashException(CrashReport.create(var7, "Ticking memory connection"));
							}

							LOGGER.warn(
									"Failed to handle packet for {}",
									clientConnection.getAddressAsString(this.server.shouldLogIps()),
									var7
							);
							Text text = Text.literal("Internal server error");
							clientConnection.send(
									new DisconnectS2CPacket(text),
									PacketCallbacks.always(() -> clientConnection.disconnect(text))
							);
							clientConnection.tryDisableAutoRead();
						}
					}
					else {
						iterator.remove();
						clientConnection.handleDisconnection();
					}
				}
			}
		}
	}

	public MinecraftServer getServer() {
		return this.server;
	}

	public List<ClientConnection> getConnections() {
		return this.connections;
	}

	/**
	 * {@code DelayingChannelInboundHandler}.
	 */
	static class DelayingChannelInboundHandler extends ChannelInboundHandlerAdapter {

		private static final Timer TIMER = new HashedWheelTimer();
		private final int baseDelay;
		private final int extraDelay;
		private final List<ServerNetworkIo.DelayingChannelInboundHandler.Packet> packets = Lists.newArrayList();

		public DelayingChannelInboundHandler(int baseDelay, int extraDelay) {
			this.baseDelay = baseDelay;
			this.extraDelay = extraDelay;
		}

		/**
		 * Channel read.
		 *
		 * @param ctx ctx
		 * @param msg msg
		 */
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			this.delay(ctx, msg);
		}

		private void delay(ChannelHandlerContext ctx, Object msg) {
			int i = this.baseDelay + (int) (Math.random() * this.extraDelay);
			this.packets.add(new ServerNetworkIo.DelayingChannelInboundHandler.Packet(ctx, msg));
			TIMER.newTimeout(this::forward, i, TimeUnit.MILLISECONDS);
		}

		private void forward(Timeout timeout) {
			ServerNetworkIo.DelayingChannelInboundHandler.Packet packet = this.packets.remove(0);
			packet.context.fireChannelRead(packet.message);
		}

		/**
		 * {@code Packet}.
		 */
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
