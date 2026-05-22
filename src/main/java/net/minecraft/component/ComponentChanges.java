package net.minecraft.component;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
	 * Хранит набор изменений компонентов предмета: добавленных (с новым значением) и удалённых.
	 * Используется для передачи дельты состояния компонентов между клиентом и сервером,
	 * а также при применении изменений к {@link MergedComponentMap}.
	 */
public final class ComponentChanges {

	/**
		 * Максимальный размер карты изменений при декодировании пакета.
		 * Ограничение защищает от атак с переполнением памяти.
		 */
	private static final int MAX_PACKET_MAP_SIZE = 65536;

	private static final String REMOVE_PREFIX = "!";

	public static final ComponentChanges EMPTY = new ComponentChanges(Reference2ObjectMaps.emptyMap());

	public static final Codec<ComponentChanges> CODEC = Codec.dispatchedMap(
			ComponentChanges.Type.CODEC,
			ComponentChanges.Type::getValueCodec
	).xmap(
			changes -> {
				if (changes.isEmpty()) {
					return EMPTY;
				}

				Reference2ObjectMap<ComponentType<?>, Optional<?>> result = new Reference2ObjectArrayMap<>(changes.size());

				for (Entry<ComponentChanges.Type, ?> entry : changes.entrySet()) {
					ComponentChanges.Type type = entry.getKey();
					if (type.removed()) {
						result.put(type.type(), Optional.empty());
					} else {
						result.put(type.type(), Optional.of(entry.getValue()));
					}
				}

				return new ComponentChanges(result);
			},
			changes -> {
				@SuppressWarnings("unchecked")
				Reference2ObjectMap<ComponentChanges.Type, Object> result = new Reference2ObjectArrayMap<>(
						changes.changedComponents.size()
				);

				for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changes.changedComponents)) {
					ComponentType<?> componentType = entry.getKey();
					if (componentType.shouldSkipSerialization()) {
						continue;
					}

					Optional<?> optional = entry.getValue();
					if (optional.isPresent()) {
						result.put(new ComponentChanges.Type(componentType, false), optional.get());
					} else {
						result.put(new ComponentChanges.Type(componentType, true), Unit.INSTANCE);
					}
				}

