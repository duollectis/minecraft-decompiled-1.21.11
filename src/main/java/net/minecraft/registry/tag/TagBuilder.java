package net.minecraft.registry.tag;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code TagBuilder}.
 */
public class TagBuilder {

	private final List<TagEntry> entries = new ArrayList<>();

	/**
	 * Create.
	 *
	 * @return TagBuilder — результат операции
	 */
	public static TagBuilder create() {
		return new TagBuilder();
	}

	/**
	 * Build.
	 *
	 * @return List — результат операции
	 */
	public List<TagEntry> build() {
		return List.copyOf(this.entries);
	}

	/**
	 * Add.
	 *
	 * @param entry entry
	 *
	 * @return TagBuilder — результат операции
	 */
	public TagBuilder add(TagEntry entry) {
		this.entries.add(entry);
		return this;
	}

	/**
	 * Add.
	 *
	 * @param id id
	 *
	 * @return TagBuilder — результат операции
	 */
	public TagBuilder add(Identifier id) {
		return this.add(TagEntry.create(id));
	}

	/**
	 * Добавляет optional.
	 *
	 * @param id id
	 *
	 * @return TagBuilder — результат операции
	 */
	public TagBuilder addOptional(Identifier id) {
		return this.add(TagEntry.createOptional(id));
	}

	/**
	 * Добавляет tag.
	 *
	 * @param id id
	 *
	 * @return TagBuilder — результат операции
	 */
	public TagBuilder addTag(Identifier id) {
		return this.add(TagEntry.createTag(id));
	}

	/**
	 * Добавляет optional tag.
	 *
	 * @param id id
	 *
	 * @return TagBuilder — результат операции
	 */
	public TagBuilder addOptionalTag(Identifier id) {
		return this.add(TagEntry.createOptionalTag(id));
	}
}
