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

/**
 * {@code ReferenceLootFunction}.
 */
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
			reporter.report(new LootTableReporter.ReferenceNotAllowedError(this.name));
		}
		else if (reporter.isInStack(this.name)) {
			reporter.report(new LootTableReporter.RecursionError(this.name));
		}
		else {
			super.validate(reporter);
			reporter.getDataLookup()
			        .getOptionalEntry(this.name)
			        .ifPresentOrElse(
					        reference -> reference
							        .value()
							        .validate(reporter.makeChild(
									        new ErrorReporter.ReferenceLootTableContext(this.name),
									        this.name
							        )),
					        () -> reporter.report(new LootTableReporter.MissingElementError(this.name))
			        );
		}
	}

	@Override
	protected ItemStack process(ItemStack stack, LootContext context) {
		LootFunction
				lootFunction =
				context.getLookup().getOptionalEntry(this.name).map(RegistryEntry::value).orElse(null);
		if (lootFunction == null) {
			LOGGER.warn("Unknown function: {}", this.name.getValue());
			return stack;
		}
		else {
			LootContext.Entry<?> entry = LootContext.itemModifier(lootFunction);
			if (context.markActive(entry)) {
				ItemStack var5;
				try {
					var5 = lootFunction.apply(stack, context);
				}
				finally {
					context.markInactive(entry);
				}

				return var5;
			}
			else {
				LOGGER.warn("Detected infinite loop in loot tables");
				return stack;
			}
		}
	}

	public static ConditionalLootFunction.Builder<?> builder(RegistryKey<LootFunction> name) {
		return builder(conditions -> new ReferenceLootFunction(conditions, name));
	}
}
