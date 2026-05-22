package net.minecraft.server.debug;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockValueDebugS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkValueDebugS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityValueDebugS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.debug.DebugSubscriptionType;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.debug.data.PoiDebugData;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * Базовый класс отслеживания отладочной подписки для конкретного типа данных.
 * Управляет множеством подписанных игроков и рассылает им начальные и обновлённые данные.
 */
public abstract class TrackedSubscription<T> {

	protected final DebugSubscriptionType<T> type;
	private final Set<UUID> subscribingPlayers = new ObjectOpenHashSet();

	public TrackedSubscription(DebugSubscriptionType<T> type) {
		this.type = type;
	}

	public final void refreshTracking(ServerWorld world) {
		for (ServerPlayerEntity player : world.getPlayers()) {
			boolean wasSubscribed = subscribingPlayers.contains(player.getUuid());
			boolean isSubscribed = player.getSubscribedTypes().contains(type);

			if (isSubscribed == wasSubscribed) {
				continue;
			}

			if (isSubscribed) {
				startTracking(player);
			} else {
				subscribingPlayers.remove(player.getUuid());
			}
		}

		subscribingPlayers.removeIf(uuid -> world.getPlayerByUuid(uuid) == null);

		if (!subscribingPlayers.isEmpty()) {
			sendUpdate(world);
		}
	}

	private void startTracking(ServerPlayerEntity player) {
		subscribingPlayers.add(player.getUuid());
		player.getChunkFilter().forEach(chunkPos -> {
			if (!player.networkHandler.chunkDataSender.isInNextBatch(chunkPos.toLong())) {
				sendInitialIfSubscribed(player, chunkPos);
			}
		});
		player.getEntityWorld().getChunkManager().chunkLoadingManager.forEachEntityTrackedBy(
				player,
				entity -> sendInitialIfSubscribed(player, entity)
		);
	}

	protected final void sendToTrackingPlayers(
			ServerWorld world,
			ChunkPos chunkPos,
			Packet<? super ClientPlayPacketListener> packet
	) {
		ServerChunkLoadingManager chunkLoadingManager = world.getChunkManager().chunkLoadingManager;

		for (UUID uuid : subscribingPlayers) {
			if (world.getPlayerByUuid(uuid) instanceof ServerPlayerEntity player
					&& chunkLoadingManager.isTracked(player, chunkPos.x, chunkPos.z)) {
				player.networkHandler.sendPacket(packet);
			}
		}
	}

	protected final void sendToTrackingPlayers(
			ServerWorld world,
			Entity entity,
			Packet<? super ClientPlayPacketListener> packet
	) {
		world.getChunkManager().chunkLoadingManager.sendToOtherNearbyPlayersIf(
				entity,
				packet,
				player -> subscribingPlayers.contains(player.getUuid())
		);
	}

	public final void sendInitialIfSubscribed(ServerPlayerEntity player, ChunkPos chunkPos) {
		if (subscribingPlayers.contains(player.getUuid())) {
			sendInitial(player, chunkPos);
		}
	}

	public final void sendInitialIfSubscribed(ServerPlayerEntity player, Entity entity) {
		if (subscribingPlayers.contains(player.getUuid())) {
			sendInitial(player, entity);
		}
	}

	protected void clear() {
	}

	protected void sendUpdate(ServerWorld world) {
	}

	protected void sendInitial(ServerPlayerEntity player, ChunkPos chunkPos) {
	}

	protected void sendInitial(ServerPlayerEntity player, Entity entity) {
	}

	public static class TrackedPoi extends TrackedSubscription<PoiDebugData> {

		public TrackedPoi() {
			super(DebugSubscriptionTypes.POIS);
		}

		@Override
		protected void sendInitial(ServerPlayerEntity player, ChunkPos chunkPos) {
			ServerWorld serverWorld = player.getEntityWorld();
			serverWorld.getPointOfInterestStorage()
					.getInChunk(poiType -> true, chunkPos, PointOfInterestStorage.OccupationStatus.ANY)
					.forEach(poi -> player.networkHandler.sendPacket(new BlockValueDebugS2CPacket(
							poi.getPos(),
							type.optionalValueFor(new PoiDebugData(poi))
					)));
		}

