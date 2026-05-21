package net.minecraft.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import org.slf4j.Logger;

/**
 * Соединение с ограничением скорости входящих пакетов.
 * <p>Расширяет {@link ClientConnection}, добавляя проверку частоты пакетов.
 * При превышении лимита игрок получает сообщение об отключении и отсоединяется.
 */
public class RateLimitedConnection extends ClientConnection {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text RATE_LIMIT_EXCEEDED_MESSAGE = Text.translatable("disconnect.exceeded_packet_rate");

	private final int rateLimit;

	/**
	 * Создаёт соединение с заданным лимитом пакетов в секунду.
	 *
	 * @param rateLimit максимально допустимое количество пакетов в секунду
	 */
	public RateLimitedConnection(int rateLimit) {
		super(NetworkSide.SERVERBOUND);
		this.rateLimit = rateLimit;
	}

	@Override
	protected void updateStats() {
		super.updateStats();

		float packetsPerSecond = getAveragePacketsReceived();

		if (packetsPerSecond > rateLimit) {
			LOGGER.warn("Player exceeded rate-limit (sent {} packets per second)", packetsPerSecond);
			send(
					new DisconnectS2CPacket(RATE_LIMIT_EXCEEDED_MESSAGE),
					PacketCallbacks.always(() -> disconnect(RATE_LIMIT_EXCEEDED_MESSAGE))
			);
			tryDisableAutoRead();
		}
	}
}
