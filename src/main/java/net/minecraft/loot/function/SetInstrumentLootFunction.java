package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.InstrumentComponent;
import net.minecraft.item.Instrument;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

import java.util.List;
import java.util.Optional;

/**
 * Функция лута, устанавливающая случайный инструмент из указанного тега реестра.
 */
public class SetInstrumentLootFunction extends ConditionalLootFunction {

	public static final MapCodec<SetInstrumentLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(TagKey
				.codec(RegistryKeys.INSTRUMENT)
				.fieldOf("options")
				.forGetter(function -> function.options))
			.apply(instance, SetInstrumentLootFunction::new)
	);

	private final TagKey<Instrument> options;

	private SetInstrumentLootFunction(List<LootCondition> conditions, TagKey<Instrument> options) {
		super(conditions);
		this.options = options;
	}

	@Override
	public LootFunctionType<SetInstrumentLootFunction> getType() {
		return LootFunctionTypes.SET_INSTRUMENT;
	}

	@Override
	public ItemStack process(ItemStack stack, LootContext context) {
		Registry<Instrument> registry = context.getWorld().getRegistryManager().getOrThrow(RegistryKeys.INSTRUMENT);
		Optional<RegistryEntry<Instrument>> instrument = registry.getRandomEntry(options, context.getRandom());
		instrument.ifPresent(entry -> stack.set(DataComponentTypes.INSTRUMENT, new InstrumentComponent(entry)));
		return stack;
	}

	public static ConditionalLootFunction.Builder<?> builder(TagKey<Instrument> options) {
		return builder(conditions -> new SetInstrumentLootFunction(conditions, options));
	}
}
