package net.minecraft.registry.tag;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Одна запись в файле тега. Может ссылаться на конкретный объект реестра
 * или на другой тег (через префикс {@code #}). Запись может быть обязательной
 * или опциональной (суффикс {@code ?}).
 */
public class TagEntry {

	private static final Codec<TagEntry> ENTRY_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							Codecs.TAG_ENTRY_ID.fieldOf("id").forGetter(TagEntry::getIdForCodec),
							Codec.BOOL.optionalFieldOf("required", true).forGetter(entry -> entry.required)
					)
					.apply(instance, TagEntry::new)
	);

	/**
	 * Кодек для {@link TagEntry}: поддерживает как краткую форму (просто строка-идентификатор),
	 * так и расширенную (объект с полями {@code id} и {@code required}).
	 */
	public static final Codec<TagEntry> CODEC = Codec.either(Codecs.TAG_ENTRY_ID, ENTRY_CODEC)
			.xmap(
					either -> (TagEntry) either.map(id -> new TagEntry(id, true), entry -> entry),
					entry -> entry.required
							? Either.left(entry.getIdForCodec())
							: Either.right(entry)
			);

	private final Identifier id;
	private final boolean tag;
	private final boolean required;

	private TagEntry(Identifier id, boolean tag, boolean required) {
		this.id = id;
		this.tag = tag;
		this.required = required;
	}

	private TagEntry(Codecs.TagEntryId id, boolean required) {
		this.id = id.id();
		this.tag = id.tag();
		this.required = required;
	}

	private Codecs.TagEntryId getIdForCodec() {
		return new Codecs.TagEntryId(id, tag);
	}

	public static TagEntry create(Identifier id) {
		return new TagEntry(id, false, true);
	}

	public static TagEntry createOptional(Identifier id) {
		return new TagEntry(id, false, false);
	}

	public static TagEntry createTag(Identifier id) {
		return new TagEntry(id, true, true);
	}

	public static TagEntry createOptionalTag(Identifier id) {
		return new TagEntry(id, true, false);
	}

	/**
	 * Разрешает запись тега в конкретные значения через {@link ValueGetter}.
	 * Для тегов — рекурсивно разворачивает содержимое тега.
	 * Для прямых ссылок — получает значение по идентификатору.
	 *
	 * @param valueGetter поставщик значений по идентификатору
	 * @param idConsumer  получатель разрешённых значений
	 * @return {@code true} если разрешение прошло успешно,
	 *         {@code false} если обязательная ссылка не найдена
	 */
	public <T> boolean resolve(TagEntry.ValueGetter<T> valueGetter, Consumer<T> idConsumer) {
		if (tag) {
			Collection<T> collection = valueGetter.tag(id);
			if (collection == null) {
				return !required;
			}

			collection.forEach(idConsumer);
		} else {
			T object = valueGetter.direct(id, required);
			if (object == null) {
				return !required;
			}

			idConsumer.accept(object);
		}

		return true;
	}

	public void forEachRequiredTagId(Consumer<Identifier> idConsumer) {
		if (tag && required) {
			idConsumer.accept(id);
		}
	}

	public void forEachOptionalTagId(Consumer<Identifier> idConsumer) {
		if (tag && !required) {
			idConsumer.accept(id);
		}
	}

	public boolean canAdd(Predicate<Identifier> directEntryPredicate, Predicate<Identifier> tagEntryPredicate) {
		return !required || (tag ? tagEntryPredicate : directEntryPredicate).test(id);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (tag) {
			builder.append('#');
		}

		builder.append(id);
		if (!required) {
			builder.append('?');
		}

		return builder.toString();
	}

	/**
	 * Поставщик значений для разрешения записей тега.
	 *
	 * @param <T> тип значений реестра
	 */
	public interface ValueGetter<T> {

		/**
		 * Возвращает прямое значение по идентификатору.
		 *
		 * @param id       идентификатор объекта
		 * @param required если {@code true} — объект обязан существовать
		 * @return значение или {@code null}, если не найдено
		 */
		@Nullable T direct(Identifier id, boolean required);

		/**
		 * Возвращает все значения тега по его идентификатору.
		 *
		 * @param id идентификатор тега
		 * @return коллекция значений или {@code null}, если тег не найден
		 */
		@Nullable Collection<T> tag(Identifier id);
	}
}
