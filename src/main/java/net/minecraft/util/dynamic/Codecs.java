package net.minecraft.util.dynamic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import org.joml.AxisAngle4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Набор вспомогательных кодеков и фабричных методов для сериализации
 * стандартных типов Minecraft: векторов, цветов, профилей игроков,
 * диапазонов, регулярных выражений и других структур данных.
 */
public class Codecs {

	private static final int MAX_PROFILE_PROPERTIES_LENGTH = 16;

	public static final Codec<Vector2fc> VECTOR_2F = Codec.FLOAT
			.listOf()
			.comapFlatMap(
					list -> Util.decodeFixedLengthList(list, 2)
							.map(l -> new org.joml.Vector2f(l.get(0), l.get(1))),
					vec -> List.of(vec.x(), vec.y())
			);

	public static final Codec<Vector3fc> VECTOR_3F = Codec.FLOAT
			.listOf()
			.comapFlatMap(
					list -> Util.decodeFixedLengthList(list, 3)
							.map(l -> new Vector3f(l.get(0), l.get(1), l.get(2))),
					vec -> List.of(vec.x(), vec.y(), vec.z())
			);

	public static final Codec<Vector3ic> VECTOR_3I = Codec.INT
			.listOf()
			.comapFlatMap(
					list -> Util.decodeFixedLengthList(list, 3)
							.map(l -> new Vector3i(l.get(0), l.get(1), l.get(2))),
					vec -> List.of(vec.x(), vec.y(), vec.z())
			);

	public static final Codec<Vector4fc> VECTOR_4F = Codec.FLOAT
			.listOf()
			.comapFlatMap(
					list -> Util.decodeFixedLengthList(list, 4)
							.map(l -> new Vector4f(l.get(0), l.get(1), l.get(2), l.get(3))),
					vec -> List.of(vec.x(), vec.y(), vec.z(), vec.w())
			);

	public static final Codec<Quaternionfc> QUATERNION_F = Codec.FLOAT
			.listOf()
			.comapFlatMap(
					list -> Util.decodeFixedLengthList(list, 4)
							.map(l -> new Quaternionf(l.get(0), l.get(1), l.get(2), l.get(3))),
					q -> List.of(q.x(), q.y(), q.z(), q.w())
			);

