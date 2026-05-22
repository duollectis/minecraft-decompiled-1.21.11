package net.minecraft.util.profiler.log;

import net.minecraft.network.packet.s2c.play.DebugSampleS2CPacket;
import net.minecraft.server.debug.SubscriberTracker;

/**
 * Реализация {@link ArrayDebugSampleLog}, отправляющая данные подписчикам через
 * {@link SubscriberTracker} при каждом push, если есть активные подписчики.
 */
public class SubscribableDebugSampleLog extends ArrayDebugSampleLog {

	private final SubscriberTracker tracker;
	private final DebugSampleType type;

	public SubscribableDebugSampleLog(int size, SubscriberTracker tracker, DebugSampleType type) {
		this(size, tracker, type, new long[size]);
	}

	public SubscribableDebugSampleLog(int size, SubscriberTracker tracker, DebugSampleType type, long[] defaults) {
		super(size, defaults);
		this.tracker = tracker;
		this.type = type;
	}

	@Override
	protected void onPush() {
		if (tracker.hasSubscriber(type.getSubscriptionType())) {
			tracker.send(
				type.getSubscriptionType(),
				new DebugSampleS2CPacket((long[]) values.clone(), type)
			);
		}
	}
}
