package net.minecraft.loot.function;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProviderTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.context.ContextParameter;

import java.util.List;
import java.util.Set;

/** Функция лута, увеличивающая количество предметов в зависимости от уровня зачарования (например, Добыча). */
public class EnchantedCountIncreaseLootFunction extends ConditionalLootFunction {

	public static final int NO_LIMIT = 0;

	public static final MapCodec<EnchantedCountIncreaseLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				Enchantment.ENTRY_CODEC
					.fieldOf("enchantment")
					.forGetter(function -> function.enchantment),
				LootNumberProviderTypes.CODEC
					.fieldOf("count")
					.forGetter(function -> function.count),
				Codec.INT.optionalFieldOf("limit", NO_LIMIT).forGetter(function -> function.limit)
			))
			.apply(instance, EnchantedCountIncreaseLootFunction::new)
	);

	private final RegistryEntry<Enchantment> enchantment;
	private final LootNumberProvider count;
	private final int limit;

	EnchantedCountIncreaseLootFunction(
		List<LootCondition> conditions,
		RegistryEntry<Enchantment> enchantment,
		LootNumberProvider count,
		int limit
	) {
		super(conditions);
		this.enchantment = enchantment;
		this.count = count;
		this.limit = limit;
	}

	@Override
	public LootFunctionType<EnchantedCountIncreaseLootFunction> getType() {
		return LootFunctionTypes.ENCHANTED_COUNT_INCREASE;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Sets.union(ImmutableSet.of(LootContextParameters.ATTACKING_ENTITY), count.getAllowedParameters());
	}

	private boolean hasLimit() {
		return limit > NO_LIMIT;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		Entity entity = context.get(LootContextParameters.ATTACKING_ENTITY);

		if (entity instanceof LivingEntity livingEntity) {
			int enchantLevel = EnchantmentHelper.getEquipmentLevel(enchantment, livingEntity);

			if (enchantLevel == 0) {
				return stack;
			}

			float countIncrease = enchantLevel * count.nextFloat(context);
			stack.increment(Math.round(countIncrease));

			if (hasLimit()) {
				stack.capCount(limit);
			}
		}

		return stack;
	}

	public static EnchantedCountIncreaseLootFunction.Builder builder(
		RegistryWrapper.WrapperLookup registries,
		LootNumberProvider count
	) {
		RegistryWrapper.Impl<Enchantment> enchantmentRegistry = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		return new Builder(enchantmentRegistry.getOrThrow(Enchantments.LOOTING), count);
	}

	/** Строитель функции увеличения количества предметов от зачарования. */
	public static class Builder extends ConditionalLootFunction.Builder<Builder> {

		private final RegistryEntry<Enchantment> enchantment;
		private final LootNumberProvider count;
		private int limit = NO_LIMIT;

		public Builder(RegistryEntry<Enchantment> enchantment, LootNumberProvider count) {
			this.enchantment = enchantment;
			this.count = count;
		}

		@Override
		protected Builder getThisBuilder() {
			return this;
		}

		public Builder withLimit(int limit) {
			this.limit = limit;
			return this;
		}

		@Override
		public LootFunction build() {
			return new EnchantedCountIncreaseLootFunction(getConditions(), enchantment, count, limit);
		}
	}
}
