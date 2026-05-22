package net.minecraft.util.collection;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.math.random.Random;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Список с весами и поддержкой перемешивания по алгоритму взвешенного случайного порядка.
 * При вызове {@link #shuffle()} каждому элементу присваивается случайный порядковый номер,
 * пропорциональный его весу, после чего список сортируется по этому номеру.
 *
 * @param <U> тип элементов
 */
public class WeightedList<U> implements Iterable<U> {

	protected final List<Entry<U>> entries;
	private final Random random = Random.create();

	public WeightedList() {
		entries = Lists.newArrayList();
	}

	private WeightedList(List<Entry<U>> list) {
		entries = Lists.newArrayList(list);
	}

	public static <U> Codec<WeightedList<U>> createCodec(Codec<U> codec) {
		return Entry.createCodec(codec)
			.listOf()
			.xmap(WeightedList::new, list -> list.entries);
	}

	public WeightedList<U> add(U data, int weight) {
		entries.add(new Entry<>(data, weight));
		return this;
	}

	/**
	 * Перемешивает список с учётом весов элементов.
	 * Элементы с большим весом с большей вероятностью окажутся в начале.
	 *
	 * @return {@code this} для цепочки вызовов
	 */
	public WeightedList<U> shuffle() {
		entries.forEach(entry -> entry.setShuffledOrder(random.nextFloat()));
		entries.sort(Comparator.comparingDouble(Entry::getShuffledOrder));
		return this;
	}

	public Stream<U> stream() {
		return entries.stream().map(Entry::getElement);
	}

	@Override
	public Iterator<U> iterator() {
		return Iterators.transform(entries.iterator(), Entry::getElement);
	}

	@Override
	public String toString() {
		return "ShufflingList[" + entries + "]";
	}

	public static class Entry<T> {

		final T data;
		final int weight;
		private double shuffledOrder;

		Entry(T data, int weight) {
			this.weight = weight;
			this.data = data;
		}

		private double getShuffledOrder() {
			return shuffledOrder;
		}

		void setShuffledOrder(float randomValue) {
			shuffledOrder = -Math.pow(randomValue, 1.0F / weight);
		}

		public T getElement() {
			return data;
		}

		public int getWeight() {
			return weight;
		}

		@Override
		public String toString() {
			return weight + ":" + data;
		}

		public static <E> Codec<Entry<E>> createCodec(Codec<E> codec) {
			return new Codec<>() {

				@Override
				public <T> DataResult<Pair<Entry<E>, T>> decode(DynamicOps<T> ops, T input) {
					Dynamic<T> dynamic = new Dynamic<>(ops, input);
					return dynamic.get("data")
						.flatMap(codec::parse)
						.map(parsed -> new Entry<>(parsed, dynamic.get("weight").asInt(1)))
						.map(entry -> Pair.of(entry, ops.empty()));
				}

				@Override
				public <T> DataResult<T> encode(Entry<E> entry, DynamicOps<T> dynamicOps, T prefix) {
					return dynamicOps.mapBuilder()
						.add("weight", dynamicOps.createInt(entry.weight))
						.add("data", codec.encodeStart(dynamicOps, entry.data))
						.build(prefix);
				}
			};
		}
	}
}
