package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.ErrorReporter;

import java.util.List;
import java.util.Optional;

/** Функция лута, применяющая одну из двух дочерних функций в зависимости от соответствия предмета фильтру. */
public class FilteredLootFunction extends ConditionalLootFunction {

	public static final MapCodec<FilteredLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				ItemPredicate.CODEC
					.fieldOf("item_filter")
					.forGetter(function -> function.itemFilter),
				LootFunctionTypes.CODEC
					.optionalFieldOf("on_pass")
					.forGetter(function -> function.onPass),
				LootFunctionTypes.CODEC
					.optionalFieldOf("on_fail")
					.forGetter(function -> function.onFail)
			))
			.apply(instance, FilteredLootFunction::new)
	);

	private final ItemPredicate itemFilter;
	private final Optional<LootFunction> onPass;
	private final Optional<LootFunction> onFail;

	FilteredLootFunction(
		List<LootCondition> conditions,
		ItemPredicate itemFilter,
		Optional<LootFunction> onPass,
		Optional<LootFunction> onFail
	) {
		super(conditions);
		this.itemFilter = itemFilter;
		this.onPass = onPass;
		this.onFail = onFail;
	}

	@Override
	public LootFunctionType<FilteredLootFunction> getType() {
		return LootFunctionTypes.FILTERED;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		Optional<LootFunction> branch = itemFilter.test(stack) ? onPass : onFail;
		return branch.isPresent() ? branch.get().apply(stack, context) : stack;
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);
		onPass.ifPresent(fn -> fn.validate(reporter.makeChild(new ErrorReporter.MapElementContext("on_pass"))));
		onFail.ifPresent(fn -> fn.validate(reporter.makeChild(new ErrorReporter.MapElementContext("on_fail"))));
	}

	public static Builder builder(ItemPredicate itemFilter) {
		return new Builder(itemFilter);
	}

	/** Строитель функции фильтрации предметов. */
	public static class Builder extends ConditionalLootFunction.Builder<Builder> {

		private final ItemPredicate itemFilter;
		private Optional<LootFunction> onPass = Optional.empty();
		private Optional<LootFunction> onFail = Optional.empty();

		Builder(ItemPredicate itemFilter) {
			this.itemFilter = itemFilter;
		}

		@Override
		protected Builder getThisBuilder() {
			return this;
		}

		public Builder onPass(Optional<LootFunction> onPass) {
			this.onPass = onPass;
			return this;
		}

		public Builder onFail(Optional<LootFunction> onFail) {
			this.onFail = onFail;
			return this;
		}

		@Override
		public LootFunction build() {
			return new FilteredLootFunction(getConditions(), itemFilter, onPass, onFail);
		}
	}
}
