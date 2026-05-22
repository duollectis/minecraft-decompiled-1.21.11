package net.minecraft.network.codec;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.*;
import io.netty.buffer.ByteBuf;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Комбинированный кодек пакета: объединяет {@link PacketDecoder} и {@link PacketEncoder} в одном интерфейсе.
 *
 * <p>Предоставляет фабричные методы ({@link #of}, {@link #ofStatic}, {@link #unit}),
 * трансформации ({@link #xmap}, {@link #mapBuf}, {@link #dispatch}) и
 * семейство методов {@code tuple} для сборки кодека из нескольких полей объекта.</p>
 *
 * @param <B> тип буфера (например, {@link ByteBuf} или {@link net.minecraft.network.RegistryByteBuf})
 * @param <V> тип кодируемого/декодируемого значения
 */
public interface PacketCodec<B, V> extends PacketDecoder<B, V>, PacketEncoder<B, V> {

	static <B, V> PacketCodec<B, V> ofStatic(PacketEncoder<B, V> encoder, PacketDecoder<B, V> decoder) {
		return new PacketCodec<B, V>() {
			@Override
			public V decode(B buf) {
				return decoder.decode(buf);
			}

			@Override
			public void encode(B buf, V value) {
				encoder.encode(buf, value);
			}
		};
	}

	static <B, V> PacketCodec<B, V> of(ValueFirstEncoder<B, V> encoder, PacketDecoder<B, V> decoder) {
		return new PacketCodec<B, V>() {
			@Override
			public V decode(B buf) {
				return decoder.decode(buf);
			}

			@Override
			public void encode(B buf, V value) {
				encoder.encode(value, buf);
			}
		};
	}

	/**
	 * Создаёт кодек для константного значения: декодирование всегда возвращает {@code value},
	 * а кодирование проверяет, что переданное значение совпадает с константой.
	 */
	static <B, V> PacketCodec<B, V> unit(V value) {
		return new PacketCodec<B, V>() {
			@Override
			public V decode(B buf) {
				return value;
			}

			@Override
			public void encode(B buf, V encoded) {
				if (!encoded.equals(value)) {
					throw new IllegalStateException("Can't encode '" + encoded + "', expected '" + value + "'");
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
			public O decode(B buf) {
				return (O) to.apply(PacketCodec.this.decode(buf));
			}

			@Override
			public void encode(B buf, O value) {
				PacketCodec.this.encode(buf, (V) from.apply(value));
			}
		};
	}

	/**
	 * Адаптирует кодек к другому типу буфера {@code O}, применяя {@code function} для получения {@code B} из {@code O}.
	 * Используется, например, для оборачивания {@link ByteBuf}-кодека в {@link net.minecraft.network.RegistryByteBuf}-кодек.
	 */
	default <O extends ByteBuf> PacketCodec<O, V> mapBuf(Function<O, ? extends B> function) {
		return new PacketCodec<O, V>() {
			@Override
			public V decode(O buf) {
				B mapped = (B) function.apply(buf);
				return PacketCodec.this.decode(mapped);
			}

			@Override
			public void encode(O buf, V value) {
				B mapped = (B) function.apply(buf);
				PacketCodec.this.encode(mapped, value);
			}
		};
	}

	/**
	 * Создаёт диспетчерный кодек: тип {@code U} определяется по значению {@code V},
	 * прочитанному из буфера, после чего выбирается соответствующий кодек для декодирования тела.
	 */
	default <U> PacketCodec<B, U> dispatch(
			Function<? super U, ? extends V> type,
			Function<? super V, ? extends PacketCodec<? super B, ? extends U>> codec
	) {
		return new PacketCodec<B, U>() {
			@Override
			public U decode(B buf) {
				V typeValue = PacketCodec.this.decode(buf);
				PacketCodec<? super B, ? extends U> typeCodec =
						(PacketCodec<? super B, ? extends U>) codec.apply(typeValue);
				return (U) typeCodec.decode(buf);
			}

			@Override
			public void encode(B buf, U value) {
				V typeValue = (V) type.apply(value);
				PacketCodec<B, U> typeCodec = (PacketCodec<B, U>) codec.apply(typeValue);
				PacketCodec.this.encode(buf, typeValue);
				typeCodec.encode(buf, value);
			}
		};
	}

	static <B, C, T1> PacketCodec<B, C> tuple(
			PacketCodec<? super B, T1> codec,
			Function<C, T1> from,
			Function<T1, C> to
	) {
		return new PacketCodec<B, C>() {
			@Override
			public C decode(B buf) {
				T1 t1 = codec.decode(buf);
				return to.apply(t1);
			}

			@Override
			public void encode(B buf, C value) {
				codec.encode(buf, from.apply(value));
			}
		};
	}

	static <B, C, T1, T2> PacketCodec<B, C> tuple(
			PacketCodec<? super B, T1> codec1,
			Function<C, T1> from1,
			PacketCodec<? super B, T2> codec2,
			Function<C, T2> from2,
			BiFunction<T1, T2, C> to
	) {
		return new PacketCodec<B, C>() {
			@Override
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				return to.apply(t1, t2);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				return (C) to.apply(t1, t2, t3);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				return (C) to.apply(t1, t2, t3, t4);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5, t6);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
				codec6.encode(buf, from6.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5, t6, t7);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
				codec6.encode(buf, from6.apply(value));
				codec7.encode(buf, from7.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				T8 t8 = codec8.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5, t6, t7, t8);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
				codec6.encode(buf, from6.apply(value));
				codec7.encode(buf, from7.apply(value));
				codec8.encode(buf, from8.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				T8 t8 = codec8.decode(buf);
				T9 t9 = codec9.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
				codec6.encode(buf, from6.apply(value));
				codec7.encode(buf, from7.apply(value));
				codec8.encode(buf, from8.apply(value));
				codec9.encode(buf, from9.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				T8 t8 = codec8.decode(buf);
				T9 t9 = codec9.decode(buf);
				T10 t10 = codec10.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
				codec6.encode(buf, from6.apply(value));
				codec7.encode(buf, from7.apply(value));
				codec8.encode(buf, from8.apply(value));
				codec9.encode(buf, from9.apply(value));
				codec10.encode(buf, from10.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				T8 t8 = codec8.decode(buf);
				T9 t9 = codec9.decode(buf);
				T10 t10 = codec10.decode(buf);
				T11 t11 = codec11.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
			}

			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
				codec6.encode(buf, from6.apply(value));
				codec7.encode(buf, from7.apply(value));
				codec8.encode(buf, from8.apply(value));
				codec9.encode(buf, from9.apply(value));
				codec10.encode(buf, from10.apply(value));
				codec11.encode(buf, from11.apply(value));
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
			public C decode(B buf) {
				T1 t1 = codec1.decode(buf);
				T2 t2 = codec2.decode(buf);
				T3 t3 = codec3.decode(buf);
				T4 t4 = codec4.decode(buf);
				T5 t5 = codec5.decode(buf);
				T6 t6 = codec6.decode(buf);
				T7 t7 = codec7.decode(buf);
				T8 t8 = codec8.decode(buf);
				T9 t9 = codec9.decode(buf);
				T10 t10 = codec10.decode(buf);
				T11 t11 = codec11.decode(buf);
				T12 t12 = codec12.decode(buf);
				return (C) to.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
			}
	
			@Override
			public void encode(B buf, C value) {
				codec1.encode(buf, from1.apply(value));
				codec2.encode(buf, from2.apply(value));
				codec3.encode(buf, from3.apply(value));
				codec4.encode(buf, from4.apply(value));
				codec5.encode(buf, from5.apply(value));
				codec6.encode(buf, from6.apply(value));
				codec7.encode(buf, from7.apply(value));
				codec8.encode(buf, from8.apply(value));
				codec9.encode(buf, from9.apply(value));
				codec10.encode(buf, from10.apply(value));
				codec11.encode(buf, from11.apply(value));
				codec12.encode(buf, from12.apply(value));
			}
		};
	}
	
		/**
			* Создаёт рекурсивный кодек, ссылающийся на самого себя через мемоизированный {@link Supplier}.
			* Используется для кодирования рекурсивных структур данных (например, деревьев).
			*/
		static <B, T> PacketCodec<B, T> recursive(UnaryOperator<PacketCodec<B, T>> codecGetter) {
			return new PacketCodec<B, T>() {
				private final Supplier<PacketCodec<B, T>> codecSupplier = Suppliers.memoize(() -> codecGetter.apply(this));
	
				@Override
				public T decode(B buf) {
					return codecSupplier.get().decode(buf);
				}
	
				@Override
				public void encode(B buf, T value) {
					codecSupplier.get().encode(buf, value);
				}
			};
		}
	
		@SuppressWarnings("unchecked")
		default <S extends B> PacketCodec<S, V> cast() {
			return (PacketCodec<S, V>) this;
		}
	
		/**
			* Функциональный интерфейс для трансформации одного кодека в другой.
			* Используется в {@link #collect(ResultFunction)} для применения цепочечных преобразований.
			*
			* @param <B> тип буфера
			* @param <S> исходный тип значения
			* @param <T> целевой тип значения
			*/
		@FunctionalInterface
		interface ResultFunction<B, S, T> {
	
			PacketCodec<B, T> apply(PacketCodec<B, S> codec);
		}
	}
