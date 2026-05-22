package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootEntityValueSource;
import net.minecraft.util.Nameable;
import net.minecraft.util.context.ContextParameter;

import java.util.List;
import java.util.Set;

/** Функция лута, копирующая пользовательское имя из именуемого источника в предмет. */
public class CopyNameLootFunction extends ConditionalLootFunction {

	public static final MapCodec<CopyNameLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(LootEntityValueSource.ENTITY_OR_BLOCK_ENTITY_CODEC.fieldOf("source").forGetter(function -> function.source))
			.apply(instance, CopyNameLootFunction::new)
	);

	private final LootEntityValueSource<Object> source;

	private CopyNameLootFunction(List<LootCondition> conditions, LootEntityValueSource<?> source) {
		super(conditions);
		this.source = LootEntityValueSource.cast((LootEntityValueSource<? extends Object>) source);
	}

	@Override
	public LootFunctionType<CopyNameLootFunction> getType() {
		return LootFunctionTypes.COPY_NAME;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(source.contextParam());
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (source.get(context) instanceof Nameable nameable) {
			stack.set(DataComponentTypes.CUSTOM_NAME, nameable.getCustomName());
		}

		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder(LootEntityValueSource<?> source) {
		return builder(conditions -> new CopyNameLootFunction(conditions, source));
	}
}
