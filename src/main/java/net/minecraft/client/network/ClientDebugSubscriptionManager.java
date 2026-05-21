package net.minecraft.client.network;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.DebugSubscriptionRequestC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.log.DebugSampleType;
import net.minecraft.world.World;
import net.minecraft.world.debug.DebugDataStore;
import net.minecraft.world.debug.DebugSubscriptionType;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Менеджер подписок на отладочные данные сервера.
 * Отслеживает активные подписки, хранит полученные значения
 * и предоставляет их через {@link DebugDataStore}.
 */
@Environment(EnvType.CLIENT)
public class ClientDebugSubscriptionManager {

	private static final long INEXPIRABLE = -1L;

	private final ClientPlayNetworkHandler networkHandler;
	private final DebugHud debugHud;
	private Set<DebugSubscriptionType<?>> clientSubscriptions = Set.of();
	private final Map<DebugSubscriptionType<?>, ClientDebugSubscriptionManager.TrackableValueMap<?>>
			valuesBySubscription =
			new HashMap<>();

	/**
	 * @param networkHandler сетевой обработчик для отправки пакетов подписки
	 * @param debugHud       отладочный HUD для определения нужных подписок
	 */
	public ClientDebugSubscriptionManager(ClientPlayNetworkHandler networkHandler, DebugHud debugHud) {
		this.debugHud = debugHud;
		this.networkHandler = networkHandler;
	}

	/**
	 * Обновляет подписки в начале тика и удаляет устаревшие значения.
	 *
	 * @param time текущее игровое время
	 */
	public void startTick(long time) {
		Set<DebugSubscriptionType<?>> requested = getRequestedSubscriptions();

		if (requested.equals(clientSubscriptions) == false) {
			clientSubscriptions = requested;
			onSubscriptionsChanged(requested);
		}

		valuesBySubscription.forEach((type, valueMap) -> {
			if (type.getExpiry() != 0) {
				valueMap.ejectExpiredSubscriptions(time);
			}
		});
	}

	/**
	 * Сбрасывает все активные подписки и очищает хранилище значений.
	 */
	public void clearAllSubscriptions() {
		clientSubscriptions = Set.of();
		clearValues();
	}

	/**
	 * Создаёт хранилище отладочных данных для указанного мира.
	 *
	 * @param world мир, для которого создаётся хранилище
	 * @return хранилище отладочных данных
	 */
	public DebugDataStore createDebugDataStore(World world) {
		return new DebugDataStore() {
			@Override
			public <T> void forEachChunkData(DebugSubscriptionType<T> type, BiConsumer<ChunkPos, T> action) {
				forEachValue(type, forChunks(), action);
			}

			@Override
			public <T> @Nullable T getChunkData(DebugSubscriptionType<T> type, ChunkPos chunkPos) {
				return getValue(type, chunkPos, forChunks());
			}

			@Override
			public <T> void forEachBlockData(DebugSubscriptionType<T> type, BiConsumer<BlockPos, T> action) {
				forEachValue(type, forBlocks(), action);
			}

			@Override
			public <T> @Nullable T getBlockData(DebugSubscriptionType<T> type, BlockPos pos) {
				return getValue(type, pos, forBlocks());
			}

			@Override
			public <T> void forEachEntityData(DebugSubscriptionType<T> type, BiConsumer<Entity, T> action) {
				forEachValue(
						type, forEntities(), (uuid, value) -> {
							Entity entity = world.getEntity(uuid);
							if (entity != null) {
								action.accept(entity, (T) value);
							}
						}
				);
			}

			@Override
			public <T> @Nullable T getEntityData(DebugSubscriptionType<T> type, Entity entity) {
				return getValue(type, entity.getUuid(), forEntities());
			}

			@Override
			public <T> void forEachEvent(DebugSubscriptionType<T> type, DebugDataStore.EventConsumer<T> action) {
				ClientDebugSubscriptionManager.TrackableValueMap<T> map = getTrackableValueMaps(type);

				if (map == null) {
					return;
				}

				long worldTime = world.getTime();
				for (ClientDebugSubscriptionManager.ValueWithExpiry<T> entry : map.values) {
					int remaining = (int) (entry.expiresAfterTime() - worldTime);
					action.accept(entry.value(), remaining, type.getExpiry());
				}
			}
		};
	}

