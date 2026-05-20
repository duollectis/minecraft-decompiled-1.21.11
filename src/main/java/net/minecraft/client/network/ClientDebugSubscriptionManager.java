package net.minecraft.client.network;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
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

@Environment(EnvType.CLIENT)
public class ClientDebugSubscriptionManager {
   private final ClientPlayNetworkHandler networkHandler;
   private final DebugHud debugHud;
   private Set<DebugSubscriptionType<?>> clientSubscriptions = Set.of();
   private final Map<DebugSubscriptionType<?>, ClientDebugSubscriptionManager.TrackableValueMap<?>> valuesBySubscription = new HashMap<>();

   public ClientDebugSubscriptionManager(ClientPlayNetworkHandler networkHandler, DebugHud debugHud) {
      this.debugHud = debugHud;
      this.networkHandler = networkHandler;
   }

   private static void addDebugSubscription(Set<DebugSubscriptionType<?>> types, DebugSubscriptionType<?> type, boolean enable) {
      if (enable) {
         types.add(type);
      }
   }

   private Set<DebugSubscriptionType<?>> getRequestedSubscriptions() {
      Set<DebugSubscriptionType<?>> set = new ReferenceOpenHashSet();
      addDebugSubscription(set, DebugSampleType.TICK_TIME.getSubscriptionType(), this.debugHud.shouldRenderTickCharts());
      if (SharedConstants.DEBUG_ENABLED) {
         addDebugSubscription(set, DebugSubscriptionTypes.BEES, SharedConstants.BEES);
         addDebugSubscription(set, DebugSubscriptionTypes.BEE_HIVES, SharedConstants.BEES);
         addDebugSubscription(set, DebugSubscriptionTypes.BRAINS, SharedConstants.BRAIN);
         addDebugSubscription(set, DebugSubscriptionTypes.BREEZES, SharedConstants.BREEZE_MOB);
         addDebugSubscription(set, DebugSubscriptionTypes.ENTITY_BLOCK_INTERSECTIONS, SharedConstants.ENTITY_BLOCK_INTERSECTION);
         addDebugSubscription(set, DebugSubscriptionTypes.ENTITY_PATHS, SharedConstants.PATHFINDING);
         addDebugSubscription(set, DebugSubscriptionTypes.GAME_EVENTS, SharedConstants.GAME_EVENT_LISTENERS);
         addDebugSubscription(set, DebugSubscriptionTypes.GAME_EVENT_LISTENERS, SharedConstants.GAME_EVENT_LISTENERS);
         addDebugSubscription(set, DebugSubscriptionTypes.GOAL_SELECTORS, SharedConstants.GOAL_SELECTOR || SharedConstants.BEES);
         addDebugSubscription(set, DebugSubscriptionTypes.NEIGHBOR_UPDATES, SharedConstants.NEIGHBORSUPDATE);
         addDebugSubscription(set, DebugSubscriptionTypes.POIS, SharedConstants.POI);
         addDebugSubscription(set, DebugSubscriptionTypes.RAIDS, SharedConstants.RAIDS);
         addDebugSubscription(set, DebugSubscriptionTypes.REDSTONE_WIRE_ORIENTATIONS, SharedConstants.EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER);
         addDebugSubscription(set, DebugSubscriptionTypes.STRUCTURES, SharedConstants.STRUCTURES);
         addDebugSubscription(set, DebugSubscriptionTypes.VILLAGE_SECTIONS, SharedConstants.VILLAGE_SECTIONS);
      }

      return set;
   }

   public void clearAllSubscriptions() {
      this.clientSubscriptions = Set.of();
      this.clearValues();
   }

   public void startTick(long time) {
      Set<DebugSubscriptionType<?>> set = this.getRequestedSubscriptions();
      if (!set.equals(this.clientSubscriptions)) {
         this.clientSubscriptions = set;
         this.onSubscriptionsChanged(set);
      }

      this.valuesBySubscription.forEach((type, valueMap) -> {
         if (type.getExpiry() != 0) {
            valueMap.ejectExpiredSubscriptions(time);
         }
      });
   }

   private void onSubscriptionsChanged(Set<DebugSubscriptionType<?>> set) {
      this.valuesBySubscription.keySet().retainAll(set);
      this.method_76759(set);
      this.networkHandler.sendPacket(new DebugSubscriptionRequestC2SPacket(set));
   }

   private void method_76759(Set<DebugSubscriptionType<?>> set) {
      for (DebugSubscriptionType<?> debugSubscriptionType : set) {
         this.valuesBySubscription.computeIfAbsent(debugSubscriptionType, debugSubscriptionTypex -> new ClientDebugSubscriptionManager.TrackableValueMap());
      }
   }

