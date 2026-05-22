package net.minecraft.registry.tag;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сериализует и десериализует теги реестров для передачи по сети.
 *
 * <p>При синхронизации клиента сервер отправляет теги в виде карты
 * {@code registryKey -> Serialized}, где {@link Serialized} хранит
 * карту {@code tagId -> список raw-идентификаторов элементов}.</p>
 */
public class TagPacketSerializer {

	/**
	 * Сериализует все теги из всех синхронизируемых реестров.
	 *
	 * @param dynamicRegistryManager менеджер динамических реестров сервера
	 * @return карта {@code registryKey -> сериализованные теги} (только непустые)
	 */
	public static Map<RegistryKey<? extends Registry<?>>, Serialized> serializeTags(
			CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistryManager
	) {
		return SerializableRegistries.streamRegistryManagerEntries(dynamicRegistryManager)
				.map(registry -> Pair.of(registry.key(), serializeTags(registry.value())))
				.filter(pair -> !((Serialized) pair.getSecond()).isEmpty())
				.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
	}

	private static <T> Serialized serializeTags(Registry<T> registry) {
		Map<Identifier, IntList> tagMap = new HashMap<>();

		registry.streamTags().forEach(tag -> {
			IntList rawIds = new IntArrayList(tag.size());

			for (RegistryEntry<T> registryEntry : tag) {
				if (registryEntry.getType() != RegistryEntry.Type.REFERENCE) {
					throw new IllegalStateException("Can't serialize unregistered value " + registryEntry);
				}

				rawIds.add(registry.getRawId(registryEntry.value()));
			}

			tagMap.put(tag.getTag().id(), rawIds);
		});

		return new Serialized(tagMap);
	}

	static <T> TagGroupLoader.RegistryTags<T> toRegistryTags(Registry<T> registry, Serialized tags) {
		RegistryKey<? extends Registry<T>> registryKey = registry.getKey();
		Map<TagKey<T>, List<RegistryEntry<T>>> tagMap = new HashMap<>();

		tags.contents.forEach((tagId, rawIds) -> {
			TagKey<T> tagKey = TagKey.of(registryKey, tagId);
			List<RegistryEntry<T>> entries = rawIds.intStream()
					.mapToObj(registry::getEntry)
					.flatMap(Optional::stream)
					.collect(Collectors.toUnmodifiableList());
			tagMap.put(tagKey, entries);
		});

		return new TagGroupLoader.RegistryTags<>(registryKey, tagMap);
	}

	/**
	 * Сериализованное представление тегов одного реестра для передачи по сети.
	 *
	 * <p>Хранит карту {@code tagId -> список raw-идентификаторов (int)} вместо
	 * полных объектов, что значительно уменьшает размер пакета.</p>
	 */
	public static final class Serialized {

		public static final Serialized NONE = new Serialized(Map.of());

		final Map<Identifier, IntList> contents;

		Serialized(Map<Identifier, IntList> contents) {
			this.contents = contents;
		}

		public void writeBuf(PacketByteBuf buf) {
			buf.writeMap(contents, PacketByteBuf::writeIdentifier, PacketByteBuf::writeIntList);
		}

		public static Serialized fromBuf(PacketByteBuf buf) {
			return new Serialized(buf.readMap(
					PacketByteBuf::readIdentifier,
					PacketByteBuf::readIntList
			));
		}

		public boolean isEmpty() {
			return contents.isEmpty();
		}

		public int size() {
			return contents.size();
		}

		public <T> TagGroupLoader.RegistryTags<T> toRegistryTags(Registry<T> registry) {
			return TagPacketSerializer.toRegistryTags(registry, this);
		}
	}
}
