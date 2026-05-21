package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.InetSocketAddress;

/**
 * Абстракция над сетевым адресом, предоставляющая имя хоста, IP-адрес и порт.
 * <p>Используется для проверки адресов через {@link BlockListChecker} и разрешения
 * адресов через {@link AddressResolver}.
 */
@Environment(EnvType.CLIENT)
public interface Address {

	/**
	 * Возвращает доменное имя хоста.
	 *
	 * @return имя хоста (например, {@code "mc.example.com"})
	 */
	String getHostName();

	/**
	 * Возвращает строковое представление IP-адреса.
	 *
	 * @return IP-адрес (например, {@code "192.168.1.1"})
	 */
	String getHostAddress();

	/**
	 * Возвращает порт соединения.
	 *
	 * @return номер порта
	 */
	int getPort();

	/**
	 * Возвращает полный {@link InetSocketAddress} для установки соединения.
	 *
	 * @return адрес сокета
	 */
	InetSocketAddress getInetSocketAddress();

	/**
	 * Создаёт {@link Address} на основе уже разрешённого {@link InetSocketAddress}.
	 *
	 * @param address разрешённый адрес сокета
	 * @return обёртка над адресом
	 */
	static Address create(InetSocketAddress address) {
		return new Address() {
			@Override
			public String getHostName() {
				return address.getAddress().getHostName();
			}

			@Override
			public String getHostAddress() {
				return address.getAddress().getHostAddress();
			}

			@Override
			public int getPort() {
				return address.getPort();
			}

			@Override
			public InetSocketAddress getInetSocketAddress() {
				return address;
			}
		};
	}
}
