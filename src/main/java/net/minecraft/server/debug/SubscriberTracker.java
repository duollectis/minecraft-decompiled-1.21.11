package net.minecraft.server.debug;

import net.minecraft.SharedConstants;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.debug.DebugSubscriptionType;

import java.util.*;

/**
 * Отслеживает, какие игроки подписаны на какие типы отладочных данных.
 * На каждом тике перестраивает карту подписчиков и позволяет рассылать
 * отладочные пакеты только заинтересованным игрокам.
 */
public class SubscriberTracker {

	private final MinecraftServer server;
	private final Map<DebugSubscriptionType<?>, List<ServerPlayerEntity>> subscribers = new HashMap<>();

	public SubscriberTracker(MinecraftServer server) {
		this.server = server;
	}

	private List<ServerPlayerEntity> getSubscribers(DebugSubscriptionType<?> type) {
		return subscribers.getOrDefault(type, List.of());
	}

	public void tick() {
		subscribers.values().forEach(List::clear);

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			for (DebugSubscriptionType<?> subscriptionType : player.getSubscribedTypes()) {
				subscribers
						.computeIfAbsent(subscriptionType, type -> new ArrayList<>())
						.add(player);
			}
		}

		subscribers.values().removeIf(List::isEmpty);
	}

	public void send(DebugSubscriptionType<?> type, Packet<?> packet) {
		for (ServerPlayerEntity player : getSubscribers(type)) {
			player.networkHandler.sendPacket(packet);
		}
	}

	public Set<DebugSubscriptionType<?>> getSubscribedTypes() {
		return Set.copyOf(subscribers.keySet());
	}

	public boolean hasSubscriber(DebugSubscriptionType<?> type) {
		return !getSubscribers(type).isEmpty();
	}

	/**
	 * Проверяет, может ли игрок подписаться на отладочные данные.
	 * В режиме разработки разрешено хосту, в продакшне — только операторам.
	 *
	 * @param player проверяемый игрок
	 * @return {@code true} если игрок имеет право на подписку
	 */
	public boolean canSubscribe(ServerPlayerEntity player) {
		PlayerConfigEntry configEntry = player.getPlayerConfigEntry();
		return SharedConstants.isDevelopment && server.isHost(configEntry)
				|| server.getPlayerManager().isOperator(configEntry);
	}
}
