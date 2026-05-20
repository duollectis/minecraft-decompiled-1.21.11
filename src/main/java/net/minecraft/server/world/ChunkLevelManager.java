package net.minecraft.server.world;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.SharedConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TriState;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class ChunkLevelManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int NEARBY_PLAYER_TICKET_LEVEL = ChunkLevels.getLevelFromType(ChunkLevelType.ENTITY_TICKING);
   final Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos = new Long2ObjectOpenHashMap();
   private final TicketDistanceLevelPropagator ticketDistanceLevelPropagator;
   private final SimulationDistanceLevelPropagator simulationDistanceLevelPropagator;
   final ChunkTicketManager ticketManager;
   private final ChunkLevelManager.DistanceFromNearestPlayerTracker distanceFromNearestPlayerTracker = new ChunkLevelManager.DistanceFromNearestPlayerTracker(8);
   private final ChunkLevelManager.NearbyChunkTicketUpdater nearbyChunkTicketUpdater = new ChunkLevelManager.NearbyChunkTicketUpdater(32);
   protected final Set<ChunkHolder> chunkHoldersWithPendingUpdates = new ReferenceOpenHashSet();
   final ThrottledChunkTaskScheduler scheduler;
   final LongSet freshPlayerTicketPositions = new LongOpenHashSet();
   final Executor mainThreadExecutor;
   private int simulationDistance = 10;

   protected ChunkLevelManager(ChunkTicketManager ticketManager, Executor executor, Executor mainThreadExecutor) {
      this.ticketManager = ticketManager;
      this.ticketDistanceLevelPropagator = new TicketDistanceLevelPropagator(this, ticketManager);
      this.simulationDistanceLevelPropagator = new SimulationDistanceLevelPropagator(ticketManager);
      TaskExecutor<Runnable> taskExecutor = TaskExecutor.of("player ticket throttler", mainThreadExecutor);
      this.scheduler = new ThrottledChunkTaskScheduler(taskExecutor, executor, 4);
      this.mainThreadExecutor = mainThreadExecutor;
   }

   protected abstract boolean isUnloaded(long pos);

   protected abstract @Nullable ChunkHolder getChunkHolder(long pos);

   protected abstract @Nullable ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i);

   public boolean update(ServerChunkLoadingManager chunkLoadingManager) {
      this.distanceFromNearestPlayerTracker.updateLevels();
      this.simulationDistanceLevelPropagator.updateLevels();
      this.nearbyChunkTicketUpdater.updateLevels();
      int i = Integer.MAX_VALUE - this.ticketDistanceLevelPropagator.update(Integer.MAX_VALUE);
      boolean bl = i != 0;
      if (bl && SharedConstants.VERBOSE_SERVER_EVENTS) {
         LOGGER.debug("DMU {}", i);
      }

      if (!this.chunkHoldersWithPendingUpdates.isEmpty()) {
         for (ChunkHolder chunkHolder : this.chunkHoldersWithPendingUpdates) {
            chunkHolder.updateStatus(chunkLoadingManager);
         }

         for (ChunkHolder chunkHolder : this.chunkHoldersWithPendingUpdates) {
            chunkHolder.updateFutures(chunkLoadingManager, this.mainThreadExecutor);
         }

         this.chunkHoldersWithPendingUpdates.clear();
         return true;
      } else {
         if (!this.freshPlayerTicketPositions.isEmpty()) {
            LongIterator longIterator = this.freshPlayerTicketPositions.iterator();

            while (longIterator.hasNext()) {
               long l = longIterator.nextLong();
               if (this.ticketManager.getTickets(l).stream().anyMatch(ticket -> ticket.getType() == ChunkTicketType.PLAYER_LOADING)) {
                  ChunkHolder chunkHolder2 = chunkLoadingManager.getCurrentChunkHolder(l);
                  if (chunkHolder2 == null) {
                     throw new IllegalStateException();
                  }

                  CompletableFuture<OptionalChunk<WorldChunk>> completableFuture = chunkHolder2.getEntityTickingFuture();
                  completableFuture.thenAccept(optionalChunk -> this.mainThreadExecutor.execute(() -> this.scheduler.remove(l, () -> {}, false)));
               }
            }

            this.freshPlayerTicketPositions.clear();
         }

         return bl;
      }
   }

   public void handleChunkEnter(ChunkSectionPos pos, ServerPlayerEntity player) {
      ChunkPos chunkPos = pos.toChunkPos();
      long l = chunkPos.toLong();
      ((ObjectSet)this.playersByChunkPos.computeIfAbsent(l, sectionPos -> new ObjectOpenHashSet())).add(player);
      this.distanceFromNearestPlayerTracker.updateLevel(l, 0, true);
      this.nearbyChunkTicketUpdater.updateLevel(l, 0, true);
      this.ticketManager.addTicket(new ChunkTicket(ChunkTicketType.PLAYER_SIMULATION, this.getPlayerSimulationLevel()), chunkPos);
   }

   public void handleChunkLeave(ChunkSectionPos pos, ServerPlayerEntity player) {
      ChunkPos chunkPos = pos.toChunkPos();
      long l = chunkPos.toLong();
      ObjectSet<ServerPlayerEntity> objectSet = (ObjectSet<ServerPlayerEntity>)this.playersByChunkPos.get(l);
      objectSet.remove(player);
      if (objectSet.isEmpty()) {
         this.playersByChunkPos.remove(l);
         this.distanceFromNearestPlayerTracker.updateLevel(l, Integer.MAX_VALUE, false);
         this.nearbyChunkTicketUpdater.updateLevel(l, Integer.MAX_VALUE, false);
         this.ticketManager.removeTicket(new ChunkTicket(ChunkTicketType.PLAYER_SIMULATION, this.getPlayerSimulationLevel()), chunkPos);
      }
   }

   private int getPlayerSimulationLevel() {
      return Math.max(0, ChunkLevels.getLevelFromType(ChunkLevelType.ENTITY_TICKING) - this.simulationDistance);
   }

   public boolean shouldTickEntities(long chunkPos) {
      return ChunkLevels.shouldTickEntities(this.simulationDistanceLevelPropagator.getLevel(chunkPos));
   }

   public boolean shouldTickBlocks(long chunkPos) {
      return ChunkLevels.shouldTickBlocks(this.simulationDistanceLevelPropagator.getLevel(chunkPos));
   }

   public int getLevel(long pos, boolean forSimulation) {
      return forSimulation ? this.simulationDistanceLevelPropagator.getLevel(pos) : this.ticketDistanceLevelPropagator.getLevel(pos);
   }

   protected void setWatchDistance(int viewDistance) {
      this.nearbyChunkTicketUpdater.setWatchDistance(viewDistance);
   }

   public void setSimulationDistance(int simulationDistance) {
      if (simulationDistance != this.simulationDistance) {
         this.simulationDistance = simulationDistance;
         this.ticketManager.updateLevel(this.getPlayerSimulationLevel(), ChunkTicketType.PLAYER_SIMULATION);
      }
   }

   public int getTickedChunkCount() {
      this.distanceFromNearestPlayerTracker.updateLevels();
      return this.distanceFromNearestPlayerTracker.distanceFromNearestPlayer.size();
   }

   public TriState shouldTick(long chunkPos) {
      this.distanceFromNearestPlayerTracker.updateLevels();
      int i = this.distanceFromNearestPlayerTracker.getLevel(chunkPos);
      if (i <= SpawnHelper.field_56560) {
         return TriState.TRUE;
      } else {
         return i > 8 ? TriState.FALSE : TriState.DEFAULT;
      }
   }

   public void forEachBlockTickingChunk(LongConsumer chunkPosConsumer) {
      ObjectIterator var2 = Long2ByteMaps.fastIterable(this.simulationDistanceLevelPropagator.levels).iterator();

      while (var2.hasNext()) {
         Entry entry = (Entry)var2.next();
         byte b = entry.getByteValue();
         long l = entry.getLongKey();
         if (ChunkLevels.shouldTickEntities(b)) {
            chunkPosConsumer.accept(l);
         }
      }
   }

   public LongIterator iterateChunkPosToTick() {
      this.distanceFromNearestPlayerTracker.updateLevels();
      return this.distanceFromNearestPlayerTracker.distanceFromNearestPlayer.keySet().iterator();
   }

   public String toDumpString() {
      return this.scheduler.toDumpString();
   }

   public boolean shouldDelayShutdown() {
      return this.ticketManager.hasTickets();
   }

   class DistanceFromNearestPlayerTracker extends ChunkPosDistanceLevelPropagator {
      protected final Long2ByteMap distanceFromNearestPlayer = new Long2ByteOpenHashMap();
      protected final int maxDistance;

      protected DistanceFromNearestPlayerTracker(final int maxDistance) {
         super(maxDistance + 2, 16, 256);
         this.maxDistance = maxDistance;
         this.distanceFromNearestPlayer.defaultReturnValue((byte)(maxDistance + 2));
      }

      @Override
      protected int getLevel(long id) {
         return this.distanceFromNearestPlayer.get(id);
      }

      @Override
      protected void setLevel(long id, int level) {
         byte b;
         if (level > this.maxDistance) {
            b = this.distanceFromNearestPlayer.remove(id);
         } else {
            b = this.distanceFromNearestPlayer.put(id, (byte)level);
         }

         this.onDistanceChange(id, b, level);
      }

      protected void onDistanceChange(long pos, int oldDistance, int distance) {
      }

      @Override
      protected int getInitialLevel(long id) {
         return this.isPlayerInChunk(id) ? 0 : Integer.MAX_VALUE;
      }

      private boolean isPlayerInChunk(long chunkPos) {
         ObjectSet<ServerPlayerEntity> objectSet = (ObjectSet<ServerPlayerEntity>)ChunkLevelManager.this.playersByChunkPos.get(chunkPos);
         return objectSet != null && !objectSet.isEmpty();
      }

      public void updateLevels() {
         this.applyPendingUpdates(Integer.MAX_VALUE);
      }
   }

   class NearbyChunkTicketUpdater extends ChunkLevelManager.DistanceFromNearestPlayerTracker {
      private int watchDistance;
      private final Long2IntMap distances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
      private final LongSet positionsAffected = new LongOpenHashSet();

      protected NearbyChunkTicketUpdater(final int i) {
         super(i);
         this.watchDistance = 0;
         this.distances.defaultReturnValue(i + 2);
      }

      @Override
      protected void onDistanceChange(long pos, int oldDistance, int distance) {
         this.positionsAffected.add(pos);
      }

      public void setWatchDistance(int watchDistance) {
         ObjectIterator var2 = this.distanceFromNearestPlayer.long2ByteEntrySet().iterator();

         while (var2.hasNext()) {
            Entry entry = (Entry)var2.next();
            byte b = entry.getByteValue();
            long l = entry.getLongKey();
            this.updateTicket(l, b, this.isWithinViewDistance(b), b <= watchDistance);
         }

         this.watchDistance = watchDistance;
      }

      private void updateTicket(long pos, int distance, boolean oldWithinViewDistance, boolean withinViewDistance) {
         if (oldWithinViewDistance != withinViewDistance) {
            ChunkTicket chunkTicket = new ChunkTicket(ChunkTicketType.PLAYER_LOADING, ChunkLevelManager.NEARBY_PLAYER_TICKET_LEVEL);
            if (withinViewDistance) {
               ChunkLevelManager.this.scheduler.add(() -> ChunkLevelManager.this.mainThreadExecutor.execute(() -> {
                  if (this.isWithinViewDistance(this.getLevel(pos))) {
                     ChunkLevelManager.this.ticketManager.addTicket(pos, chunkTicket);
                     ChunkLevelManager.this.freshPlayerTicketPositions.add(pos);
                  } else {
                     ChunkLevelManager.this.scheduler.remove(pos, () -> {}, false);
                  }
               }), pos, () -> distance);
            } else {
               ChunkLevelManager.this.scheduler
                  .remove(
                     pos,
                     () -> ChunkLevelManager.this.mainThreadExecutor.execute(() -> ChunkLevelManager.this.ticketManager.removeTicket(pos, chunkTicket)),
                     true
                  );
            }
         }
      }

      @Override
      public void updateLevels() {
         super.updateLevels();
         if (!this.positionsAffected.isEmpty()) {
            LongIterator longIterator = this.positionsAffected.iterator();

            while (longIterator.hasNext()) {
               long l = longIterator.nextLong();
               int i = this.distances.get(l);
               int j = this.getLevel(l);
               if (i != j) {
                  ChunkLevelManager.this.scheduler.updateLevel(new ChunkPos(l), () -> this.distances.get(l), j, level -> {
                     if (level >= this.distances.defaultReturnValue()) {
                        this.distances.remove(l);
                     } else {
                        this.distances.put(l, level);
                     }
                  });
                  this.updateTicket(l, j, this.isWithinViewDistance(i), this.isWithinViewDistance(j));
               }
            }

            this.positionsAffected.clear();
         }
      }

      private boolean isWithinViewDistance(int distance) {
         return distance <= this.watchDistance;
      }
   }
}