   <V> ClientDebugSubscriptionManager.@Nullable TrackableValueMap<V> getTrackableValueMaps(DebugSubscriptionType<V> type) {
      return (ClientDebugSubscriptionManager.TrackableValueMap<V>)this.valuesBySubscription.get(type);
   }

   private <K, V> ClientDebugSubscriptionManager.@Nullable TrackableValue<K, V> getValue(
      DebugSubscriptionType<V> type, ClientDebugSubscriptionManager.TrackableValueGetter<K, V> getter
   ) {
      ClientDebugSubscriptionManager.TrackableValueMap<V> trackableValueMap = this.getTrackableValueMaps(type);
      return trackableValueMap != null ? getter.get(trackableValueMap) : null;
   }

   <K, V> @Nullable V getValue(DebugSubscriptionType<V> type, K object, ClientDebugSubscriptionManager.TrackableValueGetter<K, V> getter) {
      ClientDebugSubscriptionManager.TrackableValue<K, V> trackableValue = this.getValue(type, getter);
      return trackableValue != null ? trackableValue.get(object) : null;
   }

   public DebugDataStore createDebugDataStore(World world) {
      return new DebugDataStore() {
         @Override
         public <T> void forEachChunkData(DebugSubscriptionType<T> type, BiConsumer<ChunkPos, T> action) {
            ClientDebugSubscriptionManager.this.forEachValue(type, ClientDebugSubscriptionManager.forChunks(), action);
         }

         @Override
         public <T> @Nullable T getChunkData(DebugSubscriptionType<T> type, ChunkPos chunkPos) {
            return ClientDebugSubscriptionManager.this.getValue(type, chunkPos, ClientDebugSubscriptionManager.forChunks());
         }

         @Override
         public <T> void forEachBlockData(DebugSubscriptionType<T> type, BiConsumer<BlockPos, T> action) {
            ClientDebugSubscriptionManager.this.forEachValue(type, ClientDebugSubscriptionManager.forBlocks(), action);
         }

         @Override
         public <T> @Nullable T getBlockData(DebugSubscriptionType<T> type, BlockPos pos) {
            return ClientDebugSubscriptionManager.this.getValue(type, pos, ClientDebugSubscriptionManager.forBlocks());
         }

         @Override
         public <T> void forEachEntityData(DebugSubscriptionType<T> type, BiConsumer<Entity, T> action) {
            ClientDebugSubscriptionManager.this.forEachValue(type, ClientDebugSubscriptionManager.forEntities(), (uuid, typex) -> {
               Entity entity = world.getEntity(uuid);
               if (entity != null) {
                  action.accept(entity, (T)typex);
               }
            });
         }

         @Override
         public <T> @Nullable T getEntityData(DebugSubscriptionType<T> type, Entity entity) {
            return ClientDebugSubscriptionManager.this.getValue(type, entity.getUuid(), ClientDebugSubscriptionManager.forEntities());
         }

         @Override
         public <T> void forEachEvent(DebugSubscriptionType<T> type, DebugDataStore.EventConsumer<T> action) {
            ClientDebugSubscriptionManager.TrackableValueMap<T> trackableValueMap = ClientDebugSubscriptionManager.this.getTrackableValueMaps(type);
            if (trackableValueMap != null) {
               long l = world.getTime();

               for (ClientDebugSubscriptionManager.ValueWithExpiry<T> valueWithExpiry : trackableValueMap.values) {
                  int i = (int)(valueWithExpiry.expiresAfterTime() - l);
                  int j = type.getExpiry();
                  action.accept(valueWithExpiry.value(), i, j);
               }
            }
         }
      };
   }

   public <T> void updateChunk(long lifetime, ChunkPos pos, DebugSubscriptionType.OptionalValue<T> optional) {
      this.updateTrackableValueMap(lifetime, pos, optional, forChunks());
   }

   public <T> void updateBlock(long lifetime, BlockPos pos, DebugSubscriptionType.OptionalValue<T> optional) {
      this.updateTrackableValueMap(lifetime, pos, optional, forBlocks());
   }

   public <T> void updateEntity(long lifetime, Entity entity, DebugSubscriptionType.OptionalValue<T> optional) {
      this.updateTrackableValueMap(lifetime, entity.getUuid(), optional, forEntities());
   }

   public <T> void addEvent(long lifetime, DebugSubscriptionType.Value<T> value) {
      ClientDebugSubscriptionManager.TrackableValueMap<T> trackableValueMap = this.getTrackableValueMaps(value.subscription());
      if (trackableValueMap != null) {
         trackableValueMap.values.add(new ClientDebugSubscriptionManager.ValueWithExpiry<>(value.value(), lifetime + value.subscription().getExpiry()));
      }
   }

