package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class ExplosionDecayLootFunction extends ConditionalLootFunction {

	public static final MapCodec<ExplosionDecayLootFunction> CODEC = RecordCodecBuilder.mapCodec(
			instance -> addConditionsField(instance).apply(instance, ExplosionDecayLootFunction::new)
	);

	private ExplosionDecayLootFunction(List<LootCondition> conditions) {
		super(conditions);
	}

	@Override
	public LootFunctionType<ExplosionDecayLootFunction> getType() {
		return LootFunctionTypes.EXPLOSION_DECAY;
	}

	/**
	 * Уменьшает количество предметов в стаке пропорционально радиусу взрыва.
	 * Каждый предмет выживает с вероятностью {@code 1 / explosionRadius}.
	 */
	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		Float explosionRadius = context.get(LootContextParameters.EXPLOSION_RADIUS);

		if (explosionRadius == null) {
			return stack;
		}

		Random random = context.getRandom();
		float survivalChance = 1.0F / explosionRadius;
		int survivors = 0;

		for (int item = 0; item < stack.getCount(); item++) {
			if (random.nextFloat() <= survivalChance) {
				survivors++;
			}
		}

		stack.setCount(survivors);

		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder() {
		return builder(ExplosionDecayLootFunction::new);
	}
}
