package net.minecraft.loot.function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.context.ContextParameter;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Функция лута, устанавливающая зачарования предмета.
 * Если {@code add = true}, уровни зачарований прибавляются к существующим (с зажимом в [0, 255]).
 * Книга автоматически превращается в зачарованную книгу.
 */
public class SetEnchantmentsLootFunction extends ConditionalLootFunction {

	public static final MapCodec<SetEnchantmentsLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(
				instance.group(
					Codec.unboundedMap(Enchantment.ENTRY_CODEC, LootNumberProviderTypes.CODEC)
						.optionalFieldOf("enchantments", Map.of())
						.forGetter(function -> function.enchantments),
					Codec.BOOL.fieldOf("add").orElse(false).forGetter(function -> function.add)
				)
			)
			.apply(instance, SetEnchantmentsLootFunction::new)
	);

	private final Map<RegistryEntry<Enchantment>, LootNumberProvider> enchantments;
	private final boolean add;

	SetEnchantmentsLootFunction(
		List<LootCondition> conditions,
		Map<RegistryEntry<Enchantment>, LootNumberProvider> enchantments,
		boolean add
	) {
		super(conditions);
		this.enchantments = Map.copyOf(enchantments);
		this.add = add;
	}

	@Override
	public LootFunctionType<SetEnchantmentsLootFunction> getType() {
		return LootFunctionTypes.SET_ENCHANTMENTS;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return enchantments
			.values()
			.stream()
			.flatMap(numberProvider -> numberProvider.getAllowedParameters().stream())
			.collect(ImmutableSet.toImmutableSet());
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		if (stack.isOf(Items.BOOK)) {
			stack = stack.withItem(Items.ENCHANTED_BOOK);
		}

		EnchantmentHelper.apply(stack, builder -> {
			if (add) {
				enchantments.forEach((enchantment, level) -> builder.set(
					enchantment,
					MathHelper.clamp(builder.getLevel(enchantment) + level.nextInt(context), 0, 255)
				));
			} else {
				enchantments.forEach((enchantment, level) -> builder.set(
					enchantment,
					MathHelper.clamp(level.nextInt(context), 0, 255)
				));
			}
		});
		return stack;
	}

	/** Строитель функции установки зачарований. */
	public static class Builder extends ConditionalLootFunction.Builder<SetEnchantmentsLootFunction.Builder> {

		private final ImmutableMap.Builder<RegistryEntry<Enchantment>, LootNumberProvider> enchantments =
			ImmutableMap.builder();
		private final boolean add;

		public Builder() {
			this(false);
		}

		public Builder(boolean add) {
			this.add = add;
		}

		@Override
		protected SetEnchantmentsLootFunction.Builder getThisBuilder() {
			return this;
		}

		public SetEnchantmentsLootFunction.Builder enchantment(
			RegistryEntry<Enchantment> enchantment,
			LootNumberProvider level
		) {
			enchantments.put(enchantment, level);
			return this;
		}

		@Override
		public LootFunction build() {
			return new SetEnchantmentsLootFunction(getConditions(), enchantments.build(), add);
		}
	}
}
