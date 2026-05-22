package net.minecraft.registry.tag;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Строитель тега — накапливает записи и создаёт иммутабельный список.
 *
 * <p>Поддерживает четыре вида записей:
 * <ul>
 *   <li>обязательная прямая ссылка на элемент ({@link #add(Identifier)})</li>
 *   <li>необязательная прямая ссылка ({@link #addOptional(Identifier)})</li>
 *   <li>обязательная ссылка на другой тег ({@link #addTag(Identifier)})</li>
 *   <li>необязательная ссылка на другой тег ({@link #addOptionalTag(Identifier)})</li>
 * </ul>
 */
public class TagBuilder {

	private final List<TagEntry> entries = new ArrayList<>();

	public static TagBuilder create() {
		return new TagBuilder();
	}

	public List<TagEntry> build() {
		return List.copyOf(entries);
	}

	public TagBuilder add(TagEntry entry) {
		entries.add(entry);
		return this;
	}

	public TagBuilder add(Identifier id) {
		return add(TagEntry.create(id));
	}

	public TagBuilder addOptional(Identifier id) {
		return add(TagEntry.createOptional(id));
	}

	public TagBuilder addTag(Identifier id) {
		return add(TagEntry.createTag(id));
	}

	public TagBuilder addOptionalTag(Identifier id) {
		return add(TagEntry.createOptionalTag(id));
	}
}
