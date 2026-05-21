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

/**
 * {@code LootPool}.
 */
public class LootPool {

	public static final Codec<LootPool> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					                    LootPoolEntryTypes.CODEC.listOf().fieldOf("entries").forGetter(pool -> pool.entries),
					                    LootCondition.CODEC
							                    .listOf()
							                    .optionalFieldOf("conditions", List.of())
							                    .forGetter(pool -> pool.conditions),
					                    LootFunctionTypes.CODEC
							                    .listOf()
							                    .optionalFieldOf("functions", List.of())
							                    .forGetter(pool -> pool.functions),
					                    LootNumberProviderTypes.CODEC.fieldOf("rolls").forGetter(pool -> pool.rolls),
					                    LootNumberProviderTypes.CODEC
							                    .fieldOf("bonus_rolls")
							                    .orElse(ConstantLootNumberProvider.create(0.0F))
							                    .forGetter(pool -> pool.bonusRolls)
			                    )
			                    .apply(instance, LootPool::new)
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
		this.predicate = Util.allOf(conditions);
		this.functions = functions;
		this.javaFunctions = LootFunctionTypes.join(functions);
		this.rolls = rolls;
		this.bonusRolls = bonusRolls;
	}

	private void supplyOnce(Consumer<ItemStack> lootConsumer, LootContext context) {
		Random random = context.getRandom();
		List<LootChoice> list = Lists.newArrayList();
		MutableInt mutableInt = new MutableInt();

		for (LootPoolEntry lootPoolEntry : this.entries) {
			lootPoolEntry.expand(
					context, choice -> {
						int i = choice.getWeight(context.getLuck());
						if (i > 0) {
							list.add(choice);
							mutableInt.add(i);
						}
					}
			);
		}

		int i = list.size();
		if (mutableInt.intValue() != 0 && i != 0) {
			if (i == 1) {
				list.get(0).generateLoot(lootConsumer, context);
			}
			else {
				int j = random.nextInt(mutableInt.intValue());

				for (LootChoice lootChoice : list) {
					j -= lootChoice.getWeight(context.getLuck());
					if (j < 0) {
						lootChoice.generateLoot(lootConsumer, context);
						return;
					}
				}
			}
		}
	}

	/**
	 * Добавляет generated loot.
	 *
	 * @param lootConsumer loot consumer
	 * @param context context
	 */
	public void addGeneratedLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
		if (this.predicate.test(context)) {
			Consumer<ItemStack> consumer = LootFunction.apply(this.javaFunctions, lootConsumer, context);
			int
					i =
					this.rolls.nextInt(context) + MathHelper.floor(
							this.bonusRolls.nextFloat(context) * context.getLuck());

			for (int j = 0; j < i; j++) {
				this.supplyOnce(consumer, context);
			}
		}
	}

	/**
	 * Validate.
	 *
	 * @param reporter reporter
	 */
	public void validate(LootTableReporter reporter) {
		for (int i = 0; i < this.conditions.size(); i++) {
			this.conditions
					.get(i)
					.validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("conditions", i)));
		}

		for (int i = 0; i < this.functions.size(); i++) {
			this.functions
					.get(i)
					.validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("functions", i)));
		}

		for (int i = 0; i < this.entries.size(); i++) {
			this.entries.get(i).validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("entries", i)));
		}

		this.rolls.validate(reporter.makeChild(new ErrorReporter.MapElementContext("rolls")));
		this.bonusRolls.validate(reporter.makeChild(new ErrorReporter.MapElementContext("bonus_rolls")));
	}

	public static LootPool.Builder builder() {
		return new LootPool.Builder();
	}

	public static class Builder
			implements LootFunctionConsumingBuilder<LootPool.Builder>,
			LootConditionConsumingBuilder<LootPool.Builder>,
			FabricLootPoolBuilder {

		@Override
		public LootPool.Builder getThisConditionConsumingBuilder() {
			return this;
		}

		private final com.google.common.collect.ImmutableList.Builder<LootPoolEntry> entries = ImmutableList.builder();
		private final com.google.common.collect.ImmutableList.Builder<LootCondition>
				conditions =
				ImmutableList.builder();
		private final com.google.common.collect.ImmutableList.Builder<LootFunction> functions = ImmutableList.builder();
		private LootNumberProvider rolls = ConstantLootNumberProvider.create(1.0F);
		private LootNumberProvider bonusRollsRange = ConstantLootNumberProvider.create(0.0F);

		public LootPool.Builder rolls(LootNumberProvider rolls) {
			this.rolls = rolls;
			return this;
		}

		public LootPool.Builder getThisFunctionConsumingBuilder() {
			return this;
		}

		public LootPool.Builder bonusRolls(LootNumberProvider bonusRolls) {
			this.bonusRollsRange = bonusRolls;
			return this;
		}

		public LootPool.Builder with(LootPoolEntry.Builder<?> entry) {
			this.entries.add(entry.build());
			return this;
		}

		public LootPool.Builder conditionally(LootCondition.Builder builder) {
			this.conditions.add(builder.build());
			return this;
		}

		public LootPool.Builder apply(LootFunction.Builder builder) {
			this.functions.add(builder.build());
			return this;
		}

		/**
		 * Build.
		 *
		 * @return LootPool — результат операции
		 */
		public LootPool build() {
			return new LootPool(
					this.entries.build(),
					this.conditions.build(),
					this.functions.build(),
					this.rolls,
					this.bonusRollsRange
			);
		}
	}
}
