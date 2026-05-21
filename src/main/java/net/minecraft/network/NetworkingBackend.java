package net.minecraft.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadFactory;

/**
 * Абстракция над транспортным бэкендом Netty.
 * <p>Инкапсулирует выбор оптимального I/O-механизма в зависимости от операционной системы:
 * KQueue (macOS), Epoll (Linux) или NIO (кроссплатформенный fallback).
 * Для внутрипроцессного взаимодействия используется {@link #local()}.
 * <p>{@link EventLoopGroup} создаётся лениво при первом обращении и кэшируется.
 */
public abstract class NetworkingBackend {

	private static final NetworkingBackend NIO = new NetworkingBackend(
			"NIO",
			NioSocketChannel.class,
			NioServerSocketChannel.class
	) {
		@Override
		protected IoHandlerFactory newFactory() {
			return NioIoHandler.newFactory();
		}
	};

	private static final NetworkingBackend EPOLL = new NetworkingBackend(
			"Epoll",
			EpollSocketChannel.class,
			EpollServerSocketChannel.class
	) {
		@Override
		protected IoHandlerFactory newFactory() {
			return EpollIoHandler.newFactory();
		}
	};

	private static final NetworkingBackend KQUEUE = new NetworkingBackend(
			"Kqueue",
			KQueueSocketChannel.class,
			KQueueServerSocketChannel.class
	) {
		@Override
		protected IoHandlerFactory newFactory() {
			return KQueueIoHandler.newFactory();
		}
	};

	private static final NetworkingBackend LOCAL = new NetworkingBackend(
			"Local",
			LocalChannel.class,
			LocalServerChannel.class
	) {
		@Override
		protected IoHandlerFactory newFactory() {
			return LocalIoHandler.newFactory();
		}
	};

	private final String name;
	private final Class<? extends Channel> channelClass;
	private final Class<? extends ServerChannel> serverChannelClass;
	private volatile @Nullable EventLoopGroup eventLoopGroup;

	NetworkingBackend(
			String name,
			Class<? extends Channel> channelClass,
			Class<? extends ServerChannel> serverChannelClass
	) {
		this.name = name;
		this.channelClass = channelClass;
		this.serverChannelClass = serverChannelClass;
	}

	/**
	 * Возвращает оптимальный бэкенд для удалённых соединений.
	 * <p>При {@code useNativeTransport = true} выбирает KQueue или Epoll (если доступны),
	 * иначе возвращает NIO.
	 *
	 * @param useNativeTransport использовать нативный транспорт ОС
	 * @return наиболее производительный доступный бэкенд
	 */
	public static NetworkingBackend remote(boolean useNativeTransport) {
		if (useNativeTransport) {
			if (KQueue.isAvailable()) {
				return KQUEUE;
			}

			if (Epoll.isAvailable()) {
				return EPOLL;
			}
		}

		return NIO;
	}

	/**
	 * Возвращает бэкенд для внутрипроцессного (локального) соединения.
	 *
	 * @return локальный бэкенд на основе {@link LocalChannel}
	 */
	public static NetworkingBackend local() {
		return LOCAL;
	}

	/**
	 * Возвращает класс клиентского канала этого бэкенда.
	 *
	 * @return класс канала
	 */
	public Class<? extends Channel> getChannelClass() {
		return channelClass;
	}

	/**
	 * Возвращает класс серверного канала этого бэкенда.
	 *
	 * @return класс серверного канала
	 */
	public Class<? extends ServerChannel> getServerChannelClass() {
		return serverChannelClass;
	}

	/**
	 * Возвращает группу событийных циклов, создавая её при первом вызове (double-checked locking).
	 *
	 * @return группа событийных циклов Netty
	 */
	public EventLoopGroup getEventLoopGroup() {
		EventLoopGroup group = eventLoopGroup;

		if (group == null) {
			synchronized (this) {
				group = eventLoopGroup;

				if (group == null) {
					group = createEventLoopGroup();
					eventLoopGroup = group;
				}
			}
		}

		return group;
	}

	/**
	 * Создаёт фабрику потоков с именованием по шаблону {@code "Netty <name> IO #N"}.
	 */
	private ThreadFactory createThreadFactory() {
		return new ThreadFactoryBuilder()
				.setNameFormat("Netty " + name + " IO #%d")
				.setDaemon(true)
				.build();
	}

	/**
	 * Создаёт новую группу событийных циклов с фабрикой потоков этого бэкенда.
	 */
	private EventLoopGroup createEventLoopGroup() {
		return new MultiThreadIoEventLoopGroup(createThreadFactory(), newFactory());
	}

	/**
	 * Создаёт фабрику I/O-обработчиков, специфичную для данного бэкенда.
	 *
	 * @return фабрика I/O-обработчиков Netty
	 */
	protected abstract IoHandlerFactory newFactory();
}
