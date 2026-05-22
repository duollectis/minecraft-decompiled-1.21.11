package net.minecraft.loot.function;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ErrorReporter;
import org.slf4j.Logger;

import java.util.List;

/** Функция лута, делегирующая обработку предмета именованной функции из реестра модификаторов предметов. */
public class ReferenceLootFunction extends ConditionalLootFunction {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<ReferenceLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		instance -> addConditionsField(instance)
			.and(RegistryKey
				.createCodec(RegistryKeys.ITEM_MODIFIER)
				.fieldOf("name")
				.forGetter(function -> function.name))
			.apply(instance, ReferenceLootFunction::new)
	);

	private final RegistryKey<LootFunction> name;

	private ReferenceLootFunction(List<LootCondition> conditions, RegistryKey<LootFunction> name) {
		super(conditions);
		this.name = name;
	}

	@Override
	public LootFunctionType<ReferenceLootFunction> getType() {
		return LootFunctionTypes.REFERENCE;
	}

	@Override
	public void validate(LootTableReporter reporter) {
		if (!reporter.canUseReferences()) {
			reporter.report(new LootTableReporter.ReferenceNotAllowedError(name));
			return;
		}

		if (reporter.isInStack(name)) {
			reporter.report(new LootTableReporter.RecursionError(name));
			return;
		}

		super.validate(reporter);
		reporter.getDataLookup()
			.getOptionalEntry(name)
			.ifPresentOrElse(
				reference -> reference
					.value()
					.validate(reporter.makeChild(new ErrorReporter.ReferenceLootTableContext(name), name)),
				() -> reporter.report(new LootTableReporter.MissingElementError(name))
			);
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		LootFunction lootFunction = context.getLookup()
			.getOptionalEntry(name)
			.map(RegistryEntry::value)
			.orElse(null);

		if (lootFunction == null) {
			LOGGER.warn("Unknown function: {}", name.getValue());
			return stack;
		}

		LootContext.Entry<?> entry = LootContext.itemModifier(lootFunction);

		if (!context.markActive(entry)) {
			LOGGER.warn("Detected infinite loop in loot tables");
			return stack;
		}

		try {
			return lootFunction.apply(stack, context);
		} finally {
			context.markInactive(entry);
		}
	}

	public static ConditionalLootFunction.Builder<?> builder(RegistryKey<LootFunction> name) {
		return builder(conditions -> new ReferenceLootFunction(conditions, name));
	}
}
