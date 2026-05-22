package net.minecraft.server.debug;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockValueDebugS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityValueDebugS2CPacket;
import net.minecraft.network.packet.s2c.play.EventDebugS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.DebugSubscriptionType;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.poi.PointOfInterest;

import java.util.*;

/**
 * Отслеживает отладочные подписки для конкретного {@link ServerWorld}.
 * Управляет регистрацией чанков, блок-энтити и сущностей в системе отладочных подписок,
 * а также рассылает обновления подписанным игрокам.
 */
public class SubscriptionTracker {

	private final ServerWorld world;
	private final List<TrackedSubscription<?>> subscriptions = new ArrayList<>();
	private final Map<DebugSubscriptionType<?>, TrackedSubscription.UpdateTrackedSubscription<?>> subscriptionsByTypes = new HashMap<>();
	private final TrackedSubscription.TrackedPoi trackedPoi = new TrackedSubscription.TrackedPoi();
	private final TrackedSubscription.TrackedVillageSections trackedVillageSections = new TrackedSubscription.TrackedVillageSections();
	private boolean stopped = true;
	private Set<DebugSubscriptionType<?>> subscribedTypes = Set.of();

	public SubscriptionTracker(ServerWorld world) {
		this.world = world;

		for (DebugSubscriptionType<?> subscriptionType : Registries.DEBUG_SUBSCRIPTION) {
			if (subscriptionType.getPacketCodec() != null) {
				subscriptionsByTypes.put(
						subscriptionType,
						new TrackedSubscription.UpdateTrackedSubscription<>(subscriptionType)
				);
			}
		}

		subscriptions.addAll(subscriptionsByTypes.values());
		subscriptions.add(trackedPoi);
		subscriptions.add(trackedVillageSections);
	}

	public void tick(SubscriberTracker subscriberTracker) {
		subscribedTypes = subscriberTracker.getSubscribedTypes();
		boolean nowEmpty = subscribedTypes.isEmpty();

		if (stopped != nowEmpty) {
			stopped = nowEmpty;
			if (nowEmpty) {
				subscriptions.forEach(TrackedSubscription::clear);
			} else {
				startTracking();
			}
		}

		if (!stopped) {
			subscriptions.forEach(sub -> sub.refreshTracking(world));
		}
	}

	private void startTracking() {
		ServerChunkLoadingManager chunkLoadingManager = world.getChunkManager().chunkLoadingManager;
		chunkLoadingManager.forEachChunk(this::trackChunk);

		for (Entity entity : world.iterateEntities()) {
			if (chunkLoadingManager.hasTrackingPlayer(entity)) {
				trackEntity(entity);
			}
		}
	}

	@SuppressWarnings("unchecked")
	<T> TrackedSubscription.UpdateTrackedSubscription<T> get(DebugSubscriptionType<T> type) {
		return (TrackedSubscription.UpdateTrackedSubscription<T>) subscriptionsByTypes.get(type);
	}

	public void trackChunk(WorldChunk chunk) {
		if (stopped) {
			return;
		}

		chunk.registerTracking(world, new DebugTrackable.Tracker() {
			@Override
			public <T> void track(DebugSubscriptionType<T> type, DebugTrackable.DebugDataSupplier<T> dataSupplier) {
				SubscriptionTracker.this.get(type).trackChunk(chunk.getPos(), dataSupplier);
			}
		});
		chunk.getBlockEntities().values().forEach(this::trackBlockEntity);
	}

	public void untrackChunk(ChunkPos chunkPos) {
		if (stopped) {
			return;
		}

		for (TrackedSubscription.UpdateTrackedSubscription<?> subscription : subscriptionsByTypes.values()) {
			subscription.untrackChunk(chunkPos);
		}
	}

	public void trackBlockEntity(BlockEntity blockEntity) {
		if (stopped) {
			return;
		}

		blockEntity.registerTracking(world, new DebugTrackable.Tracker() {
			@Override
			public <T> void track(DebugSubscriptionType<T> type, DebugTrackable.DebugDataSupplier<T> dataSupplier) {
				SubscriptionTracker.this.get(type).trackBlockEntity(blockEntity.getPos(), dataSupplier);
			}
		});
	}

	public void untrackBlockEntity(BlockPos pos) {
		if (stopped) {
			return;
		}

		for (TrackedSubscription.UpdateTrackedSubscription<?> subscription : subscriptionsByTypes.values()) {
			subscription.untrackBlockEntity(world, pos);
		}
	}

