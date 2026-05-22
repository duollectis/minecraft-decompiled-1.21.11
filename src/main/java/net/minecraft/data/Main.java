package net.minecraft.data;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.data.advancement.vanilla.VanillaAdvancementProviders;
import net.minecraft.data.dev.NbtProvider;
import net.minecraft.data.loottable.rebalance.TradeRebalanceLootTableProviders;
import net.minecraft.data.loottable.vanilla.VanillaLootTableProviders;
import net.minecraft.data.recipe.VanillaRecipeGenerator;
import net.minecraft.data.report.*;
import net.minecraft.data.tag.TagProvider;
import net.minecraft.data.tag.rebalance.TradeRebalanceEnchantmentTagProvider;
import net.minecraft.data.tag.vanilla.*;
import net.minecraft.data.validate.StructureValidatorProvider;
import net.minecraft.item.Item;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.TradeRebalanceBuiltinRegistries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.dedicated.management.schema.RpcSchemaReferenceJsonProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.SuppressLinter;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Точка входа для генерации данных Minecraft.
 * Разбирает аргументы командной строки и запускает {@link DataGenerator}
 * с нужным набором провайдеров.
 */
public class Main {

	@SuppressLinter(reason = "System.out needed before bootstrap")
	@DontObfuscate
	public static void main(String[] args) throws IOException {
		SharedConstants.createGameVersion();

		OptionParser optionParser = new OptionParser();
		OptionSpec<Void> helpOption = optionParser.accepts("help", "Show the help menu").forHelp();
		OptionSpec<Void> serverOption = optionParser.accepts("server", "Include server generators");
		OptionSpec<Void> devOption = optionParser.accepts("dev", "Include development tools");
		OptionSpec<Void> reportsOption = optionParser.accepts("reports", "Include data reports");
		optionParser.accepts("validate", "Validate inputs");
		OptionSpec<Void> allOption = optionParser.accepts("all", "Include all generators");
		OptionSpec<String> outputOption = optionParser
				.accepts("output", "Output folder")
				.withRequiredArg()
				.defaultsTo("generated", new String[0]);
		OptionSpec<String> inputOption = optionParser.accepts("input", "Input folder").withRequiredArg();

		OptionSet optionSet = optionParser.parse(args);

		if (optionSet.has(helpOption) || !optionSet.hasOptions()) {
			optionParser.printHelpOn(System.out);
			return;
		}

		Path outputPath = Paths.get(outputOption.value(optionSet));
		boolean includeAll = optionSet.has(allOption);
		boolean includeServer = includeAll || optionSet.has(serverOption);
		boolean includeDev = includeAll || optionSet.has(devOption);
		boolean includeReports = includeAll || optionSet.has(reportsOption);
		Collection<Path> inputPaths = optionSet.valuesOf(inputOption).stream().map(Paths::get).toList();

		DataGenerator dataGenerator = new DataGenerator(outputPath, SharedConstants.getGameVersion(), true);
		create(dataGenerator, inputPaths, includeServer, includeDev, includeReports);
		dataGenerator.run();
		Util.shutdownExecutors();
	}

	private static <T extends DataProvider> DataProvider.Factory<T> toFactory(
			BiFunction<DataOutput, CompletableFuture<RegistryWrapper.WrapperLookup>, T> baseFactory,
			CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture
	) {
		return output -> baseFactory.apply(output, registriesFuture);
	}

