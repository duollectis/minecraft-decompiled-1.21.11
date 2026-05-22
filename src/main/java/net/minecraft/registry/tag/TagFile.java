package net.minecraft.registry.tag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Представляет содержимое одного JSON-файла тега из датапака.
 *
 * <p>Поле {@code replace} управляет поведением слияния: если {@code true},
 * все записи из датапаков с более низким приоритетом игнорируются.
 * По умолчанию {@code false} — записи накапливаются из всех датапаков.</p>
 *
 * @param entries список записей тега (прямые ссылки и ссылки на другие теги)
 * @param replace если {@code true}, заменяет записи из предыдущих датапаков
 */
public record TagFile(List<TagEntry> entries, boolean replace) {

	public static final Codec<TagFile> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					TagEntry.CODEC.listOf().fieldOf("values").forGetter(TagFile::entries),
					Codec.BOOL.optionalFieldOf("replace", false).forGetter(TagFile::replace)
			).apply(instance, TagFile::new)
	);
}
