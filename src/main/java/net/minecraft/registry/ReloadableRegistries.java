package net.minecraft.registry;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.loot.LootDataType;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * Управляет перезагружаемыми реестрами — теми, что могут быть обновлены
 * без перезапуска сервера (лут-таблицы и аналогичные данные).
 * <p>
 * Процесс перезагрузки асинхронный: сначала данные подготавливаются
 * параллельно через {@link #prepare}, затем объединяются в {@link ReloadResult}.
 */
public class ReloadableRegistries {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final RegistryEntryInfo DEFAULT_REGISTRY_ENTRY_INFO =
			new RegistryEntryInfo(Optional.empty(), Lifecycle.experimental());

	/**
	 * Запускает асинхронную перезагрузку всех перезагружаемых реестров.
	 * Параллельно загружает каждый {@link LootDataType}, затем объединяет
	 * результаты в {@link ReloadResult} с обновлёнными тегами.
	 *
	 * @param dynamicRegistries текущее состояние слоёв реестров
	 * @param pendingTagLoads   незавершённые загрузки тегов из предыдущего цикла
	 * @param resourceManager   источник ресурсов для чтения JSON-данных
	 * @param prepareExecutor   исполнитель для фоновых задач подготовки
	 * @return будущий результат перезагрузки
	 */
	public static CompletableFuture<ReloadableRegistries.ReloadResult> reload(
			CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries,
			List<Registry.PendingTagLoad<?>> pendingTagLoads,
			ResourceManager resourceManager,
			Executor prepareExecutor
	) {
		List<RegistryWrapper.Impl<?>> wrappers = TagGroupLoader.collectRegistries(
				dynamicRegistries.getPrecedingRegistryManagers(ServerDynamicRegistryType.RELOADABLE),
				pendingTagLoads
		);
		RegistryWrapper.WrapperLookup wrapperLookup = RegistryWrapper.WrapperLookup.of(wrappers.stream());
		RegistryOps<JsonElement> registryOps = wrapperLookup.getOps(JsonOps.INSTANCE);

		List<CompletableFuture<MutableRegistry<?>>> prepareFutures = LootDataType.stream()
				.map(type -> prepare((LootDataType<?>) type, registryOps, resourceManager, prepareExecutor))
				.toList();

		return Util.combineSafe(prepareFutures)
				.thenApplyAsync(
						registries -> toResult(dynamicRegistries, wrapperLookup, (List<MutableRegistry<?>>) registries),
						prepareExecutor
				);
	}

	/**
	 * Асинхронно загружает один тип лут-данных в новый изолированный реестр.
	 *
	 * @param type            тип лут-данных (лут-таблицы, предикаты и т.д.)
	 * @param ops             операции сериализации с контекстом реестров
	 * @param resourceManager источник ресурсов
	 * @param prepareExecutor исполнитель для фоновой задачи
	 * @return будущий изменяемый реестр с загруженными данными
	 */
	private static <T> CompletableFuture<MutableRegistry<?>> prepare(
			LootDataType<T> type,
			RegistryOps<JsonElement> ops,
			ResourceManager resourceManager,
			Executor prepareExecutor
	) {
		return CompletableFuture.supplyAsync(
				() -> {
					MutableRegistry<T> mutableRegistry = new SimpleRegistry<>(type.registryKey(), Lifecycle.experimental());
					Map<Identifier, T> loaded = new HashMap<>();
					JsonDataLoader.load(resourceManager, type.registryKey(), ops, type.codec(), loaded);
					loaded.forEach((id, value) -> mutableRegistry.add(
							RegistryKey.of(type.registryKey(), id),
							value,
							DEFAULT_REGISTRY_ENTRY_INFO
					));
					TagGroupLoader.loadInitial(resourceManager, mutableRegistry);
					return mutableRegistry;
				},
				prepareExecutor
		);
	}

	private static ReloadableRegistries.ReloadResult toResult(
			CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries,
			RegistryWrapper.WrapperLookup nonReloadables,
			List<MutableRegistry<?>> registries
	) {
		CombinedDynamicRegistries<ServerDynamicRegistryType> updated = with(dynamicRegistries, registries);
		RegistryWrapper.WrapperLookup fullLookup = concat(
				nonReloadables,
				updated.get(ServerDynamicRegistryType.RELOADABLE)
		);
		validate(fullLookup);
		return new ReloadableRegistries.ReloadResult(updated, fullLookup);
	}

	private static RegistryWrapper.WrapperLookup concat(
			RegistryWrapper.WrapperLookup first,
			RegistryWrapper.WrapperLookup second
	) {
		return RegistryWrapper.WrapperLookup.of(Stream.concat(first.stream(), second.stream()));
	}

	private static void validate(RegistryWrapper.WrapperLookup registries) {
		ErrorReporter.Impl reporter = new ErrorReporter.Impl();
		LootTableReporter lootTableReporter = new LootTableReporter(reporter, LootContextTypes.GENERIC, registries);
		LootDataType.stream().forEach(type -> validateLootData(lootTableReporter, (LootDataType<?>) type, registries));
		reporter.apply((id, error) -> LOGGER.warn(
				"Found loot table element validation problem in {}: {}",
				id,
				error.getMessage()
		));
	}

	private static CombinedDynamicRegistries<ServerDynamicRegistryType> with(
			CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistries,
			List<MutableRegistry<?>> registries
	) {
		return dynamicRegistries.with(
				ServerDynamicRegistryType.RELOADABLE,
				new DynamicRegistryManager.ImmutableImpl(registries).toImmutable()
		);
	}

	private static <T> void validateLootData(
			LootTableReporter reporter,
			LootDataType<T> lootDataType,
			RegistryWrapper.WrapperLookup registries
	) {
		RegistryWrapper<T> registryWrapper = registries.getOrThrow(lootDataType.registryKey());
		registryWrapper
				.streamEntries()
				.forEach(entry -> lootDataType.validate(reporter, entry.registryKey(), entry.value()));
	}

	/**
	 * Предоставляет доступ к перезагружаемым реестрам после завершения перезагрузки.
	 * Используется как точка входа для получения лут-таблиц во время выполнения игры.
	 */
	public static class Lookup {

		private final RegistryWrapper.WrapperLookup registries;

		public Lookup(RegistryWrapper.WrapperLookup registries) {
			this.registries = registries;
		}

		public RegistryWrapper.WrapperLookup createRegistryLookup() {
			return registries;
		}

		public LootTable getLootTable(RegistryKey<LootTable> key) {
			return registries
					.getOptional(RegistryKeys.LOOT_TABLE)
					.flatMap(registryEntryLookup -> registryEntryLookup.getOptional(key))
					.map(RegistryEntry::value)
					.orElse(LootTable.EMPTY);
		}
	}

	/**
	 * Результат перезагрузки реестров: обновлённые слои и полный lookup с тегами.
	 *
	 * @param layers                  обновлённые слои {@link CombinedDynamicRegistries}
	 * @param lookupWithUpdatedTags   полный lookup, включающий обновлённые теги
	 */
	public record ReloadResult(
			CombinedDynamicRegistries<ServerDynamicRegistryType> layers,
			RegistryWrapper.WrapperLookup lookupWithUpdatedTags
	) {
	}
}
