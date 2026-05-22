package net.minecraft.loot.condition;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ErrorReporter;
import org.slf4j.Logger;

/**
 * Условие лута, делегирующее проверку внешнему предикату из реестра по ключу.
 * Защищает от бесконечной рекурсии через механизм активных записей контекста.
 */
public record ReferenceLootCondition(RegistryKey<LootCondition> id) implements LootCondition {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<ReferenceLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(RegistryKey.createCodec(RegistryKeys.PREDICATE).fieldOf("name").forGetter(ReferenceLootCondition::id))
			.apply(instance, ReferenceLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.REFERENCE;
	}

	@Override
	public void validate(LootTableReporter reporter) {
		if (!reporter.canUseReferences()) {
			reporter.report(new LootTableReporter.ReferenceNotAllowedError(id));
			return;
		}

		if (reporter.isInStack(id)) {
			reporter.report(new LootTableReporter.RecursionError(id));
			return;
		}

		LootCondition.super.validate(reporter);
		reporter.getDataLookup()
			.getOptionalEntry(id)
			.ifPresentOrElse(
				entry -> entry.value().validate(
					reporter.makeChild(new ErrorReporter.ReferenceLootTableContext(id), id)
				),
				() -> reporter.report(new LootTableReporter.MissingElementError(id))
			);
	}

	@Override
	public boolean test(LootContext lootContext) {
		LootCondition condition = lootContext.getLookup()
			.getOptionalEntry(id)
			.map(RegistryEntry.Reference::value)
			.orElse(null);

		if (condition == null) {
			LOGGER.warn("Tried using unknown condition table called {}", id.getValue());
			return false;
		}

		LootContext.Entry<?> entry = LootContext.predicate(condition);

		if (!lootContext.markActive(entry)) {
			LOGGER.warn("Detected infinite loop in loot tables");
			return false;
		}

		try {
			return condition.test(lootContext);
		} finally {
			lootContext.markInactive(entry);
		}
	}

	public static LootCondition.Builder builder(RegistryKey<LootCondition> key) {
		return () -> new ReferenceLootCondition(key);
	}
}
