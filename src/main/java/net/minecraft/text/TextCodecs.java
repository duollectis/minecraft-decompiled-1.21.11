package net.minecraft.text;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.dynamic.Codecs;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Набор кодеков для сериализации {@link Text} в различные форматы:
 * JSON (через DFU), сетевые пакеты (с реестром и без), а также
 * вспомогательные методы для построения диспетчеризирующих кодеков
 * текстовых компонентов.
 */
public class TextCodecs {

	public static final Codec<Text> CODEC = Codec.recursive("Component", TextCodecs::createCodec);
	public static final PacketCodec<RegistryByteBuf, Text> REGISTRY_PACKET_CODEC =
		PacketCodecs.registryCodec(CODEC);
	public static final PacketCodec<RegistryByteBuf, Optional<Text>> OPTIONAL_PACKET_CODEC =
		REGISTRY_PACKET_CODEC.collect(PacketCodecs::optional);
	public static final PacketCodec<RegistryByteBuf, Text> UNLIMITED_REGISTRY_PACKET_CODEC =
		PacketCodecs.unlimitedRegistryCodec(CODEC);
	public static final PacketCodec<RegistryByteBuf, Optional<Text>> OPTIONAL_UNLIMITED_REGISTRY_PACKET_CODEC =
		UNLIMITED_REGISTRY_PACKET_CODEC.collect(PacketCodecs::optional);
	public static final PacketCodec<ByteBuf, Text> PACKET_CODEC = PacketCodecs.unlimitedCodec(CODEC);

