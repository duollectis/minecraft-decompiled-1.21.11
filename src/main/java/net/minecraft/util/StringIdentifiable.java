package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Интерфейс для объектов, имеющих строковый идентификатор для сериализации.
 * Реализуется перечислениями, которые нужно сериализовать в JSON или по сети.
 * При количестве значений больше {@value CACHED_MAP_THRESHOLD} используется HashMap для O(1) поиска.
 */
public interface StringIdentifiable {

	/** Порог количества значений, при превышении которого создаётся кэш-карта для поиска. */
	int CACHED_MAP_THRESHOLD = 16;

	String asString();

	static <E extends Enum<E> & StringIdentifiable> EnumCodec<E> createCodec(Supplier<E[]> enumValues) {
		return createCodec(enumValues, id -> id);
	}

	/**
	 * Создаёт {@link EnumCodec} с опциональным преобразованием имён значений.
	 *
	 * @param enumValues поставщик массива значений перечисления
	 * @param valueNameTransformer функция преобразования строкового идентификатора
	 * @return кодек для перечисления
	 */
	static <E extends Enum<E> & StringIdentifiable> EnumCodec<E> createCodec(
		Supplier<E[]> enumValues,
		Function<String, String> valueNameTransformer
	) {
		E[] enums = enumValues.get();
		Function<String, E> mapper = createMapper(enums, e -> valueNameTransformer.apply(e.asString()));
		return new EnumCodec<>(enums, mapper);
	}

	/**
	 * Создаёт базовый {@link Codec} для не-enum типов, реализующих {@link StringIdentifiable}.
	 * Поддерживает как строковую, так и числовую (сжатую) сериализацию.
	 *
	 * @param values поставщик массива всех значений
	 * @return кодек для типа
	 */
	static <T extends StringIdentifiable> Codec<T> createBasicCodec(Supplier<T[]> values) {
		T[] array = values.get();
		Function<String, T> mapper = createMapper(array);
		ToIntFunction<T> ordinalGetter = Util.lastIndexGetter(Arrays.asList(array));
		return new BasicCodec<>(array, mapper, ordinalGetter);
	}

	static <T extends StringIdentifiable> Function<String, @Nullable T> createMapper(T[] values) {
		return createMapper(values, StringIdentifiable::asString);
	}

	/**
	 * Создаёт функцию поиска значения по строковому идентификатору.
	 * При количестве значений больше {@value CACHED_MAP_THRESHOLD} использует HashMap.
	 *
	 * @param values массив значений
	 * @param valueNameTransformer функция получения строкового ключа из значения
	 * @return функция поиска значения по ключу
	 */
	static <T> Function<String, @Nullable T> createMapper(T[] values, Function<T, String> valueNameTransformer) {
		if (values.length > CACHED_MAP_THRESHOLD) {
			Map<String, T> cache = Arrays.stream(values)
				.collect(Collectors.toMap(valueNameTransformer, value -> value));
			return cache::get;
		}

		return name -> {
			for (T value : values) {
				if (valueNameTransformer.apply(value).equals(name)) {
					return value;
				}
			}
			return null;
		};
	}

	static Keyable toKeyable(StringIdentifiable[] values) {
		return new Keyable() {
			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return Arrays.stream(values)
					.map(StringIdentifiable::asString)
					.map(ops::createString);
			}
		};
	}

	/**
	 * Базовый кодек для типов, реализующих {@link StringIdentifiable}.
	 * Поддерживает сжатую (числовую) сериализацию через {@link Codecs#orCompressed}.
	 */
	class BasicCodec<S extends StringIdentifiable> implements Codec<S> {

		private final Codec<S> codec;

		public BasicCodec(
			S[] values,
			Function<String, @Nullable S> idToIdentifiable,
			ToIntFunction<S> identifiableToOrdinal
		) {
			codec = Codecs.orCompressed(
				Codec.stringResolver(StringIdentifiable::asString, idToIdentifiable),
				Codecs.rawIdChecked(
					identifiableToOrdinal,
					ordinal -> ordinal >= 0 && ordinal < values.length ? values[ordinal] : null,
					-1
				)
			);
		}

		@Override
		public <T> DataResult<com.mojang.datafixers.util.Pair<S, T>> decode(DynamicOps<T> ops, T input) {
			return codec.decode(ops, input);
		}

		@Override
		public <T> DataResult<T> encode(S value, DynamicOps<T> ops, T prefix) {
			return codec.encode(value, ops, prefix);
		}
	}

	/**
	 * Кодек для перечислений, реализующих {@link StringIdentifiable}.
	 * Дополнительно предоставляет метод {@link #byId(String)} для прямого поиска по идентификатору.
	 */
	class EnumCodec<E extends Enum<E> & StringIdentifiable> extends BasicCodec<E> {

		private final Function<String, @Nullable E> idToIdentifiable;

		public EnumCodec(E[] values, Function<String, E> idToIdentifiable) {
			super(values, idToIdentifiable, Enum::ordinal);
			this.idToIdentifiable = idToIdentifiable;
		}

		public @Nullable E byId(String id) {
			return idToIdentifiable.apply(id);
		}

		public E byId(String id, E fallback) {
			return Objects.requireNonNullElse(byId(id), fallback);
		}

		public E byId(String id, Supplier<? extends E> fallbackSupplier) {
			return Objects.requireNonNullElseGet(byId(id), fallbackSupplier);
		}
	}
}
