package net.minecraft.client.session.telemetry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

@Environment(EnvType.CLIENT)
/**
 * {@code SampleEvent}.
 */
public abstract class SampleEvent {

	private static final int INTERVAL_IN_MILLIS = 60000;
	private static final int BATCH_SIZE = 10;
	private int sampleCount;
	private boolean enabled = false;
	private @Nullable Instant lastSampleTime;

	/**
	 * Start.
	 */
	public void start() {
		this.enabled = true;
		this.lastSampleTime = Instant.now();
		this.sampleCount = 0;
	}

	/**
	 * Tick.
	 *
	 * @param sender sender
	 */
	public void tick(TelemetrySender sender) {
		if (this.shouldSample()) {
			this.sample();
			this.sampleCount++;
			this.lastSampleTime = Instant.now();
		}

		if (this.shouldSend()) {
			this.send(sender);
			this.sampleCount = 0;
		}
	}

	/**
	 * Определяет, следует ли sample.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldSample() {
		return this.enabled && this.lastSampleTime != null
				&& Duration.between(this.lastSampleTime, Instant.now()).toMillis() > INTERVAL_IN_MILLIS;
	}

	/**
	 * Определяет, следует ли send.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldSend() {
		return this.sampleCount >= BATCH_SIZE;
	}

	/**
	 * Отключает sampling.
	 */
	public void disableSampling() {
		this.enabled = false;
	}

	protected int getSampleCount() {
		return this.sampleCount;
	}

	/**
	 * Sample.
	 */
	public abstract void sample();

	/**
	 * Send.
	 *
	 * @param sender sender
	 */
	public abstract void send(TelemetrySender sender);
}
