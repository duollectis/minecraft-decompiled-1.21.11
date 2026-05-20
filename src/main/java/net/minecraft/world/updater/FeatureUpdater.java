package net.minecraft.world.updater;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkUpdateState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public class FeatureUpdater implements ChunkUpdater {
   public static final int TARGET_DATA_VERSION = 1493;
   private static final Map<String, String> OLD_TO_NEW = Util.make(Maps.newHashMap(), map -> {
      map.put("Village", "Village");
      map.put("Mineshaft", "Mineshaft");
      map.put("Mansion", "Mansion");
      map.put("Igloo", "Temple");
      map.put("Desert_Pyramid", "Temple");
      map.put("Jungle_Pyramid", "Temple");
      map.put("Swamp_Hut", "Temple");
      map.put("Stronghold", "Stronghold");
      map.put("Monument", "Monument");
      map.put("Fortress", "Fortress");
      map.put("EndCity", "EndCity");
   });
   private static final Map<String, String> ANCIENT_TO_OLD = Util.make(Maps.newHashMap(), map -> {
      map.put("Iglu", "Igloo");
      map.put("TeDP", "Desert_Pyramid");
      map.put("TeJP", "Jungle_Pyramid");
      map.put("TeSH", "Swamp_Hut");
   });
   private static final Set<String> NEW_STRUCTURE_NAMES = Set.of(
      "pillager_outpost",
      "mineshaft",
      "mansion",
      "jungle_pyramid",
      "desert_pyramid",
      "igloo",
      "ruined_portal",
      "shipwreck",
      "swamp_hut",
      "stronghold",
      "monument",
      "ocean_ruin",
      "fortress",
      "endcity",
      "buried_treasure",
      "village",
      "nether_fossil",
      "bastion_remnant"
   );
   private final boolean needsUpdate;
   private final Map<String, Long2ObjectMap<NbtCompound>> featureIdToChunkNbt = Maps.newHashMap();
   private final Map<String, ChunkUpdateState> updateStates = Maps.newHashMap();
   private final @Nullable PersistentStateManager persistentStateManager;
   private final List<String> oldNames;
   private final List<String> newNames;
   private final DataFixer dataFixer;
   private boolean initialized;

   public FeatureUpdater(@Nullable PersistentStateManager persistentStateManager, List<String> oldNames, List<String> newNames, DataFixer dataFixer) {
      this.persistentStateManager = persistentStateManager;
      this.oldNames = oldNames;
      this.newNames = newNames;
      this.dataFixer = dataFixer;
      boolean bl = false;

      for (String string : this.newNames) {
         bl |= this.featureIdToChunkNbt.get(string) != null;
      }

      this.needsUpdate = bl;
   }

   @Override
   public void markChunkDone(ChunkPos chunkPos) {
      long l = chunkPos.toLong();

      for (String string : this.oldNames) {
         ChunkUpdateState chunkUpdateState = this.updateStates.get(string);
         if (chunkUpdateState != null && chunkUpdateState.isRemaining(l)) {
            chunkUpdateState.markResolved(l);
         }
      }
   }

   @Override
   public int targetDataVersion() {
      return 1493;
   }

   @Override
   public NbtCompound applyFix(NbtCompound nbtCompound) {
      if (!this.initialized && this.persistentStateManager != null) {
         this.init(this.persistentStateManager);
      }

      int i = NbtHelper.getDataVersion(nbtCompound);
      if (i < 1493) {
         nbtCompound = DataFixTypes.CHUNK.update(this.dataFixer, nbtCompound, i, 1493);
         if (nbtCompound.getCompound("Level").flatMap(levelTag -> levelTag.getBoolean("hasLegacyStructureData")).orElse(false)) {
            nbtCompound = this.getUpdatedReferences(nbtCompound);
         }
      }

      return nbtCompound;
   }

   private NbtCompound getUpdatedReferences(NbtCompound nbt) {
      NbtCompound nbtCompound = nbt.getCompoundOrEmpty("Level");
      ChunkPos chunkPos = new ChunkPos(nbtCompound.getInt("xPos", 0), nbtCompound.getInt("zPos", 0));
      if (this.needsUpdate(chunkPos.x, chunkPos.z)) {
         nbt = this.getUpdatedStarts(nbt, chunkPos);
      }

      NbtCompound nbtCompound2 = nbtCompound.getCompoundOrEmpty("Structures");
      NbtCompound nbtCompound3 = nbtCompound2.getCompoundOrEmpty("References");

      for (String string : this.newNames) {
         boolean bl = NEW_STRUCTURE_NAMES.contains(string.toLowerCase(Locale.ROOT));
         if (!nbtCompound3.getLongArray(string).isPresent() && bl) {
            int i = 8;
            LongList longList = new LongArrayList();

            for (int j = chunkPos.x - 8; j <= chunkPos.x + 8; j++) {
               for (int k = chunkPos.z - 8; k <= chunkPos.z + 8; k++) {
                  if (this.needsUpdate(j, k, string)) {
                     longList.add(ChunkPos.toLong(j, k));
                  }
               }
            }

            nbtCompound3.putLongArray(string, longList.toLongArray());
         }
      }

      nbtCompound2.put("References", nbtCompound3);
      nbtCompound.put("Structures", nbtCompound2);
      nbt.put("Level", nbtCompound);
      return nbt;
   }

   private boolean needsUpdate(int chunkX, int chunkZ, String id) {
      return !this.needsUpdate
         ? false
         : this.featureIdToChunkNbt.get(id) != null && this.updateStates.get(OLD_TO_NEW.get(id)).contains(ChunkPos.toLong(chunkX, chunkZ));
   }

   private boolean needsUpdate(int chunkX, int chunkZ) {
      if (!this.needsUpdate) {
         return false;
      } else {
         for (String string : this.newNames) {
            if (this.featureIdToChunkNbt.get(string) != null && this.updateStates.get(OLD_TO_NEW.get(string)).isRemaining(ChunkPos.toLong(chunkX, chunkZ))) {
               return true;
            }
         }

         return false;
      }
   }

   private NbtCompound getUpdatedStarts(NbtCompound nbt, ChunkPos pos) {
      NbtCompound nbtCompound = nbt.getCompoundOrEmpty("Level");
      NbtCompound nbtCompound2 = nbtCompound.getCompoundOrEmpty("Structures");
      NbtCompound nbtCompound3 = nbtCompound2.getCompoundOrEmpty("Starts");

      for (String string : this.newNames) {
         Long2ObjectMap<NbtCompound> long2ObjectMap = this.featureIdToChunkNbt.get(string);
         if (long2ObjectMap != null) {
            long l = pos.toLong();
            if (this.updateStates.get(OLD_TO_NEW.get(string)).isRemaining(l)) {
               NbtCompound nbtCompound4 = (NbtCompound)long2ObjectMap.get(l);
               if (nbtCompound4 != null) {
                  nbtCompound3.put(string, nbtCompound4);
               }
            }
         }
      }

      nbtCompound2.put("Starts", nbtCompound3);
      nbtCompound.put("Structures", nbtCompound2);
      nbt.put("Level", nbtCompound);
      return nbt;
   }

   private synchronized void init(PersistentStateManager persistentStateManager) {
      if (!this.initialized) {
         for (String string : this.oldNames) {
            NbtCompound nbtCompound = new NbtCompound();

            try {
               nbtCompound = persistentStateManager.readNbt(string, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES, 1493)
                  .getCompoundOrEmpty("data")
                  .getCompoundOrEmpty("Features");
               if (nbtCompound.isEmpty()) {
                  continue;
               }
            } catch (IOException var8) {
            }

            nbtCompound.forEach(
               (key, nbt) -> {
                  if (nbt instanceof NbtCompound nbtCompoundx) {
                     long l = ChunkPos.toLong(nbtCompoundx.getInt("ChunkX", 0), nbtCompoundx.getInt("ChunkZ", 0));
                     NbtList nbtList = nbtCompoundx.getListOrEmpty("Children");
                     if (!nbtList.isEmpty()) {
                        Optional<String> optional = nbtList.getCompound(0).flatMap(child -> child.getString("id"));
                        optional.map(ANCIENT_TO_OLD::get).ifPresent(id -> nbtCompoundx.putString("id", id));
                     }

                     nbtCompoundx.getString("id")
                        .ifPresent(id -> this.featureIdToChunkNbt.computeIfAbsent(id, featureId -> new Long2ObjectOpenHashMap()).put(l, nbtCompoundx));
                  }
               }
            );
            String string2 = string + "_index";
            ChunkUpdateState chunkUpdateState = persistentStateManager.getOrCreate(ChunkUpdateState.createStateType(string2));
            if (chunkUpdateState.getAll().isEmpty()) {
               ChunkUpdateState chunkUpdateState2 = new ChunkUpdateState();
               this.updateStates.put(string, chunkUpdateState2);
               nbtCompound.forEach((key, nbt) -> {
                  if (nbt instanceof NbtCompound nbtCompoundx) {
                     chunkUpdateState2.add(ChunkPos.toLong(nbtCompoundx.getInt("ChunkX", 0), nbtCompoundx.getInt("ChunkZ", 0)));
                  }
               });
            } else {
               this.updateStates.put(string, chunkUpdateState);
            }
         }

         this.initialized = true;
      }
   }

   public static Supplier<ChunkUpdater> create(
      RegistryKey<World> world, Supplier<@Nullable PersistentStateManager> persistentStateManagerSupplier, DataFixer dataFixer
   ) {
      if (world == World.OVERWORLD) {
         return () -> new FeatureUpdater(
            persistentStateManagerSupplier.get(),
            ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"),
            ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"),
            dataFixer
         );
      } else if (world == World.NETHER) {
         List<String> list = ImmutableList.of("Fortress");
         return () -> new FeatureUpdater(persistentStateManagerSupplier.get(), list, list, dataFixer);
      } else if (world == World.END) {
         List<String> list = ImmutableList.of("EndCity");
         return () -> new FeatureUpdater(persistentStateManagerSupplier.get(), list, list, dataFixer);
      } else {
         return ChunkUpdater.PASSTHROUGH_FACTORY;
      }
   }
}
