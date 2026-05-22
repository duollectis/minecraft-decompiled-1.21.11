package net.minecraft.client.data;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.data.DataGenerator;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.SuppressLinter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Точка входа для генерации клиентских данных (модели, атласы, снаряжение, стили путевых точек).
 * Запускается как отдельный процесс при сборке ресурс-пака.
 */
@Environment(EnvType.CLIENT)
public class Main {

	/**
	 * Точка входа генератора клиентских данных.
	 * Принимает флаги {@code --client} и {@code --all} для включения клиентских провайдеров,
	 * а также {@code --output} для указания целевой директории.
	 *
	 * @param args аргументы командной строки
	 * @throws IOException при ошибке записи файлов
	 */
	@DontObfuscate
	@SuppressLinter(reason = "System.out needed before bootstrap")
	public static void main(String[] args) throws IOException {
		SharedConstants.createGameVersion();
		OptionParser optionParser = new OptionParser();
		OptionSpec<Void> helpOption = optionParser.accepts("help", "Show the help menu").forHelp();
		OptionSpec<Void> clientOption = optionParser.accepts("client", "Include client generators");
		OptionSpec<Void> allOption = optionParser.accepts("all", "Include all generators");
		OptionSpec<String> outputOption = optionParser
				.accepts("output", "Output folder")
				.withRequiredArg()
				.defaultsTo("generated", new String[0]);
		OptionSet options = optionParser.parse(args);

		if (!options.has(helpOption) && options.hasOptions()) {
			Path outputPath = Paths.get((String) outputOption.value(options));
			boolean includeAll = options.has(allOption);
			boolean includeClient = includeAll || options.has(clientOption);

			Bootstrap.initialize();
			ClientBootstrap.initialize();

			DataGenerator dataGenerator = new DataGenerator(outputPath, SharedConstants.getGameVersion(), true);
			create(dataGenerator, includeClient);
			dataGenerator.run();
			Util.shutdownExecutors();
		} else {
			optionParser.printHelpOn(System.out);
		}
	}

	/**
	 * Регистрирует все клиентские провайдеры данных в генераторе.
	 *
	 * @param dataGenerator генератор данных
	 * @param includeClient {@code true} — включить клиентские провайдеры
	 */
	public static void create(DataGenerator dataGenerator, boolean includeClient) {
		DataGenerator.Pack pack = dataGenerator.createVanillaPack(includeClient);
		pack.addProvider(ModelProvider::new);
		pack.addProvider(EquipmentAssetProvider::new);
		pack.addProvider(WaypointStyleProvider::new);
		pack.addProvider(AtlasDefinitionProvider::new);
	}
}
