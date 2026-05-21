package net.minecraft.loot.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.util.ErrorReporter;

import java.util.List;
import java.util.function.BiFunction;

/**
 * {@code AndLootFunction}.
 */
public class AndLootFunction implements LootFunction {

	public static final MapCodec<AndLootFunction> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(LootFunctionTypes.BASE_CODEC
							.listOf()
							.fieldOf("functions")
							.forGetter(function -> function.terms))
					.apply(instance, AndLootFunction::new)
	);
	public static final Codec<AndLootFunction>
			INLINE_CODEC =
			LootFunctionTypes.BASE_CODEC.listOf().xmap(AndLootFunction::new, function -> function.terms);
	private final List<LootFunction> terms;
	private final BiFunction<ItemStack, LootContext, ItemStack> applier;

	private AndLootFunction(List<LootFunction> terms) {
		this.terms = terms;
		this.applier = LootFunctionTypes.join(terms);
	}

	/**
	 * Create.
	 *
	 * @param terms terms
	 *
	 * @return AndLootFunction — результат операции
	 */
	public static AndLootFunction create(List<LootFunction> terms) {
		return new AndLootFunction(List.copyOf(terms));
	}

	/**
	 * Apply.
	 *
	 * @param itemStack item stack
	 * @param lootContext loot context
	 *
	 * @return ItemStack — результат операции
	 */
	public ItemStack apply(ItemStack itemStack, LootContext lootContext) {
		return this.applier.apply(itemStack, lootContext);
	}

	@Override
	public void validate(LootTableReporter reporter) {
		LootFunction.super.validate(reporter);

		for (int i = 0; i < this.terms.size(); i++) {
			this.terms.get(i).validate(reporter.makeChild(new ErrorReporter.NamedListElementContext("functions", i)));
		}
	}

	@Override
	public LootFunctionType<AndLootFunction> getType() {
		return LootFunctionTypes.SEQUENCE;
	}
}
