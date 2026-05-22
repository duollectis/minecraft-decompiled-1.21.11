package net.minecraft.loot.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;

import java.util.List;
import java.util.function.Function;

/**
 * Базовый класс для источников слотов, объединяющих несколько дочерних {@link SlotSource}.
 * Кэширует скомпилированную функцию конкатенации потоков для повторного использования.
 */
public abstract class CombinedSlotSource implements SlotSource {

	protected final List<SlotSource> terms;
	private final Function<LootContext, ItemStream> source;

	protected CombinedSlotSource(List<SlotSource> terms) {
		this.terms = terms;
		this.source = SlotSources.concat(terms);
	}

	/**
	 * Создаёт {@link MapCodec} для подкласса, сериализующий список дочерних источников.
	 *
	 * @param termsToSource фабричная функция, создающая экземпляр подкласса из списка источников
	 * @return кодек для сериализации/десериализации подкласса
	 */
	protected static <T extends CombinedSlotSource> MapCodec<T> createCodec(Function<List<SlotSource>, T> termsToSource) {
		return RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(SlotSources.CODEC.listOf().fieldOf("terms").forGetter(combined -> combined.terms))
						.apply(instance, termsToSource)
		);
	}

	/**
	 * Создаёт инлайн {@link Codec} для подкласса, сериализующий список дочерних источников напрямую.
	 *
	 * @param termsToSource фабричная функция, создающая экземпляр подкласса из списка источников
	 * @return инлайн-кодек для сокращённой JSON-записи
	 */
	protected static <T extends CombinedSlotSource> Codec<T> createInlineCodec(Function<List<SlotSource>, T> termsToSource) {
		return SlotSources.CODEC.listOf().xmap(termsToSource, combined -> combined.terms);
	}

	@Override
	public abstract MapCodec<? extends CombinedSlotSource> getCodec();

	@Override
	public ItemStream stream(LootContext context) {
		return source.apply(context);
	}

	@Override
	public void validate(LootTableReporter reporter) {
		SlotSource.super.validate(reporter);

		for (int index = 0; index < terms.size(); index++) {
			terms.get(index).validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("terms", index)));
		}
	}
}
