package net.minecraft.registry.entry;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Either;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Коллекция записей реестра. Существует в двух формах:
 * <ul>
 *   <li>{@link Direct} — явный список конкретных записей</li>
 *   <li>{@link Named} — именованный тег, разрешаемый в список при загрузке данных</li>
 * </ul>
 *
 * @param <T> тип хранимых значений
 */
public interface RegistryEntryList<T> extends Iterable<RegistryEntry<T>> {

	Stream<RegistryEntry<T>> stream();

	int size();

	boolean isBound();

	Either<TagKey<T>, List<RegistryEntry<T>>> getStorage();

	Optional<RegistryEntry<T>> getRandom(Random random);

	RegistryEntry<T> get(int index);

	boolean contains(RegistryEntry<T> entry);

	boolean ownerEquals(RegistryEntryOwner<T> owner);

	Optional<TagKey<T>> getTagKey();

	/**
	 * Создаёт {@link Named} список, который нельзя разыменовать во время конструирования.
	 * Используется только в тестах для создания заглушек тегов.
	 */
	@Deprecated
	@VisibleForTesting
	static <T> RegistryEntryList.Named<T> of(RegistryEntryOwner<T> owner, TagKey<T> tagKey) {
		return new RegistryEntryList.Named<T>(owner, tagKey) {
			@Override
			protected List<RegistryEntry<T>> getEntries() {
				throw new UnsupportedOperationException(
						"Tag " + getTag() + " can't be dereferenced during construction");
			}
		};
	}

	static <T> RegistryEntryList<T> empty() {
		return (RegistryEntryList<T>) RegistryEntryList.Direct.EMPTY;
	}

	@SafeVarargs
	static <T> RegistryEntryList.Direct<T> of(RegistryEntry<T>... entries) {
		return new RegistryEntryList.Direct<>(List.of(entries));
	}

	static <T> RegistryEntryList.Direct<T> of(List<? extends RegistryEntry<T>> entries) {
		return new RegistryEntryList.Direct<>(List.copyOf(entries));
	}

	@SafeVarargs
	static <E, T> RegistryEntryList.Direct<T> of(Function<E, RegistryEntry<T>> mapper, E... values) {
		return of(Stream.of(values).map(mapper).toList());
	}

	static <E, T> RegistryEntryList.Direct<T> of(Function<E, RegistryEntry<T>> mapper, Collection<E> values) {
		return of(values.stream().map(mapper).toList());
	}

	/**
	 * Явный список записей реестра без привязки к тегу.
	 * Всегда считается привязанным ({@link #isBound()} возвращает {@code true}).
	 *
	 * @param <T> тип хранимых значений
	 */
	final class Direct<T> extends RegistryEntryList.ListBacked<T> {

		static final RegistryEntryList.Direct<?> EMPTY = new RegistryEntryList.Direct<>(List.of());
		private final List<RegistryEntry<T>> entries;
		private @Nullable Set<RegistryEntry<T>> entrySet;

		Direct(List<RegistryEntry<T>> entries) {
			this.entries = entries;
		}

		@Override
		protected List<RegistryEntry<T>> getEntries() {
			return entries;
		}

		@Override
		public boolean isBound() {
			return true;
		}

		@Override
		public Either<TagKey<T>, List<RegistryEntry<T>>> getStorage() {
			return Either.right(entries);
		}

		@Override
		public Optional<TagKey<T>> getTagKey() {
			return Optional.empty();
		}

		@Override
		public boolean contains(RegistryEntry<T> entry) {
			if (entrySet == null) {
				entrySet = Set.copyOf(entries);
			}

			return entrySet.contains(entry);
		}

		@Override
		public String toString() {
			return "DirectSet[" + entries + "]";
		}

		@Override
		public boolean equals(Object o) {
			return this == o
					? true
					: o instanceof RegistryEntryList.Direct<?> direct && entries.equals(direct.entries);
		}

		@Override
		public int hashCode() {
			return entries.hashCode();
		}
	}

	/**
	 * Базовый класс для реализаций, хранящих записи в виде {@link List}.
	 * Делегирует все операции итерации и доступа к {@link #getEntries()}.
	 *
	 * @param <T> тип хранимых значений
	 */
	abstract class ListBacked<T> implements RegistryEntryList<T> {

		protected abstract List<RegistryEntry<T>> getEntries();

		@Override
		public int size() {
			return getEntries().size();
		}

		@Override
		public Spliterator<RegistryEntry<T>> spliterator() {
			return getEntries().spliterator();
		}

		@Override
		public Iterator<RegistryEntry<T>> iterator() {
			return getEntries().iterator();
		}

		@Override
		public Stream<RegistryEntry<T>> stream() {
			return getEntries().stream();
		}

		@Override
		public Optional<RegistryEntry<T>> getRandom(Random random) {
			return Util.getRandomOrEmpty(getEntries(), random);
		}

		@Override
		public RegistryEntry<T> get(int index) {
			return getEntries().get(index);
		}

		@Override
		public boolean ownerEquals(RegistryEntryOwner<T> owner) {
			return true;
		}
	}

	/**
	 * Именованный список записей реестра, привязанный к тегу.
	 * Список записей устанавливается при загрузке тегов через {@link #setEntries}.
	 * До установки список считается непривязанным ({@link #isBound()} возвращает {@code false}).
	 *
	 * @param <T> тип хранимых значений
	 */
	class Named<T> extends RegistryEntryList.ListBacked<T> {

		private final RegistryEntryOwner<T> owner;
		private final TagKey<T> tag;
		private @Nullable List<RegistryEntry<T>> entries;

		public Named(RegistryEntryOwner<T> owner, TagKey<T> tag) {
			this.owner = owner;
			this.tag = tag;
		}

		public void setEntries(List<RegistryEntry<T>> entries) {
			this.entries = List.copyOf(entries);
		}

		public TagKey<T> getTag() {
			return tag;
		}

		@Override
		protected List<RegistryEntry<T>> getEntries() {
			if (entries == null) {
				throw new IllegalStateException(
						"Trying to access unbound tag '" + tag + "' from registry " + owner);
			}

			return entries;
		}

		@Override
		public boolean isBound() {
			return entries != null;
		}

		@Override
		public Either<TagKey<T>, List<RegistryEntry<T>>> getStorage() {
			return Either.left(tag);
		}

		@Override
		public Optional<TagKey<T>> getTagKey() {
			return Optional.of(tag);
		}

		@Override
		public boolean contains(RegistryEntry<T> entry) {
			return entry.isIn(tag);
		}

		@Override
		public String toString() {
			return "NamedSet(" + tag + ")[" + entries + "]";
		}

		@Override
		public boolean ownerEquals(RegistryEntryOwner<T> owner) {
			return this.owner.ownerEquals(owner);
		}
	}
}
