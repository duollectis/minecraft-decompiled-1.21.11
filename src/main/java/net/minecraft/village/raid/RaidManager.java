package net.minecraft.village.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

public class RaidManager extends PersistentState {
   private static final String RAIDS = "raids";
   public static final Codec<RaidManager> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            RaidManager.RaidWithId.CODEC
               .listOf()
               .optionalFieldOf("raids", List.of())
               .forGetter(raidManager -> raidManager.raids.int2ObjectEntrySet().stream().map(RaidManager.RaidWithId::fromMapEntry).toList()),
            Codec.INT.fieldOf("next_id").forGetter(raidManager -> raidManager.nextAvailableId),
            Codec.INT.fieldOf("tick").forGetter(raidManager -> raidManager.currentTime)
         )
         .apply(instance, RaidManager::new)
   );
   public static final PersistentStateType<RaidManager> STATE_TYPE = new PersistentStateType<>("raids", RaidManager::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
   public static final PersistentStateType<RaidManager> END_STATE_TYPE = new PersistentStateType<>(
      "raids_end", RaidManager::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS
   );
   private final Int2ObjectMap<Raid> raids = new Int2ObjectOpenHashMap();
   private int nextAvailableId = 1;
   private int currentTime;

   public static PersistentStateType<RaidManager> getPersistentStateType(RegistryEntry<DimensionType> dimensionType) {
      return dimensionType.matchesKey(DimensionTypes.THE_END) ? END_STATE_TYPE : STATE_TYPE;
   }

   public RaidManager() {
      this.markDirty();
   }

   private RaidManager(List<RaidManager.RaidWithId> raids, int nextAvailableId, int currentTime) {
      for (RaidManager.RaidWithId raidWithId : raids) {
         this.raids.put(raidWithId.id, raidWithId.raid);
      }

      this.nextAvailableId = nextAvailableId;
      this.currentTime = currentTime;
   }

   public @Nullable Raid getRaid(int id) {
      return (Raid)this.raids.get(id);
   }

   public OptionalInt getRaidId(Raid raid) {
      ObjectIterator var2 = this.raids.int2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<Raid> entry = (Entry<Raid>)var2.next();
         if (entry.getValue() == raid) {
            return OptionalInt.of(entry.getIntKey());
         }
      }

      return OptionalInt.empty();
   }

   public void tick(ServerWorld world) {
      this.currentTime++;
      Iterator<Raid> iterator = this.raids.values().iterator();

      while (iterator.hasNext()) {
         Raid raid = iterator.next();
         if (!world.getGameRules().getValue(GameRules.DISABLE_RAIDS)) {
            raid.invalidate();
         }

         if (raid.hasStopped()) {
            iterator.remove();
            this.markDirty();
         } else {
            raid.tick(world);
         }
      }

      if (this.currentTime % 200 == 0) {
         this.markDirty();
      }
   }

   public static boolean isValidRaiderFor(RaiderEntity raider) {
      return raider.isAlive() && raider.canJoinRaid() && raider.getDespawnCounter() <= 2400;
   }

   public @Nullable Raid startRaid(ServerPlayerEntity player, BlockPos pos) {
      if (player.isSpectator()) {
         return null;
      } else {
         ServerWorld serverWorld = player.getEntityWorld();
         if (!serverWorld.getGameRules().getValue(GameRules.DISABLE_RAIDS)) {
            return null;
         } else if (!serverWorld.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.CAN_START_RAID_GAMEPLAY, pos)) {
            return null;
         } else {
            List<PointOfInterest> list = serverWorld.getPointOfInterestStorage()
               .getInCircle(poiType -> poiType.isIn(PointOfInterestTypeTags.VILLAGE), pos, 64, PointOfInterestStorage.OccupationStatus.IS_OCCUPIED)
               .toList();
            int i = 0;
            Vec3d vec3d = Vec3d.ZERO;

            for (PointOfInterest pointOfInterest : list) {
               BlockPos blockPos = pointOfInterest.getPos();
               vec3d = vec3d.add(blockPos.getX(), blockPos.getY(), blockPos.getZ());
               i++;
            }

            BlockPos blockPos2;
            if (i > 0) {
               vec3d = vec3d.multiply(1.0 / i);
               blockPos2 = BlockPos.ofFloored(vec3d);
            } else {
               blockPos2 = pos;
            }

            Raid raid = this.getOrCreateRaid(serverWorld, blockPos2);
            if (!raid.hasStarted() && !this.raids.containsValue(raid)) {
               this.raids.put(this.nextId(), raid);
            }

            if (!raid.hasStarted() || raid.getBadOmenLevel() < raid.getMaxAcceptableBadOmenLevel()) {
               raid.start(player);
            }

            this.markDirty();
            return raid;
         }
      }
   }

   private Raid getOrCreateRaid(ServerWorld world, BlockPos pos) {
      Raid raid = world.getRaidAt(pos);
      return raid != null ? raid : new Raid(pos, world.getDifficulty());
   }

   public static RaidManager fromNbt(NbtCompound nbt) {
      return CODEC.parse(NbtOps.INSTANCE, nbt).resultOrPartial().orElseGet(RaidManager::new);
   }

   private int nextId() {
      return ++this.nextAvailableId;
   }

   public @Nullable Raid getRaidAt(BlockPos pos, int searchDistance) {
      Raid raid = null;
      double d = searchDistance;
      ObjectIterator var6 = this.raids.values().iterator();

      while (var6.hasNext()) {
         Raid raid2 = (Raid)var6.next();
         double e = raid2.getCenter().getSquaredDistance(pos);
         if (raid2.isActive() && e < d) {
            raid = raid2;
            d = e;
         }
      }

      return raid;
   }

   @Debug
   public List<BlockPos> getRaidCenters(ChunkPos chunkPos) {
      return this.raids.values().stream().map(Raid::getCenter).filter(chunkPos::contains).toList();
   }

   record RaidWithId(int id, Raid raid) {
      public static final Codec<RaidManager.RaidWithId> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(Codec.INT.fieldOf("id").forGetter(RaidManager.RaidWithId::id), Raid.CODEC.forGetter(RaidManager.RaidWithId::raid))
            .apply(instance, RaidManager.RaidWithId::new)
      );

      public static RaidManager.RaidWithId fromMapEntry(Entry<Raid> entry) {
         return new RaidManager.RaidWithId(entry.getIntKey(), (Raid)entry.getValue());
      }
   }
}
