package net.minecraft.loot.entry;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ErrorReporter;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Запись пула лута, делегирующая генерацию другой таблице лута.
 * Поддерживает как ссылку по ключу реестра, так и встроенную таблицу.
 */
public class LootTableEntry extends LeafEntry {

	public static final MapCodec<LootTableEntry> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(Codec.either(LootTable.TABLE_KEY, LootTable.CODEC).fieldOf("value").forGetter(entry -> entry.value))
			.and(addLeafFields(instance))
			.apply(instance, LootTableEntry::new)
	);

	public static final ErrorReporter.Context INLINE_CONTEXT = new ErrorReporter.Context() {
		@Override
		public String getName() {
			return "->{inline}";
		}
	};

	private final Either<RegistryKey<LootTable>, LootTable> value;

	private LootTableEntry(
		Either<RegistryKey<LootTable>, LootTable> value,
		int weight,
		int quality,
		List<LootCondition> conditions,
		List<LootFunction> functions
	) {
		super(weight, quality, conditions, functions);
		this.value = value;
	}

	@Override
	public LootPoolEntryType getType() {
		return LootPoolEntryTypes.LOOT_TABLE;
	}

	@Override
	public void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
		LootTable table = value.map(
			key -> context.getLookup()
				.getOptionalEntry(key)
				.map(RegistryEntry::value)
				.orElse(LootTable.EMPTY),
			inlineTable -> inlineTable
		);
		table.generateUnprocessedLoot(context, lootConsumer);
	}

	@Override
	public void validate(LootTableReporter reporter) {
		Optional<RegistryKey<LootTable>> keyOptional = value.left();

		if (keyOptional.isPresent()) {
			RegistryKey<LootTable> key = keyOptional.get();

			if (!reporter.canUseReferences()) {
				reporter.report(new LootTableReporter.ReferenceNotAllowedError(key));
				return;
			}

			if (reporter.isInStack(key)) {
				reporter.report(new LootTableReporter.RecursionError(key));
				return;
			}
		}

		super.validate(reporter);
		value.ifLeft(
			key -> reporter.getDataLookup()
				.getOptionalEntry(key)
				.ifPresentOrElse(
					entry -> ((LootTable) entry.value()).validate(
						reporter.makeChild(new ErrorReporter.ReferenceLootTableContext(key), key)
					),
					() -> reporter.report(new LootTableReporter.MissingElementError(key))
				)
		).ifRight(table -> table.validate(reporter.makeChild(INLINE_CONTEXT)));
	}

	public static LeafEntry.Builder<?> builder(RegistryKey<LootTable> key) {
		return builder((weight, quality, conditions, functions) -> new LootTableEntry(
			Either.left(key),
			weight,
			quality,
			conditions,
			functions
		));
	}

	public static LeafEntry.Builder<?> builder(LootTable table) {
		return builder((weight, quality, conditions, functions) -> new LootTableEntry(
			Either.right(table),
			weight,
			quality,
			conditions,
			functions
		));
	}
}
