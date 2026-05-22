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
 * Обёртка над значением реестра. Существует в двух формах:
 * <ul>
 *   <li>{@link Direct} — анонимное значение без ключа реестра (используется в inline-определениях)</li>
 *   <li>{@link Reference} — именованная ссылка на зарегистрированное значение с ключом</li>
 * </ul>
 *
 * @param <T> тип хранимого значения
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
		return getKey()
				.map(key -> key.getValue().toString())
				.orElse("[unregistered]");
	}

	static <T> RegistryEntry<T> of(T value) {
		return new RegistryEntry.Direct<>(value);
	}

	/**
	 * Анонимная запись реестра без ключа. Используется для inline-определений
	 * в JSON-файлах, где значение задаётся непосредственно, а не по ссылке.
	 * Никогда не принадлежит тегам и не имеет идентификатора.
	 *
	 * @param <T> тип хранимого значения
	 */
	record Direct<T>(T value) implements RegistryEntry<T> {

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
			return value.equals(entry.value());
		}

		@Override
		public boolean matches(Predicate<RegistryKey<T>> predicate) {
			return false;
		}

		@Override
		public Either<RegistryKey<T>, T> getKeyOrValue() {
			return Either.right(value);
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
			return "Direct{" + value + "}";
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
	 * Именованная ссылка на зарегистрированное значение. Хранит ключ реестра,
	 * само значение и набор тегов, которым принадлежит запись.
	 * <p>
	 * Существует в двух режимах:
	 * <ul>
	 *   <li>{@link Type#STAND_ALONE} — создана вне реестра (datagen, тесты)</li>
	 *   <li>{@link Type#INTRUSIVE} — создана самим объектом при инициализации (устаревший механизм)</li>
	 * </ul>
	 *
	 * @param <T> тип хранимого значения
	 */
	class Reference<T> implements RegistryEntry<T> {

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
		 * Возвращает ключ реестра этой записи.
		 *
		 * @throws IllegalStateException если запись ещё не привязана к ключу
		 */
		public RegistryKey<T> registryKey() {
			if (registryKey == null) {
				throw new IllegalStateException(
						"Trying to access unbound value '" + value + "' from registry " + owner);
			}

			return registryKey;
		}

		@Override
		public T value() {
			if (value == null) {
				throw new IllegalStateException(
						"Trying to access unbound value '" + registryKey + "' from registry " + owner);
			}

			return value;
		}

		@Override
		public boolean matchesId(Identifier id) {
			return registryKey().getValue().equals(id);
		}

		@Override
		public boolean matchesKey(RegistryKey<T> key) {
			return registryKey() == key;
		}

		private Set<TagKey<T>> getTags() {
			if (tags == null) {
				throw new IllegalStateException("Tags not bound");
			}

			return tags;
		}

		@Override
		public boolean isIn(TagKey<T> tag) {
			return getTags().contains(tag);
		}

		@Override
		public boolean matches(RegistryEntry<T> entry) {
			return entry.matchesKey(registryKey());
		}

		@Override
		public boolean matches(Predicate<RegistryKey<T>> predicate) {
			return predicate.test(registryKey());
		}

		@Override
		public boolean ownerEquals(RegistryEntryOwner<T> owner) {
			return this.owner.ownerEquals(owner);
		}

		@Override
		public Either<RegistryKey<T>, T> getKeyOrValue() {
			return Either.left(registryKey());
		}

		@Override
		public Optional<RegistryKey<T>> getKey() {
			return Optional.of(registryKey());
		}

		@Override
		public RegistryEntry.Type getType() {
			return RegistryEntry.Type.REFERENCE;
		}

		@Override
		public boolean hasKeyAndValue() {
			return registryKey != null && value != null;
		}

		/**
		 * Привязывает ключ реестра к этой записи. Вызывается при регистрации
		 * intrusive-записи в реестре.
		 *
		 * @throws IllegalStateException если запись уже имеет другой ключ
		 */
		public void setRegistryKey(RegistryKey<T> registryKey) {
			if (this.registryKey != null && registryKey != this.registryKey) {
				throw new IllegalStateException(
						"Can't change holder key: existing=" + this.registryKey + ", new=" + registryKey);
			}

			this.registryKey = registryKey;
		}

		/**
		 * Устанавливает значение записи. Для intrusive-записей значение
		 * нельзя изменить после первоначальной установки.
		 *
		 * @throws IllegalStateException если intrusive-запись уже имеет другое значение
		 */
		public void setValue(T value) {
			if (referenceType == RegistryEntry.Reference.Type.INTRUSIVE && this.value != value) {
				throw new IllegalStateException(
						"Can't change holder " + registryKey + " value: existing=" + this.value + ", new=" + value);
			}

			this.value = value;
		}

		public void setTags(Collection<TagKey<T>> tags) {
			this.tags = Set.copyOf(tags);
		}

		@Override
		public Stream<TagKey<T>> streamTags() {
			return getTags().stream();
		}

		@Override
		public String toString() {
			return "Reference{" + registryKey + "=" + value + "}";
		}

		/**
		 * Тип ссылочной записи реестра.
		 */
		protected enum Type {
			STAND_ALONE,
			INTRUSIVE
		}
	}

	/**
	 * Тип записи реестра: именованная ссылка или анонимное прямое значение.
	 */
	enum Type {
		REFERENCE,
		DIRECT
	}
}