	/**
	 * Обновляет отладочные данные для чанка.
	 *
	 * @param lifetime время жизни значения
	 * @param pos      позиция чанка
	 * @param optional новое значение
	 */
	public <T> void updateChunk(long lifetime, ChunkPos pos, DebugSubscriptionType.OptionalValue<T> optional) {
		updateTrackableValueMap(lifetime, pos, optional, forChunks());
	}

	/**
	 * Обновляет отладочные данные для блока.
	 *
	 * @param lifetime время жизни значения
	 * @param pos      позиция блока
	 * @param optional новое значение
	 */
	public <T> void updateBlock(long lifetime, BlockPos pos, DebugSubscriptionType.OptionalValue<T> optional) {
		updateTrackableValueMap(lifetime, pos, optional, forBlocks());
	}

	/**
	 * Обновляет отладочные данные для сущности.
	 *
	 * @param lifetime время жизни значения
	 * @param entity   сущность
	 * @param optional новое значение
	 */
	public <T> void updateEntity(long lifetime, Entity entity, DebugSubscriptionType.OptionalValue<T> optional) {
		updateTrackableValueMap(lifetime, entity.getUuid(), optional, forEntities());
	}

	/**
	 * Добавляет событие в список отладочных событий.
	 *
	 * @param lifetime время жизни события
	 * @param value    значение события
	 */
	public <T> void addEvent(long lifetime, DebugSubscriptionType.Value<T> value) {
		ClientDebugSubscriptionManager.TrackableValueMap<T> map = getTrackableValueMaps(value.subscription());

		if (map == null) {
			return;
		}

		map.values.add(new ClientDebugSubscriptionManager.ValueWithExpiry<>(
				value.value(),
				lifetime + value.subscription().getExpiry()
		));
	}

	/**
	 * Очищает все хранимые значения и пересоздаёт карты для текущих подписок.
	 */
	public void clearValues() {
		valuesBySubscription.clear();
		ensureValueMapsExist(clientSubscriptions);
	}

	/**
	 * Удаляет все данные, связанные с указанным чанком.
	 *
	 * @param pos позиция чанка
	 */
	public void removeChunk(ChunkPos pos) {
		if (valuesBySubscription.isEmpty()) {
			return;
		}

		for (ClientDebugSubscriptionManager.TrackableValueMap<?> map : valuesBySubscription.values()) {
			map.removeChunk(pos);
		}
	}

	/**
	 * Удаляет все данные, связанные с указанной сущностью.
	 *
	 * @param entity сущность
	 */
	public void removeEntity(Entity entity) {
		if (valuesBySubscription.isEmpty()) {
			return;
		}

		for (ClientDebugSubscriptionManager.TrackableValueMap<?> map : valuesBySubscription.values()) {
			map.entities.removeUUID(entity.getUuid());
		}
	}

	@SuppressWarnings("unchecked")
	<V> ClientDebugSubscriptionManager.@Nullable TrackableValueMap<V> getTrackableValueMaps(DebugSubscriptionType<V> type) {
		return (ClientDebugSubscriptionManager.TrackableValueMap<V>) valuesBySubscription.get(type);
	}

	<K, V> void forEachValue(
			DebugSubscriptionType<V> type,
			ClientDebugSubscriptionManager.TrackableValueGetter<K, V> getter,
			BiConsumer<K, V> visitor
	) {
		ClientDebugSubscriptionManager.TrackableValue<K, V> trackable = getValue(type, getter);

		if (trackable != null) {
			trackable.forEach(visitor);
		}
	}

	<K, V> @Nullable V getValue(
			DebugSubscriptionType<V> type,
			K object,
			ClientDebugSubscriptionManager.TrackableValueGetter<K, V> getter
	) {
		ClientDebugSubscriptionManager.TrackableValue<K, V> trackable = getValue(type, getter);
		return trackable != null ? trackable.get(object) : null;
	}

	private <K, V> ClientDebugSubscriptionManager.@Nullable TrackableValue<K, V> getValue(
			DebugSubscriptionType<V> type,
			ClientDebugSubscriptionManager.TrackableValueGetter<K, V> getter
	) {
		ClientDebugSubscriptionManager.TrackableValueMap<V> map = getTrackableValueMaps(type);
		return map != null ? getter.get(map) : null;
	}

