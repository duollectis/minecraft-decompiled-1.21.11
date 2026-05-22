package net.minecraft.loot.function;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionConsumingBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Базовый класс для функций лута с поддержкой условий применения. */
public abstract class ConditionalLootFunction implements LootFunction {

	protected final List<LootCondition> conditions;
	private final Predicate<LootContext> predicate;

	protected ConditionalLootFunction(List<LootCondition> conditions) {
		this.conditions = conditions;
		this.predicate = Util.allOf(conditions);
	}

	@Override
	public abstract LootFunctionType<? extends ConditionalLootFunction> getType();

	protected static <T extends ConditionalLootFunction> P1<Mu<T>, List<LootCondition>> addConditionsField(
		Instance<T> instance
	) {
		return instance.group(LootCondition.CODEC
			.listOf()
			.optionalFieldOf("conditions", List.of())
			.forGetter(function -> function.conditions));
	}

	@Override
	public final ItemStack apply(ItemStack itemStack, LootContext lootContext) {
		return predicate.test(lootContext) ? process(itemStack, lootContext) : itemStack;
	}

	protected abstract ItemStack process(ItemStack stack, LootContext context);

	@Override
	public void validate(LootTableReporter reporter) {
		LootFunction.super.validate(reporter);

		for (int index = 0; index < conditions.size(); index++) {
			conditions.get(index).validate(
				reporter.makeChild(new ErrorReporter.NamedListElementContext("conditions", index))
			);
		}
	}

	protected static ConditionalLootFunction.Builder<?> builder(Function<List<LootCondition>, LootFunction> joiner) {
		return new ConditionalLootFunction.Joiner(joiner);
	}

	/** Базовый строитель условной функции лута. */
	public abstract static class Builder<T extends ConditionalLootFunction.Builder<T>>
		implements LootFunction.Builder, LootConditionConsumingBuilder<T> {

		private final ImmutableList.Builder<LootCondition> conditionList = ImmutableList.builder();

		public T conditionally(LootCondition.Builder builder) {
			conditionList.add(builder.build());
			return getThisBuilder();
		}

		public final T getThisConditionConsumingBuilder() {
			return getThisBuilder();
		}

		protected abstract T getThisBuilder();

		protected List<LootCondition> getConditions() {
			return conditionList.build();
		}
	}

	static final class Joiner extends ConditionalLootFunction.Builder<ConditionalLootFunction.Joiner> {

		private final Function<List<LootCondition>, LootFunction> joiner;

		public Joiner(Function<List<LootCondition>, LootFunction> joiner) {
			this.joiner = joiner;
		}

		@Override
		protected ConditionalLootFunction.Joiner getThisBuilder() {
			return this;
		}

		@Override
		public LootFunction build() {
			return joiner.apply(getConditions());
		}
	}
}
