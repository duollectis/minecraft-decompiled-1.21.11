package net.minecraft.network;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import net.minecraft.SharedConstants;
import net.minecraft.network.encryption.PacketDecryptor;
import net.minecraft.network.encryption.PacketEncryptor;
import net.minecraft.network.handler.*;
import net.minecraft.network.listener.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket;
import net.minecraft.network.state.HandshakeStates;
import net.minecraft.network.state.LoginStates;
import net.minecraft.network.state.NetworkState;
import net.minecraft.network.state.QueryStates;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.crypto.Cipher;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

/**
 * Управляет сетевым соединением между клиентом и сервером через Netty.
 * <p>Отвечает за полный жизненный цикл соединения: установку канала, обработку входящих
 * и исходящих пакетов, переходы состояний протокола ({@link NetworkPhase}),
 * шифрование, сжатие и корректное отключение.
 * <p>Экземпляр является {@link SimpleChannelInboundHandler} и напрямую встраивается
 * в Netty-пайплайн как финальный обработчик пакетов.
 */
public class ClientConnection extends SimpleChannelInboundHandler<Packet<?>> {

	private static final float PACKET_COUNTER_LERP_WEIGHT = 0.75F;
	private static final int STATS_UPDATE_INTERVAL_TICKS = 20;
	private static final int READ_TIMEOUT_SECONDS = 30;

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Marker NETWORK_MARKER = MarkerFactory.getMarker("NETWORK");
	public static final Marker NETWORK_PACKETS_MARKER = Util.make(
			MarkerFactory.getMarker("NETWORK_PACKETS"),
			marker -> marker.add(NETWORK_MARKER)
	);
	public static final Marker PACKET_RECEIVED_MARKER = Util.make(
			MarkerFactory.getMarker("PACKET_RECEIVED"),
			marker -> marker.add(NETWORK_PACKETS_MARKER)
	);
	public static final Marker PACKET_SENT_MARKER = Util.make(
			MarkerFactory.getMarker("PACKET_SENT"),
			marker -> marker.add(NETWORK_PACKETS_MARKER)
	);

	private static final NetworkState<ServerHandshakePacketListener> C2S_HANDSHAKE_STATE = HandshakeStates.C2S;

	private final NetworkSide side;
	private volatile boolean duringLogin = true;
	private final Queue<Consumer<ClientConnection>> queuedTasks = Queues.newConcurrentLinkedQueue();

	private Channel channel;
	private SocketAddress address;
	private volatile @Nullable PacketListener prePlayStateListener;
	private volatile @Nullable PacketListener packetListener;
	private @Nullable DisconnectionInfo disconnectionInfo;
	private boolean encrypted;
	private boolean disconnected;
	private int packetsReceivedCounter;
	private int packetsSentCounter;
	private float averagePacketsReceived;
	private float averagePacketsSent;
	private int ticks;
	private boolean errored;
	private volatile @Nullable DisconnectionInfo pendingDisconnectionInfo;
	@Nullable PacketSizeLogger packetSizeLogger;

	/**
	 * Создаёт соединение для указанной стороны протокола.
	 *
	 * @param side сторона соединения ({@link NetworkSide#CLIENTBOUND} или {@link NetworkSide#SERVERBOUND})
	 */
	public ClientConnection(NetworkSide side) {
		this.side = side;
	}

	/**
	 * Вызывается при активации канала — сохраняет канал и адрес, обрабатывает отложенное отключение.
	 */
	@Override
	public void channelActive(ChannelHandlerContext context) throws Exception {
		super.channelActive(context);
		channel = context.channel();
		address = channel.remoteAddress();

		if (pendingDisconnectionInfo != null) {
			disconnect(pendingDisconnectionInfo);
		}
	}

	/**
	 * Вызывается при закрытии канала — инициирует отключение с сообщением об обрыве потока.
	 */
	@Override
	public void channelInactive(ChannelHandlerContext context) {
		disconnect(Text.translatable("disconnect.endOfStream"));
	}

