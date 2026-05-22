package net.minecraft.client.resource;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.LongMath;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Менеджер периодических уведомлений, загружаемых из JSON-ресурса.
 * Фильтрует записи по стране через {@code countryPredicate}, вычисляет
 * НОД периодов для планирования единого таймера и показывает тосты
 * через {@link SystemToast} в нужные моменты времени.
 */
@Environment(EnvType.CLIENT)
public class PeriodicNotificationManager
		extends SinglePreparationResourceReloader<Map<String, List<PeriodicNotificationManager.Entry>>>
		implements AutoCloseable {

	private static final Codec<Map<String, List<PeriodicNotificationManager.Entry>>> CODEC = Codec.unboundedMap(
			Codec.STRING,
			RecordCodecBuilder.<PeriodicNotificationManager.Entry>create(
					instance -> instance.group(
							Codec.LONG.optionalFieldOf("delay", 0L).forGetter(PeriodicNotificationManager.Entry::delay),
							Codec.LONG.fieldOf("period").forGetter(PeriodicNotificationManager.Entry::period),
							Codec.STRING.fieldOf("title").forGetter(PeriodicNotificationManager.Entry::title),
							Codec.STRING.fieldOf("message").forGetter(PeriodicNotificationManager.Entry::message)
					)
					.apply(instance, PeriodicNotificationManager.Entry::new)
			)
			.listOf()
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Identifier id;
	private final Object2BooleanFunction<String> countryPredicate;
	private @Nullable Timer timer;
	private PeriodicNotificationManager.@Nullable NotifyTask task;

	public PeriodicNotificationManager(Identifier id, Object2BooleanFunction<String> countryPredicate) {
		this.id = id;
		this.countryPredicate = countryPredicate;
	}

	@Override
	protected Map<String, List<PeriodicNotificationManager.Entry>> prepare(
			ResourceManager resourceManager,
			Profiler profiler
	) {
		try (Reader reader = resourceManager.openAsReader(id)) {
			return (Map) CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(reader)).result().orElseThrow();
		}
		catch (Exception exception) {
			LOGGER.warn("Failed to load {}", id, exception);
			return ImmutableMap.of();
		}
	}

	@Override
	protected void apply(
			Map<String, List<PeriodicNotificationManager.Entry>> map,
			ResourceManager resourceManager,
			Profiler profiler
	) {
		List<PeriodicNotificationManager.Entry> entries = map.entrySet()
				.stream()
				.filter(entry -> (Boolean) countryPredicate.apply(entry.getKey()))
				.map(Map.Entry::getValue)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());

		if (entries.isEmpty()) {
			cancelTimer();
			return;
		}

		if (entries.stream().anyMatch(entry -> entry.period == 0L)) {
			Util.logErrorOrPause("A periodic notification in " + id + " has a period of zero minutes");
			cancelTimer();
			return;
		}

		long minDelay = getMinDelay(entries);
		long period = getPeriod(entries, minDelay);

		if (timer == null) {
			timer = new Timer();
		}

		task = task == null
				? new PeriodicNotificationManager.NotifyTask(entries, minDelay, period)
				: task.reload(entries, period);

		timer.scheduleAtFixedRate(task, TimeUnit.MINUTES.toMillis(minDelay), TimeUnit.MINUTES.toMillis(period));
	}

	@Override
	public void close() {
		cancelTimer();
	}

	private void cancelTimer() {
		if (timer != null) {
			timer.cancel();
		}
	}

	/**
	 * Вычисляет общий период таймера как НОД всех периодов и смещений записей.
	 * Это позволяет одному таймеру обслуживать несколько уведомлений с разными периодами.
	 *
	 * @param entries  список записей уведомлений
	 * @param minDelay минимальная задержка среди всех записей
	 * @return НОД периодов, определяющий частоту срабатывания таймера
	 */
	private long getPeriod(List<PeriodicNotificationManager.Entry> entries, long minDelay) {
		return entries.stream()
				.mapToLong(entry -> {
					long offset = entry.delay - minDelay;
					return LongMath.gcd(offset, entry.period);
				})
				.reduce(LongMath::gcd)
				.orElseThrow(() -> new IllegalStateException("Empty notifications from: " + id));
	}

	private long getMinDelay(List<PeriodicNotificationManager.Entry> entries) {
		return entries.stream().mapToLong(entry -> entry.delay).min().orElse(0L);
	}

	/**
	 * Запись уведомления с задержкой, периодом и текстами тоста.
	 * Если {@code delay == 0}, задержка приравнивается к периоду,
	 * чтобы первое уведомление не показывалось немедленно при старте.
	 */
	@Environment(EnvType.CLIENT)
	public record Entry(long delay, long period, String title, String message) {

		public Entry(final long delay, final long period, final String title, final String message) {
			this.delay = delay != 0L ? delay : period;
			this.period = period;
			this.title = title;
			this.message = message;
		}
	}

	/**
	 * Задача таймера, которая при каждом срабатывании проверяет,
	 * пересёк ли счётчик времени границу очередного периода для каждой записи,
	 * и показывает первый подходящий тост через основной поток клиента.
	 */
	@Environment(EnvType.CLIENT)
	static class NotifyTask extends TimerTask {

		private final MinecraftClient client = MinecraftClient.getInstance();
		private final List<PeriodicNotificationManager.Entry> entries;
		private final long periodMs;
		private final AtomicLong delayMs;

		public NotifyTask(List<PeriodicNotificationManager.Entry> entries, long minDelayMs, long periodMs) {
			this.entries = entries;
			this.periodMs = periodMs;
			delayMs = new AtomicLong(minDelayMs);
		}

		public PeriodicNotificationManager.NotifyTask reload(
				List<PeriodicNotificationManager.Entry> newEntries,
				long period
		) {
			cancel();
			return new PeriodicNotificationManager.NotifyTask(newEntries, delayMs.get(), period);
		}

		@Override
		public void run() {
			long prevDelay = delayMs.getAndAdd(periodMs);
			long nextDelay = delayMs.get();

			for (PeriodicNotificationManager.Entry entry : entries) {
				if (prevDelay >= entry.delay) {
					long prevPeriodIndex = prevDelay / entry.period;
					long nextPeriodIndex = nextDelay / entry.period;
					if (prevPeriodIndex != nextPeriodIndex) {
						client.execute(
								() -> SystemToast.add(
										MinecraftClient.getInstance().getToastManager(),
										SystemToast.Type.PERIODIC_NOTIFICATION,
										Text.translatable(entry.title, prevPeriodIndex),
										Text.translatable(entry.message, prevPeriodIndex)
								)
						);
						return;
					}
				}
			}
		}
	}
}
