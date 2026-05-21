package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagPacketSerializer;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Накопитель данных реестров, полученных от сервера во время конфигурации.
 * Собирает динамические реестры и теги, затем применяет их одним вызовом
 * {@link #createRegistryManager}.
 */
@Environment(EnvType.CLIENT)
public class ClientRegistries {

	private ClientRegistries.@Nullable DynamicRegistries dynamicRegistries;
	private ClientRegistries.@Nullable Tags tags;

	/**
	 * Добавляет записи динамического реестра, полученные от сервера.
	 *
	 * @param registryRef ключ реестра
	 * @param entries     список сериализованных записей
	 */
	public void putDynamicRegistry(
			RegistryKey<? extends Registry<?>> registryRef,
			List<SerializableRegistries.SerializedRegistryEntry> entries
	) {
		if (dynamicRegistries == null) {
			dynamicRegistries = new ClientRegistries.DynamicRegistries();
		}

		dynamicRegistries.put(registryRef, entries);
	}

	/**
	 * Добавляет теги реестров, полученные от сервера.
	 *
	 * @param tags карта тегов по ключам реестров
	 */
	public void putTags(Map<RegistryKey<? extends Registry<?>>, TagPacketSerializer.Serialized> tags) {
		if (this.tags == null) {
			this.tags = new ClientRegistries.Tags();
		}

		tags.forEach(this.tags::put);
	}

	/**
	 * Создаёт итоговый менеджер реестров, объединяя накопленные данные.
	 * Если динамические реестры присутствуют — загружает их с нуля.
	 * Иначе применяет только теги к переданному менеджеру.
	 *
	 * @param resourceFactory фабрика ресурсов для загрузки данных
	 * @param registryManager базовый менеджер реестров
	 * @param local           {@code true} если соединение локальное (LAN/интегрированный сервер)
	 * @return иммутабельный менеджер реестров
	 */
	public DynamicRegistryManager.Immutable createRegistryManager(
			ResourceFactory resourceFactory,
			DynamicRegistryManager.Immutable registryManager,
			boolean local
	) {
		DynamicRegistryManager result;

		if (dynamicRegistries != null) {
			result = buildFromDynamicRegistries(resourceFactory, dynamicRegistries, local);
		}
		else {
			if (tags != null) {
				loadTags(tags, registryManager, local == false);
			}

			result = registryManager;
		}

		return result.toImmutable();
	}

	private DynamicRegistryManager buildFromDynamicRegistries(
			ResourceFactory resourceFactory,
			ClientRegistries.DynamicRegistries dynRegistries,
			boolean local
	) {
		CombinedDynamicRegistries<ClientDynamicRegistryType> combined =
				ClientDynamicRegistryType.createCombinedDynamicRegistries();
		DynamicRegistryManager.Immutable preceding =
				combined.getPrecedingRegistryManagers(ClientDynamicRegistryType.REMOTE);

		Map<RegistryKey<? extends Registry<?>>, RegistryLoader.ElementsAndTags> elementsMap = new HashMap<>();
		dynRegistries.dynamicRegistries.forEach(
				(registryRef, entries) -> elementsMap.put(
						(RegistryKey<? extends Registry<?>>) registryRef,
						new RegistryLoader.ElementsAndTags(
								(List<SerializableRegistries.SerializedRegistryEntry>) entries,
								TagPacketSerializer.Serialized.NONE
						)
				)
		);

		List<Registry.PendingTagLoad<?>> pendingTagLoads = new ArrayList<>();

		if (tags != null) {
			tags.forEach((registryRef, serialized) -> {
				if (serialized.isEmpty()) {
					return;
				}

				if (SerializableRegistries.isSynced((RegistryKey<? extends Registry<?>>) registryRef)) {
					elementsMap.compute(
							(RegistryKey<? extends Registry<?>>) registryRef,
							(key, value) -> {
								List<SerializableRegistries.SerializedRegistryEntry> existing =
										value != null ? value.elements() : List.of();
								return new RegistryLoader.ElementsAndTags(existing, serialized);
							}
					);
				}
				else if (local == false) {
					pendingTagLoads.add(startTagReload(
							preceding,
							(RegistryKey<? extends Registry<?>>) registryRef,
							serialized
					));
				}
			});
		}

		List<RegistryWrapper.Impl<?>> collected = TagGroupLoader.collectRegistries(preceding, pendingTagLoads);

		DynamicRegistryManager.Immutable loaded;
		try {
			loaded = RegistryLoader.loadFromNetwork(
					elementsMap,
					resourceFactory,
					collected,
					RegistryLoader.SYNCED_REGISTRIES
			).toImmutable();
		}
		catch (Exception e) {
			CrashReport report = CrashReport.create(e, "Network Registry Load");
			addCrashReportSection(report, elementsMap, pendingTagLoads);
			throw new CrashException(report);
		}

		DynamicRegistryManager merged = combined
				.with(ClientDynamicRegistryType.REMOTE, loaded)
				.getCombinedRegistryManager();
		pendingTagLoads.forEach(Registry.PendingTagLoad::apply);
		return merged;
	}

	private void loadTags(
			ClientRegistries.Tags tagsToLoad,
			DynamicRegistryManager.Immutable registryManager,
			boolean local
	) {
		tagsToLoad.forEach((registryRef, serialized) -> {
			if (local || SerializableRegistries.isSynced((RegistryKey<? extends Registry<?>>) registryRef)) {
				startTagReload(registryManager, (RegistryKey<? extends Registry<?>>) registryRef, serialized).apply();
			}
		});
	}

	private static <T> Registry.PendingTagLoad<T> startTagReload(
			DynamicRegistryManager.Immutable registryManager,
			RegistryKey<? extends Registry<? extends T>> registryRef,
			TagPacketSerializer.Serialized serialized
	) {
		Registry<T> registry = registryManager.getOrThrow(registryRef);
		return registry.startTagReload(serialized.toRegistryTags(registry));
	}

	private static void addCrashReportSection(
			CrashReport report,
			Map<RegistryKey<? extends Registry<?>>, RegistryLoader.ElementsAndTags> data,
			List<Registry.PendingTagLoad<?>> pendingTags
	) {
		CrashReportSection section = report.addElement("Received Elements and Tags");
		section.add(
				"Dynamic Registries",
				() -> data.entrySet()
				          .stream()
				          .sorted(Comparator.comparing(entry -> entry.getKey().getValue()))
				          .map(entry -> String.format(
						          Locale.ROOT,
						          "\n\t\t%s: elements=%d tags=%d",
						          entry.getKey().getValue(),
						          entry.getValue().elements().size(),
						          entry.getValue().tags().size()
				          ))
				          .collect(Collectors.joining())
		);
		section.add(
				"Static Registries",
				() -> pendingTags.stream()
				                 .sorted(Comparator.comparing(tag -> tag.getKey().getValue()))
				                 .map(tag -> String.format(
						                 Locale.ROOT,
						                 "\n\t\t%s: tags=%d",
						                 tag.getKey().getValue(),
						                 tag.size()
				                 ))
				                 .collect(Collectors.joining())
		);
	}

	/**
	 * Накопитель динамических реестров, полученных от сервера.
	 */
	@Environment(EnvType.CLIENT)
	static class DynamicRegistries {

		final Map<RegistryKey<? extends Registry<?>>, List<SerializableRegistries.SerializedRegistryEntry>>
				dynamicRegistries =
				new HashMap<>();

		public void put(
				RegistryKey<? extends Registry<?>> registryRef,
				List<SerializableRegistries.SerializedRegistryEntry> entries
		) {
			dynamicRegistries.computeIfAbsent(registryRef, key -> new ArrayList<>()).addAll(entries);
		}
	}

	/**
	 * Накопитель тегов реестров, полученных от сервера.
	 */
	@Environment(EnvType.CLIENT)
	static class Tags {

		private final Map<RegistryKey<? extends Registry<?>>, TagPacketSerializer.Serialized> tags = new HashMap<>();

		public void put(RegistryKey<? extends Registry<?>> registryRef, TagPacketSerializer.Serialized serialized) {
			tags.put(registryRef, serialized);
		}

		public void forEach(BiConsumer<? super RegistryKey<? extends Registry<?>>, ? super TagPacketSerializer.Serialized> consumer) {
			tags.forEach(consumer);
		}
	}
}
