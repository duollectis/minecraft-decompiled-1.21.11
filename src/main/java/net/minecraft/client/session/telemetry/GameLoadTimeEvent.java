package net.minecraft.client.session.telemetry;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
/**
 * {@code GameLoadTimeEvent}.
 */
public class GameLoadTimeEvent {

	public static final GameLoadTimeEvent INSTANCE = new GameLoadTimeEvent(Ticker.systemTicker());
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Ticker ticker;
	private final Map<TelemetryEventProperty<GameLoadTimeEvent.Measurement>, Stopwatch> stopwatches = new HashMap<>();
	private OptionalLong bootstrapTime = OptionalLong.empty();

	protected GameLoadTimeEvent(Ticker ticker) {
		this.ticker = ticker;
	}

	/**
	 * Запускает timer.
	 *
	 * @param property property
	 */
	public synchronized void startTimer(TelemetryEventProperty<GameLoadTimeEvent.Measurement> property) {
		this.addTimer(
				property,
				(Function<TelemetryEventProperty<GameLoadTimeEvent.Measurement>, Stopwatch>) (propertyx -> Stopwatch.createStarted(
						this.ticker)
				)
		);
	}

	public synchronized void addTimer(
			TelemetryEventProperty<GameLoadTimeEvent.Measurement> property,
			Stopwatch stopwatch
	) {
		this.addTimer(
				property,
				(Function<TelemetryEventProperty<GameLoadTimeEvent.Measurement>, Stopwatch>) (propertyx -> stopwatch)
		);
	}

	private synchronized void addTimer(
			TelemetryEventProperty<GameLoadTimeEvent.Measurement> property,
			Function<TelemetryEventProperty<GameLoadTimeEvent.Measurement>, Stopwatch> stopwatchProvider
	) {
		this.stopwatches.computeIfAbsent(property, stopwatchProvider);
	}

	/**
	 * Останавливает timer.
	 *
	 * @param property property
	 */
	public synchronized void stopTimer(TelemetryEventProperty<GameLoadTimeEvent.Measurement> property) {
		Stopwatch stopwatch = this.stopwatches.get(property);
		if (stopwatch == null) {
			LOGGER.warn("Attempted to end step for {} before starting it", property.id());
		}
		else {
			if (stopwatch.isRunning()) {
				stopwatch.stop();
			}
		}
	}

	/**
	 * Send.
	 *
	 * @param sender sender
	 */
	public void send(TelemetrySender sender) {
		sender.send(
				TelemetryEventType.GAME_LOAD_TIMES,
				properties -> {
					synchronized (this) {
						this.stopwatches
								.forEach(
										(property, stopwatch) -> {
											if (!stopwatch.isRunning()) {
												long l = stopwatch.elapsed(TimeUnit.MILLISECONDS);
												properties.put(
														(TelemetryEventProperty<GameLoadTimeEvent.Measurement>) property,
														new GameLoadTimeEvent.Measurement((int) l)
												);
											}
											else {
												LOGGER.warn(
														"Measurement {} was discarded since it was still ongoing when the event {} was sent.",
														property.id(),
														TelemetryEventType.GAME_LOAD_TIMES.getId()
												);
											}
										}
								);
						this.bootstrapTime
								.ifPresent(
										bootstrapTime -> properties.put(
												TelemetryEventProperty.LOAD_TIME_BOOTSTRAP_MS,
												new GameLoadTimeEvent.Measurement((int) bootstrapTime)
										)
								);
						this.stopwatches.clear();
					}
				}
		);
	}

	public synchronized void setBootstrapTime(long bootstrapTime) {
		this.bootstrapTime = OptionalLong.of(bootstrapTime);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Measurement}.
	 */
	public record Measurement(int millis) {

		public static final Codec<GameLoadTimeEvent.Measurement>
				CODEC =
				Codec.INT.xmap(GameLoadTimeEvent.Measurement::new, measurement -> measurement.millis);
	}
}
