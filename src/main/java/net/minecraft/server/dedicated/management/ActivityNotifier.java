package net.minecraft.server.dedicated.management;

import net.minecraft.server.dedicated.management.listener.CompositeManagementListener;
import net.minecraft.util.Util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code ActivityNotifier}.
 */
public class ActivityNotifier {

	private final long rateLimitMs;
	private final AtomicLong lastUpdated = new AtomicLong();
	private final AtomicBoolean rateLimited = new AtomicBoolean(false);
	private final CompositeManagementListener listener;

	public ActivityNotifier(CompositeManagementListener listener, int rateLimitSeconds) {
		this.listener = listener;
		this.rateLimitMs = TimeUnit.SECONDS.toMillis(rateLimitSeconds);
	}

	/**
	 * Уведомляет listeners.
	 */
	public void notifyListeners() {
		this.notifyListenerImpl();
	}

	/**
	 * Уведомляет listeners with rate limit.
	 */
	public void notifyListenersWithRateLimit() {
		this.rateLimited.set(true);
		this.notifyListenerImpl();
	}

	private void notifyListenerImpl() {
		long l = Util.getMeasuringTimeMs();
		if (this.rateLimited.get() && l - this.lastUpdated.get() >= this.rateLimitMs) {
			this.listener.onServerActivity();
			this.lastUpdated.set(Util.getMeasuringTimeMs());
		}

		this.rateLimited.set(false);
	}
}
