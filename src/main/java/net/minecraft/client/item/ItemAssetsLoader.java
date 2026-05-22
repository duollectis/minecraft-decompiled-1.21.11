package net.minecraft.client.item;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
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

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Асинхронно загружает все клиентские ассеты предметов из директории {@code items/}
 * ресурс-пака. Каждый JSON-файл парсится в {@link ItemAsset} с учётом динамических
 * реестров. Ошибки парсинга логируются, но не прерывают загрузку остальных ассетов.
 */
@Environment(EnvType.CLIENT)
public class ItemAssetsLoader {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ResourceFinder FINDER = ResourceFinder.json("items");

	/**
	 * Запускает асинхронную загрузку всех ассетов предметов.
	 *
	 * @param resourceManager менеджер ресурсов для поиска JSON-файлов
	 * @param executor исполнитель для параллельного парсинга
	 * @return future с результатом, содержащим карту id → ассет
	 */
	public static CompletableFuture<ItemAssetsLoader.Result> load(ResourceManager resourceManager, Executor executor) {
		DynamicRegistryManager.Immutable registryManager =
				ClientDynamicRegistryType.createCombinedDynamicRegistries().getCombinedRegistryManager();

		return CompletableFuture
				.<Map<Identifier, Resource>>supplyAsync(() -> FINDER.findResources(resourceManager), executor)
				.thenCompose(
						itemResources -> {
							List<CompletableFuture<ItemAssetsLoader.Definition>> futures =
									new ArrayList<>(itemResources.size());

							itemResources.forEach(
									(resourcePath, resource) -> futures.add(
											CompletableFuture.supplyAsync(
													() -> parseDefinition(resourcePath, resource, registryManager),
													executor
											)
									)
							);

							return Util.combineSafe(futures).thenApply(definitions -> {
								Map<Identifier, ItemAsset> assets = new HashMap<>();

								for (ItemAssetsLoader.Definition definition : definitions) {
									if (definition.clientItemInfo != null) {
										assets.put(definition.id, definition.clientItemInfo);
									}
								}

								return new ItemAssetsLoader.Result(assets);
							});
						}
				);
	}

	private static ItemAssetsLoader.Definition parseDefinition(
			Identifier resourcePath,
			Resource resource,
			DynamicRegistryManager.Immutable registryManager
	) {
		Identifier itemId = FINDER.toResourceId(resourcePath);

		try {
			try (Reader reader = resource.getReader()) {
				ContextSwappableRegistryLookup registryLookup = new ContextSwappableRegistryLookup(registryManager);
				DynamicOps<JsonElement> ops = registryLookup.createRegistryOps(JsonOps.INSTANCE);

				ItemAsset asset = ItemAsset.CODEC
						.parse(ops, StrictJsonParser.parse(reader))
						.ifError(
								error -> LOGGER.error(
										"Couldn't parse item model '{}' from pack '{}': {}",
										itemId,
										resource.getPackId(),
										error.message()
								)
						)
						.result()
						.map(
								parsed -> registryLookup.hasEntries()
										? parsed.withContextSwapper(registryLookup.createContextSwapper())
										: parsed
						)
						.orElse(null);

				return new ItemAssetsLoader.Definition(itemId, asset);
			}
		} catch (Exception exception) {
			LOGGER.error(
					"Failed to open item model {} from pack '{}'",
					resourcePath,
					resource.getPackId(),
					exception
			);
			return new ItemAssetsLoader.Definition(itemId, null);
		}
	}

	@Environment(EnvType.CLIENT)
	record Definition(Identifier id, @Nullable ItemAsset clientItemInfo) {
	}

	@Environment(EnvType.CLIENT)
	public record Result(Map<Identifier, ItemAsset> contents) {
	}
}
