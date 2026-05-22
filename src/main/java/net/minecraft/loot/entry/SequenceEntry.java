package net.minecraft.loot.entry;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.minecraft.loot.condition.LootCondition;

import java.util.List;

/**
 * Запись пула лута, выполняющая дочерние записи последовательно с коротким замыканием.
 * Аналог логического AND: останавливается при первой неудачной записи.
 */
public class SequenceEntry extends CombinedEntry {

	public static final MapCodec<SequenceEntry> CODEC = createCodec(SequenceEntry::new);

	SequenceEntry(List<LootPoolEntry> children, List<LootCondition> conditions) {
		super(children, conditions);
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.SEQUENCE;
	}

	@Override
	protected EntryCombiner combine(List<? extends EntryCombiner> terms) {
		return switch (terms.size()) {
			case 0 -> ALWAYS_TRUE;
			case 1 -> (EntryCombiner) terms.get(0);
			case 2 -> terms.get(0).and(terms.get(1));
			default -> (context, choiceConsumer) -> {
				for (EntryCombiner combiner : terms) {
					if (!combiner.expand(context, choiceConsumer)) {
						return false;
					}
				}

				return true;
			};
		};
	}

	public static SequenceEntry.Builder create(LootPoolEntry.Builder<?>... entries) {
		return new SequenceEntry.Builder(entries);
	}

	/** Строитель последовательной записи пула лута. */
	public static class Builder extends LootPoolEntry.Builder<SequenceEntry.Builder> {

		private final ImmutableList.Builder<LootPoolEntry> entries = ImmutableList.builder();

		public Builder(LootPoolEntry.Builder<?>... entries) {
			for (LootPoolEntry.Builder<?> builder : entries) {
				this.entries.add(builder.build());
			}
		}

		@Override
		protected SequenceEntry.Builder getThisBuilder() {
			return this;
		}

		@Override
		public SequenceEntry.Builder sequenceEntry(LootPoolEntry.Builder<?> entry) {
			entries.add(entry.build());
			return this;
		}

		@Override
		public LootPoolEntry build() {
			return new SequenceEntry(entries.build(), getConditions());
		}
	}
}
