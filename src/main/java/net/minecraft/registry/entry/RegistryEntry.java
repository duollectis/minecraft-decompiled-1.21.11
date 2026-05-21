package net.minecraft.registry.entry;

import com.mojang.datafixers.util.Either;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * {@code RegistryEntry}.
 */
public interface RegistryEntry<T> {

	T value();

	boolean hasKeyAndValue();

	boolean matchesId(Identifier id);

	boolean matchesKey(RegistryKey<T> key);

	boolean matches(Predicate<RegistryKey<T>> predicate);

	boolean isIn(TagKey<T> tag);

	@Deprecated
	boolean matches(RegistryEntry<T> entry);

	Stream<TagKey<T>> streamTags();

	Either<RegistryKey<T>, T> getKeyOrValue();

	Optional<RegistryKey<T>> getKey();

	RegistryEntry.Type getType();

	boolean ownerEquals(RegistryEntryOwner<T> owner);

	default String getIdAsString() {
		return this.getKey().map(key -> key.getValue().toString()).orElse("[unregistered]");
	}

	static <T> RegistryEntry<T> of(T value) {
		return new RegistryEntry.Direct<>(value);
	}

	/**
	 * {@code Direct}.
	 */
	public record Direct<T>(T value) implements RegistryEntry<T> {

		@Override
		public boolean hasKeyAndValue() {
			return true;
		}

		@Override
		public boolean matchesId(Identifier id) {
			return false;
		}

		@Override
		public boolean matchesKey(RegistryKey<T> key) {
			return false;
		}

		@Override
		public boolean isIn(TagKey<T> tag) {
			return false;
		}

		@Override
		public boolean matches(RegistryEntry<T> entry) {
			return this.value.equals(entry.value());
		}

		@Override
		public boolean matches(Predicate<RegistryKey<T>> predicate) {
			return false;
		}

		@Override
		public Either<RegistryKey<T>, T> getKeyOrValue() {
			return Either.right(this.value);
		}

		@Override
		public Optional<RegistryKey<T>> getKey() {
			return Optional.empty();
		}

		@Override
		public RegistryEntry.Type getType() {
			return RegistryEntry.Type.DIRECT;
		}

		@Override
		public String toString() {
			return "Direct{" + this.value + "}";
		}

		@Override
		public boolean ownerEquals(RegistryEntryOwner<T> owner) {
			return true;
		}

		@Override
		public Stream<TagKey<T>> streamTags() {
			return Stream.of();
		}
	}

	/**
	 * {@code Reference}.
	 */
	public static class Reference<T> implements RegistryEntry<T> {

		private final RegistryEntryOwner<T> owner;
		private @Nullable Set<TagKey<T>> tags;
		private final RegistryEntry.Reference.Type referenceType;
		private @Nullable RegistryKey<T> registryKey;
		private @Nullable T value;

		protected Reference(
				RegistryEntry.Reference.Type referenceType,
				RegistryEntryOwner<T> owner,
				@Nullable RegistryKey<T> registryKey,
				@Nullable T value
		) {
			this.owner = owner;
			this.referenceType = referenceType;
			this.registryKey = registryKey;
			this.value = value;
		}

		public static <T> RegistryEntry.Reference<T> standAlone(
				RegistryEntryOwner<T> owner,
				RegistryKey<T> registryKey
		) {
			return new RegistryEntry.Reference<>(RegistryEntry.Reference.Type.STAND_ALONE, owner, registryKey, null);
		}

		@Deprecated
		public static <T> RegistryEntry.Reference<T> intrusive(RegistryEntryOwner<T> owner, @Nullable T value) {
			return new RegistryEntry.Reference<>(RegistryEntry.Reference.Type.INTRUSIVE, owner, null, value);
		}

		/**
		 * Registry key.
		 *
		 * @return RegistryKey — результат операции
		 */
		public RegistryKey<T> registryKey() {
			if (this.registryKey == null) {
				throw new IllegalStateException(
						"Trying to access unbound value '" + this.value + "' from registry " + this.owner);
			}
			else {
				return this.registryKey;
			}
		}

		@Override
		public T value() {
			if (this.value == null) {
				throw new IllegalStateException(
						"Trying to access unbound value '" + this.registryKey + "' from registry " + this.owner);
			}
			else {
				return this.value;
			}
		}

		@Override
		public boolean matchesId(Identifier id) {
			return this.registryKey().getValue().equals(id);
		}

		@Override
		public boolean matchesKey(RegistryKey<T> key) {
			return this.registryKey() == key;
		}

		private Set<TagKey<T>> getTags() {
			if (this.tags == null) {
				throw new IllegalStateException("Tags not bound");
			}
			else {
				return this.tags;
			}
		}

		@Override
		public boolean isIn(TagKey<T> tag) {
			return this.getTags().contains(tag);
		}

		@Override
		public boolean matches(RegistryEntry<T> entry) {
			return entry.matchesKey(this.registryKey());
		}

		@Override
		public boolean matches(Predicate<RegistryKey<T>> predicate) {
			return predicate.test(this.registryKey());
		}

		@Override
		public boolean ownerEquals(RegistryEntryOwner<T> owner) {
			return this.owner.ownerEquals(owner);
		}

		@Override
		public Either<RegistryKey<T>, T> getKeyOrValue() {
			return Either.left(this.registryKey());
		}

		@Override
		public Optional<RegistryKey<T>> getKey() {
			return Optional.of(this.registryKey());
		}

		@Override
		public RegistryEntry.Type getType() {
			return RegistryEntry.Type.REFERENCE;
		}

		@Override
		public boolean hasKeyAndValue() {
			return this.registryKey != null && this.value != null;
		}

		public void setRegistryKey(RegistryKey<T> registryKey) {
			if (this.registryKey != null && registryKey != this.registryKey) {
				throw new IllegalStateException(
						"Can't change holder key: existing=" + this.registryKey + ", new=" + registryKey);
			}
			else {
				this.registryKey = registryKey;
			}
		}

		public void setValue(T value) {
			if (this.referenceType == RegistryEntry.Reference.Type.INTRUSIVE && this.value != value) {
				throw new IllegalStateException(
						"Can't change holder " + this.registryKey + " value: existing=" + this.value + ", new="
								+ value);
			}
			else {
				this.value = value;
			}
		}

		public void setTags(Collection<TagKey<T>> tags) {
			this.tags = Set.copyOf(tags);
		}

		@Override
		public Stream<TagKey<T>> streamTags() {
			return this.getTags().stream();
		}

		@Override
		public String toString() {
			return "Reference{" + this.registryKey + "=" + this.value + "}";
		}

		/**
		 * {@code Type}.
		 */
		protected static enum Type {
			STAND_ALONE,
			INTRUSIVE;
		}
	}

	/**
	 * {@code Type}.
	 */
	public static enum Type {
		REFERENCE,
		DIRECT;
	}
}