   private <K, V> void updateTrackableValueMap(
      long lifetime, K object, DebugSubscriptionType.OptionalValue<V> optional, ClientDebugSubscriptionManager.TrackableValueGetter<K, V> trackableValueGetter
   ) {
      ClientDebugSubscriptionManager.TrackableValue<K, V> trackableValue = this.getValue(optional.subscription(), trackableValueGetter);
      if (trackableValue != null) {
         trackableValue.apply(lifetime, object, optional);
      }
   }

   <K, V> void forEachValue(DebugSubscriptionType<V> type, ClientDebugSubscriptionManager.TrackableValueGetter<K, V> getter, BiConsumer<K, V> visitor) {
      ClientDebugSubscriptionManager.TrackableValue<K, V> trackableValue = this.getValue(type, getter);
      if (trackableValue != null) {
         trackableValue.forEach(visitor);
      }
   }

   public void clearValues() {
      this.valuesBySubscription.clear();
      this.method_76759(this.clientSubscriptions);
   }

   public void removeChunk(ChunkPos pos) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriptionManager.TrackableValueMap<?> trackableValueMap : this.valuesBySubscription.values()) {
            trackableValueMap.removeChunk(pos);
         }
      }
   }

   public void removeEntity(Entity entity) {
      if (!this.valuesBySubscription.isEmpty()) {
         for (ClientDebugSubscriptionManager.TrackableValueMap<?> trackableValueMap : this.valuesBySubscription.values()) {
            trackableValueMap.entities.removeUUID(entity.getUuid());
         }
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

   @Environment(EnvType.CLIENT)
   static class TrackableValue<K, V> {
      private final Map<K, ClientDebugSubscriptionManager.ValueWithExpiry<V>> trackableValues = new HashMap<>();

      public void removeAll(Predicate<ClientDebugSubscriptionManager.ValueWithExpiry<V>> predicate) {
         this.trackableValues.values().removeIf(predicate);
      }

      public void removeUUID(K object) {
         this.trackableValues.remove(object);
      }

      public void removeKeys(Predicate<K> predicate) {
         this.trackableValues.keySet().removeIf(predicate);
      }

      public @Nullable V get(K object) {
         ClientDebugSubscriptionManager.ValueWithExpiry<V> valueWithExpiry = this.trackableValues.get(object);
         return valueWithExpiry != null ? valueWithExpiry.value() : null;
      }

      public void apply(long l, K object, DebugSubscriptionType.OptionalValue<V> value) {
         if (value.value().isPresent()) {
            this.trackableValues.put(object, new ClientDebugSubscriptionManager.ValueWithExpiry<>(value.value().get(), l + value.subscription().getExpiry()));
         } else {
            this.trackableValues.remove(object);
         }
      }

      public void forEach(BiConsumer<K, V> biConsumer) {
         this.trackableValues.forEach((object, valueWithExpiry) -> biConsumer.accept((K)object, valueWithExpiry.value()));
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   interface TrackableValueGetter<K, V> {
      ClientDebugSubscriptionManager.TrackableValue<K, V> get(ClientDebugSubscriptionManager.TrackableValueMap<V> map);
   }

   @Environment(EnvType.CLIENT)
   static class TrackableValueMap<V> {
      final ClientDebugSubscriptionManager.TrackableValue<ChunkPos, V> chunks = new ClientDebugSubscriptionManager.TrackableValue<>();
      final ClientDebugSubscriptionManager.TrackableValue<BlockPos, V> blocks = new ClientDebugSubscriptionManager.TrackableValue<>();
      final ClientDebugSubscriptionManager.TrackableValue<UUID, V> entities = new ClientDebugSubscriptionManager.TrackableValue<>();
      final List<ClientDebugSubscriptionManager.ValueWithExpiry<V>> values = new ArrayList<>();

      public void ejectExpiredSubscriptions(long time) {
         Predicate<ClientDebugSubscriptionManager.ValueWithExpiry<V>> predicate = timex -> timex.hasExpired(time);
         this.chunks.removeAll(predicate);
         this.blocks.removeAll(predicate);
         this.entities.removeAll(predicate);
         this.values.removeIf(predicate);
      }

      public void removeChunk(ChunkPos pos) {
         this.chunks.removeUUID(pos);
         this.blocks.removeKeys(pos::contains);
      }
   }

   @Environment(EnvType.CLIENT)
   record ValueWithExpiry<T>(T value, long expiresAfterTime) {
      private static final long INEXPIRABLE = -1L;

      public boolean hasExpired(long time) {
         return this.expiresAfterTime == -1L ? false : time >= this.expiresAfterTime;
      }
   }
}
