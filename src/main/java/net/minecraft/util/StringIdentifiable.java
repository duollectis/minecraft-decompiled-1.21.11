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
 * {@code StringIdentifiable}.
 */
public interface StringIdentifiable {

	int CACHED_MAP_THRESHOLD = 16;

	String asString();

	static <E extends Enum<E> & StringIdentifiable> StringIdentifiable.EnumCodec<E> createCodec(Supplier<E[]> enumValues) {
		return createCodec(enumValues, id -> id);
	}

	static <E extends Enum<E> & StringIdentifiable> StringIdentifiable.EnumCodec<E> createCodec(
			Supplier<E[]> enumValues, Function<String, String> valueNameTransformer
	) {
		E[] enums = (E[]) enumValues.get();
		Function<String, E> function = createMapper(enums, enum_ -> valueNameTransformer.apply(enum_.asString()));
		return new StringIdentifiable.EnumCodec<>(enums, function);
	}

	static <T extends StringIdentifiable> Codec<T> createBasicCodec(Supplier<T[]> values) {
		T[] stringIdentifiables = (T[]) values.get();
		Function<String, T> function = createMapper(stringIdentifiables);
		ToIntFunction<T> toIntFunction = Util.lastIndexGetter(Arrays.asList(stringIdentifiables));
		return new StringIdentifiable.BasicCodec<>(stringIdentifiables, function, toIntFunction);
	}

	static <T extends StringIdentifiable> Function<String, @Nullable T> createMapper(T[] values) {
		return createMapper(values, StringIdentifiable::asString);
	}

	static <T> Function<String, @Nullable T> createMapper(T[] values, Function<T, String> valueNameTransformer) {
		if (values.length > 16) {
			Map<String, T>
					map =
					Arrays.<T>stream(values).collect(Collectors.toMap(valueNameTransformer, object -> (T) object));
			return map::get;
		}
		else {
			return name -> {
				for (T object : values) {
					if (valueNameTransformer.apply(object).equals(name)) {
						return object;
					}
				}

				return null;
			};
		}
	}

	static Keyable toKeyable(StringIdentifiable[] values) {
		return new Keyable() {
			/**
			 * Keys.
			 *
			 * @param ops ops
			 *
			 * @return Stream — результат операции
			 */
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return Arrays.stream(values).map(StringIdentifiable::asString).map(ops::createString);
			}
		};
	}

	/**
	 * {@code BasicCodec}.
	 */
	public static class BasicCodec<S extends StringIdentifiable> implements Codec<S> {

		private final Codec<S> codec;

		public BasicCodec(
				S[] values,
				Function<String, @Nullable S> idToIdentifiable,
				ToIntFunction<S> identifiableToOrdinal
		) {
			this.codec = Codecs.orCompressed(
					Codec.stringResolver(StringIdentifiable::asString, idToIdentifiable),
					Codecs.rawIdChecked(
							identifiableToOrdinal,
							ordinal -> ordinal >= 0 && ordinal < values.length ? values[ordinal] : null,
							-1
					)
			);
		}

		public <T> DataResult<com.mojang.datafixers.util.Pair<S, T>> decode(DynamicOps<T> ops, T input) {
			return this.codec.decode(ops, input);
		}

		/**
		 * Encode.
		 *
		 * @param stringIdentifiable string identifiable
		 * @param dynamicOps dynamic ops
		 * @param object object
		 *
		 * @return DataResult — результат операции
		 */
		public <T> DataResult<T> encode(S stringIdentifiable, DynamicOps<T> dynamicOps, T object) {
			return this.codec.encode(stringIdentifiable, dynamicOps, object);
		}
	}

	/**
	 * {@code EnumCodec}.
	 */
	public static class EnumCodec<E extends Enum<E> & StringIdentifiable> extends StringIdentifiable.BasicCodec<E> {

		private final Function<String, @Nullable E> idToIdentifiable;

		public EnumCodec(E[] values, Function<String, E> idToIdentifiable) {
			super(values, idToIdentifiable, enum_ -> enum_.ordinal());
			this.idToIdentifiable = idToIdentifiable;
		}

		/**
		 * By id.
		 *
		 * @param id id
		 *
		 * @return @Nullable E — результат операции
		 */
		public @Nullable E byId(String id) {
			return this.idToIdentifiable.apply(id);
		}

		/**
		 * By id.
		 *
		 * @param id id
		 * @param fallback fallback
		 *
		 * @return E — результат операции
		 */
		public E byId(String id, E fallback) {
			return Objects.requireNonNullElse(this.byId(id), fallback);
		}

		/**
		 * By id.
		 *
		 * @param id id
		 * @param fallbackSupplier fallback supplier
		 *
		 * @return E — результат операции
		 */
		public E byId(String id, Supplier<? extends E> fallbackSupplier) {
			return Objects.requireNonNullElseGet(this.byId(id), fallbackSupplier);
		}
	}
}
