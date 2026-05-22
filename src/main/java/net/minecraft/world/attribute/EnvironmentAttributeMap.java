package net.minecraft.world.attribute;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Иммутабельная карта атрибутов окружения: хранит пары «атрибут → запись модификатора».
 * Используется в биомах и измерениях для задания значений атрибутов.
 * <p>
 * Поддерживает три codec-а: полный, сетевой (только синхронизируемые атрибуты)
 * и позиционный (только позиционные атрибуты).
 */
public final class EnvironmentAttributeMap {

	public static final EnvironmentAttributeMap EMPTY = new EnvironmentAttributeMap(Map.of());

	@SuppressWarnings("unchecked")
	public static final Codec<EnvironmentAttributeMap> CODEC = Codec.lazyInitialized(
		() -> Codec.dispatchedMap(
			EnvironmentAttributes.CODEC,
			Util.memoize(Entry::createCodec)
		).<EnvironmentAttributeMap>xmap(
			EnvironmentAttributeMap::fromRawMap,
			map -> (Map) map.entries
		)
	);

	public static final Codec<EnvironmentAttributeMap> NETWORK_CODEC = CODEC.xmap(
		EnvironmentAttributeMap::retainSyncedAttributes,
		EnvironmentAttributeMap::retainSyncedAttributes
	);

	public static final Codec<EnvironmentAttributeMap> POSITIONAL_CODEC = CODEC.validate(map -> {
		List<EnvironmentAttribute<?>> nonPositional = map.keySet()
			.stream()
			.filter(attribute -> !attribute.isPositional())
			.toList();

		return nonPositional.isEmpty()
			? DataResult.success(map)
			: DataResult.error(() -> "The following attributes cannot be positional: " + nonPositional);
	});

	final Map<EnvironmentAttribute<?>, Entry<?, ?>> entries;

	@SuppressWarnings("unchecked")
	private static EnvironmentAttributeMap fromRawMap(Map<?, ?> raw) {
		return new EnvironmentAttributeMap((Map<EnvironmentAttribute<?>, Entry<?, ?>>) raw);
	}

	private static EnvironmentAttributeMap retainSyncedAttributes(EnvironmentAttributeMap map) {
		return new EnvironmentAttributeMap(Map.copyOf(Maps.filterKeys(map.entries, EnvironmentAttribute::isSynced)));
	}

	EnvironmentAttributeMap(Map<EnvironmentAttribute<?>, Entry<?, ?>> entries) {
		this.entries = entries;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Возвращает запись модификатора для заданного атрибута, или {@code null} если не задана.
	 *
	 * @param key атрибут
	 * @return запись или {@code null}
	 */
	@SuppressWarnings("unchecked")
	public <Value> @Nullable Entry<Value, ?> getEntry(EnvironmentAttribute<Value> key) {
		return (Entry<Value, ?>) entries.get(key);
	}

	/**
	 * Применяет модификатор атрибута к переданному значению.
	 * Если атрибут не задан в карте — возвращает значение без изменений.
	 *
	 * @param key атрибут
	 * @param value исходное значение
	 * @return модифицированное значение
	 */
	public <Value> Value apply(EnvironmentAttribute<Value> key, Value value) {
		Entry<Value, ?> entry = getEntry(key);
		return entry != null ? entry.apply(value) : value;
	}

	/** Проверяет, задан ли атрибут в данной карте. */
	public boolean containsKey(EnvironmentAttribute<?> key) {
		return entries.containsKey(key);
	}

	/** Возвращает множество всех атрибутов, заданных в данной карте. */
	public Set<EnvironmentAttribute<?>> keySet() {
		return entries.keySet();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EnvironmentAttributeMap map && entries.equals(map.entries);
	}

	@Override
	public int hashCode() {
		return entries.hashCode();
	}

	@Override
	public String toString() {
		return entries.toString();
	}

	/**
	 * Билдер для создания {@link EnvironmentAttributeMap}.
	 */
	public static class Builder {

		private final Map<EnvironmentAttribute<?>, Entry<?, ?>> entries = new HashMap<>();

		Builder() {
		}

		/** Добавляет все записи из другой карты атрибутов. */
		public Builder addAll(EnvironmentAttributeMap map) {
			entries.putAll(map.entries);
			return this;
		}

		/**
		 * Добавляет запись с явным модификатором и аргументом.
		 *
		 * @param key атрибут
		 * @param modifier модификатор
		 * @param param аргумент модификатора
		 */
		public <Value, Parameter> Builder with(
			EnvironmentAttribute<Value> key,
			EnvironmentAttributeModifier<Value, Parameter> modifier,
			Parameter param
		) {
			key.getType().validate(modifier);
			entries.put(key, new Entry<>(param, modifier));
			return this;
		}

		/**
		 * Добавляет запись с модификатором-перезаписью (override).
		 *
		 * @param key атрибут
		 * @param value новое значение
		 */
		public <Value> Builder with(EnvironmentAttribute<Value> key, Value value) {
			return with(key, EnvironmentAttributeModifier.override(), value);
		}

		/** Собирает иммутабельную карту атрибутов. */
		public EnvironmentAttributeMap build() {
			return entries.isEmpty() ? EMPTY : new EnvironmentAttributeMap(Map.copyOf(entries));
		}
	}

	/**
	 * Запись карты атрибутов: хранит аргумент и модификатор для одного атрибута.
	 *
	 * @param <Value> тип значения атрибута
	 * @param <Argument> тип аргумента модификатора
	 */
	public record Entry<Value, Argument>(Argument argument, EnvironmentAttributeModifier<Value, Argument> modifier) {

		/**
		 * Создаёт codec для записи конкретного атрибута.
		 * Поддерживает компактную форму (просто значение) и полную форму (с модификатором).
		 */
		@SuppressWarnings("unchecked")
		private static <Value> Codec<Entry<Value, ?>> createCodec(EnvironmentAttribute<Value> attribute) {
			Codec<Entry<Value, ?>> fullCodec = attribute.getType()
				.modifierCodec()
				.dispatch(
					"modifier",
					Entry::modifier,
					Util.memoize(modifier -> createModifierDependentCodec(attribute, modifier))
				);

			return Codec.either(attribute.getCodec(), fullCodec)
				.xmap(
					either -> (Entry<Value, ?>) either.map(
						value -> new Entry<>(value, EnvironmentAttributeModifier.override()),
						entry -> entry
					),
					entry -> entry.modifier == EnvironmentAttributeModifier.override()
						? Either.<Value, Entry<Value, ?>>left((Value) entry.argument())
						: Either.right(entry)
				);
		}

		private static <Value, Argument> MapCodec<Entry<Value, Argument>> createModifierDependentCodec(
			EnvironmentAttribute<Value> attribute,
			EnvironmentAttributeModifier<Value, Argument> modifier
		) {
			return RecordCodecBuilder.mapCodec(
				instance -> instance
					.group(modifier.argumentCodec(attribute).fieldOf("argument").forGetter(Entry::argument))
					.apply(instance, argument -> new Entry<>(argument, modifier))
			);
		}

		/**
		 * Применяет модификатор к переданному значению атрибута.
		 *
		 * @param value исходное значение
		 * @return модифицированное значение
		 */
		public Value apply(Value value) {
			return modifier.apply(value, argument);
		}
	}
}
