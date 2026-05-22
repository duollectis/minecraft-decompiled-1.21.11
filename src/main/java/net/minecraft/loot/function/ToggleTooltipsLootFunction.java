package net.minecraft.loot.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Функция лута, переключающая видимость подсказок (tooltip) компонентов предмета.
 * Значение {@code true} в карте означает, что подсказка должна быть скрыта.
 */
public class ToggleTooltipsLootFunction extends ConditionalLootFunction {

	public static final MapCodec<ToggleTooltipsLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(Codec
				.unboundedMap(ComponentType.CODEC, Codec.BOOL)
				.fieldOf("toggles")
				.forGetter(lootFunction -> lootFunction.toggles))
			.apply(instance, ToggleTooltipsLootFunction::new)
	);

	private final Map<ComponentType<?>, Boolean> toggles;

	private ToggleTooltipsLootFunction(List<LootCondition> conditions, Map<ComponentType<?>, Boolean> toggles) {
		super(conditions);
		this.toggles = toggles;
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		stack.apply(
			DataComponentTypes.TOOLTIP_DISPLAY,
			TooltipDisplayComponent.DEFAULT,
			current -> {
				TooltipDisplayComponent result = current;
				for (Entry<ComponentType<?>, Boolean> entry : toggles.entrySet()) {
					boolean hidden = entry.getValue();
					result = result.with(entry.getKey(), !hidden);
				}
				return result;
			}
		);
		return stack;
	}

	@Override
	public LootFunctionType<ToggleTooltipsLootFunction> getType() {
		return LootFunctionTypes.TOGGLE_TOOLTIPS;
	}
}
