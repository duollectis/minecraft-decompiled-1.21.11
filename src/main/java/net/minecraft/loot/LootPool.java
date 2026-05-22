package net.minecraft.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.loot.v3.FabricLootPoolBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionConsumingBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootPoolEntryTypes;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionConsumingBuilder;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class LootPool {

	public static final Codec<LootPool> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					LootPoolEntryTypes.CODEC
							.listOf()
							.fieldOf("entries")
							.forGetter(pool -> pool.entries),
					LootCondition.CODEC
							.listOf()
							.optionalFieldOf("conditions", List.of())
							.forGetter(pool -> pool.conditions),
					LootFunctionTypes.CODEC
							.listOf()
							.optionalFieldOf("functions", List.of())
							.forGetter(pool -> pool.functions),
					LootNumberProviderTypes.CODEC
							.fieldOf("rolls")
							.forGetter(pool -> pool.rolls),
					LootNumberProviderTypes.CODEC
							.fieldOf("bonus_rolls")
							.orElse(ConstantLootNumberProvider.create(0.0F))
							.forGetter(pool -> pool.bonusRolls)
			).apply(instance, LootPool::new)
	);

	public final List<LootPoolEntry> entries;
	public final List<LootCondition> conditions;
	private final Predicate<LootContext> predicate;
	public final List<LootFunction> functions;
	private final BiFunction<ItemStack, LootContext, ItemStack> javaFunctions;
	public final LootNumberProvider rolls;
	public final LootNumberProvider bonusRolls;

	LootPool(
			List<LootPoolEntry> entries,
			List<LootCondition> conditions,
			List<LootFunction> functions,
			LootNumberProvider rolls,
			LootNumberProvider bonusRolls
	) {
		this.entries = entries;
		this.conditions = conditions;
		predicate = Util.allOf(conditions);
		this.functions = functions;
		javaFunctions = LootFunctionTypes.join(functions);
		this.rolls = rolls;
		this.bonusRolls = bonusRolls;
	}

	/**
	 * Выполняет один розыгрыш из пула: собирает все доступные варианты с их весами,
	 * затем выбирает один случайным образом пропорционально весу.
	 */
	private void supplyOnce(Consumer<ItemStack> lootConsumer, LootContext context) {
		Random random = context.getRandom();
		List<LootChoice> choices = Lists.newArrayList();
		MutableInt totalWeight = new MutableInt();

		for (LootPoolEntry entry : entries) {
			entry.expand(context, choice -> {
				int weight = choice.getWeight(context.getLuck());

				if (weight > 0) {
					choices.add(choice);
					totalWeight.add(weight);
				}
			});
		}

		int choiceCount = choices.size();

		if (totalWeight.intValue() == 0 || choiceCount == 0) {
			return;
		}

		if (choiceCount == 1) {
			choices.get(0).generateLoot(lootConsumer, context);
			return;
		}

		int roll = random.nextInt(totalWeight.intValue());

		for (LootChoice choice : choices) {
			roll -= choice.getWeight(context.getLuck());

			if (roll < 0) {
				choice.generateLoot(lootConsumer, context);
				return;
			}
		}
	}

	/**
	 * Генерирует лут для данного пула: вычисляет количество розыгрышей с учётом бонусных бросков
	 * от удачи игрока, затем выполняет каждый розыгрыш через {@link #supplyOnce}.
	 *
	 * @param lootConsumer получатель сгенерированных стаков
	 * @param context контекст лута с параметрами мира и игрока
	 */
	public void addGeneratedLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
		if (!predicate.test(context)) {
			return;
		}

		Consumer<ItemStack> consumer = LootFunction.apply(javaFunctions, lootConsumer, context);
		int rollCount = rolls.nextInt(context)
				+ MathHelper.floor(bonusRolls.nextFloat(context) * context.getLuck());

		for (int rollIndex = 0; rollIndex < rollCount; rollIndex++) {
			supplyOnce(consumer, context);
		}
	}

	public void validate(LootTableReporter reporter) {
		for (int index = 0; index < conditions.size(); index++) {
			conditions.get(index)
					.validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("conditions", index)));
		}

		for (int index = 0; index < functions.size(); index++) {
			functions.get(index)
					.validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("functions", index)));
		}

		for (int index = 0; index < entries.size(); index++) {
			entries.get(index)
					.validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("entries", index)));
		}

		rolls.validate(reporter.makeChild(new ErrorReporter.MapElementContext("rolls")));
		bonusRolls.validate(reporter.makeChild(new ErrorReporter.MapElementContext("bonus_rolls")));
	}

	public static LootPool.Builder builder() {
		return new LootPool.Builder();
	}

	public static class Builder
			implements LootFunctionConsumingBuilder<LootPool.Builder>,
			LootConditionConsumingBuilder<LootPool.Builder>,
			FabricLootPoolBuilder {

		private final ImmutableList.Builder<LootPoolEntry> entries = ImmutableList.builder();
		private final ImmutableList.Builder<LootCondition> conditions = ImmutableList.builder();
		private final ImmutableList.Builder<LootFunction> functions = ImmutableList.builder();
		private LootNumberProvider rolls = ConstantLootNumberProvider.create(1.0F);
		private LootNumberProvider bonusRollsRange = ConstantLootNumberProvider.create(0.0F);

		@Override
		public LootPool.Builder getThisConditionConsumingBuilder() {
			return this;
		}

		public LootPool.Builder rolls(LootNumberProvider rolls) {
			this.rolls = rolls;
			return this;
		}

		public LootPool.Builder getThisFunctionConsumingBuilder() {
			return this;
		}

		public LootPool.Builder bonusRolls(LootNumberProvider bonusRolls) {
			bonusRollsRange = bonusRolls;
			return this;
		}

		public LootPool.Builder with(LootPoolEntry.Builder<?> entry) {
			entries.add(entry.build());
			return this;
		}

		public LootPool.Builder conditionally(LootCondition.Builder builder) {
			conditions.add(builder.build());
			return this;
		}

		public LootPool.Builder apply(LootFunction.Builder builder) {
			functions.add(builder.build());
			return this;
		}

		public LootPool build() {
			return new LootPool(
					entries.build(),
					conditions.build(),
					functions.build(),
					rolls,
					bonusRollsRange
			);
		}
	}
}
