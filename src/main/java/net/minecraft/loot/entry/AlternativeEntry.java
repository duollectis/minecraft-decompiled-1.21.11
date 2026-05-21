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
 * {@code AlternativeEntry}.
 */
public class AlternativeEntry extends CombinedEntry {

	public static final MapCodec<AlternativeEntry> CODEC = createCodec(AlternativeEntry::new);
	public static final ErrorReporter.Error UNREACHABLE_ENTRY_ERROR = new ErrorReporter.Error() {
		@Override
		public String getMessage() {
			return "Unreachable entry!";
		}
	};

	AlternativeEntry(List<LootPoolEntry> list, List<LootCondition> list2) {
		super(list, list2);
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
			default -> (context, lootChoiceExpander) -> {
				for (EntryCombiner entryCombiner : terms) {
					if (entryCombiner.expand(context, lootChoiceExpander)) {
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

		for (int i = 0; i < this.children.size() - 1; i++) {
			if (this.children.get(i).conditions.isEmpty()) {
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

	/**
	 * {@code Builder}.
	 */
	public static class Builder extends LootPoolEntry.Builder<AlternativeEntry.Builder> {

		private final com.google.common.collect.ImmutableList.Builder<LootPoolEntry> children = ImmutableList.builder();

		public Builder(LootPoolEntry.Builder<?>... children) {
			for (LootPoolEntry.Builder<?> builder : children) {
				this.children.add(builder.build());
			}
		}

		protected AlternativeEntry.Builder getThisBuilder() {
			return this;
		}

		@Override
		public AlternativeEntry.Builder alternatively(LootPoolEntry.Builder<?> builder) {
			this.children.add(builder.build());
			return this;
		}

		@Override
		public LootPoolEntry build() {
			return new AlternativeEntry(this.children.build(), this.getConditions());
		}
	}
}
