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
 * {@code TagPacketSerializer}.
 */
public class TagPacketSerializer {

	public static Map<RegistryKey<? extends Registry<?>>, TagPacketSerializer.Serialized> serializeTags(
			CombinedDynamicRegistries<ServerDynamicRegistryType> dynamicRegistryManager
	) {
		return SerializableRegistries.streamRegistryManagerEntries(dynamicRegistryManager)
		                             .map(registry -> Pair.of(registry.key(), serializeTags(registry.value())))
		                             .filter(pair -> !((TagPacketSerializer.Serialized) pair.getSecond()).isEmpty())
		                             .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
	}

	private static <T> TagPacketSerializer.Serialized serializeTags(Registry<T> registry) {
		Map<Identifier, IntList> map = new HashMap<>();
		registry.streamTags().forEach(tag -> {
			IntList intList = new IntArrayList(tag.size());

			for (RegistryEntry<T> registryEntry : tag) {
				if (registryEntry.getType() != RegistryEntry.Type.REFERENCE) {
					throw new IllegalStateException("Can't serialize unregistered value " + registryEntry);
				}

				intList.add(registry.getRawId(registryEntry.value()));
			}

			map.put(tag.getTag().id(), intList);
		});
		return new TagPacketSerializer.Serialized(map);
	}

	static <T> TagGroupLoader.RegistryTags<T> toRegistryTags(
			Registry<T> registry,
			TagPacketSerializer.Serialized tags
	) {
		RegistryKey<? extends Registry<T>> registryKey = registry.getKey();
		Map<TagKey<T>, List<RegistryEntry<T>>> map = new HashMap<>();
		tags.contents.forEach((tagId, rawIds) -> {
			TagKey<T> tagKey = TagKey.of(registryKey, tagId);
			List<RegistryEntry<T>>
					list =
					rawIds
							.intStream()
							.mapToObj(registry::getEntry)
							.flatMap(Optional::stream)
							.collect(Collectors.toUnmodifiableList());
			map.put(tagKey, list);
		});
		return new TagGroupLoader.RegistryTags<>(registryKey, map);
	}

	/**
	 * {@code Serialized}.
	 */
	public static final class Serialized {

		public static final TagPacketSerializer.Serialized NONE = new TagPacketSerializer.Serialized(Map.of());
		final Map<Identifier, IntList> contents;

		Serialized(Map<Identifier, IntList> contents) {
			this.contents = contents;
		}

		/**
		 * Записывает buf.
		 *
		 * @param buf buf
		 */
		public void writeBuf(PacketByteBuf buf) {
			buf.writeMap(this.contents, PacketByteBuf::writeIdentifier, PacketByteBuf::writeIntList);
		}

		public static TagPacketSerializer.Serialized fromBuf(PacketByteBuf buf) {
			return new TagPacketSerializer.Serialized(buf.readMap(
					PacketByteBuf::readIdentifier,
					PacketByteBuf::readIntList
			));
		}

		public boolean isEmpty() {
			return this.contents.isEmpty();
		}

		/**
		 * Size.
		 *
		 * @return int — результат операции
		 */
		public int size() {
			return this.contents.size();
		}

		public <T> TagGroupLoader.RegistryTags<T> toRegistryTags(Registry<T> registry) {
			return TagPacketSerializer.toRegistryTags(registry, this);
		}
	}
}
