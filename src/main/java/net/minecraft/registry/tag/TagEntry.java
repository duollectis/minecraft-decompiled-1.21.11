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
 * {@code TagEntry}.
 */
public class TagEntry {

	private static final Codec<TagEntry> ENTRY_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    Codecs.TAG_ENTRY_ID.fieldOf("id").forGetter(TagEntry::getIdForCodec),
					                    Codec.BOOL.optionalFieldOf("required", true).forGetter(entry -> entry.required)
			                    )
			                    .apply(instance, TagEntry::new)
	);
	public static final Codec<TagEntry> CODEC = Codec.either(Codecs.TAG_ENTRY_ID, ENTRY_CODEC)
	                                                 .xmap(
			                                                 either -> (TagEntry) either.map(
					                                                 id -> new TagEntry(
							                                                 id,
							                                                 true
					                                                 ), entry -> entry
			                                                 ),
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
		return new Codecs.TagEntryId(this.id, this.tag);
	}

	/**
	 * Create.
	 *
	 * @param id id
	 *
	 * @return TagEntry — результат операции
	 */
	public static TagEntry create(Identifier id) {
		return new TagEntry(id, false, true);
	}

	/**
	 * Создаёт optional.
	 *
	 * @param id id
	 *
	 * @return TagEntry — результат операции
	 */
	public static TagEntry createOptional(Identifier id) {
		return new TagEntry(id, false, false);
	}

	/**
	 * Создаёт tag.
	 *
	 * @param id id
	 *
	 * @return TagEntry — результат операции
	 */
	public static TagEntry createTag(Identifier id) {
		return new TagEntry(id, true, true);
	}

	/**
	 * Создаёт optional tag.
	 *
	 * @param id id
	 *
	 * @return TagEntry — результат операции
	 */
	public static TagEntry createOptionalTag(Identifier id) {
		return new TagEntry(id, true, false);
	}

	/**
	 * Resolve.
	 *
	 * @param valueGetter value getter
	 * @param idConsumer id consumer
	 *
	 * @return boolean — результат операции
	 */
	public <T> boolean resolve(TagEntry.ValueGetter<T> valueGetter, Consumer<T> idConsumer) {
		if (this.tag) {
			Collection<T> collection = valueGetter.tag(this.id);
			if (collection == null) {
				return !this.required;
			}

			collection.forEach(idConsumer);
		}
		else {
			T object = valueGetter.direct(this.id, this.required);
			if (object == null) {
				return !this.required;
			}

			idConsumer.accept(object);
		}

		return true;
	}

	/**
	 * For each required tag id.
	 *
	 * @param idConsumer id consumer
	 */
	public void forEachRequiredTagId(Consumer<Identifier> idConsumer) {
		if (this.tag && this.required) {
			idConsumer.accept(this.id);
		}
	}

	/**
	 * For each optional tag id.
	 *
	 * @param idConsumer id consumer
	 */
	public void forEachOptionalTagId(Consumer<Identifier> idConsumer) {
		if (this.tag && !this.required) {
			idConsumer.accept(this.id);
		}
	}

	/**
	 * Проверяет возможность add.
	 *
	 * @param directEntryPredicate direct entry predicate
	 * @param tagEntryPredicate tag entry predicate
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canAdd(Predicate<Identifier> directEntryPredicate, Predicate<Identifier> tagEntryPredicate) {
		return !this.required || (this.tag ? tagEntryPredicate : directEntryPredicate).test(this.id);
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		if (this.tag) {
			stringBuilder.append('#');
		}

		stringBuilder.append(this.id);
		if (!this.required) {
			stringBuilder.append('?');
		}

		return stringBuilder.toString();
	}

	/**
	 * {@code ValueGetter}.
	 */
	public interface ValueGetter<T> {

		@Nullable T direct(Identifier id, boolean required);

		@Nullable Collection<T> tag(Identifier id);
	}
}
