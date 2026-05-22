package net.minecraft.loot.entry;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import net.minecraft.loot.condition.LootCondition;

import java.util.List;

/**
 * Запись пула лута, выполняющая все дочерние записи безусловно.
 * Аналог логического AND без короткого замыкания: все дочерние записи всегда раскрываются.
 */
public class GroupEntry extends CombinedEntry {

	public static final MapCodec<GroupEntry> CODEC = createCodec(GroupEntry::new);

	GroupEntry(List<LootPoolEntry> children, List<LootCondition> conditions) {
		super(children, conditions);
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.GROUP;
	}

	@Override
	protected EntryCombiner combine(List<? extends EntryCombiner> terms) {
		return switch (terms.size()) {
			case 0 -> ALWAYS_TRUE;
			case 1 -> (EntryCombiner) terms.get(0);
			case 2 -> {
				EntryCombiner first = terms.get(0);
				EntryCombiner second = terms.get(1);
				yield (context, choiceConsumer) -> {
					first.expand(context, choiceConsumer);
					second.expand(context, choiceConsumer);
					return true;
				};
			}
			default -> (context, choiceConsumer) -> {
				for (EntryCombiner combiner : terms) {
					combiner.expand(context, choiceConsumer);
				}

				return true;
			};
		};
	}

	public static GroupEntry.Builder create(LootPoolEntry.Builder<?>... entries) {
		return new GroupEntry.Builder(entries);
	}

	/** Строитель групповой записи пула лута. */
	public static class Builder extends LootPoolEntry.Builder<GroupEntry.Builder> {

		private final ImmutableList.Builder<LootPoolEntry> entries = ImmutableList.builder();

		public Builder(LootPoolEntry.Builder<?>... entries) {
			for (LootPoolEntry.Builder<?> builder : entries) {
				this.entries.add(builder.build());
			}
		}

		@Override
		protected GroupEntry.Builder getThisBuilder() {
			return this;
		}

		@Override
		public GroupEntry.Builder groupEntry(LootPoolEntry.Builder<?> entry) {
			entries.add(entry.build());
			return this;
		}

		@Override
		public LootPoolEntry build() {
			return new GroupEntry(entries.build(), getConditions());
		}
	}
}