	public void trackEntity(Entity entity) {
		if (stopped) {
			return;
		}

		entity.registerTracking(world, new DebugTrackable.Tracker() {
			@Override
			public <T> void track(DebugSubscriptionType<T> type, DebugTrackable.DebugDataSupplier<T> dataSupplier) {
				SubscriptionTracker.this.get(type).trackEntity(entity.getUuid(), dataSupplier);
			}
		});
	}

	public void untrackEntity(Entity entity) {
		if (stopped) {
			return;
		}

		for (TrackedSubscription.UpdateTrackedSubscription<?> subscription : subscriptionsByTypes.values()) {
			subscription.untrackEntity(entity);
		}
	}

	public void sendInitialIfSubscribed(ServerPlayerEntity player, ChunkPos chunkPos) {
		if (stopped) {
			return;
		}

		subscriptions.forEach(sub -> sub.sendInitialIfSubscribed(player, chunkPos));
	}

	public void sendInitialIfSubscribed(ServerPlayerEntity player, Entity entity) {
		if (stopped) {
			return;
		}

		subscriptions.forEach(sub -> sub.sendInitialIfSubscribed(player, entity));
	}

	public void onPoiAdded(PointOfInterest poi) {
		if (stopped) {
			return;
		}

		trackedPoi.onPoiAdded(world, poi);
		trackedVillageSections.onPoiAdded(world, poi);
	}

	public void onPoiUpdated(BlockPos pos) {
		if (stopped) {
			return;
		}

		trackedPoi.onPoiUpdated(world, pos);
	}

	public void onPoiRemoved(BlockPos pos) {
		if (stopped) {
			return;
		}

		trackedPoi.onPoiRemoved(world, pos);
		trackedVillageSections.onPoiRemoved(world, pos);
	}

	public boolean isSubscribed(DebugSubscriptionType<?> type) {
		return subscribedTypes.contains(type);
	}

	public <T> void sendBlockDebugData(BlockPos pos, DebugSubscriptionType<T> type, T value) {
		if (!isSubscribed(type)) {
			return;
		}

		sendToTrackingPlayers(new ChunkPos(pos), type, new BlockValueDebugS2CPacket(pos, type.optionalValueFor(value)));
	}

	public <T> void removeBlockDebugData(BlockPos pos, DebugSubscriptionType<T> type) {
		if (!isSubscribed(type)) {
			return;
		}

		sendToTrackingPlayers(new ChunkPos(pos), type, new BlockValueDebugS2CPacket(pos, type.optionalValueFor()));
	}

	public <T> void sendEntityDebugData(Entity entity, DebugSubscriptionType<T> type, T value) {
		if (!isSubscribed(type)) {
			return;
		}

		sendToTrackingPlayers(entity, type, new EntityValueDebugS2CPacket(entity.getId(), type.optionalValueFor(value)));
	}

	public <T> void removeEntityDebugData(Entity entity, DebugSubscriptionType<T> type) {
		if (!isSubscribed(type)) {
			return;
		}

		sendToTrackingPlayers(entity, type, new EntityValueDebugS2CPacket(entity.getId(), type.optionalValueFor()));
	}

	public <T> void sendEventDebugData(BlockPos pos, DebugSubscriptionType<T> type, T value) {
		if (!isSubscribed(type)) {
			return;
		}

		sendToTrackingPlayers(new ChunkPos(pos), type, new EventDebugS2CPacket(type.valueFor(value)));
	}

	private void sendToTrackingPlayers(
			ChunkPos chunkPos,
			DebugSubscriptionType<?> type,
			Packet<? super ClientPlayPacketListener> packet
	) {
		for (ServerPlayerEntity player : world.getChunkManager().chunkLoadingManager.getPlayersWatchingChunk(chunkPos, false)) {
			if (player.getSubscribedTypes().contains(type)) {
				player.networkHandler.sendPacket(packet);
			}
		}
	}

	private void sendToTrackingPlayers(
			Entity entity,
			DebugSubscriptionType<?> type,
			Packet<? super ClientPlayPacketListener> packet
	) {
		world.getChunkManager().chunkLoadingManager.sendToOtherNearbyPlayersIf(
				entity,
				packet,
				player -> player.getSubscribedTypes().contains(type)
		);
	}
}
