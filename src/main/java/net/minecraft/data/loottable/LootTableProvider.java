package net.minecraft.data.loottable;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.*;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.entry.RegistryEntryInfo;

import java.util.Optional;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.context.ContextType;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.RandomSequence;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Провайдер данных для генерации таблиц лута.
 * Собирает все таблицы из зарегистрированных {@link LootTypeGenerator}, проверяет их валидность
 * (коллизии seed-ов, отсутствующие обязательные таблицы, структурные ошибки) и записывает на диск.
 */
public class LootTableProvider implements DataProvider {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final DataOutput.PathResolver pathResolver;
	private final Set<RegistryKey<LootTable>> lootTableIds;
	private final List<LootTableProvider.LootTypeGenerator> lootTypeGenerators;
	private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

	public LootTableProvider(
		DataOutput output,
		Set<RegistryKey<LootTable>> lootTableIds,
		List<LootTableProvider.LootTypeGenerator> lootTypeGenerators,
		CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		this.pathResolver = output.getResolver(RegistryKeys.LOOT_TABLE);
		this.lootTypeGenerators = lootTypeGenerators;
		this.lootTableIds = lootTableIds;
		this.registriesFuture = registriesFuture;
	}

	@Override
	public CompletableFuture<?> run(DataWriter writer) {
		return registriesFuture.thenCompose(registries -> run(writer, registries));
	}

	/**
	 * Основная логика генерации: собирает все таблицы лута из генераторов, проверяет коллизии
	 * random seed-ов, валидирует структуру каждой таблицы и записывает результат в JSON-файлы.
	 */
	private CompletableFuture<?> run(DataWriter writer, RegistryWrapper.WrapperLookup registries) {
		MutableRegistry<LootTable> registry = new SimpleRegistry<>(RegistryKeys.LOOT_TABLE, Lifecycle.experimental());
		Map<RandomSeed.XoroshiroSeed, Identifier> seedToId = new Object2ObjectOpenHashMap<>();

		lootTypeGenerators.forEach(lootTypeGenerator -> lootTypeGenerator
			.provider()
			.apply(registries)
			.accept((tableKey, builder) -> {
				Identifier tableId = getId(tableKey);
				Identifier conflictingId = seedToId.put(RandomSequence.createSeed(tableId), tableId);

				if (conflictingId != null) {
					Util.logErrorOrPause(
						"Loot table random sequence seed collision on " + conflictingId + " and " + tableKey.getValue()
					);
				}

				builder.randomSequenceId(tableId);
				LootTable builtTable = builder.type(lootTypeGenerator.paramSet).build();
				registry.add(tableKey, builtTable, new RegistryEntryInfo(Optional.empty(), Lifecycle.stable()));
			})
		);

		registry.freeze();

		ErrorReporter.Impl errorReporter = new ErrorReporter.Impl();
		RegistryEntryLookup.RegistryLookup registryLookup =
			new DynamicRegistryManager.ImmutableImpl(List.of(registry)).toImmutable();
		LootTableReporter reporter = new LootTableReporter(errorReporter, LootContextTypes.GENERIC, registryLookup);

		for (RegistryKey<LootTable> missingKey : Sets.difference(lootTableIds, registry.getKeys())) {
			errorReporter.report(new LootTableProvider.MissingTableError(missingKey));
		}

		registry.streamEntries().forEach(entry ->
			entry.value().validate(
				reporter.withContextType(entry.value().getType())
					.makeChild(
						new ErrorReporter.LootTableContext(entry.registryKey()),
						entry.registryKey()
					)
			)
		);

		if (errorReporter.isEmpty()) {
			return CompletableFuture.allOf(
				registry.getEntrySet().stream().map(entry -> {
					RegistryKey<LootTable> tableKey = entry.getKey();
					LootTable lootTable = entry.getValue();
					Path path = pathResolver.resolveJson(tableKey.getValue());
					return DataProvider.writeCodecToPath(writer, registries, LootTable.CODEC, lootTable, path);
				}).toArray(CompletableFuture[]::new)
			);
		}

		errorReporter.apply((name, error) -> LOGGER.warn("Found validation problem in {}: {}", name, error.getMessage()));
		throw new IllegalStateException("Failed to validate loot tables, see logs");
	}

	private static Identifier getId(RegistryKey<LootTable> lootTableKey) {
		return lootTableKey.getValue();
	}

	@Override
	public String getName() {
		return "Loot Tables";
	}

	/**
	 * Связывает фабрику генератора таблиц лута с типом контекста лута (блок, сущность, сундук и т.д.).
	 */
	public record LootTypeGenerator(
		Function<RegistryWrapper.WrapperLookup, LootTableGenerator> provider,
		ContextType paramSet
	) {
	}

	/**
	 * Ошибка валидации: обязательная таблица лута не была сгенерирована ни одним из провайдеров.
	 */
	public record MissingTableError(RegistryKey<LootTable> id) implements ErrorReporter.Error {

		@Override
		public String getMessage() {
			return "Missing built-in table: " + id.getValue();
		}
	}
}