		public void onPoiAdded(ServerWorld world, PointOfInterest poi) {
			sendToTrackingPlayers(
					world,
					new ChunkPos(poi.getPos()),
					new BlockValueDebugS2CPacket(poi.getPos(), type.optionalValueFor(new PoiDebugData(poi)))
			);
		}

		public void onPoiRemoved(ServerWorld world, BlockPos pos) {
			sendToTrackingPlayers(world, new ChunkPos(pos), new BlockValueDebugS2CPacket(pos, type.optionalValueFor()));
		}

		public void onPoiUpdated(ServerWorld world, BlockPos pos) {
			sendToTrackingPlayers(
					world,
					new ChunkPos(pos),
					new BlockValueDebugS2CPacket(pos, type.optionalValueFor(world.getPointOfInterestStorage().getDebugData(pos)))
			);
		}
	}

	public static class TrackedVillageSections extends TrackedSubscription<Unit> {

		public TrackedVillageSections() {
			super(DebugSubscriptionTypes.VILLAGE_SECTIONS);
		}

		@Override
		protected void sendInitial(ServerPlayerEntity player, ChunkPos chunkPos) {
			ServerWorld serverWorld = player.getEntityWorld();
			serverWorld.getPointOfInterestStorage()
					.getInChunk(poiType -> true, chunkPos, PointOfInterestStorage.OccupationStatus.ANY)
					.forEach(poi -> {
						ChunkSectionPos sectionPos = ChunkSectionPos.from(poi.getPos());
						forEachSurrounding(serverWorld, sectionPos, (surroundingPos, nearOccupied) -> {
							BlockPos center = surroundingPos.getCenterPos();
							player.networkHandler.sendPacket(new BlockValueDebugS2CPacket(
									center,
									type.optionalValueFor(nearOccupied ? Unit.INSTANCE : null)
							));
						});
					});
		}

		public void onPoiAdded(ServerWorld world, PointOfInterest poi) {
			handlePoiUpdate(world, poi.getPos());
		}

		public void onPoiRemoved(ServerWorld world, BlockPos pos) {
			handlePoiUpdate(world, pos);
		}

		private void handlePoiUpdate(ServerWorld world, BlockPos pos) {
			forEachSurrounding(world, ChunkSectionPos.from(pos), (sectionPos, nearOccupied) -> {
				BlockPos center = sectionPos.getCenterPos();
				DebugSubscriptionType.OptionalValue<Unit> value = nearOccupied
						? type.optionalValueFor(Unit.INSTANCE)
						: type.optionalValueFor();
				sendToTrackingPlayers(world, new ChunkPos(center), new BlockValueDebugS2CPacket(center, value));
			});
		}

