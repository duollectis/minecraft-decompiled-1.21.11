package net.minecraft.loot.function;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/** Функция лута, добавляющая случайное зачарование к предмету. */
public class EnchantRandomlyLootFunction extends ConditionalLootFunction {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<EnchantRandomlyLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(instance.group(
				RegistryCodecs
					.entryList(RegistryKeys.ENCHANTMENT)
					.optionalFieldOf("options")
					.forGetter(function -> function.options),
				Codec.BOOL
					.optionalFieldOf("only_compatible", true)
					.forGetter(function -> function.onlyCompatible)
			))
			.apply(instance, EnchantRandomlyLootFunction::new)
	);

	private final Optional<RegistryEntryList<Enchantment>> options;
	private final boolean onlyCompatible;

	EnchantRandomlyLootFunction(
		List<LootCondition> conditions,
		Optional<RegistryEntryList<Enchantment>> options,
		boolean onlyCompatible
	) {
		super(conditions);
		this.options = options;
		this.onlyCompatible = onlyCompatible;
	}

	@Override
	public LootFunctionType<EnchantRandomlyLootFunction> getType() {
		return LootFunctionTypes.ENCHANT_RANDOMLY;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		Random random = context.getRandom();
		boolean isBook = stack.isOf(Items.BOOK);
		boolean filterCompatible = !isBook && onlyCompatible;

		Stream<RegistryEntry<Enchantment>> enchantmentStream = options
			.map(RegistryEntryList::stream)
			.orElseGet(() -> context
				.getWorld()
				.getRegistryManager()
				.getOrThrow(RegistryKeys.ENCHANTMENT)
				.streamEntries()
				.map(Function.identity()))
			.filter(entry -> !filterCompatible || entry.value().isAcceptableItem(stack));

		List<RegistryEntry<Enchantment>> candidates = enchantmentStream.toList();
		Optional<RegistryEntry<Enchantment>> chosen = Util.getRandomOrEmpty(candidates, random);

		if (chosen.isEmpty()) {
			LOGGER.warn("Couldn't find a compatible enchantment for {}", stack);
			return stack;
		}

		return addEnchantmentToStack(stack, chosen.get(), random);
	}

	private static ItemStack addEnchantmentToStack(
		ItemStack stack,
		RegistryEntry<Enchantment> enchantment,
		Random random
	) {
		int level = MathHelper.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());

		if (stack.isOf(Items.BOOK)) {
			stack = new ItemStack(Items.ENCHANTED_BOOK);
		}

		stack.addEnchantment(enchantment, level);

		return stack;
	}

	public static EnchantRandomlyLootFunction.Builder create() {
		return new Builder();
	}

	public static EnchantRandomlyLootFunction.Builder builder(RegistryWrapper.WrapperLookup registries) {
		return create().options(registries
			.getOrThrow(RegistryKeys.ENCHANTMENT)
			.getOrThrow(EnchantmentTags.ON_RANDOM_LOOT));
	}

	/** Строитель функции случайного зачарования. */
	public static class Builder extends ConditionalLootFunction.Builder<Builder> {

		private Optional<RegistryEntryList<Enchantment>> options = Optional.empty();
		private boolean onlyCompatible = true;

		@Override
		protected Builder getThisBuilder() {
			return this;
		}

		public Builder option(RegistryEntry<Enchantment> enchantment) {
			options = Optional.of(RegistryEntryList.of(enchantment));
			return this;
		}

		public Builder options(RegistryEntryList<Enchantment> options) {
			this.options = Optional.of(options);
			return this;
		}

		public Builder allowIncompatible() {
			onlyCompatible = false;
			return this;
		}

		@Override
		public LootFunction build() {
			return new EnchantRandomlyLootFunction(getConditions(), options, onlyCompatible);
		}
	}
}
