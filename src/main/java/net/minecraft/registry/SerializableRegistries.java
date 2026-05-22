package net.minecraft.registry;

import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Утилитарный класс для сериализации динамических реестров при синхронизации с клиентом.
 * Определяет, какие реестры подлежат синхронизации, и предоставляет методы
 * для их сериализации в NBT-формат.
 */
public class SerializableRegistries {

	private static final Set<RegistryKey<? extends Registry<?>>> SYNCED_REGISTRIES = RegistryLoader.SYNCED_REGISTRIES
			.stream()
			.map(RegistryLoader.Entry::key)
			.collect(Collectors.toUnmodifiableSet());

	/**
	 * Итерирует по всем синхронизируемым реестрам и вызывает callback для каждого.
	 * Элементы, чей пакет уже известен клиенту (есть в {@code knownPacks}),
	 * передаются без данных (только идентификатор).
	 *
	 * @param nbtOps         операции для NBT-сериализации
	 * @param registryManager менеджер реестров сервера
	 * @param knownPacks     набор пакетов, известных клиенту
	 * @param callback       обработчик для каждого реестра и его сериализованных записей
	 */
	public static void forEachSyncedRegistry(
			DynamicOps<NbtElement> nbtOps,
			DynamicRegistryManager registryManager,
			Set<VersionedIdentifier> knownPacks,
			BiConsumer<RegistryKey<? extends Registry<?>>, List<SerializedRegistryEntry>> callback
	) {
		RegistryLoader.SYNCED_REGISTRIES.forEach(entry -> serialize(
				nbtOps,
				(RegistryLoader.Entry<?>) entry,
				registryManager,
				knownPacks,
				callback
		));
	}

	@SuppressWarnings("unchecked")
	private static <T> void serialize(
			DynamicOps<NbtElement> nbtOps,
			RegistryLoader.Entry<T> entry,
			DynamicRegistryManager registryManager,
			Set<VersionedIdentifier> knownPacks,
			BiConsumer<RegistryKey<? extends Registry<?>>, List<SerializedRegistryEntry>> callback
	) {
		registryManager.getOptional(entry.key()).ifPresent(registry -> {
			List<SerializedRegistryEntry> serializedEntries = new ArrayList<>(registry.size());

			registry.streamEntries().forEach(registryEntry -> {
				boolean isKnownPack = registry.getEntryInfo(registryEntry.registryKey())
						.flatMap(RegistryEntryInfo::knownPackInfo)
						.filter(knownPacks::contains)
						.isPresent();

				Optional<NbtElement> data;

				if (isKnownPack) {
					data = Optional.empty();
				} else {
					NbtElement nbtElement = (NbtElement) entry.elementCodec()
							.encodeStart(nbtOps, registryEntry.value())
							.getOrThrow(error -> new IllegalArgumentException(
									"Failed to serialize " + registryEntry.registryKey() + ": " + error
							));
					data = Optional.of(nbtElement);
				}

				serializedEntries.add(new SerializedRegistryEntry(
						registryEntry.registryKey().getValue(),
						data
				));
			});

			callback.accept(registry.getKey(), serializedEntries);
		});
	}

	private static Stream<DynamicRegistryManager.Entry<?>> stream(DynamicRegistryManager dynamicRegistryManager) {
		return dynamicRegistryManager.streamAllRegistries().filter(registry -> isSynced(registry.key()));
	}

	/**
	 * Возвращает поток динамических записей реестра, подлежащих синхронизации.
	 * Включает реестры начиная со слоя WORLDGEN и выше.
	 *
	 * @param combinedRegistries многоуровневый менеджер реестров сервера
	 * @return поток синхронизируемых записей динамических реестров
	 */
	public static Stream<DynamicRegistryManager.Entry<?>> streamDynamicEntries(
			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedRegistries
	) {
		return stream(combinedRegistries.getSucceedingRegistryManagers(ServerDynamicRegistryType.WORLDGEN));
	}

	/**
	 * Возвращает поток всех записей реестра для синхронизации с клиентом:
	 * статические реестры плюс динамические начиная со слоя WORLDGEN.
	 *
	 * @param combinedRegistries многоуровневый менеджер реестров сервера
	 * @return объединённый поток всех синхронизируемых записей
	 */
	public static Stream<DynamicRegistryManager.Entry<?>> streamRegistryManagerEntries(
			CombinedDynamicRegistries<ServerDynamicRegistryType> combinedRegistries
	) {
		Stream<DynamicRegistryManager.Entry<?>> staticEntries = combinedRegistries
				.get(ServerDynamicRegistryType.STATIC)
				.streamAllRegistries();
		Stream<DynamicRegistryManager.Entry<?>> dynamicEntries = streamDynamicEntries(combinedRegistries);
		return Stream.concat(dynamicEntries, staticEntries);
	}

	/**
	 * Проверяет, подлежит ли реестр с данным ключом синхронизации с клиентом.
	 *
	 * @param key ключ реестра
	 * @return {@code true} если реестр синхронизируется
	 */
	public static boolean isSynced(RegistryKey<? extends Registry<?>> key) {
		return SYNCED_REGISTRIES.contains(key);
	}

	/**
	 * Сериализованная запись реестра для передачи по сети.
	 * Если {@code data} пуст — клиент должен загрузить данные из локального пакета.
	 *
	 * @param id   идентификатор записи реестра
	 * @param data NBT-данные записи, или пусто если клиент уже имеет эти данные
	 */
	public record SerializedRegistryEntry(Identifier id, Optional<NbtElement> data) {

		public static final PacketCodec<ByteBuf, SerializedRegistryEntry> PACKET_CODEC = PacketCodec.tuple(
				Identifier.PACKET_CODEC,
				SerializedRegistryEntry::id,
				PacketCodecs.NBT_ELEMENT.collect(PacketCodecs::optional),
				SerializedRegistryEntry::data,
				SerializedRegistryEntry::new
		);
	}
}
