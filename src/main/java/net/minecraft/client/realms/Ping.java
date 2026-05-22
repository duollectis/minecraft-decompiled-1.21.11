package net.minecraft.client.realms;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.dto.RegionPingResult;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Comparator;
import java.util.List;

/**
 * Утилита для измерения задержки (ping) до серверов AWS Realms в разных регионах.
 * Каждый регион пингуется {@link #PING_ATTEMPTS} раз через TCP-соединение на порт 80,
 * а результат усредняется. Тайм-аут одной попытки — {@link #TIMEOUT_MS} мс.
 */
@Environment(EnvType.CLIENT)
public class Ping {

	private static final int PING_ATTEMPTS = 5;
	private static final int TIMEOUT_MS = 700;
	private static final int HTTP_PORT = 80;

	/**
	 * Измеряет задержку до каждого из переданных регионов и возвращает
	 * список результатов, отсортированный по возрастанию пинга.
	 *
	 * @param regions регионы для проверки
	 * @return отсортированный список результатов пинга
	 */
	public static List<RegionPingResult> ping(Ping.Region... regions) {
		// Прогревочный проход — первый пинг часто завышен из-за DNS-резолвинга
		for (Ping.Region region : regions) {
			pingHost(region.endpoint);
		}

		List<RegionPingResult> results = Lists.newArrayList();

		for (Ping.Region region : regions) {
			results.add(new RegionPingResult(region.name, pingHost(region.endpoint)));
		}

		results.sort(Comparator.comparingInt(RegionPingResult::ping));
		return results;
	}

	/**
	 * Пингует все известные регионы AWS и возвращает отсортированный список результатов.
	 *
	 * @return отсортированный список результатов пинга по всем регионам
	 */
	public static List<RegionPingResult> pingAllRegions() {
		return ping(Ping.Region.values());
	}

	private static int pingHost(String host) {
		long totalMs = 0L;
		Socket socket = null;

		for (int attempt = 0; attempt < PING_ATTEMPTS; attempt++) {
			try {
				SocketAddress address = new InetSocketAddress(host, HTTP_PORT);
				socket = new Socket();
				long startMs = now();
				socket.connect(address, TIMEOUT_MS);
				totalMs += now() - startMs;
			} catch (Exception ignored) {
				totalMs += TIMEOUT_MS;
			} finally {
				IOUtils.closeQuietly(socket);
			}
		}

		return (int) (totalMs / (double) PING_ATTEMPTS);
	}

	private static long now() {
		return Util.getMeasuringTimeMs();
	}

	@Environment(EnvType.CLIENT)
	enum Region {
		US_EAST_1("us-east-1", "ec2.us-east-1.amazonaws.com"),
		US_WEST_2("us-west-2", "ec2.us-west-2.amazonaws.com"),
		US_WEST_1("us-west-1", "ec2.us-west-1.amazonaws.com"),
		EU_WEST_1("eu-west-1", "ec2.eu-west-1.amazonaws.com"),
		AP_SOUTHEAST_1("ap-southeast-1", "ec2.ap-southeast-1.amazonaws.com"),
		AP_SOUTHEAST_2("ap-southeast-2", "ec2.ap-southeast-2.amazonaws.com"),
		AP_NORTHEAST_1("ap-northeast-1", "ec2.ap-northeast-1.amazonaws.com"),
		SA_EAST_1("sa-east-1", "ec2.sa-east-1.amazonaws.com");

		final String name;
		final String endpoint;

		Region(String name, String endpoint) {
			this.name = name;
			this.endpoint = endpoint;
		}
	}
}