	public static final Codec<AxisAngle4f> AXIS_ANGLE_4F = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.FLOAT.fieldOf("angle").forGetter(a -> a.angle),
					Codec.FLOAT.fieldOf("x").forGetter(a -> a.x),
					Codec.FLOAT.fieldOf("y").forGetter(a -> a.y),
					Codec.FLOAT.fieldOf("z").forGetter(a -> a.z)
			).apply(instance, AxisAngle4f::new)
	);

	public static final Codec<Matrix4fc> MATRIX_4F = Codec.FLOAT
			.listOf()
			.comapFlatMap(
					list -> Util.decodeFixedLengthList(list, MAX_PROFILE_PROPERTIES_LENGTH).map(l -> {
						org.joml.Matrix4f matrix = new org.joml.Matrix4f();
						matrix.set(
								l.get(0), l.get(1), l.get(2), l.get(3),
								l.get(4), l.get(5), l.get(6), l.get(7),
								l.get(8), l.get(9), l.get(10), l.get(11),
								l.get(12), l.get(13), l.get(14), l.get(15)
						);
						return matrix;
					}),
					matrix -> {
						float[] values = new float[MAX_PROFILE_PROPERTIES_LENGTH];
						matrix.get(values);
						List<Float> result = new java.util.ArrayList<>(MAX_PROFILE_PROPERTIES_LENGTH);
						for (float v : values) {
							result.add(v);
						}
						return result;
					}
			);

	public static final Codec<Integer> ARGB = Codec.withAlternative(
			Codec.INT,
			hexColor(8)
	);

	public static final Codec<Integer> RGB = Codec.withAlternative(
			Codec.INT,
			hexColor(6)
	);
	public static final Codec<Integer> HEX_RGB = RGB;
	public static final Codec<Integer> HEX_ARGB = ARGB;
	public static final Codec<Float> NON_NEGATIVE_FLOAT = rangedFloat(0.0f, Float.MAX_VALUE);
	public static final Codec<Float> POSITIVE_FLOAT = rangedFloat(Float.MIN_VALUE, Float.MAX_VALUE);
	public static final Codec<Quaternionfc> ROTATION = QUATERNION_F;
	public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM.xmap(
			stream -> BitSet.valueOf(stream.toArray()),
			bitSet -> LongStream.of(bitSet.toLongArray())
	);
	public static final Codec<Instant> INSTANT = Codec.LONG.xmap(
			Instant::ofEpochMilli,
			Instant::toEpochMilli
	);
	public static final Codec<String> NON_EMPTY_STRING = Codec.STRING.validate(
			s -> s.isEmpty()
					? DataResult.error(() -> "String must be non-empty")
					: DataResult.success(s)
	);

	public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
			.flatXmap(
					b -> DataResult.success(b & 0xFF),
					i -> i >= 0 && i <= 255
							? DataResult.success(i.byteValue())
							: DataResult.error(() -> "Byte value not in range [0, 255]: " + i)
			);

	public static final Codec<Integer> NON_NEGATIVE_INT = rangedInt(0, Integer.MAX_VALUE, i -> "Value must be non-negative: " + i);
	public static final Codec<Integer> POSITIVE_INT = rangedInt(1, Integer.MAX_VALUE, i -> "Value must be positive: " + i);
	public static final Codec<Long> POSITIVE_LONG = rangedLong(1L, Long.MAX_VALUE, l -> "Value must be positive: " + l);
	public static final Codec<Float> UNIT_FLOAT = rangedInclusiveFloat(0.0f, 1.0f);

	public static final Codec<Pattern> REGULAR_EXPRESSION = Codec.STRING.comapFlatMap(
			pattern -> {
				try {
					return DataResult.success(Pattern.compile(pattern));
				} catch (PatternSyntaxException e) {
					return DataResult.error(() -> "Invalid regex pattern '" + pattern + "': " + e.getMessage());
				}
			},
			Pattern::pattern
	);

	public static final Codec<byte[]> BASE_64 = Codec.STRING.comapFlatMap(
			encoded -> {
				try {
					return DataResult.success(Base64.getDecoder().decode(encoded));
				} catch (IllegalArgumentException e) {
					return DataResult.error(() -> "Invalid base64 string: " + e.getMessage());
				}
			},
			bytes -> Base64.getEncoder().encodeToString(bytes)
	);

	public static final Codec<String> ESCAPED_STRING = Codec.STRING
			.xmap(StringHelper::stripTextFormat, Function.identity());

	public static final Codec<TagEntryId> TAG_ENTRY_ID = Codec.lazyInitialized(
			() -> Codec.STRING.comapFlatMap(
					string -> string.startsWith("#")
							? Identifier.validate(string.substring(1)).map(id -> new TagEntryId(id, true))
							: Identifier.validate(string).map(id -> new TagEntryId(id, false)),
					TagEntryId::toString
			)
	);

	public static final Function<Optional<Long>, OptionalLong> OPTIONAL_OF_LONG_TO_OPTIONAL_LONG =
			optional -> optional.map(OptionalLong::of).orElseGet(OptionalLong::empty);

	public static final Function<OptionalLong, Optional<Long>> OPTIONAL_LONG_TO_OPTIONAL_OF_LONG =
			optionalLong -> optionalLong.isPresent() ? Optional.of(optionalLong.getAsLong()) : Optional.empty();

	private static final Codec<Property> GAME_PROFILE_PROPERTY = RecordCodecBuilder.create(
			instance -> instance.group(
					Codec.STRING.fieldOf("name").forGetter(Property::name),
					Codec.STRING.fieldOf("value").forGetter(Property::value),
					Codec.STRING.optionalFieldOf("signature").forGetter(p -> Optional.ofNullable(p.signature()))
			).apply(instance, (name, value, signature) -> new Property(name, value, signature.orElse(null)))
	);

	public static final Codec<PropertyMap> GAME_PROFILE_PROPERTY_MAP = Codec.either(
			Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()),
			GAME_PROFILE_PROPERTY.listOf()
	).xmap(
			either -> {
				com.google.common.collect.LinkedListMultimap<String, Property> backing =
						com.google.common.collect.LinkedListMultimap.create();
				either.ifLeft(map -> map.forEach(
						(key, values) -> values.forEach(value -> backing.put(key, new Property(key, value)))
				));
				either.ifRight(properties -> properties.forEach(p -> backing.put(p.name(), p)));
				PropertyMap propertyMap = new PropertyMap(backing);
				return propertyMap;
			},
			properties -> Either.right(properties.values().stream().toList())
	);

	public static final Codec<String> PLAYER_NAME = Codec.string(0, MAX_PROFILE_PROPERTIES_LENGTH)
			.validate(
					name -> StringHelper.isValidPlayerName(name)
							? DataResult.success(name)
							: DataResult.error(() -> "Player name contained invalid characters: '" + name + "'")
			);

	private static MapCodec<GameProfile> createGameProfileCodec(Codec<UUID> uuidCodec) {
		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						uuidCodec.fieldOf("id").forGetter(GameProfile::id),
						PLAYER_NAME.fieldOf("name").forGetter(GameProfile::name),
						GAME_PROFILE_PROPERTY_MAP
								.optionalFieldOf("properties", PropertyMap.EMPTY)
								.forGetter(GameProfile::properties)
				).apply(instance, GameProfile::new)
		);
	}

	public static final Codec<GameProfile> GAME_PROFILE = Codec.withAlternative(
			createGameProfileCodec(Uuids.STRING_CODEC).codec(),
			createGameProfileCodec(Uuids.INT_STREAM_CODEC).codec()
	);
	public static final MapCodec<GameProfile> INT_STREAM_UUID_GAME_PROFILE_CODEC =
			createGameProfileCodec(Uuids.INT_STREAM_CODEC);

	public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(
			string -> {
				int[] codepoints = string.codePoints().toArray();
				return codepoints.length == 1
						? DataResult.success(codepoints[0])
						: DataResult.error(() -> "Expected one codepoint, got: " + string);
			},
			Character::toString
	);

	public static final Codec<String> IDENTIFIER_PATH = Codec.STRING
			.validate(
					path -> Identifier.isPathValid(path)
							? DataResult.success(path)
							: DataResult.error(() -> "Invalid identifier path: " + path)
			);

	public static final Codec<URI> URI = Codec.STRING.comapFlatMap(
			value -> {
				try {
					return DataResult.success(new java.net.URI(value));
				} catch (java.net.URISyntaxException e) {
					return DataResult.error(() -> "Not a valid URI: " + value + " " + e.getMessage());
				}
			},
			java.net.URI::toString
	);

	public static final Codec<String> CHAT_TEXT = Codec.STRING.validate(s -> {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\n' || c == '\t' || c >= ' ') {
				continue;
			}
			return DataResult.error(() -> "Illegal character in chat: '" + c + "' (" + (int) c + ")");
		}
		return DataResult.success(s);
	});

	private static Codec<Integer> hexColor(int hexDigits) {
		return Codec.STRING.comapFlatMap(
				value -> {
					if (value.length() != hexDigits + 1 || value.charAt(0) != '#') {
						return DataResult.error(() -> "Not a hex color: " + value);
					}
					try {
						return DataResult.success((int) Long.parseLong(value.substring(1), 16));
					} catch (NumberFormatException e) {
						return DataResult.error(() -> "Not a hex color: " + value);
					}
				},
				color -> String.format("#%0" + hexDigits + "X", color)
		);
	}

	/**
	 * Создаёт кодек для объекта, представляющего пару значений одного типа.
	 * Поддерживает три формата декодирования:
	 * <ul>
	 *   <li>Одиночное значение — оба элемента пары равны этому значению</li>
	 *   <li>Список из двух элементов — {@code [min, max]}</li>
	 *   <li>Объект с именованными полями — {@code {"min": ..., "max": ...}}</li>
	 * </ul>
	 */
	public static <P, I> Codec<I> createCodecForPairObject(
			Codec<P> codec,
			String leftFieldName,
			String rightFieldName,
			BiFunction<P, P, DataResult<I>> combineFunction,
			Function<I, P> leftFunction,
			Function<I, P> rightFunction
	) {
		Codec<I> listCodec = Codec.list(codec).comapFlatMap(
				list -> Util.decodeFixedLengthList(list, 2).flatMap(l -> combineFunction.apply(l.get(0), l.get(1))),
				pair -> ImmutableList.of(leftFunction.apply(pair), rightFunction.apply(pair))
		);

		Codec<I> recordCodec = RecordCodecBuilder.<Pair<P, P>>create(
				instance -> instance.group(
						codec.fieldOf(leftFieldName).forGetter(Pair::getFirst),
						codec.fieldOf(rightFieldName).forGetter(Pair::getSecond)
				).apply(instance, Pair::of)
		).comapFlatMap(
				pair -> combineFunction.apply(pair.getFirst(), pair.getSecond()),
				pair -> Pair.of(leftFunction.apply(pair), rightFunction.apply(pair))
		);

		Codec<I> pairCodec = Codec.withAlternative(listCodec, recordCodec);

		return Codec.either(codec, pairCodec)
				.comapFlatMap(
						either -> either.map(
								value -> combineFunction.apply(value, value),
								DataResult::success
						),
						pair -> {
							P left = leftFunction.apply(pair);
							P right = rightFunction.apply(pair);
							return Objects.equals(left, right) ? Either.left(left) : Either.right(pair);
						}
				);
	}

	/**
	 * Создаёт {@link com.mojang.serialization.Codec.ResultFunction}, которая при ошибке декодирования
	 * возвращает частичный результат с переданным объектом-заглушкой.
	 * Используется для graceful degradation при чтении устаревших данных.
	 *
	 * @param object значение-заглушка, возвращаемое при ошибке
	 * @return функция-обработчик результата
	 */
	public static <A> Codec.ResultFunction<A> orElsePartial(A object) {
		return new Codec.ResultFunction<>() {
			@Override
			public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> result) {
				Optional<Pair<A, T>> partialResult = result.result();

				if (partialResult.isPresent()) {
					return result;
				}

				String message = result.error()
						.map(e -> "(" + e.message() + " -> using default)")
						.orElse("(unknown error -> using default)");

				return DataResult.error(() -> message, Pair.of(object, input));
			}

			@Override
			public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> result) {
				return result;
			}

			@Override
			public String toString() {
				return "OrElsePartial[" + object + "]";
			}
		};
	}

	/**
	 * Создаёт кодек, который кодирует/декодирует элементы через их числовой идентификатор.
	 *
	 * @param idToElement функция получения элемента по числовому id
	 * @param elementToId функция получения числового id по элементу
	 * @return кодек с валидацией диапазона id
	 */
	public static <E> Codec<E> rawIdChecked(
			ToIntFunction<E> elementToId,
			java.util.function.IntFunction<E> idToElement,
			int errorId
	) {
		return Codec.INT.flatXmap(
				id -> {
					E element = idToElement.apply(id);
					return element == null
							? DataResult.error(() -> "Unknown element id: " + id)
							: DataResult.success(element);
				},
				element -> {
					int id = elementToId.applyAsInt(element);
					return id == errorId
							? DataResult.error(() -> "Element with unknown id: " + element)
							: DataResult.success(id);
				}
		);
	}

	public static <E> Codec<E> rawIdChecked(
			Function<Integer, Optional<E>> idToElement,
			Function<E, Integer> elementToId,
			int errorId
	) {
		return Codec.INT.flatXmap(
				id -> idToElement.apply(id)
						.map(DataResult::success)
						.orElseGet(() -> DataResult.error(() -> "Unknown element id: " + id)),
				element -> {
					int id = elementToId.apply(element);
					return id == errorId
							? DataResult.error(() -> "Element with unknown id: " + element)
							: DataResult.success(id);
				}
		);
	}

	/**
	 * Создаёт кодек, который кодирует/декодирует элементы через строковый идентификатор.
	 *
	 * @param idCodec         кодек для типа идентификатора
	 * @param idToElement     функция получения элемента по id
	 * @param elementToId     функция получения id по элементу
	 * @return кодек с валидацией существования элемента
	 */
	public static <I, E> Codec<E> idChecked(
			Codec<I> idCodec,
			Function<I, E> idToElement,
			Function<E, I> elementToId
	) {
		return idCodec.flatXmap(
				id -> {
					E element = idToElement.apply(id);
					return element == null
							? DataResult.error(() -> "Unknown element: " + id)
							: DataResult.success(element);
				},
				element -> {
					I id = elementToId.apply(element);
					return id == null
							? DataResult.error(() -> "Element with unknown id: " + element)
							: DataResult.success(id);
				}
		);
	}

	/**
	 * Создаёт кодек, который использует сжатый формат при наличии сжатия в ops,
	 * и несжатый — в противном случае.
	 */
	public static <E> Codec<E> orCompressed(Codec<E> uncompressedCodec, Codec<E> compressedCodec) {
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
				return ops.compressMaps()
						? compressedCodec.decode(ops, input)
						: uncompressedCodec.decode(ops, input);
			}

			@Override
			public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
				return ops.compressMaps()
						? compressedCodec.encode(input, ops, prefix)
						: uncompressedCodec.encode(input, ops, prefix);
			}

			@Override
			public String toString() {
				return uncompressedCodec + " orCompressed " + compressedCodec;
			}
		};
	}

	/**
	 * Создаёт MapCodec, который использует сжатый формат при наличии сжатия в ops.
	 */
	public static <E> MapCodec<E> orCompressed(MapCodec<E> uncompressedCodec, MapCodec<E> compressedCodec) {
		return new MapCodec<>() {
			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return ops.compressMaps()
						? compressedCodec.keys(ops)
						: uncompressedCodec.keys(ops);
			}

			@Override
			public <T> DataResult<E> decode(DynamicOps<T> ops, MapLike<T> input) {
				return ops.compressMaps()
						? compressedCodec.decode(ops, input)
						: uncompressedCodec.decode(ops, input);
			}

			@Override
			public <T> RecordBuilder<T> encode(E input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				return ops.compressMaps()
						? compressedCodec.encode(input, ops, prefix)
						: uncompressedCodec.encode(input, ops, prefix);
			}

			@Override
			public String toString() {
				return uncompressedCodec + " orCompressed " + compressedCodec;
			}
		};
	}

	/**
	 * Оборачивает кодек, добавляя к результатам декодирования/кодирования
	 * указанный {@link Lifecycle}.
	 */
	public static <E> Codec<E> withLifecycle(Codec<E> originalCodec, Function<E, Lifecycle> decodeLifecycle, Function<E, Lifecycle> encodeLifecycle) {
		return originalCodec.mapResult(new Codec.ResultFunction<>() {
			@Override
			public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<E, T>> result) {
				return result.result()
						.map(pair -> result.setLifecycle(decodeLifecycle.apply(pair.getFirst())))
						.orElse(result);
			}

			@Override
			public <T> DataResult<T> coApply(DynamicOps<T> ops, E input, DataResult<T> result) {
				return result.setLifecycle(encodeLifecycle.apply(input));
			}

			@Override
			public String toString() {
				return "WithLifecycle[" + decodeLifecycle + " " + encodeLifecycle + "]";
			}
		});
	}

	public static <K, V> StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> keyCodec, Codec<V> elementCodec) {
		return new StrictUnboundedMapCodec<>(keyCodec, elementCodec);
	}

	public static <E> Codec<List<E>> listOrSingle(Codec<E> entryCodec) {
		return listOrSingle(entryCodec, entryCodec.listOf());
	}

	public static <E> Codec<List<E>> listOrSingle(Codec<E> entryCodec, Codec<List<E>> listCodec) {
		return Codec.withAlternative(listCodec, entryCodec.xmap(List::of, List::getFirst));
	}

	private static Codec<Integer> rangedInt(int min, int max, Function<Integer, String> messageFactory) {
		return Codec.INT.validate(
				value -> value >= min && value <= max
						? DataResult.success(value)
						: DataResult.error(() -> messageFactory.apply(value) + " [" + min + ", " + max + "]")
		);
	}

	private static Codec<Long> rangedLong(long min, long max, Function<Long, String> messageFactory) {
		return Codec.LONG.validate(
				value -> value >= min && value <= max
						? DataResult.success(value)
						: DataResult.error(() -> messageFactory.apply(value) + " [" + min + ", " + max + "]")
		);
	}

	private static Codec<Float> rangedInclusiveFloatInternal(float min, float max) {
		return Codec.FLOAT.validate(
				value -> value >= min && value <= max
						? DataResult.success(value)
						: DataResult.error(() -> "Value " + value + " outside of range [" + min + ":" + max + "]")
		);
	}

	private static Codec<Float> rangedFloat(float min, float max) {
		return Codec.FLOAT.validate(
				value -> value >= min && value < max
						? DataResult.success(value)
						: DataResult.error(() -> "Value " + value + " outside of range [" + min + ":" + max + ")")
		);
	}

	public static Codec<Integer> rangedInt(int minInclusive, int maxInclusive) {
		return rangedInt(minInclusive, maxInclusive, i -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: ");
	}

	public static Codec<Long> rangedLong(long minInclusive, long maxInclusive) {
		return rangedLong(minInclusive, maxInclusive, l -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: ");
	}

	public static Codec<Float> rangedInclusiveFloat(float minInclusive, float maxInclusive) {
		return rangedInclusiveFloatInternal(minInclusive, maxInclusive);
	}

	public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> originalCodec) {
		return originalCodec.validate(
				list -> list.isEmpty()
						? DataResult.error(() -> "List must have contents")
						: DataResult.success(list)
		);
	}

	public static <T> Codec<RegistryEntryList<T>> nonEmptyEntryList(Codec<RegistryEntryList<T>> originalCodec) {
		return originalCodec.validate(
				entryList -> entryList.getStorage().right().filter(List::isEmpty).isPresent()
						? DataResult.error(() -> "List must have contents")
						: DataResult.success(entryList)
		);
	}

	public static <M extends Map<?, ?>> Codec<M> nonEmptyMap(Codec<M> originalCodec) {
		return originalCodec.validate(
				map -> map.isEmpty()
						? DataResult.error(() -> "Map must have contents")
						: DataResult.success(map)
		);
	}

	/**
	 * Создаёт MapCodec, который извлекает значение из контекста {@link DynamicOps},
	 * а не из входных данных. Используется для передачи контекстных зависимостей
	 * (например, реестров) через ops при декодировании.
	 *
	 * @param retriever функция, извлекающая значение из ops
	 * @return MapCodec, не читающий и не записывающий никаких полей
	 */
	public static <E> MapCodec<E> createContextRetrievalCodec(Function<DynamicOps<?>, DataResult<E>> retriever) {
		class ContextRetrievalCodec extends MapCodec<E> {
			@Override
			public <T> RecordBuilder<T> encode(E input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				return prefix;
			}

			@Override
			public <T> DataResult<E> decode(DynamicOps<T> ops, MapLike<T> input) {
				return retriever.apply(ops);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return Stream.empty();
			}

			@Override
			public String toString() {
				return "ContextRetrievalCodec[" + retriever + "]";
			}
		}

		return new ContextRetrievalCodec();
	}

	/**
	 * Создаёт валидатор коллекции, проверяющий, что все элементы имеют одинаковый тип.
	 * Используется для предотвращения смешивания разных типов в одном списке.
	 *
	 * @param typeGetter функция получения типа из элемента
	 * @return функция-валидатор, возвращающая ошибку при обнаружении смешанных типов
	 */
	public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> createEqualTypeChecker(Function<E, T> typeGetter) {
		return collection -> {
			Iterator<E> iterator = collection.iterator();
			if (iterator.hasNext()) {
				T firstType = typeGetter.apply(iterator.next());

				while (iterator.hasNext()) {
					E element = iterator.next();
					T elementType = typeGetter.apply(element);
					if (elementType != firstType) {
						return DataResult.error(
								() -> "Mixed type list: element " + element + " had type " + elementType
										+ ", but list is of type " + firstType
						);
					}
				}
			}

			return DataResult.success(collection, Lifecycle.stable());
			};
		}
	
		public static <A> Codec<A> exceptionCatching(Codec<A> codec) {
			return Codec.of(
					codec,
					new Decoder<>() {
						@Override
						public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
							try {
								return codec.decode(ops, input);
							} catch (Exception e) {
								return DataResult.error(() -> "Caught exception decoding " + input + ": " + e.getMessage());
							}
						}
					}
			);
		}
	
		public static Codec<TemporalAccessor> formattedTime(DateTimeFormatter formatter) {
			return Codec.STRING.comapFlatMap(
					string -> {
						try {
							return DataResult.success(formatter.parse(string));
						} catch (Exception e) {
							return DataResult.error(e::getMessage);
						}
					},
					formatter::format
			);
		}
	
		public static MapCodec<OptionalLong> optionalLong(MapCodec<Optional<Long>> codec) {
			return codec.xmap(OPTIONAL_OF_LONG_TO_OPTIONAL_LONG, OPTIONAL_LONG_TO_OPTIONAL_OF_LONG);
		}
	
		public static <K, V> Codec<Map<K, V>> map(Codec<Map<K, V>> codec, int maxLength) {
			return codec.validate(
					map -> map.size() > maxLength
							? DataResult.error(() -> "Map is too long: " + map.size() + ", expected range [0-" + maxLength + "]")
							: DataResult.success(map)
			);
		}
	
		public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> keyCodec) {
			return Codec.unboundedMap(keyCodec, Codec.BOOL)
					.xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
		}
	
		/**
			* @deprecated Используй {@link com.mojang.serialization.codecs.KeyDispatchCodec} напрямую.
			*/
		@Deprecated
		public static <K, V> MapCodec<V> parameters(
				String typeKey,
				String parametersKey,
				Codec<K> typeCodec,
				Function<? super V, ? extends K> typeGetter,
				Function<? super K, ? extends Codec<? extends V>> parametersCodecGetter
		) {
			return new MapCodec<>() {
				@Override
				public <T> Stream<T> keys(DynamicOps<T> ops) {
					return Stream.of(ops.createString(typeKey), ops.createString(parametersKey));
				}
	
				@SuppressWarnings("unchecked")
				@Override
				public <T> DataResult<V> decode(DynamicOps<T> ops, MapLike<T> input) {
					T typeValue = input.get(typeKey);
					if (typeValue == null) {
						return DataResult.error(() -> "Missing \"" + typeKey + "\" in: " + input);
					}
	
					return typeCodec.decode(ops, typeValue).flatMap(pair -> {
						T paramsValue = Objects.requireNonNullElseGet(
								(T) input.get(parametersKey),
								ops::emptyMap
						);
						return parametersCodecGetter
								.apply((K) pair.getFirst())
								.decode(ops, paramsValue)
								.map(Pair::getFirst);
					});
				}
	
				@SuppressWarnings("unchecked")
				@Override
				public <T> RecordBuilder<T> encode(V input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
					K type = (K) typeGetter.apply(input);
					prefix.add(typeKey, typeCodec.encodeStart(ops, type));
					DataResult<T> dataResult = encodeValue(parametersCodecGetter.apply(type), input, ops);
					if (dataResult.result().isEmpty() || !Objects.equals(dataResult.result().get(), ops.emptyMap())) {
						prefix.add(parametersKey, dataResult);
					}
					return prefix;
				}
	
				@SuppressWarnings("unchecked")
				private <T, V2 extends V> DataResult<T> encodeValue(Codec<V2> codec, V value, DynamicOps<T> ops) {
					return codec.encodeStart(ops, (V2) value);
				}
			};
		}
	
		/**
			* Создаёт кодек для {@link Optional}, который при декодировании пустой карты
			* возвращает {@link Optional#empty()}, а при кодировании пустого Optional
			* записывает пустую карту.
			*
			* @param codec кодек для внутреннего типа
			* @return кодек для Optional
			*/
		public static <A> Codec<Optional<A>> optional(Codec<A> codec) {
			return new Codec<>() {
				@Override
				public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> ops, T input) {
					return isEmpty(ops, input)
							? DataResult.success(Pair.of(Optional.empty(), input))
							: codec.decode(ops, input).map(pair -> pair.mapFirst(Optional::of));
				}
	
				private static <T> boolean isEmpty(DynamicOps<T> ops, T input) {
					Optional<MapLike<T>> optional = ops.getMap(input).result();
					return optional.isPresent() && optional.get().entries().findAny().isEmpty();
				}
	
				@Override
				public <T> DataResult<T> encode(Optional<A> optional, DynamicOps<T> dynamicOps, T object) {
					return optional.isEmpty()
							? DataResult.success(dynamicOps.emptyMap())
							: codec.encode(optional.get(), dynamicOps, object);
				}
			};
		}
	
		/**
			* @deprecated Используй {@link StringIdentifiable#createCodec} или {@link Codec#STRING} с xmap.
			*/
		@Deprecated
		public static <E extends Enum<E>> Codec<E> enumByName(Function<String, E> valueOf) {
			return Codec.STRING.comapFlatMap(
					id -> {
						try {
							return DataResult.success(valueOf.apply(id));
						} catch (IllegalArgumentException e) {
							return DataResult.error(() -> "No value with id: " + id);
						}
					},
					Enum::toString
			);
		}
	
		/**
			* Двунаправленный маппер идентификаторов на значения.
			* Позволяет создавать кодеки для произвольных типов через явную регистрацию пар id→value.
			*/
		public static class IdMapper<I, V> {
	
			private final com.google.common.collect.BiMap<I, V> values = com.google.common.collect.HashBiMap.create();
	
			public Codec<V> getCodec(Codec<I> idCodec) {
				com.google.common.collect.BiMap<V, I> inverse = values.inverse();
				return idChecked(idCodec, values::get, inverse::get);
			}
	
			public IdMapper<I, V> put(I id, V value) {
				Objects.requireNonNull(value, () -> "Value for " + id + " is null");
				values.put(id, value);
				return this;
			}
	
			public Set<V> values() {
				return Collections.unmodifiableSet(values.values());
			}
		}
	
		/**
			* Строгий кодек для {@link Map}, который при декодировании отклоняет
			* любые записи с ошибками (в отличие от стандартного unboundedMap,
			* который их игнорирует).
			*/
		public record StrictUnboundedMapCodec<K, V>(
				Codec<K> keyCodec,
				Codec<V> elementCodec
		) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
	
			@Override
			public <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
				ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
	
				for (Pair<T, T> pair : input.entries().toList()) {
					DataResult<K> keyResult = keyCodec().parse(ops, pair.getFirst());
					DataResult<V> valueResult = elementCodec().parse(ops, pair.getSecond());
					DataResult<Pair<K, V>> entryResult = keyResult.apply2stable(Pair::of, valueResult);
					Optional<DataResult.Error<Pair<K, V>>> error = entryResult.error();
	
					if (error.isPresent()) {
						String message = error.get().message();
						return DataResult.error(
								() -> keyResult.result().isPresent()
										? "Map entry '" + keyResult.result().get() + "' : " + message
										: message
						);
					}
	
					if (entryResult.result().isEmpty()) {
						return DataResult.error(() -> "Empty or invalid map contents are not allowed");
					}
	
					Pair<K, V> entry = entryResult.result().get();
					builder.put(entry.getFirst(), entry.getSecond());
				}
	
				return DataResult.success(builder.build());
			}
	
			@Override
			public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
				return ops.getMap(input)
						.setLifecycle(Lifecycle.stable())
						.flatMap(map -> decode(ops, map))
						.map(map -> Pair.of(map, input));
			}
	
			@Override
			public <T> DataResult<T> encode(Map<K, V> map, DynamicOps<T> dynamicOps, T object) {
				return encode(map, dynamicOps, dynamicOps.mapBuilder()).build(object);
			}
	
			@Override
			public String toString() {
				return "StrictUnboundedMapCodec[" + keyCodec + " -> " + elementCodec + "]";
			}
		}
	
		/**
			* Идентификатор тега или обычного элемента реестра.
			* Строки, начинающиеся с {@code #}, считаются тегами.
			*/
		public record TagEntryId(Identifier id, boolean tag) {
	
			@Override
			public String toString() {
				return asString();
			}
	
			private String asString() {
				return tag ? "#" + id : id.toString();
			}
		}
	}
