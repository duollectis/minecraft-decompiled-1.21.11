package net.minecraft.client.render.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.BlockModelDefinition;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class BlockStatesLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceFinder FINDER = ResourceFinder.json("blockstates");

   public static CompletableFuture<BlockStatesLoader.LoadedModels> load(ResourceManager resourceManager, Executor prepareExecutor) {
      Function<Identifier, StateManager<Block, BlockState>> function = BlockStateManagers.createIdToManagerMapper();
      return CompletableFuture.<Map<Identifier, List<Resource>>>supplyAsync(() -> FINDER.findAllResources(resourceManager), prepareExecutor)
         .thenCompose(
            resourceMap -> {
               List<CompletableFuture<BlockStatesLoader.LoadedModels>> list = new ArrayList<>(resourceMap.size());

               for (Entry<Identifier, List<Resource>> entry : resourceMap.entrySet()) {
                  list.add(
                     CompletableFuture.supplyAsync(
                        () -> {
                           Identifier identifier = FINDER.toResourceId(entry.getKey());
                           StateManager<Block, BlockState> stateManager = function.apply(identifier);
                           if (stateManager == null) {
                              LOGGER.debug("Discovered unknown block state definition {}, ignoring", identifier);
                              return null;
                           } else {
                              List<Resource> listx = entry.getValue();
                              List<BlockStatesLoader.LoadedBlockStateDefinition> list2 = new ArrayList<>(listx.size());

                              for (Resource resource : listx) {
                                 try (Reader reader = resource.getReader()) {
                                    JsonElement jsonElement = StrictJsonParser.parse(reader);
                                    BlockModelDefinition blockModelDefinition = (BlockModelDefinition)BlockModelDefinition.CODEC
                                       .parse(JsonOps.INSTANCE, jsonElement)
                                       .getOrThrow(JsonParseException::new);
                                    list2.add(new BlockStatesLoader.LoadedBlockStateDefinition(resource.getPackId(), blockModelDefinition));
                                 } catch (Exception var14) {
                                    LOGGER.error("Failed to load blockstate definition {} from pack {}", new Object[]{identifier, resource.getPackId(), var14});
                                 }
                              }

                              try {
                                 return combine(identifier, stateManager, list2);
                              } catch (Exception var11) {
                                 LOGGER.error("Failed to load blockstate definition {}", identifier, var11);
                                 return null;
                              }
                           }
                        },
                        prepareExecutor
                     )
                  );
               }

               return Util.combineSafe(list).thenApply(definitions -> {
                  Map<BlockState, BlockStateModel.UnbakedGrouped> map = new IdentityHashMap<>();

                  for (BlockStatesLoader.LoadedModels loadedModels : definitions) {
                     if (loadedModels != null) {
                        map.putAll(loadedModels.models());
                     }
                  }

                  return new BlockStatesLoader.LoadedModels(map);
               });
            }
         );
   }

   private static BlockStatesLoader.LoadedModels combine(
      Identifier id, StateManager<Block, BlockState> stateManager, List<BlockStatesLoader.LoadedBlockStateDefinition> definitions
   ) {
      Map<BlockState, BlockStateModel.UnbakedGrouped> map = new IdentityHashMap<>();

      for (BlockStatesLoader.LoadedBlockStateDefinition loadedBlockStateDefinition : definitions) {
         map.putAll(loadedBlockStateDefinition.contents.load(stateManager, () -> id + "/" + loadedBlockStateDefinition.source));
      }

      return new BlockStatesLoader.LoadedModels(map);
   }

   @Environment(EnvType.CLIENT)
   record LoadedBlockStateDefinition(String source, BlockModelDefinition contents) {
   }

   @Environment(EnvType.CLIENT)
   public record LoadedModels(Map<BlockState, BlockStateModel.UnbakedGrouped> models) {
   }
}
