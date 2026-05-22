package net.minecraft.util.profiler.log;

import net.minecraft.world.debug.DebugSubscriptionType;
import net.minecraft.world.debug.DebugSubscriptionTypes;

/**
 * Тип отладочного сэмпла, связывающий категорию данных с конкретным типом подписки
 * для отправки клиентам через {@link net.minecraft.network.packet.s2c.play.DebugSampleS2CPacket}.
 */
public enum DebugSampleType {
	TICK_TIME(DebugSubscriptionTypes.DEDICATED_SERVER_TICK_TIME);

	private final DebugSubscriptionType<?> subscriptionType;

	DebugSampleType(DebugSubscriptionType<?> subscriptionType) {
		this.subscriptionType = subscriptionType;
	}

	public DebugSubscriptionType<?> getSubscriptionType() {
		return subscriptionType;
	}
}
