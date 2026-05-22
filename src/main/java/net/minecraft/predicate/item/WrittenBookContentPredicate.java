package net.minecraft.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.collection.CollectionPredicate;
import net.minecraft.predicate.component.ComponentSubPredicate;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Предикат для проверки содержимого написанной книги:
 * страницы, автор, название, поколение и флаг разрешения.
 */
public record WrittenBookContentPredicate(
		Optional<CollectionPredicate<RawFilteredPair<Text>, WrittenBookContentPredicate.RawTextPredicate>> pages,
		Optional<String> author,
		Optional<String> title,
		NumberRange.IntRange generation,
		Optional<Boolean> resolved
) implements ComponentSubPredicate<WrittenBookContentComponent> {

	public static final Codec<WrittenBookContentPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					CollectionPredicate.createCodec(WrittenBookContentPredicate.RawTextPredicate.CODEC)
							.optionalFieldOf("pages")
							.forGetter(WrittenBookContentPredicate::pages),
					Codec.STRING.optionalFieldOf("author").forGetter(WrittenBookContentPredicate::author),
					Codec.STRING.optionalFieldOf("title").forGetter(WrittenBookContentPredicate::title),
					NumberRange.IntRange.CODEC
							.optionalFieldOf("generation", NumberRange.IntRange.ANY)
							.forGetter(WrittenBookContentPredicate::generation),
					Codec.BOOL.optionalFieldOf("resolved").forGetter(WrittenBookContentPredicate::resolved)
			)
			.apply(instance, WrittenBookContentPredicate::new)
	);

	@Override
	public ComponentType<WrittenBookContentComponent> getComponentType() {
		return DataComponentTypes.WRITTEN_BOOK_CONTENT;
	}

	public boolean test(WrittenBookContentComponent component) {
		if (author.isPresent() && !author.get().equals(component.author())) {
			return false;
		}

		if (title.isPresent() && !title.get().equals(component.title().raw())) {
			return false;
		}

		if (!generation.test(component.generation())) {
			return false;
		}

		if (resolved.isPresent() && resolved.get() != component.resolved()) {
			return false;
		}

		return pages.isEmpty() || pages.get().test(component.pages());
	}

	/**
	 * Предикат для проверки сырого (нефильтрованного) текста страницы книги.
	 */
	public record RawTextPredicate(Text contents) implements Predicate<RawFilteredPair<Text>> {

		public static final Codec<WrittenBookContentPredicate.RawTextPredicate> CODEC = TextCodecs.CODEC
				.xmap(
						WrittenBookContentPredicate.RawTextPredicate::new,
						WrittenBookContentPredicate.RawTextPredicate::contents
				);

		public boolean test(RawFilteredPair<Text> page) {
			return page.raw().equals(contents);
		}
	}
}
