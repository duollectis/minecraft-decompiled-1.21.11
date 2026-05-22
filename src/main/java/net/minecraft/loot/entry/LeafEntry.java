package net.minecraft.loot.entry;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootChoice;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionConsumingBuilder;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Базовый класс для листовых записей пула лута, генерирующих конкретные предметы. */
public abstract class LeafEntry extends LootPoolEntry {

	public static final int DEFAULT_WEIGHT = 1;
	public static final int DEFAULT_QUALITY = 0;

	protected final int weight;
	protected final int quality;
	protected final List<LootFunction> functions;
	final BiFunction<ItemStack, LootContext, ItemStack> compiledFunctions;

	private final LootChoice choice = new LeafEntry.Choice() {
		@Override
		public void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
			LeafEntry.this.generateLoot(
				LootFunction.apply(LeafEntry.this.compiledFunctions, lootConsumer, context),
				context
			);
		}
	};

	protected LeafEntry(int weight, int quality, List<LootCondition> conditions, List<LootFunction> functions) {
		super(conditions);
		this.weight = weight;
		this.quality = quality;
		this.functions = functions;
		this.compiledFunctions = LootFunctionTypes.join(functions);
	}

	protected static <T extends LeafEntry> P4<Mu<T>, Integer, Integer, List<LootCondition>, List<LootFunction>> addLeafFields(
		Instance<T> instance
	) {
		return instance.group(
			Codec.INT.optionalFieldOf("weight", DEFAULT_WEIGHT).forGetter(entry -> entry.weight),
			Codec.INT.optionalFieldOf("quality", DEFAULT_QUALITY).forGetter(entry -> entry.quality)
		)
		.and(addConditionsField(instance).t1())
		.and(LootFunctionTypes.CODEC
			.listOf()
			.optionalFieldOf("functions", List.of())
			.forGetter(entry -> entry.functions));
	}

	@Override
	public void validate(LootTableReporter reporter) {
		super.validate(reporter);

		for (int index = 0; index < functions.size(); index++) {
			functions.get(index).validate(
				reporter.makeChild(new ErrorReporter.NamedListElementContext("functions", index))
			);
		}
	}

	protected abstract void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context);

	@Override
	public boolean expand(LootContext lootContext, Consumer<LootChoice> consumer) {
		if (!test(lootContext)) {
			return false;
		}

		consumer.accept(choice);
		return true;
	}

	public static LeafEntry.Builder<?> builder(LeafEntry.Factory factory) {
		return new LeafEntry.BasicBuilder(factory);
	}

	static class BasicBuilder extends LeafEntry.Builder<LeafEntry.BasicBuilder> {

		private final LeafEntry.Factory factory;

		public BasicBuilder(LeafEntry.Factory factory) {
			this.factory = factory;
		}

		@Override
		public LeafEntry.BasicBuilder getThisFunctionConsumingBuilder() {
			return this;
		}

		@Override
		protected LeafEntry.BasicBuilder getThisBuilder() {
			return this;
		}

		@Override
		public LootPoolEntry build() {
			return factory.build(weight, quality, getConditions(), getFunctions());
		}
	}

	/** Строитель листовой записи с поддержкой веса, качества и функций. */
	public abstract static class Builder<T extends LeafEntry.Builder<T>>
		extends LootPoolEntry.Builder<T>
		implements LootFunctionConsumingBuilder<T> {

		protected int weight = DEFAULT_WEIGHT;
		protected int quality = DEFAULT_QUALITY;
		private final ImmutableList.Builder<LootFunction> functions = ImmutableList.builder();

		public T apply(LootFunction.Builder builder) {
			functions.add(builder.build());
			return getThisBuilder();
		}

		protected List<LootFunction> getFunctions() {
			return functions.build();
		}

		public T weight(int weight) {
			this.weight = weight;
			return getThisBuilder();
		}

		public T quality(int quality) {
			this.quality = quality;
			return getThisBuilder();
		}
	}

	/** Абстрактный выбор предмета из листовой записи с учётом веса и удачи. */
	protected abstract class Choice implements LootChoice {

		@Override
		public int getWeight(float luck) {
			return Math.max(MathHelper.floor(LeafEntry.this.weight + LeafEntry.this.quality * luck), 0);
		}
	}

	/** Фабрика для создания листовых записей из параметров веса, качества, условий и функций. */
	@FunctionalInterface
	protected interface Factory {

		LeafEntry build(int weight, int quality, List<LootCondition> conditions, List<LootFunction> functions);
	}
}
