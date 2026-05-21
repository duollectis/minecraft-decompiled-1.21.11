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
 * {@code ReferenceLootCondition}.
 */
public record ReferenceLootCondition(RegistryKey<LootCondition> id) implements LootCondition {

	private static final Logger LOGGER = LogUtils.getLogger();
	public static final MapCodec<ReferenceLootCondition> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(RegistryKey
							.createCodec(RegistryKeys.PREDICATE)
							.fieldOf("name")
							.forGetter(ReferenceLootCondition::id))
					.apply(instance, ReferenceLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.REFERENCE;
	}

	@Override
	public void validate(LootTableReporter reporter) {
		if (!reporter.canUseReferences()) {
			reporter.report(new LootTableReporter.ReferenceNotAllowedError(this.id));
		}
		else if (reporter.isInStack(this.id)) {
			reporter.report(new LootTableReporter.RecursionError(this.id));
		}
		else {
			LootCondition.super.validate(reporter);
			reporter.getDataLookup()
			        .getOptionalEntry(this.id)
			        .ifPresentOrElse(
					        entry -> entry
							        .value()
							        .validate(reporter.makeChild(
									        new ErrorReporter.ReferenceLootTableContext(this.id),
									        this.id
							        )),
					        () -> reporter.report(new LootTableReporter.MissingElementError(this.id))
			        );
		}
	}

	/**
	 * Test.
	 *
	 * @param lootContext loot context
	 *
	 * @return boolean — результат операции
	 */
	public boolean test(LootContext lootContext) {
		LootCondition
				lootCondition =
				lootContext.getLookup().getOptionalEntry(this.id).map(RegistryEntry.Reference::value).orElse(null);
		if (lootCondition == null) {
			LOGGER.warn("Tried using unknown condition table called {}", this.id.getValue());
			return false;
		}
		else {
			LootContext.Entry<?> entry = LootContext.predicate(lootCondition);
			if (lootContext.markActive(entry)) {
				boolean var4;
				try {
					var4 = lootCondition.test(lootContext);
				}
				finally {
					lootContext.markInactive(entry);
				}

				return var4;
			}
			else {
				LOGGER.warn("Detected infinite loop in loot tables");
				return false;
			}
		}
	}

	public static LootCondition.Builder builder(RegistryKey<LootCondition> key) {
		return () -> new ReferenceLootCondition(key);
	}
}
