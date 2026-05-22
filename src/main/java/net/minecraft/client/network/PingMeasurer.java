package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;

/**
 * Измеряет задержку соединения с сервером (пинг).
 * <p>Отправляет пакет {@link QueryPingC2SPacket} с текущим временем и вычисляет
 * RTT при получении ответа {@link PingResultS2CPacket}. Результаты записываются
 * в {@link MultiValueDebugSampleLogImpl} для отображения в отладочном HUD.
 */
@Environment(EnvType.CLIENT)
public class PingMeasurer {

	private final ClientPlayNetworkHandler handler;
	private final MultiValueDebugSampleLogImpl log;

	/**
	 * @param handler сетевой обработчик для отправки пакетов
	 * @param log     журнал для записи измерений пинга
	 */
	public PingMeasurer(ClientPlayNetworkHandler handler, MultiValueDebugSampleLogImpl log) {
		this.handler = handler;
		this.log = log;
	}

	/**
	 * Отправляет пинг-запрос серверу с текущей меткой времени.
	 */
	public void ping() {
		handler.sendPacket(new QueryPingC2SPacket(Util.getMeasuringTimeMs()));
	}

	/**
	 * Обрабатывает ответ сервера и записывает RTT в журнал.
	 *
	 * @param packet пакет с временной меткой отправки
	 */
	public void onPingResult(PingResultS2CPacket packet) {
		log.push(Util.getMeasuringTimeMs() - packet.startTime());
	}
}