	/**
	 * Обрабатывает исключения канала: таймауты, ошибки пакетов и прочие сбои.
	 * При первой ошибке отправляет пакет отключения, при повторной — просто закрывает канал.
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext context, Throwable ex) {
		if (ex instanceof PacketException) {
			LOGGER.debug("Skipping packet due to errors", ex.getCause());
			return;
		}

		boolean firstError = errored == false;
		errored = true;

		if (channel.isOpen() == false) {
			return;
		}

		if (ex instanceof TimeoutException) {
			LOGGER.debug("Timeout", ex);
			disconnect(Text.translatable("disconnect.timeout"));
			return;
		}

		Text reason = Text.translatable("disconnect.genericReason", "Internal Exception: " + ex);
		PacketListener currentListener = packetListener;
		DisconnectionInfo info = currentListener != null
		                         ? currentListener.createDisconnectionInfo(reason, ex)
		                         : new DisconnectionInfo(reason);

		if (firstError) {
			LOGGER.debug("Failed to sent packet", ex);

			if (getOppositeSide() == NetworkSide.CLIENTBOUND) {
				Packet<?> disconnectPacket = duringLogin
				                             ? new LoginDisconnectS2CPacket(reason)
				                             : new DisconnectS2CPacket(reason);
				send(disconnectPacket, PacketCallbacks.always(() -> disconnect(info)));
			}
			else {
				disconnect(info);
			}

			tryDisableAutoRead();
		}
		else {
			LOGGER.debug("Double fault", ex);
			disconnect(info);
		}
	}

	/**
	 * Читает входящий пакет и передаёт его текущему слушателю для обработки.
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext context, Packet<?> packet) {
		if (channel.isOpen() == false) {
			return;
		}

		PacketListener currentListener = packetListener;

		if (currentListener == null) {
			throw new IllegalStateException("Received a packet before the packet listener was initialized");
		}

		if (currentListener.accepts(packet) == false) {
			return;
		}

		try {
			handlePacket(packet, currentListener);
		}
		catch (OffThreadException ignored) {
		}
		catch (RejectedExecutionException e) {
			disconnect(Text.translatable("multiplayer.disconnect.server_shutdown"));
		}
		catch (ClassCastException e) {
			LOGGER.error("Received {} that couldn't be processed", packet.getClass(), e);
			disconnect(Text.translatable("multiplayer.disconnect.invalid_packet"));
		}

		packetsReceivedCounter++;
	}

	/**
	 * Применяет пакет к слушателю с приведением типа.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener) {
		packet.apply((T) listener);
	}

	/**
	 * Переключает входящее состояние протокола и устанавливает новый слушатель пакетов.
	 *
	 * @param state          новое состояние протокола
	 * @param packetListener новый слушатель пакетов
	 * @throws IllegalStateException если состояние не соответствует стороне соединения
	 */
	public <T extends PacketListener> void transitionInbound(NetworkState<T> state, T packetListener) {
		validatePacketListener(state, packetListener);

		if (state.side() != getSide()) {
			throw new IllegalStateException("Invalid inbound protocol: " + state.id());
		}

		this.packetListener = packetListener;
		prePlayStateListener = null;

		NetworkStateTransitions.DecoderTransitioner transitioner = NetworkStateTransitions.decoderTransitioner(state);
		PacketBundleHandler bundleHandler = state.bundleHandler();

		if (bundleHandler != null) {
			PacketBundler bundler = new PacketBundler(bundleHandler);
			transitioner = transitioner.andThen(ctx -> ctx.pipeline().addAfter("decoder", "bundler", bundler));
		}

		syncUninterruptibly(channel.writeAndFlush(transitioner));
	}

	/**
	 * Переключает исходящее состояние протокола.
	 *
	 * @param newState новое исходящее состояние
	 * @throws IllegalStateException если состояние не соответствует противоположной стороне
	 */
	public void transitionOutbound(NetworkState<?> newState) {
		if (newState.side() != getOppositeSide()) {
			throw new IllegalStateException("Invalid outbound protocol: " + newState.id());
		}

		NetworkStateTransitions.EncoderTransitioner
				transitioner =
				NetworkStateTransitions.encoderTransitioner(newState);
		PacketBundleHandler bundleHandler = newState.bundleHandler();

		if (bundleHandler != null) {
			PacketUnbundler unbundler = new PacketUnbundler(bundleHandler);
			transitioner = transitioner.andThen(ctx -> ctx.pipeline().addAfter("encoder", "unbundler", unbundler));
		}

		boolean isLoginPhase = newState.id() == NetworkPhase.LOGIN;
		syncUninterruptibly(channel.writeAndFlush(transitioner.andThen(ctx -> duringLogin = isLoginPhase)));
	}

