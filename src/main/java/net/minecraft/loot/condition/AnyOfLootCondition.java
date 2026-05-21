package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.Util;

import java.util.List;

/**
 * {@code AnyOfLootCondition}.
 */
public class AnyOfLootCondition extends AlternativeLootCondition {

	public static final MapCodec<AnyOfLootCondition> CODEC = createCodec(AnyOfLootCondition::new);

	AnyOfLootCondition(List<LootCondition> terms) {
		super(terms, Util.anyOf(terms));
	}

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.ANY_OF;
	}

	public static AnyOfLootCondition.Builder builder(LootCondition.Builder... terms) {
		return new AnyOfLootCondition.Builder(terms);
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder extends AlternativeLootCondition.Builder {

		public Builder(LootCondition.Builder... builders) {
			super(builders);
		}

		@Override
		public AnyOfLootCondition.Builder or(LootCondition.Builder builder) {
			this.add(builder);
			return this;
		}

		@Override
		protected LootCondition build(List<LootCondition> terms) {
			return new AnyOfLootCondition(terms);
		}
	}
}
