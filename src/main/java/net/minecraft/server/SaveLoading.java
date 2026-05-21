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
 * {@code SaveLoading}.
 */
public class SaveLoading {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static <D, R> CompletableFuture<R> load(
			SaveLoading.ServerConfig serverConfig,
			SaveLoading.LoadContextSupplier<D> loadContextSupplier,
			SaveLoading.SaveApplierFactory<D, R> saveApplierFactory,
			Executor prepareExecutor,
			Executor applyExecutor
	) {
		try {
			Pair<DataConfiguration, LifecycledResourceManager> pair = serverConfig.dataPacks.load();
			LifecycledResourceManager lifecycledResourceManager = (LifecycledResourceManager) pair.getSecond();
			CombinedDynamicRegistries<ServerDynamicRegistryType>
					combinedDynamicRegistries =
					ServerDynamicRegistryType.createCombinedDynamicRegistries();
			List<Registry.PendingTagLoad<?>> list = TagGroupLoader.startReload(
					lifecycledResourceManager, combinedDynamicRegistries.get(ServerDynamicRegistryType.STATIC)
			);
			DynamicRegistryManager.Immutable
					immutable =
					combinedDynamicRegistries.getPrecedingRegistryManagers(ServerDynamicRegistryType.WORLDGEN);
			List<RegistryWrapper.Impl<?>> list2 = TagGroupLoader.collectRegistries(immutable, list);
			DynamicRegistryManager.Immutable
					immutable2 =
					RegistryLoader.loadFromResource(
							lifecycledResourceManager,
							list2,
							RegistryLoader.DYNAMIC_REGISTRIES
					);
			List<RegistryWrapper.Impl<?>> list3 = Stream.concat(list2.stream(), immutable2.stream()).toList();
			DynamicRegistryManager.Immutable
					immutable3 =
					RegistryLoader.loadFromResource(
							lifecycledResourceManager,
							list3,
							RegistryLoader.DIMENSION_REGISTRIES
					);
			DataConfiguration dataConfiguration = (DataConfiguration) pair.getFirst();
			RegistryWrapper.WrapperLookup wrapperLookup = RegistryWrapper.WrapperLookup.of(list3.stream());
			SaveLoading.LoadContext<D> loadContext = loadContextSupplier.get(
					new SaveLoading.LoadContextSupplierContext(
							lifecycledResourceManager,
							dataConfiguration,
							wrapperLookup,
							immutable3
					)
			);
			CombinedDynamicRegistries<ServerDynamicRegistryType>
					combinedDynamicRegistries2 =
					combinedDynamicRegistries.with(
							ServerDynamicRegistryType.WORLDGEN, immutable2, loadContext.dimensionsRegistryManager
					);
			return DataPackContents.reload(
					                       lifecycledResourceManager,
					                       combinedDynamicRegistries2,
					                       list,
					                       dataConfiguration.enabledFeatures(),
					                       serverConfig.commandEnvironment(),
					                       serverConfig.functionCompilationPermissions(),
					                       prepareExecutor,
					                       applyExecutor
			                       )
			                       .whenComplete((dataPackContents, throwable) -> {
				                       if (throwable != null) {
					                       lifecycledResourceManager.close();
				                       }
			                       })
			                       .thenApplyAsync(
					                       dataPackContents -> {
						                       dataPackContents.applyPendingTagLoads();
						                       return saveApplierFactory.create(
								                       lifecycledResourceManager,
								                       dataPackContents,
								                       combinedDynamicRegistries2,
								                       loadContext.extraData
						                       );
					                       }, applyExecutor
			                       );
		}
		catch (Exception var18) {
			return CompletableFuture.failedFuture(var18);
		}
	}

	/**
	 * {@code DataPacks}.
	 */
	public record DataPacks(
			ResourcePackManager manager,
			DataConfiguration initialDataConfig,
			boolean safeMode,
			boolean initMode
	) {

		/**
		 * Load.
		 *
		 * @return Pair — результат операции
		 */
		public Pair<DataConfiguration, LifecycledResourceManager> load() {
			DataConfiguration
					dataConfiguration =
					MinecraftServer.loadDataPacks(this.manager, this.initialDataConfig, this.initMode, this.safeMode);
			List<ResourcePack> list = this.manager.createResourcePacks();
			LifecycledResourceManager
					lifecycledResourceManager =
					new LifecycledResourceManagerImpl(ResourceType.SERVER_DATA, list);
			return Pair.of(dataConfiguration, lifecycledResourceManager);
		}
	}

	/**
	 * {@code LoadContext}.
	 */
	public record LoadContext<D>(D extraData, DynamicRegistryManager.Immutable dimensionsRegistryManager) {
	}

	@FunctionalInterface
	/**
	 * {@code LoadContextSupplier}.
	 */
	public interface LoadContextSupplier<D> {

		SaveLoading.LoadContext<D> get(SaveLoading.LoadContextSupplierContext context);
	}

	/**
	 * {@code LoadContextSupplierContext}.
	 */
	public record LoadContextSupplierContext(
			ResourceManager resourceManager,
			DataConfiguration dataConfiguration,
			RegistryWrapper.WrapperLookup worldGenRegistryManager,
			DynamicRegistryManager.Immutable dimensionsRegistryManager
	) {
	}

	@FunctionalInterface
	/**
	 * {@code SaveApplierFactory}.
	 */
	public interface SaveApplierFactory<D, R> {

		R create(
				LifecycledResourceManager resourceManager,
				DataPackContents dataPackContents,
				CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries,
				D loadContext
		);
	}

	/**
	 * {@code ServerConfig}.
	 */
	public record ServerConfig(
			SaveLoading.DataPacks dataPacks,
			CommandManager.RegistrationEnvironment commandEnvironment,
			PermissionPredicate functionCompilationPermissions
	) {
	}
}