	private <K, V> void updateTrackableValueMap(
			long lifetime,
			K object,
			DebugSubscriptionType.OptionalValue<V> optional,
			ClientDebugSubscriptionManager.TrackableValueGetter<K, V> getter
	) {
		ClientDebugSubscriptionManager.TrackableValue<K, V> trackable = getValue(optional.subscription(), getter);

		if (trackable != null) {
			trackable.apply(lifetime, object, optional);
		}
	}

	private Set<DebugSubscriptionType<?>> getRequestedSubscriptions() {
		Set<DebugSubscriptionType<?>> set = new ReferenceOpenHashSet<>();
		addIfEnabled(set, DebugSampleType.TICK_TIME.getSubscriptionType(), debugHud.shouldRenderTickCharts());

		if (SharedConstants.DEBUG_ENABLED == false) {
			return set;
		}

		addIfEnabled(set, DebugSubscriptionTypes.BEES, SharedConstants.BEES);
		addIfEnabled(set, DebugSubscriptionTypes.BEE_HIVES, SharedConstants.BEES);
		addIfEnabled(set, DebugSubscriptionTypes.BRAINS, SharedConstants.BRAIN);
		addIfEnabled(set, DebugSubscriptionTypes.BREEZES, SharedConstants.BREEZE_MOB);
		addIfEnabled(set, DebugSubscriptionTypes.ENTITY_BLOCK_INTERSECTIONS, SharedConstants.ENTITY_BLOCK_INTERSECTION);
		addIfEnabled(set, DebugSubscriptionTypes.ENTITY_PATHS, SharedConstants.PATHFINDING);
		addIfEnabled(set, DebugSubscriptionTypes.GAME_EVENTS, SharedConstants.GAME_EVENT_LISTENERS);
		addIfEnabled(set, DebugSubscriptionTypes.GAME_EVENT_LISTENERS, SharedConstants.GAME_EVENT_LISTENERS);
		addIfEnabled(set, DebugSubscriptionTypes.GOAL_SELECTORS, SharedConstants.GOAL_SELECTOR || SharedConstants.BEES);
		addIfEnabled(set, DebugSubscriptionTypes.NEIGHBOR_UPDATES, SharedConstants.NEIGHBORSUPDATE);
		addIfEnabled(set, DebugSubscriptionTypes.POIS, SharedConstants.POI);
		addIfEnabled(set, DebugSubscriptionTypes.RAIDS, SharedConstants.RAIDS);
		addIfEnabled(
				set,
				DebugSubscriptionTypes.REDSTONE_WIRE_ORIENTATIONS,
				SharedConstants.EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER
		);
		addIfEnabled(set, DebugSubscriptionTypes.STRUCTURES, SharedConstants.STRUCTURES);
		addIfEnabled(set, DebugSubscriptionTypes.VILLAGE_SECTIONS, SharedConstants.VILLAGE_SECTIONS);
		return set;
	}

	private void onSubscriptionsChanged(Set<DebugSubscriptionType<?>> subscriptions) {
		valuesBySubscription.keySet().retainAll(subscriptions);
		ensureValueMapsExist(subscriptions);
		networkHandler.sendPacket(new DebugSubscriptionRequestC2SPacket(subscriptions));
	}

	private void ensureValueMapsExist(Set<DebugSubscriptionType<?>> subscriptions) {
		for (DebugSubscriptionType<?> type : subscriptions) {
			valuesBySubscription.computeIfAbsent(type, key -> new ClientDebugSubscriptionManager.TrackableValueMap<>());
		}
	}

	private static void addIfEnabled(Set<DebugSubscriptionType<?>> set, DebugSubscriptionType<?> type, boolean enable) {
		if (enable) {
			set.add(type);
		}
	}

	static <T> ClientDebugSubscriptionManager.TrackableValueGetter<UUID, T> forEntities() {
		return maps -> maps.entities;
	}

	static <T> ClientDebugSubscriptionManager.TrackableValueGetter<BlockPos, T> forBlocks() {
		return maps -> maps.blocks;
	}

