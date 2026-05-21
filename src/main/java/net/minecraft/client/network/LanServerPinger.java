package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.logging.UncaughtExceptionLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Поток, периодически рассылающий UDP-объявления об открытом LAN-сервере.
 * <p>Отправляет широковещательные пакеты на мультикаст-адрес {@link #PING_ADDRESS}:{@link #PING_PORT}
 * каждые {@link #PING_INTERVAL_MS} миллисекунд. Клиенты в той же сети получают эти пакеты
 * и отображают сервер в списке LAN-серверов.
 */
@Environment(EnvType.CLIENT)
public class LanServerPinger extends Thread {

	/**
	 * Мультикаст-адрес для объявлений LAN-серверов.
	 */
	public static final String PING_ADDRESS = "224.0.2.60";

	/**
	 * Порт для объявлений LAN-серверов.
	 */
	public static final int PING_PORT = 4445;

	private static final long PING_INTERVAL_MS = 1500L;
	private static final AtomicInteger THREAD_ID = new AtomicInteger(0);
	private static final Logger LOGGER = LogUtils.getLogger();

	private final String motd;
	private final String addressPort;
	private final DatagramSocket socket;
	private boolean running = true;

	/**
	 * Создаёт и настраивает поток пингования LAN.
	 *
	 * @param motd        описание сервера для объявления
	 * @param addressPort адрес и порт сервера в формате {@code "host:port"}
	 * @throws IOException если не удалось создать UDP-сокет
	 */
	public LanServerPinger(String motd, String addressPort) throws IOException {
		super("LanServerPinger #" + THREAD_ID.incrementAndGet());
		this.motd = motd;
		this.addressPort = addressPort;
		setDaemon(true);
		setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
		socket = new DatagramSocket();
	}

	@Override
	public void run() {
		String announcement = createAnnouncement(motd, addressPort);
		byte[] data = announcement.getBytes(StandardCharsets.UTF_8);

		while (isInterrupted() == false && running) {
			try {
				InetAddress multicastAddress = InetAddress.getByName(PING_ADDRESS);
				DatagramPacket packet = new DatagramPacket(data, data.length, multicastAddress, PING_PORT);
				socket.send(packet);
			}
			catch (IOException e) {
				LOGGER.warn("LanServerPinger: {}", e.getMessage());
				break;
			}

			try {
				sleep(PING_INTERVAL_MS);
			}
			catch (InterruptedException ignored) {
			}
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		running = false;
	}

	/**
	 * Формирует строку объявления LAN-сервера.
	 *
	 * @param motd        описание сервера
	 * @param addressPort адрес и порт
	 * @return строка объявления в формате {@code [MOTD]...[/MOTD][AD]...[/AD]}
	 */
	public static String createAnnouncement(String motd, String addressPort) {
		return "[MOTD]" + motd + "[/MOTD][AD]" + addressPort + "[/AD]";
	}

	/**
	 * Извлекает MOTD из строки объявления LAN-сервера.
	 *
	 * @param announcement строка объявления
	 * @return MOTD или {@code "missing no"} если формат некорректен
	 */
	public static String parseAnnouncementMotd(String announcement) {
		int start = announcement.indexOf("[MOTD]");

		if (start < 0) {
			return "missing no";
		}

		int end = announcement.indexOf("[/MOTD]", start + "[MOTD]".length());
		return end < start ? "missing no" : announcement.substring(start + "[MOTD]".length(), end);
	}

	/**
	 * Извлекает адрес и порт из строки объявления LAN-сервера.
	 *
	 * @param announcement строка объявления
	 * @return строка {@code "host:port"} или {@code null} если формат некорректен
	 */
	public static @Nullable String parseAnnouncementAddressPort(String announcement) {
		int motdEnd = announcement.indexOf("[/MOTD]");

		if (motdEnd < 0) {
			return null;
		}

		int duplicateMotdEnd = announcement.indexOf("[/MOTD]", motdEnd + "[/MOTD]".length());

		if (duplicateMotdEnd >= 0) {
			return null;
		}

		int adStart = announcement.indexOf("[AD]", motdEnd + "[/MOTD]".length());

		if (adStart < 0) {
			return null;
		}

		int adEnd = announcement.indexOf("[/AD]", adStart + "[AD]".length());
		return adEnd < adStart ? null : announcement.substring(adStart + "[AD]".length(), adEnd);
	}
}
