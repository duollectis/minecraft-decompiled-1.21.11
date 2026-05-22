package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.*;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * Утилитный класс для загрузки мира: инициализирует реестры, загружает датапаки
 * и создаёт {@link SaveLoader} с готовыми ресурсами.
 */
public class SaveLoading {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Выполняет полный цикл загрузки сервера: открывает датапаки, загружает реестры,
	 * перезагружает датапак-ресурсы и применяет теги.
	 *
	 * @param serverConfig         конфигурация сервера с датапаками и окружением команд
	 * @param loadContextSupplier  фабрика контекста загрузки (свойства мира, измерения)
	 * @param saveApplierFactory   фабрика финального результата (например, {@link SaveLoader})
	 * @param prepareExecutor      исполнитель для фазы подготовки ресурсов
	 * @param applyExecutor        исполнитель для фазы применения ресурсов
	 */
	public static <D, R> CompletableFuture<R> load(
		SaveLoading.ServerConfig serverConfig,
		SaveLoading.LoadContextSupplier<D> loadContextSupplier,
		SaveLoading.SaveApplierFactory<D, R> saveApplierFactory,
		Executor prepareExecutor,
		Executor applyExecutor
	) {
		try {
			Pair<DataConfiguration, LifecycledResourceManager> loaded = serverConfig.dataPacks.load();
			LifecycledResourceManager resourceManager = loaded.getSecond();
			DataConfiguration dataConfiguration = loaded.getFirst();

			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedRegistries =
				ServerDynamicRegistryType.createCombinedDynamicRegistries();

			List<Registry.PendingTagLoad<?>> pendingTagLoads = TagGroupLoader.startReload(
				resourceManager,
				combinedRegistries.get(ServerDynamicRegistryType.STATIC)
			);

			DynamicRegistryManager.Immutable precedingRegistries =
				combinedRegistries.getPrecedingRegistryManagers(ServerDynamicRegistryType.WORLDGEN);

			List<RegistryWrapper.Impl<?>> staticRegistries =
				TagGroupLoader.collectRegistries(precedingRegistries, pendingTagLoads);

			DynamicRegistryManager.Immutable worldgenRegistries =
				RegistryLoader.loadFromResource(resourceManager, staticRegistries, RegistryLoader.DYNAMIC_REGISTRIES);

			List<RegistryWrapper.Impl<?>> allWorldgenRegistries =
				Stream.concat(staticRegistries.stream(), worldgenRegistries.stream()).toList();

			DynamicRegistryManager.Immutable dimensionRegistries =
				RegistryLoader.loadFromResource(resourceManager, allWorldgenRegistries, RegistryLoader.DIMENSION_REGISTRIES);

			RegistryWrapper.WrapperLookup wrapperLookup =
				RegistryWrapper.WrapperLookup.of(allWorldgenRegistries.stream());

			SaveLoading.LoadContext<D> loadContext = loadContextSupplier.get(
				new SaveLoading.LoadContextSupplierContext(
					resourceManager,
					dataConfiguration,
					wrapperLookup,
					dimensionRegistries
				)
			);

			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedRegistries2 =
				combinedRegistries.with(
					ServerDynamicRegistryType.WORLDGEN,
					worldgenRegistries,
					loadContext.dimensionsRegistryManager
				);

			return DataPackContents
				.reload(
					resourceManager,
					combinedRegistries2,
					pendingTagLoads,
					dataConfiguration.enabledFeatures(),
					serverConfig.commandEnvironment(),
					serverConfig.functionCompilationPermissions(),
					prepareExecutor,
					applyExecutor
				)
				.whenComplete((contents, throwable) -> {
					if (throwable != null) {
						resourceManager.close();
					}
				})
				.thenApplyAsync(
					contents -> {
						contents.applyPendingTagLoads();
						return saveApplierFactory.create(
							resourceManager,
							contents,
							combinedRegistries2,
							loadContext.extraData
						);
					},
					applyExecutor
				);
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	/**
	 * Конфигурация датапаков: менеджер паков, начальная конфигурация данных,
	 * флаги безопасного режима и режима инициализации.
	 */
	public record DataPacks(
		ResourcePackManager manager,
		DataConfiguration initialDataConfig,
		boolean safeMode,
		boolean initMode
	) {

		/**
		 * Открывает датапаки и создаёт {@link LifecycledResourceManager}.
		 * В безопасном режиме загружается только ванильный датапак.
		 */
		public Pair<DataConfiguration, LifecycledResourceManager> load() {
			DataConfiguration dataConfiguration =
				MinecraftServer.loadDataPacks(manager, initialDataConfig, initMode, safeMode);
			List<ResourcePack> packs = manager.createResourcePacks();
			LifecycledResourceManager resourceManager =
				new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, packs);
			return Pair.of(dataConfiguration, resourceManager);
		}
	}

	/** Контекст загрузки мира: дополнительные данные и реестр измерений. */
	public record LoadContext<D>(D extraData, DynamicRegistryManager.Immutable dimensionsRegistryManager) {
	}

	/** Поставщик контекста загрузки на основе открытых ресурсов и реестров. */
	@FunctionalInterface
	public interface LoadContextSupplier<D> {

		SaveLoading.LoadContext<D> get(SaveLoading.LoadContextSupplierContext context);
	}

	/** Контекст, передаваемый в {@link LoadContextSupplier}: ресурсы, конфигурация, реестры. */
	public record LoadContextSupplierContext(
		ResourceManager resourceManager,
		DataConfiguration dataConfiguration,
		RegistryWrapper.WrapperLookup worldGenRegistryManager,
		DynamicRegistryManager.Immutable dimensionsRegistryManager
	) {
	}

	/** Фабрика финального результата загрузки (например, {@link SaveLoader}). */
	@FunctionalInterface
	public interface SaveApplierFactory<D, R> {

		R create(
			LifecycledResourceManager resourceManager,
			DataPackContents dataPackContents,
			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
			D loadContext
		);
	}

	/** Полная конфигурация сервера для загрузки: датапаки, окружение команд, права функций. */
	public record ServerConfig(
		SaveLoading.DataPacks dataPacks,
		CommandManager.RegistrationEnvironment commandEnvironment,
		PermissionPredicate functionCompilationPermissions
	) {
	}
}
