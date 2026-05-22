package net.minecraft.client.realms;

import com.google.common.util.concurrent.RateLimiter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Нарратор с ограничением частоты повторений одного и того же сообщения.
 * Использует {@link RateLimiter} для предотвращения спама одинаковыми фразами
 * при частых вызовах (например, в игровом цикле). При смене текста лимитер сбрасывается.
 */
@Environment(EnvType.CLIENT)
public class RepeatedNarrator {

	private final float permitsPerSecond;
	private final AtomicReference<RepeatedNarrator.@Nullable Parameters> params = new AtomicReference<>();

	public RepeatedNarrator(Duration interval) {
		this.permitsPerSecond = 1000.0F / (float) interval.toMillis();
	}

	/**
	 * Озвучивает текст через нарратор с учётом ограничения частоты.
	 * Если текст изменился — создаёт новый {@link RateLimiter} и озвучивает немедленно.
	 * Если текст тот же — озвучивает только если лимитер разрешает.
	 *
	 * @param narratorManager менеджер нарратора
	 * @param text текст для озвучивания
	 */
	public void narrate(NarratorManager narratorManager, Text text) {
		Parameters current = params.updateAndGet(
				existing -> existing != null && text.equals(existing.message)
						? existing
						: new Parameters(text, RateLimiter.create(permitsPerSecond))
		);

		if (current.rateLimiter.tryAcquire(1)) {
			narratorManager.narrateSystemImmediately(text);
		}
	}

	@Environment(EnvType.CLIENT)
	static class Parameters {

		final Text message;
		final RateLimiter rateLimiter;

		Parameters(Text message, RateLimiter rateLimiter) {
			this.message = message;
			this.rateLimiter = rateLimiter;
		}
	}
}
