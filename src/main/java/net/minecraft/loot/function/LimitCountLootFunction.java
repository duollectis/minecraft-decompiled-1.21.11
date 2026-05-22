package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.util.context.ContextParameter;

import java.util.List;
import java.util.Set;

/** Функция лута, ограничивающая количество предметов в стаке заданным диапазоном. */
public class LimitCountLootFunction extends ConditionalLootFunction {

	public static final MapCodec<LimitCountLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(BoundedIntUnaryOperator.CODEC.fieldOf("limit").forGetter(function -> function.limit))
			.apply(instance, LimitCountLootFunction::new)
	);

	private final BoundedIntUnaryOperator limit;

	private LimitCountLootFunction(List<LootCondition> conditions, BoundedIntUnaryOperator limit) {
		super(conditions);
		this.limit = limit;
	}

	@Override
	public LootFunctionType<LimitCountLootFunction> getType() {
		return LootFunctionTypes.LIMIT_COUNT;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return limit.getRequiredParameters();
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		int newCount = limit.apply(context, stack.getCount());
		stack.setCount(newCount);
		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder(BoundedIntUnaryOperator limit) {
		return builder(conditions -> new LimitCountLootFunction(conditions, limit));
	}
}
