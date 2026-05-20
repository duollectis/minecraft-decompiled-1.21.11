package net.minecraft.predicate;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public interface NumberRange<T extends Number & Comparable<T>> {
   SimpleCommandExceptionType EXCEPTION_EMPTY = new SimpleCommandExceptionType(Text.translatable("argument.range.empty"));
   SimpleCommandExceptionType EXCEPTION_SWAPPED = new SimpleCommandExceptionType(Text.translatable("argument.range.swapped"));

   NumberRange.Bounds<T> bounds();

   default Optional<T> getMin() {
      return this.bounds().min;
   }

   default Optional<T> getMax() {
      return this.bounds().max;
   }

   default boolean isDummy() {
      return this.bounds().isAny();
   }

   public record AngleRange(NumberRange.Bounds<Float> bounds) implements NumberRange<Float> {
      public static final NumberRange.AngleRange ANY = new NumberRange.AngleRange(NumberRange.Bounds.any());
      public static final Codec<NumberRange.AngleRange> CODEC = NumberRange.Bounds.createCodec(Codec.FLOAT)
         .xmap(NumberRange.AngleRange::new, NumberRange.AngleRange::bounds);
      public static final PacketCodec<ByteBuf, NumberRange.AngleRange> PACKET_CODEC = NumberRange.Bounds.createPacketCodec(PacketCodecs.FLOAT)
         .xmap(NumberRange.AngleRange::new, NumberRange.AngleRange::bounds);

      public static NumberRange.AngleRange parse(StringReader reader) throws CommandSyntaxException {
         NumberRange.Bounds<Float> bounds = NumberRange.Bounds.parse(reader, Float::parseFloat, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidFloat);
         return new NumberRange.AngleRange(bounds);
      }
   }

   public record Bounds<T extends Number & Comparable<T>>(Optional<T> min, Optional<T> max) {

      public boolean isAny() {
         return this.min().isEmpty() && this.max().isEmpty();
      }

      public DataResult<NumberRange.Bounds<T>> validate() {
         return this.isSwapped()
            ? DataResult.error(() -> "Swapped bounds in range: " + this.min() + " is higher than " + this.max())
            : DataResult.success(this);
      }

      public boolean isSwapped() {
         return this.min.isPresent() && this.max.isPresent() && this.min.get().compareTo(this.max.get()) > 0;
      }

      public Optional<T> getPoint() {
         Optional<T> optional = this.min();
         Optional<T> optional2 = this.max();
         return optional.equals(optional2) ? optional : Optional.empty();
      }

      public static <T extends Number & Comparable<T>> NumberRange.Bounds<T> any() {
         return new NumberRange.Bounds<T>(Optional.empty(), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> NumberRange.Bounds<T> exactly(T value) {
         Optional<T> optional = Optional.of(value);
         return new NumberRange.Bounds<>(optional, optional);
      }

      public static <T extends Number & Comparable<T>> NumberRange.Bounds<T> between(T min, T max) {
         return new NumberRange.Bounds<>(Optional.of(min), Optional.of(max));
      }

      public static <T extends Number & Comparable<T>> NumberRange.Bounds<T> atLeast(T value) {
         return new NumberRange.Bounds<>(Optional.of(value), Optional.empty());
      }

      public static <T extends Number & Comparable<T>> NumberRange.Bounds<T> atMost(T value) {
         return new NumberRange.Bounds<>(Optional.empty(), Optional.of(value));
      }

      public <U extends Number & Comparable<U>> NumberRange.Bounds<U> map(Function<T, U> mappingFunction) {
         return new NumberRange.Bounds<>(this.min.map(mappingFunction), this.max.map(mappingFunction));
      }

      static <T extends Number & Comparable<T>> Codec<NumberRange.Bounds<T>> createCodec(Codec<T> valueCodec) {
         Codec<NumberRange.Bounds<T>> codec = RecordCodecBuilder.create(
            instance -> instance.group(
                  valueCodec.optionalFieldOf("min").forGetter(NumberRange.Bounds::min), valueCodec.optionalFieldOf("max").forGetter(NumberRange.Bounds::max)
               )
               .apply(instance, NumberRange.Bounds::new)
         );
         return Codec.either(codec, valueCodec).xmap(either -> (NumberRange.Bounds)either.map(bounds -> bounds, value -> exactly((T)value)), bounds -> {
            Optional<T> optional = bounds.getPoint();
            return optional.isPresent() ? Either.right(optional.get()) : Either.left(bounds);
         });
      }

      static <B extends ByteBuf, T extends Number & Comparable<T>> PacketCodec<B, NumberRange.Bounds<T>> createPacketCodec(PacketCodec<B, T> valuePacketCodec) {
         return new PacketCodec<B, NumberRange.Bounds<T>>() {
            private static final int MIN_PRESENT_FLAG = 1;
            private static final int MAX_PRESENT_FLAG = 2;

            public NumberRange.Bounds<T> decode(B byteBuf) {
               byte b = byteBuf.readByte();
               Optional<T> optional = (b & 1) != 0 ? Optional.of(valuePacketCodec.decode(byteBuf)) : Optional.empty();
               Optional<T> optional2 = (b & 2) != 0 ? Optional.of(valuePacketCodec.decode(byteBuf)) : Optional.empty();
               return new NumberRange.Bounds<>(optional, optional2);
            }

            public void encode(B byteBuf, NumberRange.Bounds<T> bounds) {
               Optional<T> optional = bounds.min();
               Optional<T> optional2 = bounds.max();
               byteBuf.writeByte((optional.isPresent() ? 1 : 0) | (optional2.isPresent() ? 2 : 0));
               optional.ifPresent(min -> valuePacketCodec.encode(byteBuf, (T)min));
               optional2.ifPresent(max -> valuePacketCodec.encode(byteBuf, (T)max));
            }
         };
      }

      public static <T extends Number & Comparable<T>> NumberRange.Bounds<T> parse(
         StringReader reader, Function<String, T> parsingFunction, Supplier<DynamicCommandExceptionType> exceptionSupplier
      ) throws CommandSyntaxException {
         if (!reader.canRead()) {
            throw NumberRange.EXCEPTION_EMPTY.createWithContext(reader);
         } else {
            int i = reader.getCursor();

            try {
               Optional<T> optional = parseNumber(reader, parsingFunction, exceptionSupplier);
               Optional<T> optional2;
               if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
                  reader.skip();
                  reader.skip();
                  optional2 = parseNumber(reader, parsingFunction, exceptionSupplier);
               } else {
                  optional2 = optional;
               }

               if (optional.isEmpty() && optional2.isEmpty()) {
                  throw NumberRange.EXCEPTION_EMPTY.createWithContext(reader);
               } else {
                  return new NumberRange.Bounds<>(optional, optional2);
               }
            } catch (CommandSyntaxException var6) {
               reader.setCursor(i);
               throw new CommandSyntaxException(var6.getType(), var6.getRawMessage(), var6.getInput(), i);
            }
         }
      }

      private static <T extends Number> Optional<T> parseNumber(
         StringReader reader, Function<String, T> parsingFunction, Supplier<DynamicCommandExceptionType> exceptionSupplier
      ) throws CommandSyntaxException {
         int i = reader.getCursor();

         while (reader.canRead() && shouldSkip(reader)) {
            reader.skip();
         }

         String string = reader.getString().substring(i, reader.getCursor());
         if (string.isEmpty()) {
            return Optional.empty();
         } else {
            try {
               return Optional.of(parsingFunction.apply(string));
            } catch (NumberFormatException var6) {
               throw exceptionSupplier.get().createWithContext(reader, string);
            }
         }
      }

      private static boolean shouldSkip(StringReader reader) {
         char c = reader.peek();
         if ((c < '0' || c > '9') && c != '-') {
            return c != '.' ? false : !reader.canRead(2) || reader.peek(1) != '.';
         } else {
            return true;
         }
      }
   }

   public record DoubleRange(NumberRange.Bounds<Double> bounds, NumberRange.Bounds<Double> boundsSqr) implements NumberRange<Double> {
      public static final NumberRange.DoubleRange ANY = new NumberRange.DoubleRange(NumberRange.Bounds.any());
      public static final Codec<NumberRange.DoubleRange> CODEC = NumberRange.Bounds.createCodec(Codec.DOUBLE)
         .validate(NumberRange.Bounds::validate)
         .xmap(NumberRange.DoubleRange::new, NumberRange.DoubleRange::bounds);
      public static final PacketCodec<ByteBuf, NumberRange.DoubleRange> PACKET_CODEC = NumberRange.Bounds.createPacketCodec(PacketCodecs.DOUBLE)
         .xmap(NumberRange.DoubleRange::new, NumberRange.DoubleRange::bounds);

      private DoubleRange(NumberRange.Bounds<Double> bounds) {
         this(bounds, bounds.map(MathHelper::square));
      }

      public static NumberRange.DoubleRange exactly(double value) {
         return new NumberRange.DoubleRange(NumberRange.Bounds.exactly(value));
      }

      public static NumberRange.DoubleRange between(double min, double max) {
         return new NumberRange.DoubleRange(NumberRange.Bounds.between(min, max));
      }

      public static NumberRange.DoubleRange atLeast(double value) {
         return new NumberRange.DoubleRange(NumberRange.Bounds.atLeast(value));
      }

      public static NumberRange.DoubleRange atMost(double value) {
         return new NumberRange.DoubleRange(NumberRange.Bounds.atMost(value));
      }

      public boolean test(double value) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > value ? false : this.bounds.max.isEmpty() || !(this.bounds.max.get() < value);
      }

      public boolean testSqrt(double value) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > value
            ? false
            : this.boundsSqr.max.isEmpty() || !(this.boundsSqr.max.get() < value);
      }

      public static NumberRange.DoubleRange parse(StringReader reader) throws CommandSyntaxException {
         int i = reader.getCursor();
         NumberRange.Bounds<Double> bounds = NumberRange.Bounds.parse(
            reader, Double::parseDouble, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidDouble
         );
         if (bounds.isSwapped()) {
            reader.setCursor(i);
            throw EXCEPTION_SWAPPED.createWithContext(reader);
         } else {
            return new NumberRange.DoubleRange(bounds);
         }
      }
   }

   public record IntRange(NumberRange.Bounds<Integer> bounds, NumberRange.Bounds<Long> boundsSqr) implements NumberRange<Integer> {
      public static final NumberRange.IntRange ANY = new NumberRange.IntRange(NumberRange.Bounds.any());
      public static final Codec<NumberRange.IntRange> CODEC = NumberRange.Bounds.createCodec(Codec.INT)
         .validate(NumberRange.Bounds::validate)
         .xmap(NumberRange.IntRange::new, NumberRange.IntRange::bounds);
      public static final PacketCodec<ByteBuf, NumberRange.IntRange> PACKET_CODEC = NumberRange.Bounds.createPacketCodec(PacketCodecs.INTEGER)
         .xmap(NumberRange.IntRange::new, NumberRange.IntRange::bounds);

      private IntRange(NumberRange.Bounds<Integer> bounds) {
         this(bounds, bounds.map(i -> MathHelper.square(i.longValue())));
      }

      public static NumberRange.IntRange exactly(int value) {
         return new NumberRange.IntRange(NumberRange.Bounds.exactly(value));
      }

      public static NumberRange.IntRange between(int min, int max) {
         return new NumberRange.IntRange(NumberRange.Bounds.between(min, max));
      }

      public static NumberRange.IntRange atLeast(int value) {
         return new NumberRange.IntRange(NumberRange.Bounds.atLeast(value));
      }

      public static NumberRange.IntRange atMost(int value) {
         return new NumberRange.IntRange(NumberRange.Bounds.atMost(value));
      }

      public boolean test(int value) {
         return this.bounds.min.isPresent() && this.bounds.min.get() > value ? false : this.bounds.max.isEmpty() || this.bounds.max.get() >= value;
      }

      public boolean testSqrt(long value) {
         return this.boundsSqr.min.isPresent() && this.boundsSqr.min.get() > value ? false : this.boundsSqr.max.isEmpty() || this.boundsSqr.max.get() >= value;
      }

      public static NumberRange.IntRange parse(StringReader reader) throws CommandSyntaxException {
         int i = reader.getCursor();
         NumberRange.Bounds<Integer> bounds = NumberRange.Bounds.parse(reader, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt);
         if (bounds.isSwapped()) {
            reader.setCursor(i);
            throw EXCEPTION_SWAPPED.createWithContext(reader);
         } else {
            return new NumberRange.IntRange(bounds);
         }
      }
   }
}
