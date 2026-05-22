package net.minecraft.loot.entry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootChoice;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;

import java.util.List;
import java.util.function.Consumer;

/** Базовый класс для записей пула лута, объединяющих несколько дочерних записей. */
public abstract class CombinedEntry extends LootPoolEntry {

	public static final ErrorReporter.Error EMPTY_CHILDREN_LIST_ERROR = new ErrorReporter.Error() {
		@Override
		public String getMessage() {
			return "Empty children list";
		}
	};

	protected final List<LootPoolEntry> children;
	private final EntryCombiner predicate;

	protected CombinedEntry(List<LootPoolEntry> terms, List<LootCondition> conditions) {
		super(conditions);
		this.children = terms;
		this.predicate = combine(terms);
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);

		if (children.isEmpty()) {
			reporter.report(EMPTY_CHILDREN_LIST_ERROR);
		}

		for (int index = 0; index < children.size(); index++) {
			children.get(index).validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("children", index)));
		}
	}

	protected abstract EntryCombiner combine(List<? extends EntryCombiner> terms);

	@Override
	public final boolean expand(LootContext lootContext, Consumer<LootChoice> consumer) {
		return test(lootContext) && predicate.expand(lootContext, consumer);
	}

	/**
	 * Создаёт {@link MapCodec} для конкретного подтипа {@link CombinedEntry}.
	 * Кодек сериализует список дочерних записей и условия.
	 */
	public static <T extends CombinedEntry> MapCodec<T> createCodec(CombinedEntry.Factory<T> factory) {
		return RecordCodecBuilder.mapCodec(
			instance -> instance
				.group(LootPoolEntryTypes.CODEC
					.listOf()
					.optionalFieldOf("children", List.of())
					.forGetter(entry -> entry.children))
				.and(addConditionsField(instance).t1())
				.apply(instance, factory::create)
		);
	}

	/** Фабрика для создания экземпляров {@link CombinedEntry} из списка дочерних записей и условий. */
	@FunctionalInterface
	public interface Factory<T extends CombinedEntry> {

		T create(List<LootPoolEntry> terms, List<LootCondition> conditions);
	}
}