		private static void forEachSurrounding(
				ServerWorld world,
				ChunkSectionPos sectionPos,
				BiConsumer<ChunkSectionPos, Boolean> action
		) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						ChunkSectionPos surrounding = sectionPos.add(dy, dz, dx);
						boolean nearOccupied = world.isNearOccupiedPointOfInterest(surrounding.getCenterPos());
						action.accept(surrounding, nearOccupied);
					}
				}
			}
		}
	}

	static class UpdateQuerier<T> {

		private final DebugTrackable.DebugDataSupplier<T> dataSupplier;
		@Nullable T lastData;

		UpdateQuerier(DebugTrackable.DebugDataSupplier<T> dataSupplier) {
			this.dataSupplier = dataSupplier;
		}

		public DebugSubscriptionType.@Nullable OptionalValue<T> queryUpdate(DebugSubscriptionType<T> type) {
			T current = dataSupplier.get();
			if (Objects.equals(current, lastData)) {
				return null;
			}

			lastData = current;
			return type.optionalValueFor(current);
		}
	}

	public static class UpdateTrackedSubscription<T> extends TrackedSubscription<T> {

		private final Map<ChunkPos, UpdateQuerier<T>> trackedChunks = new HashMap<>();
		private final Map<BlockPos, UpdateQuerier<T>> trackedBlockEntities = new HashMap<>();
		private final Map<UUID, UpdateQuerier<T>> trackedEntities = new HashMap<>();

		public UpdateTrackedSubscription(DebugSubscriptionType<T> subscriptionType) {
			super(subscriptionType);
		}

		@Override
		protected void clear() {
			trackedChunks.clear();
			trackedBlockEntities.clear();
			trackedEntities.clear();
		}

		@Override
		protected void sendUpdate(ServerWorld world) {
			for (Entry<ChunkPos, UpdateQuerier<T>> entry : trackedChunks.entrySet()) {
				DebugSubscriptionType.OptionalValue<T> update = entry.getValue().queryUpdate(type);
				if (update != null) {
					ChunkPos chunkPos = entry.getKey();
					sendToTrackingPlayers(world, chunkPos, new ChunkValueDebugS2CPacket(chunkPos, update));
				}
			}

			for (Entry<BlockPos, UpdateQuerier<T>> entry : trackedBlockEntities.entrySet()) {
				DebugSubscriptionType.OptionalValue<T> update = entry.getValue().queryUpdate(type);
				if (update != null) {
					BlockPos blockPos = entry.getKey();
					sendToTrackingPlayers(world, new ChunkPos(blockPos), new BlockValueDebugS2CPacket(blockPos, update));
				}
			}

			for (Entry<UUID, UpdateQuerier<T>> entry : trackedEntities.entrySet()) {
				DebugSubscriptionType.OptionalValue<T> update = entry.getValue().queryUpdate(type);
				if (update != null) {
					Entity entity = Objects.requireNonNull(world.getEntity(entry.getKey()));
					sendToTrackingPlayers(world, entity, new EntityValueDebugS2CPacket(entity.getId(), update));
				}
			}
		}

		public void trackChunk(ChunkPos chunkPos, DebugTrackable.DebugDataSupplier<T> dataSupplier) {
			trackedChunks.put(chunkPos, new UpdateQuerier<>(dataSupplier));
		}

		public void trackBlockEntity(BlockPos pos, DebugTrackable.DebugDataSupplier<T> dataSupplier) {
			trackedBlockEntities.put(pos, new UpdateQuerier<>(dataSupplier));
		}

		public void trackEntity(UUID uuid, DebugTrackable.DebugDataSupplier<T> dataSupplier) {
			trackedEntities.put(uuid, new UpdateQuerier<>(dataSupplier));
		}

		public void untrackChunk(ChunkPos chunkPos) {
			trackedChunks.remove(chunkPos);
			trackedBlockEntities.keySet().removeIf(chunkPos::contains);
		}

		public void untrackBlockEntity(ServerWorld world, BlockPos pos) {
			UpdateQuerier<T> removed = trackedBlockEntities.remove(pos);
			if (removed != null) {
				sendToTrackingPlayers(world, new ChunkPos(pos), new BlockValueDebugS2CPacket(pos, type.optionalValueFor()));
			}
		}

		public void untrackEntity(Entity entity) {
			trackedEntities.remove(entity.getUuid());
		}

		@Override
		protected void sendInitial(ServerPlayerEntity player, ChunkPos chunkPos) {
			UpdateQuerier<T> chunkQuerier = trackedChunks.get(chunkPos);
			if (chunkQuerier != null && chunkQuerier.lastData != null) {
				player.networkHandler.sendPacket(new ChunkValueDebugS2CPacket(
						chunkPos,
						type.optionalValueFor(chunkQuerier.lastData)
				));
			}

			for (Entry<BlockPos, UpdateQuerier<T>> entry : trackedBlockEntities.entrySet()) {
				T data = entry.getValue().lastData;
				BlockPos blockPos = entry.getKey();
				if (data != null && chunkPos.contains(blockPos)) {
					player.networkHandler.sendPacket(new BlockValueDebugS2CPacket(blockPos, type.optionalValueFor(data)));
				}
			}
		}

		@Override
		protected void sendInitial(ServerPlayerEntity player, Entity entity) {
			UpdateQuerier<T> entityQuerier = trackedEntities.get(entity.getUuid());
			if (entityQuerier != null && entityQuerier.lastData != null) {
				player.networkHandler.sendPacket(new EntityValueDebugS2CPacket(
						entity.getId(),
						type.optionalValueFor(entityQuerier.lastData)
				));
			}
		}
	}
}