	/**
	 * Создаёт кодек с ограничением на максимальный размер JSON-представления текста.
	 * Используется для защиты от слишком больших текстовых компонентов в сетевых пакетах.
	 *
	 * @param maxLength максимально допустимый размер JSON в символах
	 */
	public static Codec<Text> withJsonLengthLimit(int maxLength) {
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<Text, T>> decode(DynamicOps<T> ops, T value) {
				return TextCodecs.CODEC
					.decode(ops, value)
					.flatMap(pair -> isTooLarge(ops, pair.getFirst())
						? DataResult.error(() -> "Component was too large: greater than max size " + maxLength)
						: DataResult.success(pair)
					);
			}

			@Override
			public <T> DataResult<T> encode(Text text, DynamicOps<T> dynamicOps, T object) {
				return TextCodecs.CODEC.encodeStart(dynamicOps, text);
			}

			private <T> boolean isTooLarge(DynamicOps<T> ops, Text text) {
				DataResult<JsonElement> result = TextCodecs.CODEC.encodeStart(toJsonOps(ops), text);
				return result.isSuccess() && JsonHelper.isTooLarge(result.getOrThrow(), maxLength);
			}

			private static <T> DynamicOps<JsonElement> toJsonOps(DynamicOps<T> ops) {
				return ops instanceof RegistryOps<T> registryOps
					? registryOps.withDelegate(JsonOps.INSTANCE)
					: JsonOps.INSTANCE;
			}
		};
	}

	/**
	 * Объединяет список текстовых компонентов в один {@link MutableText},
	 * последовательно добавляя все элементы начиная со второго к копии первого.
	 */
	private static MutableText combine(List<Text> texts) {
		MutableText result = texts.get(0).copy();

		for (int index = 1; index < texts.size(); index++) {
			result.append(texts.get(index));
		}

		return result;
	}

	/**
	 * Создаёт диспетчеризирующий {@link MapCodec}, который при декодировании
	 * сначала пробует «нечёткий» кодек (без ключа типа), а при наличии ключа —
	 * стандартный диспетчеризирующий. При кодировании всегда использует нечёткий.
	 *
	 * @param idMapper     маппер идентификаторов типов на кодеки
	 * @param typeToCodec  функция получения кодека по экземпляру
	 * @param typeKey      имя поля-дискриминатора типа
	 */
	public static <T> MapCodec<T> dispatchingCodec(
		Codecs.IdMapper<String, MapCodec<? extends T>> idMapper,
		Function<T, MapCodec<? extends T>> typeToCodec,
		String typeKey
	) {
		MapCodec<T> fuzzyCodec = new FuzzyCodec<>(idMapper.values(), typeToCodec);
		MapCodec<T> withKeyCodec = idMapper.getCodec(Codec.STRING).dispatchMap(typeKey, typeToCodec, codec -> codec);
		MapCodec<T> dispatchingCodec = new DispatchingCodec<>(typeKey, withKeyCodec, fuzzyCodec);
		return Codecs.orCompressed(dispatchingCodec, withKeyCodec);
	}

	private static Codec<Text> createCodec(Codec<Text> selfCodec) {
		Codecs.IdMapper<String, MapCodec<? extends TextContent>> idMapper = new Codecs.IdMapper<>();
		registerTypes(idMapper);

		MapCodec<TextContent> contentCodec = dispatchingCodec(idMapper, TextContent::getCodec, "type");
		Codec<Text> fullCodec = RecordCodecBuilder.create(
			instance -> instance.group(
				contentCodec.forGetter(Text::getContent),
				Codecs.nonEmptyList(selfCodec.listOf())
					.optionalFieldOf("extra", List.of())
					.forGetter(Text::getSiblings),
				Style.Codecs.MAP_CODEC.forGetter(Text::getStyle)
			).apply(instance, MutableText::new)
		);

		return Codec.either(Codec.either(Codec.STRING, Codecs.nonEmptyList(selfCodec.listOf())), fullCodec)
			.xmap(
				either -> (Text) either.map(
					inner -> (Text) inner.map(Text::literal, TextCodecs::combine),
					text -> text
				),
				text -> {
					String literal = text.getLiteralString();
					return literal != null ? Either.left(Either.left(literal)) : Either.right(text);
				}
			);
	}

	private static void registerTypes(Codecs.IdMapper<String, MapCodec<? extends TextContent>> idMapper) {
		idMapper.put("text", PlainTextContent.CODEC);
		idMapper.put("translatable", TranslatableTextContent.CODEC);
		idMapper.put("keybind", KeybindTextContent.CODEC);
		idMapper.put("score", ScoreTextContent.CODEC);
		idMapper.put("selector", SelectorTextContent.CODEC);
		idMapper.put("nbt", NbtTextContent.CODEC);
		idMapper.put("object", ObjectTextContent.CODEC);
	}

	/**
	 * Кодек, выбирающий стратегию декодирования по наличию поля-дискриминатора:
	 * если поле присутствует — использует {@code withKeyCodec}, иначе — {@code withoutKeyCodec}.
	 * При кодировании всегда использует {@code withoutKeyCodec}.
	 */
	static class DispatchingCodec<T> extends MapCodec<T> {

		private final String dispatchingKey;
		private final MapCodec<T> withKeyCodec;
		private final MapCodec<T> withoutKeyCodec;

		DispatchingCodec(String dispatchingKey, MapCodec<T> withKeyCodec, MapCodec<T> withoutKeyCodec) {
			this.dispatchingKey = dispatchingKey;
			this.withKeyCodec = withKeyCodec;
			this.withoutKeyCodec = withoutKeyCodec;
		}

		@Override
		public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
			return input.get(dispatchingKey) != null
				? withKeyCodec.decode(ops, input)
				: withoutKeyCodec.decode(ops, input);
		}

		@Override
		public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
			return withoutKeyCodec.encode(input, ops, prefix);
		}

		@Override
		public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
			return Stream.concat(withKeyCodec.keys(ops), withoutKeyCodec.keys(ops)).distinct();
		}
	}

	/**
	 * Кодек, перебирающий все зарегистрированные кодеки при декодировании
	 * и возвращающий первый успешный результат («нечёткое» сопоставление без ключа типа).
	 * При кодировании делегирует кодеку, полученному через {@code codecGetter}.
	 */
	static class FuzzyCodec<T> extends MapCodec<T> {

		private final Collection<MapCodec<? extends T>> codecs;
		private final Function<T, ? extends MapEncoder<? extends T>> codecGetter;

		FuzzyCodec(
			Collection<MapCodec<? extends T>> codecs,
			Function<T, ? extends MapEncoder<? extends T>> codecGetter
		) {
			this.codecs = codecs;
			this.codecGetter = codecGetter;
		}

		@Override
		public <S> DataResult<T> decode(DynamicOps<S> ops, MapLike<S> input) {
			for (MapDecoder<? extends T> decoder : codecs) {
				DataResult<? extends T> result = decoder.decode(ops, input);

				if (result.result().isPresent()) {
					return (DataResult<T>) result;
				}
			}

			return DataResult.error(() -> "No matching codec found");
		}

		@Override
		public <S> RecordBuilder<S> encode(T input, DynamicOps<S> ops, RecordBuilder<S> prefix) {
			MapEncoder<T> encoder = (MapEncoder<T>) codecGetter.apply(input);
			return encoder.encode(input, ops, prefix);
		}

		@Override
		public <S> Stream<S> keys(DynamicOps<S> ops) {
			return codecs.stream().flatMap(codec -> codec.keys(ops)).distinct();
		}

		@Override
		public String toString() {
			return "FuzzyCodec[" + codecs + "]";
		}
	}
}
