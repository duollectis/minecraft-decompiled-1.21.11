package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Менеджер обнаружения LAN-серверов через UDP-мультикаст.
 * Содержит поток-детектор и потокобезопасный список найденных серверов.
 */
@Environment(EnvType.CLIENT)
public class LanServerQueryManager {

	static final AtomicInteger THREAD_ID = new AtomicInteger(0);
	static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Поток, слушающий UDP-мультикаст и добавляющий найденные LAN-серверы в список.
	 */
	@Environment(EnvType.CLIENT)
	public static class LanServerDetector extends Thread {

		private static final String MULTICAST_GROUP = "224.0.2.60";
		private static final int MULTICAST_PORT = 4445;
		private static final int SOCKET_TIMEOUT_MS = 5000;
		private static final int BUFFER_SIZE = 1024;

		private final LanServerQueryManager.LanServerEntryList entryList;
		private final InetAddress multicastAddress;
		private final MulticastSocket socket;

		/**
		 * @param entryList список, в который будут добавляться найденные серверы
		 * @throws IOException если не удалось создать мультикаст-сокет
		 */
		public LanServerDetector(LanServerQueryManager.LanServerEntryList entryList) throws IOException {
			super("LanServerDetector #" + LanServerQueryManager.THREAD_ID.incrementAndGet());
			this.entryList = entryList;
			setDaemon(true);
			setUncaughtExceptionHandler(new UncaughtExceptionLogger(LanServerQueryManager.LOGGER));
			socket = new MulticastSocket(MULTICAST_PORT);
			multicastAddress = InetAddress.getByName(MULTICAST_GROUP);
			socket.setSoTimeout(SOCKET_TIMEOUT_MS);
			socket.joinGroup(multicastAddress);
		}

		@Override
		public void run() {
			byte[] buffer = new byte[BUFFER_SIZE];

			while (isInterrupted() == false) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				try {
					socket.receive(packet);
				}
				catch (SocketTimeoutException ignored) {
					continue;
				}
				catch (IOException exception) {
					LanServerQueryManager.LOGGER.error("Couldn't ping server", exception);
					break;
				}

				String announcement = new String(
						packet.getData(),
						packet.getOffset(),
						packet.getLength(),
						StandardCharsets.UTF_8
				);
				LanServerQueryManager.LOGGER.debug("{}: {}", packet.getAddress(), announcement);
				entryList.addServer(announcement, packet.getAddress());
			}

			try {
				socket.leaveGroup(multicastAddress);
			}
			catch (IOException ignored) {
			}

			socket.close();
		}
	}

	/**
	 * Потокобезопасный список обнаруженных LAN-серверов с флагом изменений.
	 */
	@Environment(EnvType.CLIENT)
	public static class LanServerEntryList {

		private final List<LanServerInfo> serverEntries = Lists.newArrayList();
		private boolean dirty;

		/**
		 * Возвращает снимок списка серверов, если он изменился с последнего вызова.
		 *
		 * @return неизменяемая копия списка или {@code null}, если изменений не было
		 */
		public synchronized @Nullable List<LanServerInfo> getEntriesIfUpdated() {
			if (dirty == false) {
				return null;
			}

			List<LanServerInfo> snapshot = List.copyOf(serverEntries);
			dirty = false;
			return snapshot;
		}

		/**
		 * Добавляет или обновляет запись сервера по данным из UDP-объявления.
		 *
		 * @param announcement строка объявления (MOTD + адрес)
		 * @param address      IP-адрес отправителя пакета
		 */
		public synchronized void addServer(String announcement, InetAddress address) {
			String motd = LanServerPinger.parseAnnouncementMotd(announcement);
			String addressPort = LanServerPinger.parseAnnouncementAddressPort(announcement);

			if (addressPort == null) {
				return;
			}

			String fullAddress = address.getHostAddress() + ":" + addressPort;
			boolean alreadyKnown = false;

			for (LanServerInfo server : serverEntries) {
				if (server.getAddressPort().equals(fullAddress)) {
					server.updateLastTime();
					alreadyKnown = true;
					break;
				}
			}

			if (alreadyKnown == false) {
				serverEntries.add(new LanServerInfo(motd, fullAddress));
				dirty = true;
			}
		}
	}
}
