package net.minecraft.client.util;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

/**
 * Стратегия повторных попыток с поддержкой экспоненциальной задержки.
 * Возвращает количество циклов, которые следует пропустить перед следующей попыткой.
 */
@Environment(EnvType.CLIENT)
public interface Backoff {

	Backoff ONE_CYCLE = new Backoff() {
		@Override
		public long success() {
			return 1L;
		}

		@Override
		public long fail() {
			return 1L;
		}
	};

	long success();

	long fail();

	/**
	 * Создаёт стратегию с экспоненциальным ростом задержки при ошибках.
	 * Задержка удваивается с каждой неудачей, но не превышает {@code maxSkippableCycles}.
	 *
	 * @param maxSkippableCycles максимальное количество пропускаемых циклов
	 * @return экземпляр {@code Backoff} с экспоненциальной задержкой
	 */
	static Backoff exponential(int maxSkippableCycles) {
		return new Backoff() {
			private static final Logger LOGGER = LogUtils.getLogger();
			private int failureCount;

			@Override
			public long success() {
				failureCount = 0;
				return 1L;
			}

			@Override
			public long fail() {
				failureCount++;
				long skippedCycles = Math.min(1L << failureCount, (long) maxSkippableCycles);
				LOGGER.debug("Skipping for {} extra cycles", skippedCycles);
				return skippedCycles;
			}
		};
	}
}
