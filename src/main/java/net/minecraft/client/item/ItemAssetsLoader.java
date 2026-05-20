package net.minecraft.client.item;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientDynamicRegistryType;
import net.minecraft.registry.ContextSwappableRegistryLookup;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ItemAssetsLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceFinder FINDER = ResourceFinder.json("items");

   public static CompletableFuture<ItemAssetsLoader.Result> load(ResourceManager resourceManager, Executor executor) {
      DynamicRegistryManager.Immutable immutable = ClientDynamicRegistryType.createCombinedDynamicRegistries().getCombinedRegistryManager();
      return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> FINDER.findResources(resourceManager), executor)
         .thenCompose(
            itemAssets -> {
               List<CompletableFuture<ItemAssetsLoader.Definition>> list = new ArrayList<>(itemAssets.size());
               itemAssets.forEach(
                  (itemId, itemResource) -> list.add(
                     CompletableFuture.supplyAsync(
                        () -> {
                           Identifier identifier2 = FINDER.toResourceId(itemId);

                           try {
                              ItemAssetsLoader.Definition var8;
                              try (Reader reader = itemResource.getReader()) {
                                 ContextSwappableRegistryLookup contextSwappableRegistryLookup = new ContextSwappableRegistryLookup(immutable);
                                 DynamicOps<JsonElement> dynamicOps = contextSwappableRegistryLookup.createRegistryOps(JsonOps.INSTANCE);
                                 ItemAsset itemAsset = ItemAsset.CODEC
                                    .parse(dynamicOps, StrictJsonParser.parse(reader))
                                    .ifError(
                                       error -> LOGGER.error(
                                          "Couldn't parse item model '{}' from pack '{}': {}",
                                          new Object[]{identifier2, itemResource.getPackId(), error.message()}
                                       )
                                    )
                                    .result()
                                    .map(
                                       itemAssetx -> contextSwappableRegistryLookup.hasEntries()
                                          ? itemAssetx.withContextSwapper(contextSwappableRegistryLookup.createContextSwapper())
                                          : itemAssetx
                                    )
                                    .orElse(null);
                                 var8 = new ItemAssetsLoader.Definition(identifier2, itemAsset);
                              }

                              return var8;
                           } catch (Exception var11) {
                              LOGGER.error("Failed to open item model {} from pack '{}'", new Object[]{itemId, itemResource.getPackId(), var11});
                              return new ItemAssetsLoader.Definition(identifier2, null);
                           }
                        },
                        executor
                     )
                  )
               );
               return Util.combineSafe(list).thenApply(definitions -> {
                  Map<Identifier, ItemAsset> map = new HashMap<>();

                  for (ItemAssetsLoader.Definition definition : definitions) {
                     if (definition.clientItemInfo != null) {
                        map.put(definition.id, definition.clientItemInfo);
                     }
                  }

                  return new ItemAssetsLoader.Result(map);
               });
            }
         );
   }

   @Environment(EnvType.CLIENT)
   record Definition(Identifier id, @Nullable ItemAsset clientItemInfo) {
   }

   @Environment(EnvType.CLIENT)
   public record Result(Map<Identifier, ItemAsset> contents) {
   }
}
