package net.minecraft.util.profiler;

import java.time.Instant;

/**
 * Зафиксированное отклонение метрики: момент времени, номер тика
 * и снимок результатов профилирования в момент отклонения.
 */
public final class Deviation {

	public final Instant instant;
	public final int ticks;
	public final ProfileResult result;

	public Deviation(Instant instant, int ticks, ProfileResult result) {
		this.instant = instant;
		this.ticks = ticks;
		this.result = result;
	}
}