	/**
	 * Устанавливает начальный слушатель пакетов (только для серверной стороны в фазе рукопожатия).
	 *
	 * @param packetListener начальный слушатель
	 * @throws IllegalStateException если слушатель уже установлен или параметры некорректны
	 */
	public void setInitialPacketListener(PacketListener packetListener) {
		if (this.packetListener != null) {
			throw new IllegalStateException("Listener already set");
		}

		boolean isValidHandshake = side == NetworkSide.SERVERBOUND
				&& packetListener.getSide() == NetworkSide.SERVERBOUND
				&& packetListener.getPhase() == C2S_HANDSHAKE_STATE.id();

		if (isValidHandshake == false) {
			throw new IllegalStateException("Invalid initial listener");
		}

		this.packetListener = packetListener;
	}

	/**
	 * Инициирует подключение для запроса статуса сервера.
	 *
	 * @param address  адрес сервера
	 * @param port     порт сервера
	 * @param listener слушатель ответов на запрос статуса
	 */
	public void connect(String address, int port, ClientQueryPacketListener listener) {
		connect(address, port, QueryStates.C2S, QueryStates.S2C, listener, ConnectionIntent.STATUS);
	}

	/**
	 * Инициирует подключение для входа на сервер.
	 *
	 * @param address  адрес сервера
	 * @param port     порт сервера
	 * @param listener слушатель пакетов фазы логина
	 */
	public void connect(String address, int port, ClientLoginPacketListener listener) {
		connect(address, port, LoginStates.C2S, LoginStates.S2C, listener, ConnectionIntent.LOGIN);
	}

	/**
	 * Инициирует подключение с явным указанием состояний протокола.
	 *
	 * @param address              адрес сервера
	 * @param port                 порт сервера
	 * @param outboundState        исходящее состояние протокола
	 * @param inboundState         входящее состояние протокола
	 * @param prePlayStateListener слушатель до перехода в фазу игры
	 * @param transfer             {@code true} для переноса игрока между серверами
	 */
	public <S extends ServerPacketListener, C extends ClientPacketListener> void connect(
			String address,
			int port,
			NetworkState<S> outboundState,
			NetworkState<C> inboundState,
			C prePlayStateListener,
			boolean transfer
	) {
		connect(
				address,
				port,
				outboundState,
				inboundState,
				prePlayStateListener,
				transfer ? ConnectionIntent.TRANSFER : ConnectionIntent.LOGIN
		);
	}

	/**
	 * Отправляет пакет без колбэка.
	 *
	 * @param packet пакет для отправки
	 */
	public void send(Packet<?> packet) {
		send(packet, null);
	}

	/**
	 * Отправляет пакет с колбэком и принудительным сбросом буфера.
	 *
	 * @param packet   пакет для отправки
	 * @param listener колбэк результата отправки или {@code null}
	 */
	public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {
		send(packet, listener, true);
	}

