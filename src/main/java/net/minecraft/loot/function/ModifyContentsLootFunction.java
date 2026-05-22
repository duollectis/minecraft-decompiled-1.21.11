package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.ContainerComponentModifier;
import net.minecraft.loot.ContainerComponentModifiers;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;

import java.util.List;

/** Функция лута, применяющая дочернюю функцию к каждому предмету внутри контейнерного компонента. */
public class ModifyContentsLootFunction extends ConditionalLootFunction {

	public static final MapCodec<ModifyContentsLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				ContainerComponentModifiers.MODIFIER_CODEC
					.fieldOf("component")
					.forGetter(function -> function.component),
				LootFunctionTypes.CODEC
					.fieldOf("modifier")
					.forGetter(function -> function.modifier)
			))
			.apply(instance, ModifyContentsLootFunction::new)
	);

	private final ContainerComponentModifier<?> component;
	private final LootFunction modifier;

	private ModifyContentsLootFunction(
		List<LootCondition> conditions,
		ContainerComponentModifier<?> component,
		LootFunction modifier
	) {
		super(conditions);
		this.component = component;
		this.modifier = modifier;
	}

	@Override
	public LootFunctionType<ModifyContentsLootFunction> getType() {
		return LootFunctionTypes.MODIFY_CONTENTS;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (stack.isEmpty()) {
			return stack;
		}

		component.apply(stack, content -> modifier.apply(content, context));

		return stack;
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);
		modifier.validate(reporter.makeChild(new ErrorReporter.MapElementContext("modifier")));
	}
}
