package net.minecraft.loot.entry;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.util.ErrorReporter;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Запись пула лута, перебирающая дочерние записи и возвращающая первую успешную.
 * Аналог логического OR: выполняется первая запись, условие которой истинно.
 */
public class AlternativeEntry extends CombinedEntry {

	public static final MapCodec<AlternativeEntry> CODEC = createCodec(AlternativeEntry::new);
	public static final ErrorReporter.Error UNREACHABLE_ENTRY_ERROR = new ErrorReporter.Error() {
		@Override
		public String getMessage() {
			return "Unreachable entry!";
		}
	};

	AlternativeEntry(List<LootPoolEntry> children, List<LootCondition> conditions) {
		super(children, conditions);
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.ALTERNATIVES;
	}

	@Override
	protected EntryCombiner combine(List<? extends EntryCombiner> terms) {
		return switch (terms.size()) {
			case 0 -> ALWAYS_FALSE;
			case 1 -> (EntryCombiner) terms.get(0);
			case 2 -> terms.get(0).or(terms.get(1));
			default -> (context, choiceConsumer) -> {
				for (EntryCombiner combiner : terms) {
					if (combiner.expand(context, choiceConsumer)) {
						return true;
					}
				}

				return false;
			};
		};
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);

		for (int index = 0; index < children.size() - 1; index++) {
			if (children.get(index).conditions.isEmpty()) {
				reporter.report(UNREACHABLE_ENTRY_ERROR);
			}
		}
	}

	public static AlternativeEntry.Builder builder(LootPoolEntry.Builder<?>... children) {
		return new AlternativeEntry.Builder(children);
	}

	public static <E> AlternativeEntry.Builder builder(
		Collection<E> children,
		Function<E, LootPoolEntry.Builder<?>> toBuilderFunction
	) {
		return new AlternativeEntry.Builder(children
			.stream()
			.map(toBuilderFunction::apply)
			.toArray(LootPoolEntry.Builder[]::new));
	}

	/** Строитель альтернативной записи пула лута. */
	public static class Builder extends LootPoolEntry.Builder<AlternativeEntry.Builder> {

		private final ImmutableList.Builder<LootPoolEntry> children = ImmutableList.builder();

		public Builder(LootPoolEntry.Builder<?>... children) {
			for (LootPoolEntry.Builder<?> builder : children) {
				this.children.add(builder.build());
			}
		}

		@Override
		protected AlternativeEntry.Builder getThisBuilder() {
			return this;
		}

		@Override
		public AlternativeEntry.Builder alternatively(LootPoolEntry.Builder<?> builder) {
			children.add(builder.build());
			return this;
		}

		@Override
		public LootPoolEntry build() {
			return new AlternativeEntry(children.build(), getConditions());
		}
	}
}
