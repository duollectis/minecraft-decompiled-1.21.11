package net.minecraft.util.collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Взвешенный пул элементов для случайного выбора.
 * При суммарном весе меньше {@link #FLATTENED_CONTENT_THRESHOLD} использует
 * развёрнутый массив для O(1) выборки; иначе — линейный поиск по весам.
 *
 * @param <E> тип элементов пула
 */
public final class Pool<E> {

	private static final int FLATTENED_CONTENT_THRESHOLD = 64;

	private final int totalWeight;
	private final List<Weighted<E>> entries;
	private final @Nullable Content<E> content;

	Pool(List<? extends Weighted<E>> entries) {
		this.entries = List.copyOf(entries);
		totalWeight = Weighting.getWeightSum(entries, Weighted::weight);

		if (totalWeight == 0) {
			content = null;
		} else if (totalWeight < FLATTENED_CONTENT_THRESHOLD) {
			content = new FlattenedContent<>(this.entries, totalWeight);
		} else {
			content = new WrappedContent<>(this.entries);
		}
	}

	public static <E> Pool<E> empty() {
		return new Pool<>(List.of());
	}

	public static <E> Pool<E> of(E entry) {
		return new Pool<>(List.of(new Weighted<>(entry, 1)));
	}

	/**
	 * Создаёт пул из набора взвешенных элементов.
	 *
	 * @param entries взвешенные элементы
	 */
	@SafeVarargs
	public static <E> Pool<E> of(Weighted<E>... entries) {
		return new Pool<>(List.of(entries));
	}

	public static <E> Pool<E> of(List<Weighted<E>> entries) {
		return new Pool<>(entries);
	}

	public static <E> Builder<E> builder() {
		return new Builder<>();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public <T> Pool<T> transform(Function<E, T> function) {
		return new Pool<>(Lists.transform(entries, entry -> entry.transform(function)));
	}

	public Optional<E> getOrEmpty(Random random) {
		if (content == null) {
			return Optional.empty();
		}

		return Optional.of(content.get(random.nextInt(totalWeight)));
	}

	/**
	 * Возвращает случайный элемент пула с учётом весов.
	 *
	 * @param random источник случайности
	 * @return случайный элемент
	 * @throws IllegalStateException если пул пуст
	 */
	public E get(Random random) {
		if (content == null) {
			throw new IllegalStateException("Weighted list has no elements");
		}

		return content.get(random.nextInt(totalWeight));
	}

	public List<Weighted<E>> getEntries() {
		return entries;
	}

	public static <E> Codec<Pool<E>> createCodec(Codec<E> entryCodec) {
		return Weighted.createCodec(entryCodec).listOf().xmap(Pool::of, Pool::getEntries);
	}

	public static <E> Codec<Pool<E>> createCodec(MapCodec<E> entryCodec) {
		return Weighted.createCodec(entryCodec).listOf().xmap(Pool::of, Pool::getEntries);
	}

	public static <E> Codec<Pool<E>> createNonEmptyCodec(Codec<E> entryCodec) {
		return Codecs.nonEmptyList(Weighted.createCodec(entryCodec).listOf()).xmap(Pool::of, Pool::getEntries);
	}

	public static <E> Codec<Pool<E>> createNonEmptyCodec(MapCodec<E> entryCodec) {
		return Codecs.nonEmptyList(Weighted.createCodec(entryCodec).listOf()).xmap(Pool::of, Pool::getEntries);
	}

	public static <E, B extends ByteBuf> PacketCodec<B, Pool<E>> createPacketCodec(PacketCodec<B, E> entryCodec) {
		return Weighted.createPacketCodec(entryCodec).collect(PacketCodecs.toList()).xmap(Pool::of, Pool::getEntries);
	}

	public boolean contains(E value) {
		for (Weighted<E> weighted : entries) {
			if (weighted.value().equals(value)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}

		return other instanceof Pool<?> pool
			? totalWeight == pool.totalWeight && Objects.equals(entries, pool.entries)
			: false;
	}

	@Override
	public int hashCode() {
		return 31 * totalWeight + entries.hashCode();
	}

	public static class Builder<E> {

		private final ImmutableList.Builder<Weighted<E>> entries = ImmutableList.builder();

		public Builder<E> add(E object) {
			return add(object, 1);
		}

		public Builder<E> add(E object, int weight) {
			entries.add(new Weighted<>(object, weight));
			return this;
		}

		public Pool<E> build() {
			return new Pool<>(entries.build());
		}
	}

	interface Content<E> {

		E get(int i);
	}

	/**
	 * Развёрнутое хранилище: каждый элемент повторяется {@code weight} раз,
	 * что обеспечивает O(1) выборку по случайному индексу.
	 */
	static class FlattenedContent<E> implements Content<E> {

		private final Object[] entries;

		FlattenedContent(List<Weighted<E>> entries, int totalWeight) {
			this.entries = new Object[totalWeight];
			int offset = 0;

			for (Weighted<E> weighted : entries) {
				int weight = weighted.weight();
				Arrays.fill(this.entries, offset, offset + weight, weighted.value());
				offset += weight;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public E get(int i) {
			return (E) entries[i];
		}
	}

	/**
	 * Хранилище с линейным поиском по весам.
	 * Используется когда суммарный вес превышает {@link #FLATTENED_CONTENT_THRESHOLD}.
	 */
	static class WrappedContent<E> implements Content<E> {

		private final Weighted<?>[] entries;

		WrappedContent(List<Weighted<E>> entries) {
			this.entries = entries.toArray(Weighted[]::new);
		}

		@Override
		@SuppressWarnings("unchecked")
		public E get(int i) {
			for (Weighted<?> weighted : entries) {
				i -= weighted.weight();

				if (i < 0) {
					return (E) weighted.value();
				}
			}

			throw new IllegalStateException(i + " exceeded total weight");
		}
	}
}
