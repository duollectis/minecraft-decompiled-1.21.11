package net.minecraft.text;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.dynamic.Codecs;

public class TextCodecs {
   public static final Codec<Text> CODEC = Codec.recursive("Component", TextCodecs::createCodec);
   public static final PacketCodec<RegistryByteBuf, Text> REGISTRY_PACKET_CODEC = PacketCodecs.registryCodec(CODEC);
   public static final PacketCodec<RegistryByteBuf, Optional<Text>> OPTIONAL_PACKET_CODEC = REGISTRY_PACKET_CODEC.collect(PacketCodecs::optional);
   public static final PacketCodec<RegistryByteBuf, Text> UNLIMITED_REGISTRY_PACKET_CODEC = PacketCodecs.unlimitedRegistryCodec(CODEC);
   public static final PacketCodec<RegistryByteBuf, Optional<Text>> OPTIONAL_UNLIMITED_REGISTRY_PACKET_CODEC = UNLIMITED_REGISTRY_PACKET_CODEC.collect(
      PacketCodecs::optional
   );
   public static final PacketCodec<ByteBuf, Text> PACKET_CODEC = PacketCodecs.unlimitedCodec(CODEC);

   public static Codec<Text> withJsonLengthLimit(int maxLength) {
      return new Codec<Text>() {
         public <T> DataResult<Pair<Text, T>> decode(DynamicOps<T> ops, T value) {
            return TextCodecs.CODEC
               .decode(ops, value)
               .flatMap(
                  pair -> this.isTooLarge(ops, (Text)pair.getFirst())
                     ? DataResult.error(() -> "Component was too large: greater than max size " + maxLength)
                     : DataResult.success(pair)
               );
         }

         public <T> DataResult<T> encode(Text text, DynamicOps<T> dynamicOps, T object) {
            return TextCodecs.CODEC.encodeStart(dynamicOps, text);
         }

         private <T> boolean isTooLarge(DynamicOps<T> ops, Text text) {
            DataResult<JsonElement> dataResult = TextCodecs.CODEC.encodeStart(toJsonOps(ops), text);
            return dataResult.isSuccess() && JsonHelper.isTooLarge((JsonElement)dataResult.getOrThrow(), maxLength);
         }

         private static <T> DynamicOps<JsonElement> toJsonOps(DynamicOps<T> ops) {
            return (DynamicOps<JsonElement>)(ops instanceof RegistryOps<T> registryOps ? registryOps.withDelegate(JsonOps.INSTANCE) : JsonOps.INSTANCE);
         }
      };
   }

   private static MutableText combine(List<Text> texts) {
      MutableText mutableText = texts.get(0).copy();

      for (int i = 1; i < texts.size(); i++) {
         mutableText.append(texts.get(i));
      }

      return mutableText;
   }

   public static <T> MapCodec<T> dispatchingCodec(
      Codecs.IdMapper<String, MapCodec<? extends T>> idMapper, Function<T, MapCodec<? extends T>> typeToCodec, String typeKey
   ) {
      MapCodec<T> mapCodec = new TextCodecs.FuzzyCodec<>(idMapper.values(), typeToCodec);
      MapCodec<T> mapCodec2 = idMapper.getCodec(Codec.STRING).dispatchMap(typeKey, typeToCodec, codec -> codec);
      MapCodec<T> mapCodec3 = new TextCodecs.DispatchingCodec<>(typeKey, mapCodec2, mapCodec);
      return Codecs.orCompressed(mapCodec3, mapCodec2);
   }

   private static Codec<Text> createCodec(Codec<Text> selfCodec) {
      Codecs.IdMapper<String, MapCodec<? extends TextContent>> idMapper = new Codecs.IdMapper<>();
      registerTypes(idMapper);
      MapCodec<TextContent> mapCodec = dispatchingCodec(idMapper, TextContent::getCodec, "type");
      Codec<Text> codec = RecordCodecBuilder.create(
         instance -> instance.group(
               mapCodec.forGetter(Text::getContent),
               Codecs.nonEmptyList(selfCodec.listOf()).optionalFieldOf("extra", List.of()).forGetter(Text::getSiblings),
               Style.Codecs.MAP_CODEC.forGetter(Text::getStyle)
            )
            .apply(instance, MutableText::new)
      );
      return Codec.either(Codec.either(Codec.STRING, Codecs.nonEmptyList(selfCodec.listOf())), codec)
         .xmap(either -> (Text)either.map(either2 -> (Text)either2.map(Text::literal, TextCodecs::combine), text -> text), text -> {
            String string = text.getLiteralString();
            return string != null ? Either.left(Either.left(string)) : Either.right(text);
         });
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

   static class DispatchingCodec<T> extends MapCodec<T> {
      private final String dispatchingKey;
      private final MapCodec<T> withKeyCodec;
      private final MapCodec<T> withoutKeyCodec;

      public DispatchingCodec(String dispatchingKey, MapCodec<T> withKeyCodec, MapCodec<T> withoutKeyCodec) {
         this.dispatchingKey = dispatchingKey;
         this.withKeyCodec = withKeyCodec;
         this.withoutKeyCodec = withoutKeyCodec;
      }

      public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
         return input.get(this.dispatchingKey) != null ? this.withKeyCodec.decode(ops, input) : this.withoutKeyCodec.decode(ops, input);
      }

      public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
         return this.withoutKeyCodec.encode(input, ops, prefix);
      }

      public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
         return Stream.concat(this.withKeyCodec.keys(ops), this.withoutKeyCodec.keys(ops)).distinct();
      }
   }

   static class FuzzyCodec<T> extends MapCodec<T> {
      private final Collection<MapCodec<? extends T>> codecs;
      private final Function<T, ? extends MapEncoder<? extends T>> codecGetter;

      public FuzzyCodec(Collection<MapCodec<? extends T>> codecs, Function<T, ? extends MapEncoder<? extends T>> codecGetter) {
         this.codecs = codecs;
         this.codecGetter = codecGetter;
      }

      public <S> DataResult<T> decode(DynamicOps<S> ops, MapLike<S> input) {
         for (MapDecoder<? extends T> mapDecoder : this.codecs) {
            DataResult<? extends T> dataResult = mapDecoder.decode(ops, input);
            if (dataResult.result().isPresent()) {
               return (DataResult<T>)dataResult;
            }
         }

         return DataResult.error(() -> "No matching codec found");
      }

      public <S> RecordBuilder<S> encode(T input, DynamicOps<S> ops, RecordBuilder<S> prefix) {
         MapEncoder<T> mapEncoder = (MapEncoder<T>)this.codecGetter.apply(input);
         return mapEncoder.encode(input, ops, prefix);
      }

      public <S> Stream<S> keys(DynamicOps<S> ops) {
         return this.codecs.stream().flatMap(codec -> codec.keys(ops)).distinct();
      }

      public String toString() {
         return "FuzzyCodec[" + this.codecs + "]";
      }
   }
}