	static <T> ClientDebugSubscriptionManager.TrackableValueGetter<ChunkPos, T> forChunks() {
		return maps -> maps.chunks;
	}

	/**
	 * Хранилище отслеживаемых значений с поддержкой истечения срока.
	 */
	@Environment(EnvType.CLIENT)
	static class TrackableValue<K, V> {

		private final Map<K, ClientDebugSubscriptionManager.ValueWithExpiry<V>> trackableValues = new HashMap<>();

		/**
		 * Удаляет all.
		 *
		 * @param predicate predicate
		 */
		public void removeAll(Predicate<ClientDebugSubscriptionManager.ValueWithExpiry<V>> predicate) {
			trackableValues.values().removeIf(predicate);
		}

		/**
		 * Удаляет u u i d.
		 *
		 * @param object object
		 */
		public void removeUUID(K object) {
			trackableValues.remove(object);
		}

		/**
		 * Удаляет keys.
		 *
		 * @param predicate predicate
		 */
		public void removeKeys(Predicate<K> predicate) {
			trackableValues.keySet().removeIf(predicate);
		}

		/**
		 * Get.
		 *
		 * @param object object
		 *
		 * @return @Nullable V — 
		 */
		public @Nullable V get(K object) {
			ClientDebugSubscriptionManager.ValueWithExpiry<V> entry = trackableValues.get(object);
			return entry != null ? entry.value() : null;
		}

		/**
		 * Apply.
		 *
		 * @param lifetime lifetime
		 * @param object object
		 * @param value value
		 */
		public void apply(long lifetime, K object, DebugSubscriptionType.OptionalValue<V> value) {
			if (value.value().isPresent()) {
				trackableValues.put(
						object, new ClientDebugSubscriptionManager.ValueWithExpiry<>(
								value.value().get(),
								lifetime + value.subscription().getExpiry()
						)
				);
			}
			else {
				trackableValues.remove(object);
			}
		}

		/**
		 * For each.
		 *
		 * @param consumer consumer
		 */
		public void forEach(BiConsumer<K, V> consumer) {
			trackableValues.forEach((key, entry) -> consumer.accept(key, entry.value()));
		}
	}

	/**
	 * Геттер конкретного хранилища из карты значений.
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface TrackableValueGetter<K, V> {

		ClientDebugSubscriptionManager.TrackableValue<K, V> get(ClientDebugSubscriptionManager.TrackableValueMap<V> map);
	}

	/**
	 * Карта хранилищ значений по типу ключа (чанк, блок, сущность, события).
	 */
	@Environment(EnvType.CLIENT)
	static class TrackableValueMap<V> {

		final ClientDebugSubscriptionManager.TrackableValue<ChunkPos, V>
				chunks =
				new ClientDebugSubscriptionManager.TrackableValue<>();
		final ClientDebugSubscriptionManager.TrackableValue<BlockPos, V>
				blocks =
				new ClientDebugSubscriptionManager.TrackableValue<>();
		final ClientDebugSubscriptionManager.TrackableValue<UUID, V>
				entities =
				new ClientDebugSubscriptionManager.TrackableValue<>();
		final List<ClientDebugSubscriptionManager.ValueWithExpiry<V>> values = new ArrayList<>();

		/**
		 * Eject expired subscriptions.
		 *
		 * @param time time
		 */
		public void ejectExpiredSubscriptions(long time) {
			Predicate<ClientDebugSubscriptionManager.ValueWithExpiry<V>> expired = entry -> entry.hasExpired(time);
			chunks.removeAll(expired);
			blocks.removeAll(expired);
			entities.removeAll(expired);
			values.removeIf(expired);
		}

		/**
		 * Удаляет chunk.
		 *
		 * @param pos pos
		 */
		public void removeChunk(ChunkPos pos) {
			chunks.removeUUID(pos);
			blocks.removeKeys(pos::contains);
		}
	}

	/**
	 * Значение с временем истечения срока действия.
	 */
	@Environment(EnvType.CLIENT)
	record ValueWithExpiry<T>(T value, long expiresAfterTime) {

		public boolean hasExpired(long time) {
			return expiresAfterTime != INEXPIRABLE && time >= expiresAfterTime;
		}
	}
}