				return (java.util.Map) result;
			}
	);

	public static final PacketCodec<RegistryByteBuf, ComponentChanges> PACKET_CODEC = createPacketCodec(
			new ComponentChanges.PacketCodecFunction() {
				@Override
				public <T> PacketCodec<RegistryByteBuf, T> apply(ComponentType<T> componentType) {
					return componentType.getPacketCodec().cast();
				}
			}
	);

	public static final PacketCodec<RegistryByteBuf, ComponentChanges> LENGTH_PREPENDED_PACKET_CODEC = createPacketCodec(
			new ComponentChanges.PacketCodecFunction() {
				@Override
				public <T> PacketCodec<RegistryByteBuf, T> apply(ComponentType<T> componentType) {
					PacketCodec<RegistryByteBuf, T> packetCodec = componentType.getPacketCodec().cast();
					return packetCodec.collect(PacketCodecs.lengthPrependedRegistry(Integer.MAX_VALUE));
				}
			}
	);

	final Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents;

	/**
		 * Создаёт {@link PacketCodec} для сериализации изменений компонентов по сети.
		 * Формат пакета: [кол-во добавленных] [кол-во удалённых] [добавленные...] [удалённые...]
		 *
		 * @param packetCodecFunction функция, возвращающая кодек для конкретного типа компонента
		 * @return готовый сетевой кодек
		 */
	private static PacketCodec<RegistryByteBuf, ComponentChanges> createPacketCodec(
			ComponentChanges.PacketCodecFunction packetCodecFunction
	) {
		return new PacketCodec<>() {
			@Override
			public ComponentChanges decode(RegistryByteBuf buf) {
				int addedCount = buf.readVarInt();
				int removedCount = buf.readVarInt();

				if (addedCount == 0 && removedCount == 0) {
					return ComponentChanges.EMPTY;
				}

				int totalCount = addedCount + removedCount;
				Reference2ObjectMap<ComponentType<?>, Optional<?>> result = new Reference2ObjectArrayMap<>(
						Math.min(totalCount, MAX_PACKET_MAP_SIZE)
				);

				for (int i = 0; i < addedCount; i++) {
					ComponentType<?> componentType = ComponentType.PACKET_CODEC.decode(buf);
					Object value = packetCodecFunction.apply(componentType).decode(buf);
					result.put(componentType, Optional.of(value));
				}

				for (int i = 0; i < removedCount; i++) {
					ComponentType<?> componentType = ComponentType.PACKET_CODEC.decode(buf);
					result.put(componentType, Optional.empty());
				}

				return new ComponentChanges(result);
			}

			@Override
			public void encode(RegistryByteBuf buf, ComponentChanges componentChanges) {
				if (componentChanges.isEmpty()) {
					buf.writeVarInt(0);
					buf.writeVarInt(0);
					return;
				}

				int addedCount = 0;
				int removedCount = 0;

				for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(componentChanges.changedComponents)) {
					if (entry.getValue().isPresent()) {
						addedCount++;
					} else {
						removedCount++;
					}
				}

				buf.writeVarInt(addedCount);
				buf.writeVarInt(removedCount);

				for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(componentChanges.changedComponents)) {
					Optional<?> optional = entry.getValue();
					if (optional.isPresent()) {
						ComponentType<?> componentType = entry.getKey();
						ComponentType.PACKET_CODEC.encode(buf, componentType);
						encodeValue(buf, componentType, optional.get());
					}
				}

				for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(componentChanges.changedComponents)) {
					if (entry.getValue().isEmpty()) {
						ComponentType.PACKET_CODEC.encode(buf, entry.getKey());
					}
				}
			}

			@SuppressWarnings("unchecked")
			private <T> void encodeValue(RegistryByteBuf buf, ComponentType<T> type, Object value) {
				packetCodecFunction.apply(type).encode(buf, (T) value);
			}
		};
	}

	ComponentChanges(Reference2ObjectMap<ComponentType<?>, Optional<?>> changedComponents) {
		this.changedComponents = changedComponents;
	}

	public static ComponentChanges.Builder builder() {
		return new ComponentChanges.Builder();
	}

	/**
		 * Возвращает значение изменения для указанного типа компонента.
		 * Возвращает {@code Optional.empty()} если компонент удалён,
		 * {@code Optional.of(value)} если добавлен/изменён,
		 * или {@code null} если изменений для этого типа нет.
		 *
		 * @param type тип компонента
		 * @return обёртка над значением или {@code null}
		 */
	public <T> @Nullable Optional<? extends T> get(ComponentType<? extends T> type) {
		return (Optional<? extends T>) changedComponents.get(type);
	}

	public Set<Entry<ComponentType<?>, Optional<?>>> entrySet() {
		return changedComponents.entrySet();
	}

	public int size() {
		return changedComponents.size();
	}

	/**
		 * Возвращает новый экземпляр {@code ComponentChanges} без типов, удовлетворяющих предикату.
		 *
		 * @param removedTypePredicate предикат для фильтрации типов компонентов
		 * @return отфильтрованные изменения
		 */
	public ComponentChanges withRemovedIf(Predicate<ComponentType<?>> removedTypePredicate) {
		if (isEmpty()) {
			return EMPTY;
		}

		Reference2ObjectMap<ComponentType<?>, Optional<?>> filtered = new Reference2ObjectArrayMap<>(changedComponents);
		filtered.keySet().removeIf(removedTypePredicate);

		return filtered.isEmpty() ? EMPTY : new ComponentChanges(filtered);
	}

	public boolean isEmpty() {
		return changedComponents.isEmpty();
	}

	/**
		 * Разбивает изменения на две группы: добавленные компоненты и удалённые типы.
		 *
		 * @return пара {@link AddedRemovedPair}
		 */
	public ComponentChanges.AddedRemovedPair toAddedRemovedPair() {
		if (isEmpty()) {
			return ComponentChanges.AddedRemovedPair.EMPTY;
		}

		ComponentMap.Builder builder = ComponentMap.builder();
		Set<ComponentType<?>> removed = Sets.newIdentityHashSet();

		changedComponents.forEach((type, value) -> {
			if (value.isPresent()) {
				builder.put(type, value.get());
			} else {
				removed.add(type);
			}
		});

		return new ComponentChanges.AddedRemovedPair(builder.build(), removed);
	}

	@Override
	public boolean equals(Object o) {
		return this == o
			? true
			: o instanceof ComponentChanges other && changedComponents.equals(other.changedComponents);
	}

	@Override
	public int hashCode() {
		return changedComponents.hashCode();
	}

	@Override
	public String toString() {
		return toString(changedComponents);
	}

	static String toString(Reference2ObjectMap<ComponentType<?>, Optional<?>> changes) {
		StringBuilder builder = new StringBuilder();
		builder.append('{');
		boolean isFirst = true;

		for (Entry<ComponentType<?>, Optional<?>> entry : Reference2ObjectMaps.fastIterable(changes)) {
			if (isFirst) {
				isFirst = false;
			} else {
				builder.append(", ");
			}

			Optional<?> optional = entry.getValue();
			if (optional.isPresent()) {
				builder.append(entry.getKey());
				builder.append("=>");
				builder.append(optional.get());
			} else {
				builder.append(REMOVE_PREFIX);
				builder.append(entry.getKey());
			}
		}

		builder.append('}');
		return builder.toString();
	}

	public record AddedRemovedPair(ComponentMap added, Set<ComponentType<?>> removed) {

		public static final ComponentChanges.AddedRemovedPair EMPTY = new ComponentChanges.AddedRemovedPair(
				ComponentMap.EMPTY,
				Set.of()
		);
	}

	public static class Builder {

		private final Reference2ObjectMap<ComponentType<?>, Optional<?>> changes = new Reference2ObjectArrayMap<>();

		Builder() {
		}

		public <T> ComponentChanges.Builder add(ComponentType<T> type, T value) {
			changes.put(type, Optional.of(value));
			return this;
		}

		public <T> ComponentChanges.Builder remove(ComponentType<T> type) {
			changes.put(type, Optional.empty());
			return this;
		}

		public <T> ComponentChanges.Builder add(Component<T> component) {
			return add(component.type(), component.value());
		}

		public ComponentChanges build() {
			return changes.isEmpty() ? ComponentChanges.EMPTY : new ComponentChanges(changes);
		}
	}

	/**
		 * Функциональный интерфейс для получения сетевого кодека по типу компонента.
		 * Используется при создании {@link PacketCodec} для {@link ComponentChanges}.
		 */
	@FunctionalInterface
	interface PacketCodecFunction {

		<T> PacketCodec<? super RegistryByteBuf, T> apply(ComponentType<T> type);
	}

	/**
		 * Внутренний тип, объединяющий {@link ComponentType} с флагом удаления.
		 * Используется как ключ при сериализации изменений через {@link Codec#dispatchedMap}.
		 */
	record Type(ComponentType<?> type, boolean removed) {

		public static final Codec<ComponentChanges.Type> CODEC = Codec.STRING.flatXmap(
				id -> {
					boolean isRemoval = id.startsWith(REMOVE_PREFIX);
					String resolvedId = isRemoval ? id.substring(REMOVE_PREFIX.length()) : id;

					Identifier identifier = Identifier.tryParse(resolvedId);
					ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(identifier);

					if (componentType == null) {
						return DataResult.error(() -> "No component with type: '" + identifier + "'");
					}

					return componentType.shouldSkipSerialization()
						? DataResult.error(() -> "'" + identifier + "' is not a persistent component")
						: DataResult.success(new ComponentChanges.Type(componentType, isRemoval));
				},
				type -> {
					ComponentType<?> componentType = type.type();
					Identifier identifier = Registries.DATA_COMPONENT_TYPE.getId(componentType);

					return identifier == null
						? DataResult.error(() -> "Unregistered component: " + componentType)
						: DataResult.success(type.removed() ? REMOVE_PREFIX + identifier : identifier.toString());
				}
		);

		public Codec<?> getValueCodec() {
			return removed ? Codec.EMPTY.codec() : type.getCodecOrThrow();
		}
	}
}
