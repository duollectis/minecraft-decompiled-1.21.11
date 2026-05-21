package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

/**
 * Информация о сервере, обнаруженном в локальной сети (LAN).
 * <p>Хранит MOTD и адрес сервера, а также время последнего обнаружения
 * для отслеживания актуальности записи в списке LAN-серверов.
 */
@Environment(EnvType.CLIENT)
public class LanServerInfo {

	private final String motd;
	private final String addressPort;
	private long lastTimeMillis;

	/**
	 * Создаёт запись о LAN-сервере с текущим временем обнаружения.
	 *
	 * @param motd        описание сервера (Message Of The Day)
	 * @param addressPort адрес и порт в формате {@code "host:port"}
	 */
	public LanServerInfo(String motd, String addressPort) {
		this.motd = motd;
		this.addressPort = addressPort;
		lastTimeMillis = Util.getMeasuringTimeMs();
	}

	/**
	 * Возвращает описание сервера.
	 *
	 * @return MOTD сервера
	 */
	public String getMotd() {
		return motd;
	}

	/**
	 * Возвращает адрес и порт сервера.
	 *
	 * @return строка вида {@code "host:port"}
	 */
	public String getAddressPort() {
		return addressPort;
	}

	/**
	 * Обновляет время последнего обнаружения сервера до текущего момента.
	 */
	public void updateLastTime() {
		lastTimeMillis = Util.getMeasuringTimeMs();
	}
}
