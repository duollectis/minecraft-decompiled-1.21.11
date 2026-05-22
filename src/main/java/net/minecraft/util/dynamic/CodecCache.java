package net.minecraft.util.dynamic;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtElement;

/**
 * Кэш результатов кодирования кодеков.
 * Позволяет избежать повторного кодирования одних и тех же значений,
 * используя слабые ссылки для автоматической очистки при нехватке памяти.
 */
public class CodecCache {

	final LoadingCache<Key<?, ?>, DataResult<?>> cache;

	public CodecCache(int size) {
		cache = CacheBuilder
				.newBuilder()
				.maximumSize(size)
				.concurrencyLevel(1)
				.softValues()
				.build(CacheLoader.from(Key::encode));
	}

	/**
	 * Оборачивает кодек в кэширующую обёртку.
	 * Декодирование делегируется напрямую, кодирование — через кэш.
	 * Для {@link NbtElement} результат копируется, чтобы избежать мутации кэшированного значения.
	 *
	 * @param codec исходный кодек
	 * @return кодек с кэшированием операции encode
	 */
	public <A> Codec<A> wrap(Codec<A> codec) {
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				return codec.decode(ops, input);
			}

			@SuppressWarnings("unchecked")
			@Override
			public <T> DataResult<T> encode(A value, DynamicOps<T> ops, T prefix) {
				return ((DataResult<T>) cache.getUnchecked(new Key<>(codec, value, ops)))
						.map(object -> object instanceof NbtElement nbt ? (T) nbt.copy() : object);
			}
		};
	}

	/**
	 * Ключ кэша, идентифицирующий уникальную операцию кодирования.
	 * Использует идентичность кодека (по ссылке) и равенство значения и ops.
	 */
	record Key<A, T>(Codec<A> codec, A value, DynamicOps<T> ops) {

		DataResult<T> encode() {
			return codec.encodeStart(ops, value);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			return o instanceof Key<?, ?> key
				&& codec == key.codec
				&& value.equals(key.value)
				&& ops.equals(key.ops);
		}

		@Override
		public int hashCode() {
			int hash = System.identityHashCode(codec);
			hash = 31 * hash + value.hashCode();
			return 31 * hash + ops.hashCode();
		}
	}
}
