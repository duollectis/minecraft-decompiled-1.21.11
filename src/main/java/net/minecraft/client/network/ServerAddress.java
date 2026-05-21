package net.minecraft.client.network;

import com.google.common.net.HostAndPort;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.IDN;

/**
 * Адрес сервера Minecraft, состоящий из хоста и порта.
 * <p>Поддерживает парсинг строк вида {@code "host:port"} и {@code "host"},
 * автоматически подставляя {@link #DEFAULT_PORT} при отсутствии порта.
 * Хост нормализуется через IDN (Internationalized Domain Names).
 */
@Environment(EnvType.CLIENT)
public final class ServerAddress {

	/**
	 * Стандартный порт сервера Minecraft.
	 */
	public static final int DEFAULT_PORT = 25565;

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ServerAddress INVALID = new ServerAddress(
			HostAndPort.fromParts("server.invalid", DEFAULT_PORT)
	);

	private final HostAndPort hostAndPort;

	/**
	 * Создаёт адрес сервера из хоста и порта.
	 *
	 * @param host имя хоста или IP-адрес
	 * @param port порт сервера
	 */
	public ServerAddress(String host, int port) {
		this(HostAndPort.fromParts(host, port));
	}

	private ServerAddress(HostAndPort hostAndPort) {
		this.hostAndPort = hostAndPort;
	}

	/**
	 * Возвращает нормализованное имя хоста в формате ASCII (IDN).
	 *
	 * @return имя хоста или пустая строка при ошибке нормализации
	 */
	public String getAddress() {
		try {
			return IDN.toASCII(hostAndPort.getHost());
		}
		catch (IllegalArgumentException e) {
			return "";
		}
	}

	/**
	 * Возвращает порт сервера.
	 *
	 * @return номер порта
	 */
	public int getPort() {
		return hostAndPort.getPort();
	}

	/**
	 * Парсит строку адреса сервера.
	 * При некорректном адресе возвращает заглушку {@code INVALID}.
	 *
	 * @param address строка адреса вида {@code "host"} или {@code "host:port"}
	 * @return разобранный адрес или заглушка при ошибке
	 */
	public static ServerAddress parse(@Nullable String address) {
		if (address == null) {
			return INVALID;
		}

		try {
			HostAndPort parsed = HostAndPort.fromString(address).withDefaultPort(DEFAULT_PORT);
			return parsed.getHost().isEmpty() ? INVALID : new ServerAddress(parsed);
		}
		catch (IllegalArgumentException e) {
			LOGGER.info("Failed to parse URL {}", address, e);
			return INVALID;
		}
	}

	/**
	 * Проверяет, является ли строка допустимым адресом сервера.
	 *
	 * @param address строка для проверки
	 * @return {@code true} если адрес корректен
	 */
	public static boolean isValid(String address) {
		try {
			HostAndPort parsed = HostAndPort.fromString(address);
			String host = parsed.getHost();

			if (host.isEmpty() == false) {
				IDN.toASCII(host);
				return true;
			}
		}
		catch (IllegalArgumentException ignored) {
		}

		return false;
	}

	/**
	 * Парсит строку порта, возвращая {@link #DEFAULT_PORT} при ошибке.
	 *
	 * @param port строка с номером порта
	 * @return номер порта или {@link #DEFAULT_PORT}
	 */
	static int portOrDefault(String port) {
		try {
			return Integer.parseInt(port.trim());
		}
		catch (Exception e) {
			return DEFAULT_PORT;
		}
	}

	@Override
	public String toString() {
		return hostAndPort.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		return o instanceof ServerAddress other && hostAndPort.equals(other.hostAndPort);
	}

	@Override
	public int hashCode() {
		return hostAndPort.hashCode();
	}
}
