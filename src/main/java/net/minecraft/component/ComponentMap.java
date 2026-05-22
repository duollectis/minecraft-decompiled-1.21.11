package net.minecraft.component;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
	 * Неизменяемый набор компонентов данных предмета.
	 * Каждый тип компонента может присутствовать не более одного раза.
	 * Для изменяемого варианта используется {@link MergedComponentMap}.
	 */
public interface ComponentMap extends Iterable<Component<?>>, ComponentsAccess {

	/**
		 * Характеристики сплитератора для {@link #stream()}.
		 * {@code SIZED | DISTINCT | NONNULL} = 0x541 = 1345
		 */
	int STREAM_SPLITERATOR_CHARACTERISTICS = Spliterator.SIZED | Spliterator.DISTINCT | Spliterator.NONNULL;

	/**
		 * Порог размера карты, при котором {@link Builder} переключается
		 * с {@link Reference2ObjectArrayMap} на {@link Reference2ObjectOpenHashMap}.
		 */
	int HASH_MAP_THRESHOLD = 8;

	ComponentMap EMPTY = new ComponentMap() {
		@Override
		public <T> @Nullable T get(ComponentType<? extends T> type) {
			return null;
		}

		@Override
		public Set<ComponentType<?>> getTypes() {
			return Set.of();
		}

		@Override
		public Iterator<Component<?>> iterator() {
			return Collections.emptyIterator();
		}
	};

	Codec<ComponentMap> CODEC = createCodecFromValueMap(ComponentType.TYPE_TO_VALUE_MAP_CODEC);

	static Codec<ComponentMap> createCodec(Codec<ComponentType<?>> componentTypeCodec) {
		return createCodecFromValueMap(Codec.dispatchedMap(componentTypeCodec, ComponentType::getCodecOrThrow));
	}

	/**
		 * Создаёт кодек {@link ComponentMap} из кодека карты «тип → значение».
		 * При сериализации пропускает транзитные компоненты (без кодека).
		 *
		 * @param typeToValueMapCodec кодек карты типов к значениям
		 * @return кодек для {@link ComponentMap}
		 */
	static Codec<ComponentMap> createCodecFromValueMap(Codec<Map<ComponentType<?>, Object>> typeToValueMapCodec) {
		return typeToValueMapCodec.flatComapMap(
				ComponentMap.Builder::build,
				componentMap -> {
					int size = componentMap.size();
					if (size == 0) {
						return DataResult.success(Reference2ObjectMaps.emptyMap());
					}

					Reference2ObjectMap<ComponentType<?>, Object> result = new Reference2ObjectArrayMap<>(size);

					for (Component<?> component : componentMap) {
						if (!component.type().shouldSkipSerialization()) {
							result.put(component.type(), component.value());
						}
					}

					return DataResult.success(result);
				}
		);
	}

	/**
		 * Создаёт представление, объединяющее два набора компонентов.
		 * Значения из {@code overrides} имеют приоритет над {@code base}.
		 *
		 * @param base      базовые компоненты
		 * @param overrides компоненты-переопределения
		 * @return объединённый вид
		 */
	static ComponentMap of(ComponentMap base, ComponentMap overrides) {
		return new ComponentMap() {
			@Override
			public <T> @Nullable T get(ComponentType<? extends T> type) {
				T value = overrides.get(type);
				return value != null ? value : base.get(type);
			}

			@Override
			public Set<ComponentType<?>> getTypes() {
				return Sets.union(base.getTypes(), overrides.getTypes());
			}
		};
	}

	static ComponentMap.Builder builder() {
		return new ComponentMap.Builder();
	}

	Set<ComponentType<?>> getTypes();

	default boolean contains(ComponentType<?> type) {
		return get(type) != null;
	}

	@Override
	default Iterator<Component<?>> iterator() {
		return Iterators.transform(getTypes().iterator(), type -> Objects.requireNonNull(getTyped(type)));
	}

	default Stream<Component<?>> stream() {
		return StreamSupport.stream(
				Spliterators.spliterator(iterator(), (long) size(), STREAM_SPLITERATOR_CHARACTERISTICS),
				false
		);
	}

	default int size() {
		return getTypes().size();
	}

	default boolean isEmpty() {
		return size() == 0;
	}

	default ComponentMap filtered(Predicate<ComponentType<?>> predicate) {
		return new ComponentMap() {
			@Override
			public <T> @Nullable T get(ComponentType<? extends T> type) {
				return predicate.test(type) ? ComponentMap.this.get(type) : null;
			}

			@Override
			public Set<ComponentType<?>> getTypes() {
				return Sets.filter(ComponentMap.this.getTypes(), predicate::test);
			}
		};
	}

	class Builder implements FabricComponentMapBuilder {

		private final Reference2ObjectMap<ComponentType<?>, Object> components = new Reference2ObjectArrayMap<>();

		Builder() {
		}

		public <T> ComponentMap.Builder add(ComponentType<T> type, @Nullable T value) {
			put(type, value);
			return this;
		}

		<T> void put(ComponentType<T> type, @Nullable Object value) {
			if (value != null) {
				components.put(type, value);
			} else {
				components.remove(type);
			}
		}

		public ComponentMap.Builder addAll(ComponentMap componentSet) {
			for (Component<?> component : componentSet) {
				components.put(component.type(), component.value());
			}

			return this;
		}

		public ComponentMap build() {
			return build(components);
		}

		private static ComponentMap build(Map<ComponentType<?>, Object> components) {
			if (components.isEmpty()) {
				return ComponentMap.EMPTY;
			}

			return components.size() < HASH_MAP_THRESHOLD
				? new SimpleComponentMap(new Reference2ObjectArrayMap<>(components))
				: new SimpleComponentMap(new Reference2ObjectOpenHashMap<>(components));
		}

		record SimpleComponentMap(Reference2ObjectMap<ComponentType<?>, Object> map) implements ComponentMap {

			@Override
			public <T> @Nullable T get(ComponentType<? extends T> type) {
				return (T) map.get(type);
			}

			@Override
			public boolean contains(ComponentType<?> type) {
				return map.containsKey(type);
			}

			@Override
			public Set<ComponentType<?>> getTypes() {
				return map.keySet();
			}

			@Override
			public Iterator<Component<?>> iterator() {
				return Iterators.transform(Reference2ObjectMaps.fastIterator(map), Component::of);
			}

			@Override
			public int size() {
				return map.size();
			}

			@Override
			public String toString() {
				return map.toString();
			}
		}
	}
}
