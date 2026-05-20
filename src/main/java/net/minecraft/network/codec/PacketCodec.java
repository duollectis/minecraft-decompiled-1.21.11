package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function12;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public interface PacketCodec<B, V> extends PacketDecoder<B, V>, PacketEncoder<B, V> {
   static <B, V> PacketCodec<B, V> ofStatic(PacketEncoder<B, V> encoder, PacketDecoder<B, V> decoder) {
      return new PacketCodec<B, V>() {
         @Override
         public V decode(B object) {
            return decoder.decode(object);
         }

         @Override
         public void encode(B object, V object2) {
            encoder.encode(object, object2);
         }
      };
   }

   static <B, V> PacketCodec<B, V> of(ValueFirstEncoder<B, V> encoder, PacketDecoder<B, V> decoder) {
      return new PacketCodec<B, V>() {
         @Override
         public V decode(B object) {
            return decoder.decode(object);
         }

         @Override
         public void encode(B object, V object2) {
            encoder.encode(object2, object);
         }
      };
   }

   static <B, V> PacketCodec<B, V> unit(V value) {
      return new PacketCodec<B, V>() {
         @Override
         public V decode(B object) {
            return value;
         }

         @Override
         public void encode(B object, V object2) {
            if (!object2.equals(value)) {
               throw new IllegalStateException("Can't encode '" + object2 + "', expected '" + value + "'");
            }
         }
      };
   }

   default <O> PacketCodec<B, O> collect(PacketCodec.ResultFunction<B, V, O> function) {
      return function.apply(this);
   }

   default <O> PacketCodec<B, O> xmap(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
      return new PacketCodec<B, O>() {
         @Override
         public O decode(B object) {
            return (O)to.apply(PacketCodec.this.decode(object));
         }

         @Override
         public void encode(B object, O object2) {
            PacketCodec.this.encode(object, (V)from.apply(object2));
         }
      };
   }

   default <O extends ByteBuf> PacketCodec<O, V> mapBuf(Function<O, ? extends B> function) {
      return new PacketCodec<O, V>() {
         public V decode(O byteBuf) {
            B object = (B)function.apply(byteBuf);
            return PacketCodec.this.decode(object);
         }

         public void encode(O byteBuf, V object) {
            B object2 = (B)function.apply(byteBuf);
            PacketCodec.this.encode(object2, object);
         }
      };
   }

   default <U> PacketCodec<B, U> dispatch(Function<? super U, ? extends V> type, Function<? super V, ? extends PacketCodec<? super B, ? extends U>> codec) {
      return new PacketCodec<B, U>() {
         @Override
         public U decode(B object) {
            V object2 = PacketCodec.this.decode(object);
            PacketCodec<? super B, ? extends U> packetCodec = (PacketCodec<? super B, ? extends U>)codec.apply(object2);
            return (U)packetCodec.decode(object);
         }

         @Override
         public void encode(B object, U object2) {
            V object3 = (V)type.apply(object2);
            PacketCodec<B, U> packetCodec = (PacketCodec<B, U>)codec.apply(object3);
            PacketCodec.this.encode(object, object3);
            packetCodec.encode(object, object2);
         }
      };
   }

   static <B, C, T1> PacketCodec<B, C> tuple(PacketCodec<? super B, T1> codec, Function<C, T1> from, Function<T1, C> to) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec.decode(object);
            return to.apply(object2);
         }

         @Override
         public void encode(B object, C object2) {
            codec.encode(object, from.apply(object2));
         }
      };
   }

   static <B, C, T1, T2> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1, Function<C, T1> from1, PacketCodec<? super B, T2> codec2, Function<C, T2> from2, BiFunction<T1, T2, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            return to.apply(object2, object3);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      Function3<T1, T2, T3, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            return (C)to.apply(object2, object3, object4);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      Function4<T1, T2, T3, T4, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            return (C)to.apply(object2, object3, object4, object5);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      Function5<T1, T2, T3, T4, T5, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      PacketCodec<? super B, T6> codec6,
      Function<C, T6> from6,
      Function6<T1, T2, T3, T4, T5, T6, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6, object7);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
            codec6.encode(object, from6.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      PacketCodec<? super B, T6> codec6,
      Function<C, T6> from6,
      PacketCodec<? super B, T7> codec7,
      Function<C, T7> from7,
      Function7<T1, T2, T3, T4, T5, T6, T7, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6, object7, object8);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
            codec6.encode(object, from6.apply(object2));
            codec7.encode(object, from7.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      PacketCodec<? super B, T6> codec6,
      Function<C, T6> from6,
      PacketCodec<? super B, T7> codec7,
      Function<C, T7> from7,
      PacketCodec<? super B, T8> codec8,
      Function<C, T8> from8,
      Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6, object7, object8, object9);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
            codec6.encode(object, from6.apply(object2));
            codec7.encode(object, from7.apply(object2));
            codec8.encode(object, from8.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      PacketCodec<? super B, T6> codec6,
      Function<C, T6> from6,
      PacketCodec<? super B, T7> codec7,
      Function<C, T7> from7,
      PacketCodec<? super B, T8> codec8,
      Function<C, T8> from8,
      PacketCodec<? super B, T9> codec9,
      Function<C, T9> from9,
      Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
            codec6.encode(object, from6.apply(object2));
            codec7.encode(object, from7.apply(object2));
            codec8.encode(object, from8.apply(object2));
            codec9.encode(object, from9.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      PacketCodec<? super B, T6> codec6,
      Function<C, T6> from6,
      PacketCodec<? super B, T7> codec7,
      Function<C, T7> from7,
      PacketCodec<? super B, T8> codec8,
      Function<C, T8> from8,
      PacketCodec<? super B, T9> codec9,
      Function<C, T9> from9,
      PacketCodec<? super B, T10> codec10,
      Function<C, T10> from10,
      Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            T10 object11 = codec10.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10, object11);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
            codec6.encode(object, from6.apply(object2));
            codec7.encode(object, from7.apply(object2));
            codec8.encode(object, from8.apply(object2));
            codec9.encode(object, from9.apply(object2));
            codec10.encode(object, from10.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      PacketCodec<? super B, T6> codec6,
      Function<C, T6> from6,
      PacketCodec<? super B, T7> codec7,
      Function<C, T7> from7,
      PacketCodec<? super B, T8> codec8,
      Function<C, T8> from8,
      PacketCodec<? super B, T9> codec9,
      Function<C, T9> from9,
      PacketCodec<? super B, T10> codec10,
      Function<C, T10> from10,
      PacketCodec<? super B, T11> codec11,
      Function<C, T11> from11,
      Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            T10 object11 = codec10.decode(object);
            T11 object12 = codec11.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10, object11, object12);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
            codec6.encode(object, from6.apply(object2));
            codec7.encode(object, from7.apply(object2));
            codec8.encode(object, from8.apply(object2));
            codec9.encode(object, from9.apply(object2));
            codec10.encode(object, from10.apply(object2));
            codec11.encode(object, from11.apply(object2));
         }
      };
   }

   static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> PacketCodec<B, C> tuple(
      PacketCodec<? super B, T1> codec1,
      Function<C, T1> from1,
      PacketCodec<? super B, T2> codec2,
      Function<C, T2> from2,
      PacketCodec<? super B, T3> codec3,
      Function<C, T3> from3,
      PacketCodec<? super B, T4> codec4,
      Function<C, T4> from4,
      PacketCodec<? super B, T5> codec5,
      Function<C, T5> from5,
      PacketCodec<? super B, T6> codec6,
      Function<C, T6> from6,
      PacketCodec<? super B, T7> codec7,
      Function<C, T7> from7,
      PacketCodec<? super B, T8> codec8,
      Function<C, T8> from8,
      PacketCodec<? super B, T9> codec9,
      Function<C, T9> from9,
      PacketCodec<? super B, T10> codec10,
      Function<C, T10> from10,
      PacketCodec<? super B, T11> codec11,
      Function<C, T11> from11,
      PacketCodec<? super B, T12> codec12,
      Function<C, T12> from12,
      Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> to
   ) {
      return new PacketCodec<B, C>() {
         @Override
         public C decode(B object) {
            T1 object2 = codec1.decode(object);
            T2 object3 = codec2.decode(object);
            T3 object4 = codec3.decode(object);
            T4 object5 = codec4.decode(object);
            T5 object6 = codec5.decode(object);
            T6 object7 = codec6.decode(object);
            T7 object8 = codec7.decode(object);
            T8 object9 = codec8.decode(object);
            T9 object10 = codec9.decode(object);
            T10 object11 = codec10.decode(object);
            T11 object12 = codec11.decode(object);
            T12 object13 = codec12.decode(object);
            return (C)to.apply(object2, object3, object4, object5, object6, object7, object8, object9, object10, object11, object12, object13);
         }

         @Override
         public void encode(B object, C object2) {
            codec1.encode(object, from1.apply(object2));
            codec2.encode(object, from2.apply(object2));
            codec3.encode(object, from3.apply(object2));
            codec4.encode(object, from4.apply(object2));
            codec5.encode(object, from5.apply(object2));
            codec6.encode(object, from6.apply(object2));
            codec7.encode(object, from7.apply(object2));
            codec8.encode(object, from8.apply(object2));
            codec9.encode(object, from9.apply(object2));
            codec10.encode(object, from10.apply(object2));
            codec11.encode(object, from11.apply(object2));
            codec12.encode(object, from12.apply(object2));
         }
      };
   }

   static <B, T> PacketCodec<B, T> recursive(UnaryOperator<PacketCodec<B, T>> codecGetter) {
      return new PacketCodec<B, T>() {
         private final Supplier<PacketCodec<B, T>> codecSupplier = Suppliers.memoize(() -> codecGetter.apply(this));

         @Override
         public T decode(B object) {
            return this.codecSupplier.get().decode(object);
         }

         @Override
         public void encode(B object, T object2) {
            this.codecSupplier.get().encode(object, object2);
         }
      };
   }

   @SuppressWarnings("unchecked")
   default <S extends B> PacketCodec<S, V> cast() {
      return (PacketCodec<S, V>) this;
   }

   @FunctionalInterface
   public interface ResultFunction<B, S, T> {
      PacketCodec<B, T> apply(PacketCodec<B, S> codec);
   }
}
