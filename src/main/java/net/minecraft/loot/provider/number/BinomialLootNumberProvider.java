package net.minecraft.loot.provider.number;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.random.Random;

import java.util.Set;

/**
 * Провайдер числа, реализующий биномиальное распределение B(n, p).
 * Выполняет {@code n} независимых испытаний Бернулли с вероятностью успеха {@code p}
 * и возвращает суммарное количество успехов.
 */
public record BinomialLootNumberProvider(LootNumberProvider n, LootNumberProvider p) implements LootNumberProvider {

	public static final MapCodec<BinomialLootNumberProvider> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					LootNumberProviderTypes.CODEC.fieldOf("n").forGetter(BinomialLootNumberProvider::n),
					LootNumberProviderTypes.CODEC.fieldOf("p").forGetter(BinomialLootNumberProvider::p)
			).apply(instance, BinomialLootNumberProvider::new)
	);

	@Override
	public LootNumberProviderType getType() {
		return LootNumberProviderTypes.BINOMIAL;
	}

	@Override
	public int nextInt(LootContext context) {
		int trials = n.nextInt(context);
		float probability = p.nextFloat(context);
		Random random = context.getRandom();
		int successes = 0;

		for (int trial = 0; trial < trials; trial++) {
			if (random.nextFloat() < probability) {
				successes++;
			}
		}

		return successes;
	}

	@Override
	public float nextFloat(LootContext context) {
		return nextInt(context);
	}

	public static BinomialLootNumberProvider create(int n, float p) {
		return new BinomialLootNumberProvider(
				ConstantLootNumberProvider.create(n),
				ConstantLootNumberProvider.create(p)
		);
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Sets.union(n.getAllowedParameters(), p.getAllowedParameters());
	}
}