	/**
	 * Регистрирует все провайдеры данных в переданном {@link DataGenerator}.
	 *
	 * @param dataGenerator генератор данных
	 * @param inputs        пути к входным директориям (NBT/SNBT файлы)
	 * @param includeServer включить серверные генераторы (рецепты, теги, достижения и т.д.)
	 * @param includeDev    включить инструменты разработки (NBT → SNBT конвертер)
	 * @param includeReports включить генераторы отчётов (blocks.json, items.json и т.д.)
	 */
	public static void create(
			DataGenerator dataGenerator,
			Collection<Path> inputs,
			boolean includeServer,
			boolean includeDev,
			boolean includeReports
	) {
		DataGenerator.Pack snbtPack = dataGenerator.createVanillaPack(includeServer);
		snbtPack.addProvider(output -> new SnbtProvider(output, inputs).addWriter(new StructureValidatorProvider()));

		CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture = CompletableFuture.supplyAsync(
				BuiltinRegistries::createWrapperLookup, Util.getMainWorkerExecutor()
		);

		DataGenerator.Pack vanillaPack = dataGenerator.createVanillaPack(includeServer);
		vanillaPack.addProvider(toFactory(DynamicRegistriesProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaAdvancementProviders::createVanillaProvider, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaLootTableProviders::createVanillaProvider, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaRecipeGenerator.Provider::new, registriesFuture));

		TagProvider<Block> blockTagProvider = vanillaPack.addProvider(toFactory(VanillaBlockTagProvider::new, registriesFuture));
		TagProvider<Item> itemTagProvider = vanillaPack.addProvider(toFactory(VanillaItemTagProvider::new, registriesFuture));
		TagProvider<Biome> biomeTagProvider = vanillaPack.addProvider(toFactory(VanillaBiomeTagProvider::new, registriesFuture));
		TagProvider<BannerPattern> bannerPatternTagProvider = vanillaPack.addProvider(
				toFactory(VanillaBannerPatternTagProvider::new, registriesFuture)
		);
		TagProvider<Structure> structureTagProvider = vanillaPack.addProvider(
				toFactory(VanillaStructureTagProvider::new, registriesFuture)
		);

		vanillaPack.addProvider(toFactory(VanillaDamageTypeTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaDialogTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaEntityTypeTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaFlatLevelGeneratorPresetTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaFluidTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaGameEventTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaInstrumentTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaPaintingVariantTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaPointOfInterestTypeTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaWorldPresetTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaEnchantmentTagProvider::new, registriesFuture));
		vanillaPack.addProvider(toFactory(VanillaTimelineTagProvider::new, registriesFuture));

		DataGenerator.Pack nbtPack = dataGenerator.createVanillaPack(includeDev);
		nbtPack.addProvider(output -> new NbtProvider(output, inputs));

		DataGenerator.Pack reportsPack = dataGenerator.createVanillaPack(includeReports);
		reportsPack.addProvider(toFactory(BiomeParametersProvider::new, registriesFuture));
		reportsPack.addProvider(toFactory(ItemListProvider::new, registriesFuture));
		reportsPack.addProvider(toFactory(BlockListProvider::new, registriesFuture));
		reportsPack.addProvider(toFactory(CommandSyntaxProvider::new, registriesFuture));
		reportsPack.addProvider(RegistryDumpProvider::new);
		reportsPack.addProvider(PacketReportProvider::new);
		reportsPack.addProvider(DataPackStructureProvider::new);
		reportsPack.addProvider(RpcSchemaReferenceJsonProvider::new);

		CompletableFuture<RegistryBuilder.FullPatchesRegistriesPair> tradeRebalanceFuture =
				TradeRebalanceBuiltinRegistries.validate(registriesFuture);
		CompletableFuture<RegistryWrapper.WrapperLookup> tradeRebalanceRegistries =
				tradeRebalanceFuture.thenApply(RegistryBuilder.FullPatchesRegistriesPair::patches);

		DataGenerator.Pack tradeRebalancePack = dataGenerator.createVanillaSubPack(includeServer, "trade_rebalance");
		tradeRebalancePack.addProvider(toFactory(DynamicRegistriesProvider::new, tradeRebalanceRegistries));
		tradeRebalancePack.addProvider(
				output -> MetadataProvider.create(
						output,
						Text.translatable("dataPack.trade_rebalance.description"),
						FeatureSet.of(FeatureFlags.TRADE_REBALANCE)
				)
		);
		tradeRebalancePack.addProvider(
				toFactory(TradeRebalanceLootTableProviders::createTradeRebalanceProvider, registriesFuture)
		);
		tradeRebalancePack.addProvider(toFactory(TradeRebalanceEnchantmentTagProvider::new, registriesFuture));

		DataGenerator.Pack redstoneExperimentsPack = dataGenerator.createVanillaSubPack(
				includeServer, "redstone_experiments"
		);
		redstoneExperimentsPack.addProvider(
				output -> MetadataProvider.create(
						output,
						Text.translatable("dataPack.redstone_experiments.description"),
						FeatureSet.of(FeatureFlags.REDSTONE_EXPERIMENTS)
				)
		);

		DataGenerator.Pack minecartImprovementsPack = dataGenerator.createVanillaSubPack(
				includeServer, "minecart_improvements"
		);
		minecartImprovementsPack.addProvider(
				output -> MetadataProvider.create(
						output,
						Text.translatable("dataPack.minecart_improvements.description"),
						FeatureSet.of(FeatureFlags.MINECART_IMPROVEMENTS)
				)
		);
	}
}
