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
 * {@code RegistryEntryList}.
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

	@Deprecated
	@VisibleForTesting
	static <T> RegistryEntryList.Named<T> of(RegistryEntryOwner<T> owner, TagKey<T> tagKey) {
		return new RegistryEntryList.Named<T>(owner, tagKey) {
			@Override
			protected List<RegistryEntry<T>> getEntries() {
				throw new UnsupportedOperationException(
						"Tag " + this.getTag() + " can't be dereferenced during construction");
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
	 * {@code Direct}.
	 */
	public static final class Direct<T> extends RegistryEntryList.ListBacked<T> {

		static final RegistryEntryList.Direct<?> EMPTY = new RegistryEntryList.Direct(List.of());
		private final List<RegistryEntry<T>> entries;
		private @Nullable Set<RegistryEntry<T>> entrySet;

		Direct(List<RegistryEntry<T>> entries) {
			this.entries = entries;
		}

		@Override
		protected List<RegistryEntry<T>> getEntries() {
			return this.entries;
		}

		@Override
		public boolean isBound() {
			return true;
		}

		@Override
		public Either<TagKey<T>, List<RegistryEntry<T>>> getStorage() {
			return Either.right(this.entries);
		}

		@Override
		public Optional<TagKey<T>> getTagKey() {
			return Optional.empty();
		}

		@Override
		public boolean contains(RegistryEntry<T> entry) {
			if (this.entrySet == null) {
				this.entrySet = Set.copyOf(this.entries);
			}

			return this.entrySet.contains(entry);
		}

		@Override
		public String toString() {
			return "DirectSet[" + this.entries + "]";
		}

		@Override
		public boolean equals(Object o) {
			return this == o ? true
			                 : o instanceof RegistryEntryList.Direct<?> direct && this.entries.equals(direct.entries);
		}

		@Override
		public int hashCode() {
			return this.entries.hashCode();
		}
	}

	/**
	 * {@code ListBacked}.
	 */
	public abstract static class ListBacked<T> implements RegistryEntryList<T> {

		protected abstract List<RegistryEntry<T>> getEntries();

		@Override
		public int size() {
			return this.getEntries().size();
		}

		@Override
		public Spliterator<RegistryEntry<T>> spliterator() {
			return this.getEntries().spliterator();
		}

		@Override
		public Iterator<RegistryEntry<T>> iterator() {
			return this.getEntries().iterator();
		}

		@Override
		public Stream<RegistryEntry<T>> stream() {
			return this.getEntries().stream();
		}

		@Override
		public Optional<RegistryEntry<T>> getRandom(Random random) {
			return Util.getRandomOrEmpty(this.getEntries(), random);
		}

		@Override
		public RegistryEntry<T> get(int index) {
			return this.getEntries().get(index);
		}

		@Override
		public boolean ownerEquals(RegistryEntryOwner<T> owner) {
			return true;
		}
	}

	/**
	 * {@code Named}.
	 */
	public static class Named<T> extends RegistryEntryList.ListBacked<T> {

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
			return this.tag;
		}

		@Override
		protected List<RegistryEntry<T>> getEntries() {
			if (this.entries == null) {
				throw new IllegalStateException(
						"Trying to access unbound tag '" + this.tag + "' from registry " + this.owner);
			}
			else {
				return this.entries;
			}
		}

		@Override
		public boolean isBound() {
			return this.entries != null;
		}

		@Override
		public Either<TagKey<T>, List<RegistryEntry<T>>> getStorage() {
			return Either.left(this.tag);
		}

		@Override
		public Optional<TagKey<T>> getTagKey() {
			return Optional.of(this.tag);
		}

		@Override
		public boolean contains(RegistryEntry<T> entry) {
			return entry.isIn(this.tag);
		}

		@Override
		public String toString() {
			return "NamedSet(" + this.tag + ")[" + this.entries + "]";
		}

		@Override
		public boolean ownerEquals(RegistryEntryOwner<T> owner) {
			return this.owner.ownerEquals(owner);
		}
	}
}
