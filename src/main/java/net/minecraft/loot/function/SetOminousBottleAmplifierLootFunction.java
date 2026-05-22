package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.OminousBottleAmplifierComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Set;

/**
 * Функция лута, устанавливающая усилитель зловещей бутылки.
 * Значение зажимается в допустимом диапазоне [{@code MIN_AMPLIFIER}, {@code MAX_AMPLIFIER}].
 */
public class SetOminousBottleAmplifierLootFunction extends ConditionalLootFunction {

	private static final int MIN_AMPLIFIER = 0;
	private static final int MAX_AMPLIFIER = 4;

	static final MapCodec<SetOminousBottleAmplifierLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(LootNumberProviderTypes.CODEC
				.fieldOf("amplifier")
				.forGetter(lootFunction -> lootFunction.amplifier))
			.apply(instance, SetOminousBottleAmplifierLootFunction::new)
	);

	private final LootNumberProvider amplifier;

	private SetOminousBottleAmplifierLootFunction(List<LootCondition> conditions, LootNumberProvider amplifier) {
		super(conditions);
		this.amplifier = amplifier;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return amplifier.getAllowedParameters();
	}

	@Override
	public LootFunctionType<SetOminousBottleAmplifierLootFunction> getType() {
		return LootFunctionTypes.SET_OMINOUS_BOTTLE_AMPLIFIER;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		int clampedAmplifier = MathHelper.clamp(amplifier.nextInt(context), MIN_AMPLIFIER, MAX_AMPLIFIER);
		stack.set(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifierComponent(clampedAmplifier));
		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder(LootNumberProvider amplifier) {
		return builder(conditions -> new SetOminousBottleAmplifierLootFunction(conditions, amplifier));
	}
}
