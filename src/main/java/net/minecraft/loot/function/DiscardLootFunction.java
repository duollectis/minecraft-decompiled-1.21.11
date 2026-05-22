package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;

import java.util.List;

/** Функция лута, безусловно уничтожающая предмет (возвращает пустой стак). */
public class DiscardLootFunction extends ConditionalLootFunction {

	public static final MapCodec<DiscardLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance).apply(instance, DiscardLootFunction::new)
	);

	protected DiscardLootFunction(List<LootCondition> conditions) {
		super(conditions);
	}

	@Override
	public LootFunctionType<DiscardLootFunction> getType() {
		return LootFunctionTypes.DISCARD;
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		return ItemStack.EMPTY;
	}

	public static ConditionalLootFunction.Builder<?> builder() {
		return builder(DiscardLootFunction::new);
	}
}