	/**
	 * Отправляет пакет с опциональным колбэком и управлением сбросом буфера.
	 *
	 * @param packet   пакет для отправки
	 * @param listener колбэк результата отправки или {@code null}
	 * @param flush    {@code true} для немедленного сброса буфера
	 */
	public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
		if (isOpen()) {
			handleQueuedTasks();
			sendImmediately(packet, listener, flush);
		}
		else {
			queuedTasks.add(connection -> connection.sendImmediately(packet, listener, flush));
		}
	}

	/**
	 * Выполняет задачу в контексте соединения, откладывая её если канал ещё не открыт.
	 *
	 * @param task задача, принимающая это соединение
	 */
	public void submit(Consumer<ClientConnection> task) {
		if (isOpen()) {
			handleQueuedTasks();
			task.accept(this);
		}
		else {
			queuedTasks.add(task);
		}
	}

	/**
	 * Принудительно сбрасывает буфер исходящих пакетов.
	 */
	public void flush() {
		if (isOpen()) {
			flushInternal();
		}
		else {
			queuedTasks.add(ClientConnection::flushInternal);
		}
	}

	/**
	 * Обновляет состояние соединения за один тик: обрабатывает очередь задач,
	 * тикает слушатель, обрабатывает отключение и обновляет статистику пакетов.
	 */
	public void tick() {
		handleQueuedTasks();

		if (packetListener instanceof TickablePacketListener tickable) {
			tickable.tick();
		}

		if (isOpen() == false && disconnected == false) {
			handleDisconnection();
		}

		if (channel != null) {
			channel.flush();
		}

		if (ticks++ % STATS_UPDATE_INTERVAL_TICKS == 0) {
			updateStats();
		}

		if (packetSizeLogger != null) {
			packetSizeLogger.push();
		}
	}

	/**
	 * Обновляет скользящее среднее счётчиков отправленных и полученных пакетов.
	 */
	protected void updateStats() {
		averagePacketsSent = MathHelper.lerp(PACKET_COUNTER_LERP_WEIGHT, packetsSentCounter, averagePacketsSent);
		averagePacketsReceived =
				MathHelper.lerp(PACKET_COUNTER_LERP_WEIGHT, packetsReceivedCounter, averagePacketsReceived);
		packetsSentCounter = 0;
		packetsReceivedCounter = 0;
	}

	/**
	 * Возвращает адрес удалённой стороны соединения.
	 *
	 * @return адрес сокета
	 */
	public SocketAddress getAddress() {
		return address;
	}

	/**
	 * Возвращает строковое представление адреса с учётом настройки логирования IP.
	 *
	 * @param logIps {@code true} — вернуть реальный адрес, {@code false} — скрыть IP
	 * @return строка с адресом или {@code "local"} / {@code "IP hidden"}
	 */
	public String getAddressAsString(boolean logIps) {
		if (address == null) {
			return "local";
		}

		return logIps ? address.toString() : "IP hidden";
	}

	/**
	 * Отключает соединение с заданным текстовым сообщением.
	 *
	 * @param disconnectReason причина отключения
	 */
	public void disconnect(Text disconnectReason) {
		disconnect(new DisconnectionInfo(disconnectReason));
	}

	/**
	 * Отключает соединение с полной информацией об отключении.
	 * Если канал ещё не создан — сохраняет информацию для отложенного отключения.
	 *
	 * @param disconnectionInfo информация об отключении
	 */
	public void disconnect(DisconnectionInfo disconnectionInfo) {
		if (channel == null) {
			pendingDisconnectionInfo = disconnectionInfo;
		}

		if (isOpen()) {
			channel.close().awaitUninterruptibly();
			this.disconnectionInfo = disconnectionInfo;
		}
	}

	/**
	 * Проверяет, является ли соединение локальным (внутрипроцессным).
	 *
	 * @return {@code true} для локального соединения
	 */
	public boolean isLocal() {
		return channel instanceof LocalChannel || channel instanceof LocalServerChannel;
	}

	/**
	 * Возвращает сторону этого соединения.
	 *
	 * @return сторона соединения
	 */
	public NetworkSide getSide() {
		return side;
	}

	/**
	 * Возвращает противоположную сторону соединения.
	 *
	 * @return противоположная сторона
	 */
	public NetworkSide getOppositeSide() {
		return side.getOpposite();
	}

	/**
	 * Создаёт и синхронно подключает новое клиентское соединение к указанному адресу.
	 *
	 * @param address       адрес сервера
	 * @param backend       сетевой бэкенд
	 * @param packetSizeLog логгер размеров пакетов или {@code null}
	 * @return готовое соединение
	 */
	public static ClientConnection connect(
			InetSocketAddress address,
			NetworkingBackend backend,
			@Nullable MultiValueDebugSampleLogImpl packetSizeLog
	) {
		ClientConnection connection = new ClientConnection(NetworkSide.CLIENTBOUND);

		if (packetSizeLog != null) {
			connection.resetPacketSizeLog(packetSizeLog);
		}

		connect(address, backend, connection).syncUninterruptibly();
		return connection;
	}

	/**
	 * Создаёт Netty-канал и подключает его к указанному адресу.
	 *
	 * @param address    адрес сервера
	 * @param backend    сетевой бэкенд
	 * @param connection соединение для привязки к каналу
	 * @return future подключения
	 */
	public static ChannelFuture connect(
			InetSocketAddress address,
			NetworkingBackend backend,
			ClientConnection connection
	) {
		return new Bootstrap()
				.group(backend.getEventLoopGroup())
				.handler(new ChannelInitializer<Channel>() {
					@Override
					protected void initChannel(Channel channel) {
						try {
							channel.config().setOption(ChannelOption.TCP_NODELAY, true);
						}
						catch (ChannelException ignored) {
						}

						ChannelPipeline pipeline = channel.pipeline()
						                                  .addLast(
								                                  "timeout",
								                                  new ReadTimeoutHandler(READ_TIMEOUT_SECONDS)
						                                  );
						addHandlers(pipeline, NetworkSide.CLIENTBOUND, false, connection.packetSizeLogger);
						connection.addFlowControlHandler(pipeline);
					}
				})
				.channel(backend.getChannelClass())
				.connect(address.getAddress(), address.getPort());
	}

	/**
	 * Добавляет обработчик управления потоком и финальный обработчик пакетов в пайплайн.
	 *
	 * @param pipeline пайплайн канала
	 */
	public void addFlowControlHandler(ChannelPipeline pipeline) {
		pipeline.addLast(
				"hackfix", new ChannelOutboundHandlerAdapter() {
					@Override
					public void write(ChannelHandlerContext context, Object value, ChannelPromise promise)
					throws Exception {
						super.write(context, value, promise);
					}
				}
		).addLast("packet_handler", this);
	}

	/**
	 * Добавляет стандартные обработчики кодирования/декодирования в пайплайн Netty.
	 *
	 * @param pipeline         пайплайн канала
	 * @param side             сторона соединения
	 * @param local            {@code true} для локального (внутрипроцессного) соединения
	 * @param packetSizeLogger логгер размеров пакетов или {@code null}
	 */
	public static void addHandlers(
			ChannelPipeline pipeline,
			NetworkSide side,
			boolean local,
			@Nullable PacketSizeLogger packetSizeLogger
	) {
		NetworkSide oppositeSide = side.getOpposite();
		boolean isServerbound = side == NetworkSide.SERVERBOUND;
		boolean isOppositeServerbound = oppositeSide == NetworkSide.SERVERBOUND;

		pipeline
				.addLast("splitter", getSplitter(packetSizeLogger, local))
				.addLast(new ChannelHandler[]{new FlowControlHandler()})
				.addLast(
						getInboundHandlerName(isServerbound),
						(ChannelHandler) (isServerbound
						                  ? new DecoderHandler<ServerHandshakePacketListener>(C2S_HANDSHAKE_STATE)
						                  : new NetworkStateTransitions.InboundConfigurer()
						)
				)
				.addLast("prepender", getPrepender(local))
				.addLast(
						getOutboundHandlerName(isOppositeServerbound),
						(ChannelHandler) (isOppositeServerbound
						                  ? new EncoderHandler<ServerHandshakePacketListener>(C2S_HANDSHAKE_STATE)
						                  : new NetworkStateTransitions.OutboundConfigurer()
						)
				);
	}

	/**
	 * Добавляет обработчики для локального (внутрипроцессного) соединения.
	 *
	 * @param pipeline пайплайн канала
	 * @param side     сторона соединения
	 */
	public static void addLocalValidator(ChannelPipeline pipeline, NetworkSide side) {
		addHandlers(pipeline, side, true, null);
	}

	/**
	 * Создаёт локальное (внутрипроцессное) соединение для интегрированного сервера.
	 *
	 * @param address локальный адрес
	 * @return готовое локальное соединение
	 */
	public static ClientConnection connectLocal(SocketAddress address) {
		ClientConnection connection = new ClientConnection(NetworkSide.CLIENTBOUND);

		new Bootstrap()
				.group(NetworkingBackend.local().getEventLoopGroup())
				.handler(new ChannelInitializer<Channel>() {
					@Override
					protected void initChannel(Channel channel) {
						ChannelPipeline pipeline = channel.pipeline();
						addLocalValidator(pipeline, NetworkSide.CLIENTBOUND);
						connection.addFlowControlHandler(pipeline);
					}
				})
				.channel(NetworkingBackend.local().getChannelClass())
				.connect(address)
				.syncUninterruptibly();

		return connection;
	}

	/**
	 * Включает шифрование канала с заданными шифрами.
	 *
	 * @param decryptionCipher шифр для расшифровки входящих данных
	 * @param encryptionCipher шифр для шифрования исходящих данных
	 */
	public void setupEncryption(Cipher decryptionCipher, Cipher encryptionCipher) {
		encrypted = true;
		channel.pipeline().addBefore("splitter", "decrypt", new PacketDecryptor(decryptionCipher));
		channel.pipeline().addBefore("prepender", "encrypt", new PacketEncryptor(encryptionCipher));
	}

	/**
	 * Проверяет, зашифровано ли соединение.
	 *
	 * @return {@code true} если шифрование включено
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * Проверяет, открыт ли канал соединения.
	 *
	 * @return {@code true} если канал существует и открыт
	 */
	public boolean isOpen() {
		return channel != null && channel.isOpen();
	}

	/**
	 * Проверяет, отсутствует ли канал (соединение ещё не установлено).
	 *
	 * @return {@code true} если канал ещё не создан
	 */
	public boolean isChannelAbsent() {
		return channel == null;
	}

	/**
	 * Возвращает текущий слушатель пакетов.
	 *
	 * @return слушатель или {@code null} если не установлен
	 */
	public @Nullable PacketListener getPacketListener() {
		return packetListener;
	}

	/**
	 * Возвращает информацию об отключении.
	 *
	 * @return информация об отключении или {@code null} если соединение активно
	 */
	public @Nullable DisconnectionInfo getDisconnectionInfo() {
		return disconnectionInfo;
	}

	/**
	 * Отключает автоматическое чтение из канала (используется при превышении лимитов).
	 */
	public void tryDisableAutoRead() {
		if (channel != null) {
			channel.config().setAutoRead(false);
		}
	}

	/**
	 * Устанавливает порог сжатия пакетов.
	 * Отрицательное значение отключает сжатие.
	 *
	 * @param compressionThreshold порог в байтах; отрицательное значение отключает сжатие
	 * @param rejectsBadPackets    отклонять ли некорректно сжатые пакеты
	 */
	public void setCompressionThreshold(int compressionThreshold, boolean rejectsBadPackets) {
		if (compressionThreshold >= 0) {
			if (channel.pipeline().get("decompress") instanceof PacketInflater inflater) {
				inflater.setCompressionThreshold(compressionThreshold, rejectsBadPackets);
			}
			else {
				channel
						.pipeline()
						.addAfter(
								"splitter",
								"decompress",
								new PacketInflater(compressionThreshold, rejectsBadPackets)
						);
			}

			if (channel.pipeline().get("compress") instanceof PacketDeflater deflater) {
				deflater.setCompressionThreshold(compressionThreshold);
			}
			else {
				channel.pipeline().addAfter("prepender", "compress", new PacketDeflater(compressionThreshold));
			}
		}
		else {
			if (channel.pipeline().get("decompress") instanceof PacketInflater) {
				channel.pipeline().remove("decompress");
			}

			if (channel.pipeline().get("compress") instanceof PacketDeflater) {
				channel.pipeline().remove("compress");
			}
		}
	}

	/**
	 * Финализирует отключение: уведомляет слушателя об отключении ровно один раз.
	 * Использует {@code prePlayStateListener} как запасной вариант, если основной слушатель не установлен.
	 */
	public void handleDisconnection() {
		if (channel == null || channel.isOpen()) {
			return;
		}

		if (disconnected) {
			LOGGER.warn("handleDisconnection() called twice");
			return;
		}

		disconnected = true;

		PacketListener currentListener = getPacketListener();
		PacketListener activeListener = currentListener != null ? currentListener : prePlayStateListener;

		if (activeListener == null) {
			return;
		}

		DisconnectionInfo info = Objects.requireNonNullElseGet(
				getDisconnectionInfo(),
				() -> new DisconnectionInfo(Text.translatable("multiplayer.disconnect.generic"))
		);
		activeListener.onDisconnected(info);
	}

	/**
	 * Возвращает скользящее среднее количества полученных пакетов в секунду.
	 *
	 * @return среднее количество полученных пакетов
	 */
	public float getAveragePacketsReceived() {
		return averagePacketsReceived;
	}

	/**
	 * Возвращает скользящее среднее количества отправленных пакетов в секунду.
	 *
	 * @return среднее количество отправленных пакетов
	 */
	public float getAveragePacketsSent() {
		return averagePacketsSent;
	}

	/**
	 * Инициализирует логгер размеров пакетов.
	 *
	 * @param log журнал отладочных метрик
	 */
	public void resetPacketSizeLog(MultiValueDebugSampleLogImpl log) {
		packetSizeLogger = new PacketSizeLogger(log);
	}

	private <S extends ServerPacketListener, C extends ClientPacketListener> void connect(
			String address,
			int port,
			NetworkState<S> outboundState,
			NetworkState<C> inboundState,
			C prePlayStateListener,
			ConnectionIntent intent
	) {
		if (outboundState.id() != inboundState.id()) {
			throw new IllegalStateException("Mismatched initial protocols");
		}

		this.prePlayStateListener = prePlayStateListener;
		submit(connection -> {
			transitionInbound(inboundState, prePlayStateListener);
			connection.sendImmediately(
					new HandshakeC2SPacket(SharedConstants.getGameVersion().protocolVersion(), address, port, intent),
					null,
					true
			);
			transitionOutbound(outboundState);
		});
	}

	private void sendImmediately(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
		packetsSentCounter++;

		if (channel.eventLoop().inEventLoop()) {
			sendInternal(packet, listener, flush);
		}
		else {
			channel.eventLoop().execute(() -> sendInternal(packet, listener, flush));
		}
	}

	private void sendInternal(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
		if (listener != null) {
			ChannelFuture future = flush ? channel.writeAndFlush(packet) : channel.write(packet);
			future.addListener(listener);
		}
		else if (flush) {
			channel.writeAndFlush(packet, channel.voidPromise());
		}
		else {
			channel.write(packet, channel.voidPromise());
		}
	}

	private void flushInternal() {
		if (channel.eventLoop().inEventLoop()) {
			channel.flush();
		}
		else {
			channel.eventLoop().execute(channel::flush);
		}
	}

	private void handleQueuedTasks() {
		if (channel == null || channel.isOpen() == false) {
			return;
		}

		synchronized (queuedTasks) {
			Consumer<ClientConnection> task;

			while ((task = queuedTasks.poll()) != null) {
				task.accept(this);
			}
		}
	}

	private void validatePacketListener(NetworkState<?> state, PacketListener listener) {
		Objects.requireNonNull(listener, "packetListener");

		NetworkSide listenerSide = listener.getSide();

		if (listenerSide != side) {
			throw new IllegalStateException(
					"Trying to set listener for wrong side: connection is " + side + ", but listener is " + listenerSide
			);
		}

		NetworkPhase listenerPhase = listener.getPhase();

		if (state.id() != listenerPhase) {
			throw new IllegalStateException(
					"Listener protocol (" + listenerPhase + ") does not match requested one " + state
			);
		}
	}

	private static void syncUninterruptibly(ChannelFuture future) {
		try {
			future.syncUninterruptibly();
		}
		catch (Exception e) {
			if (e instanceof ClosedChannelException) {
				LOGGER.info("Connection closed during protocol change");
			}
			else {
				throw e;
			}
		}
	}

	private static ChannelOutboundHandler getPrepender(boolean local) {
		return local ? new LocalBufPacker() : new SizePrepender();
	}

	private static ChannelInboundHandler getSplitter(@Nullable PacketSizeLogger packetSizeLogger, boolean local) {
		if (local == false) {
			return new SplitterHandler(packetSizeLogger);
		}

		return packetSizeLogger != null
		       ? new PacketSizeLogHandler(packetSizeLogger)
		       : new LocalBufUnpacker();
	}

	private static String getOutboundHandlerName(boolean sendingSide) {
		return sendingSide ? "encoder" : "outbound_config";
	}

	private static String getInboundHandlerName(boolean receivingSide) {
		return receivingSide ? "decoder" : "inbound_config";
	}
}
