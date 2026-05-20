package net.minecraft.stat;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.path.PathUtil;
import org.slf4j.Logger;

public class ServerStatHandler extends StatHandler {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Codec<Map<Stat<?>, Integer>> CODEC = Codec.dispatchedMap(Registries.STAT_TYPE.getCodec(), Util.memoize(ServerStatHandler::createCodec))
      .xmap(statsByTypes -> {
         Map<Stat<?>, Integer> map = new HashMap<>();
         statsByTypes.forEach((type, stats) -> map.putAll((Map<? extends Stat<?>, ? extends Integer>)stats));
         return map;
      }, stats -> stats.entrySet().stream().collect(Collectors.groupingBy(entry -> ((Stat)entry.getKey()).getType(), Util.toMap())));
   private final Path path;
   private final Set<Stat<?>> pendingStats = Sets.newHashSet();

   private static <T> Codec<Map<Stat<?>, Integer>> createCodec(StatType<T> statType) {
      Codec<T> codec = statType.getRegistry().getCodec();
      Codec<Stat<?>> codec2 = codec.flatComapMap(
         statType::getOrCreateStat,
         stat -> stat.getType() == statType
            ? DataResult.<T>success((T) stat.getValue())
            : DataResult.error(() -> "Expected type " + statType + ", but got " + stat.getType())
      );
      return Codec.unboundedMap(codec2, Codec.INT);
   }

   public ServerStatHandler(MinecraftServer server, Path path) {
      this.path = path;
      if (Files.isRegularFile(path)) {
         try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement jsonElement = StrictJsonParser.parse(reader);
            this.parse(server.getDataFixer(), jsonElement);
         } catch (IOException var8) {
            LOGGER.error("Couldn't read statistics file {}", path, var8);
         } catch (JsonParseException var9) {
            LOGGER.error("Couldn't parse statistics file {}", path, var9);
         }
      }
   }

   public void save() {
      try {
         PathUtil.createDirectories(this.path.getParent());

         try (Writer writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8)) {
            GSON.toJson(this.asString(), GSON.newJsonWriter(writer));
         }
      } catch (JsonIOException | IOException var6) {
         LOGGER.error("Couldn't save stats to {}", this.path, var6);
      }
   }

   @Override
   public void setStat(PlayerEntity player, Stat<?> stat, int value) {
      super.setStat(player, stat, value);
      this.pendingStats.add(stat);
   }

   private Set<Stat<?>> takePendingStats() {
      Set<Stat<?>> set = Sets.newHashSet(this.pendingStats);
      this.pendingStats.clear();
      return set;
   }

   public void parse(DataFixer dataFixer, JsonElement json) {
      Dynamic<JsonElement> dynamic = new Dynamic(JsonOps.INSTANCE, json);
      dynamic = DataFixTypes.STATS.update(dataFixer, dynamic, NbtHelper.getDataVersion(dynamic, 1343));
      this.statMap
         .putAll(
            CODEC.parse(dynamic.get("stats").orElseEmptyMap())
               .resultOrPartial(error -> LOGGER.error("Failed to parse statistics for {}: {}", this.path, error))
               .orElse(Map.of())
         );
   }

   protected JsonElement asString() {
      JsonObject jsonObject = new JsonObject();
      jsonObject.add("stats", (JsonElement)CODEC.encodeStart(JsonOps.INSTANCE, this.statMap).getOrThrow());
      jsonObject.addProperty("DataVersion", SharedConstants.getGameVersion().dataVersion().id());
      return jsonObject;
   }

   public void updateStatSet() {
      this.pendingStats.addAll(this.statMap.keySet());
   }

   public void sendStats(ServerPlayerEntity player) {
      Object2IntMap<Stat<?>> object2IntMap = new Object2IntOpenHashMap();

      for (Stat<?> stat : this.takePendingStats()) {
         object2IntMap.put(stat, this.getStat(stat));
      }

      player.networkHandler.sendPacket(new StatisticsS2CPacket(object2IntMap));
   }
}
