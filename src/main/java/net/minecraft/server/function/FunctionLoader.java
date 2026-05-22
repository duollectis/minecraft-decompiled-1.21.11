package net.minecraft.server.function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Загружает mcfunction-файлы и теги функций из датапаков.
 * Поддерживает асинхронную загрузку через {@link ResourceReloader}.
 */
public class FunctionLoader implements ResourceReloader {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final RegistryKey<Registry<CommandFunction<ServerCommandSource>>> FUNCTION_REGISTRY_KEY =
			RegistryKey.ofRegistry(Identifier.ofVanilla("function"));
	private static final ResourceFinder FINDER =
			new ResourceFinder(RegistryKeys.getPath(FUNCTION_REGISTRY_KEY), ".mcfunction");

	private volatile Map<Identifier, CommandFunction<ServerCommandSource>> functions = ImmutableMap.of();
	private final TagGroupLoader<CommandFunction<ServerCommandSource>> tagLoader = new TagGroupLoader<>(
			(id, required) -> get(id), RegistryKeys.getTagPath(FUNCTION_REGISTRY_KEY)
	);
	private volatile Map<Identifier, List<CommandFunction<ServerCommandSource>>> tags = Map.of();
	private final PermissionPredicate permissions;
	private final CommandDispatcher<ServerCommandSource> commandDispatcher;

	public FunctionLoader(PermissionPredicate permissions, CommandDispatcher<ServerCommandSource> commandDispatcher) {
		this.permissions = permissions;
		this.commandDispatcher = commandDispatcher;
	}

	public Optional<CommandFunction<ServerCommandSource>> get(Identifier id) {
		return Optional.ofNullable(functions.get(id));
	}

	public Map<Identifier, CommandFunction<ServerCommandSource>> getFunctions() {
		return functions;
	}

	public List<CommandFunction<ServerCommandSource>> getTagOrEmpty(Identifier id) {
		return tags.getOrDefault(id, List.of());
	}

	public Iterable<Identifier> getTags() {
		return tags.keySet();
	}

	@Override
	public CompletableFuture<Void> reload(
			ResourceReloader.Store store,
			Executor prepareExecutor,
			ResourceReloader.Synchronizer synchronizer,
			Executor applyExecutor
	) {
		ResourceManager resourceManager = store.getResourceManager();

		CompletableFuture<Map<Identifier, List<TagGroupLoader.TrackedEntry>>> tagsFuture =
				CompletableFuture.supplyAsync(() -> tagLoader.loadTags(resourceManager), prepareExecutor);

		CompletableFuture<Map<Identifier, CompletableFuture<CommandFunction<ServerCommandSource>>>> functionsFuture =
				CompletableFuture.<Map<Identifier, Resource>>supplyAsync(
						() -> FINDER.findResources(resourceManager), prepareExecutor
				)
				.thenCompose(resources -> {
					Map<Identifier, CompletableFuture<CommandFunction<ServerCommandSource>>> map = Maps.newHashMap();
					ServerCommandSource source = CommandManager.createSource(permissions);

					for (Entry<Identifier, Resource> entry : resources.entrySet()) {
						Identifier resourcePath = entry.getKey();
						Identifier functionId = FINDER.toResourceId(resourcePath);
						map.put(
								functionId,
								CompletableFuture.supplyAsync(
										() -> {
											List<String> lines = readLines(entry.getValue());
											return CommandFunction.create(functionId, commandDispatcher, source, lines);
										},
										prepareExecutor
								)
						);
					}

					CompletableFuture<?>[] allFutures = map.values().toArray(new CompletableFuture[0]);
					return CompletableFuture.allOf(allFutures).handle((unused, ex) -> map);
				});

		return tagsFuture
				.thenCombine(functionsFuture, Pair::of)
				.thenCompose(synchronizer::whenPrepared)
				.thenAcceptAsync(
						intermediate -> {
							@SuppressWarnings("unchecked")
							Map<Identifier, CompletableFuture<CommandFunction<ServerCommandSource>>> pendingFunctions =
									(Map<Identifier, CompletableFuture<CommandFunction<ServerCommandSource>>>) intermediate.getSecond();

							Builder<Identifier, CommandFunction<ServerCommandSource>> builder = ImmutableMap.builder();

							pendingFunctions.forEach((id, functionFuture) -> functionFuture
									.handle((function, ex) -> {
										if (ex != null) {
											LOGGER.error("Failed to load function {}", id, ex);
										}
										else {
											builder.put(id, function);
										}

										return null;
									})
									.join());

							functions = builder.build();

							@SuppressWarnings("unchecked")
							Map<Identifier, List<TagGroupLoader.TrackedEntry>> loadedTags =
									(Map<Identifier, List<TagGroupLoader.TrackedEntry>>) intermediate.getFirst();
							tags = tagLoader.buildGroup(loadedTags);
						},
						applyExecutor
				);
	}

	private static List<String> readLines(Resource resource) {
		try (BufferedReader reader = resource.getReader()) {
			return reader.lines().toList();
		}
		catch (IOException exception) {
			throw new CompletionException(exception);
		}
	}
}
