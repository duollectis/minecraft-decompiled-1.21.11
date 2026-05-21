package net.minecraft.registry.tag;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code TagBuilder}.
 */
public class TagBuilder {

	private final List<TagEntry> entries = new ArrayList<>();

	public static TagBuilder create() {
		return new TagBuilder();
	}

	public List<TagEntry> build() {
		return List.copyOf(this.entries);
	}

	public TagBuilder add(TagEntry entry) {
		this.entries.add(entry);
		return this;
	}

	public TagBuilder add(Identifier id) {
		return this.add(TagEntry.create(id));
	}

	public TagBuilder addOptional(Identifier id) {
		return this.add(TagEntry.createOptional(id));
	}

	public TagBuilder addTag(Identifier id) {
		return this.add(TagEntry.createTag(id));
	}

	public TagBuilder addOptionalTag(Identifier id) {
		return this.add(TagEntry.createOptionalTag(id));
	}
}
