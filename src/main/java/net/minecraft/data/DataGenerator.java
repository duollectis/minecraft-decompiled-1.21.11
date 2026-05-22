package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import net.minecraft.Bootstrap;
import net.minecraft.GameVersion;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Оркестратор генерации данных. Управляет набором {@link DataProvider}-ов,
 * запускает их последовательно и координирует работу с {@link DataCache}.
 */
public class DataGenerator {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Path outputPath;
	private final DataOutput output;
	private final GameVersion gameVersion;
	private final boolean ignoreCache;
	final Set<String> providerNames = new HashSet<>();
	final Map<String, DataProvider> runningProviders = new LinkedHashMap<>();

	static {
		Bootstrap.initialize();
	}

	public DataGenerator(Path outputPath, GameVersion gameVersion, boolean ignoreCache) {
		this.outputPath = outputPath;
		this.output = new DataOutput(outputPath);
		this.gameVersion = gameVersion;
		this.ignoreCache = ignoreCache;
	}

	/**
	 * Запускает все зарегистрированные провайдеры данных, используя инкрементальный кэш
	 * для пропуска провайдеров, чьи данные не изменились с предыдущего запуска.
	 */
	public void run() throws IOException {
		DataCache dataCache = new DataCache(outputPath, providerNames, gameVersion);
		Stopwatch totalStopwatch = Stopwatch.createStarted();
		Stopwatch providerStopwatch = Stopwatch.createUnstarted();

		runningProviders.forEach((name, provider) -> {
			if (!ignoreCache && !dataCache.isVersionDifferent(name)) {
				LOGGER.debug("Generator {} already run for version {}", name, gameVersion.name());
			} else {
				LOGGER.info("Starting provider: {}", name);
				providerStopwatch.start();
				dataCache.store(dataCache.run(name, provider::run).join());
				providerStopwatch.stop();
				LOGGER.info("{} finished after {} ms", name, providerStopwatch.elapsed(TimeUnit.MILLISECONDS));
				providerStopwatch.reset();
			}
		});

		LOGGER.info("All providers took: {} ms", totalStopwatch.elapsed(TimeUnit.MILLISECONDS));
		dataCache.write();
	}

	public Pack createVanillaPack(boolean shouldRun) {
		return new Pack(shouldRun, "vanilla", output);
	}

	/**
	 * Создаёт вложенный пак данных, размещаемый по пути
	 * {@code data/minecraft/datapacks/<packName>}.
	 *
	 * @param shouldRun нужно ли запускать провайдеры этого пака
	 * @param packName  имя вложенного пака (например, {@code "trade_rebalance"})
	 */
	public Pack createVanillaSubPack(boolean shouldRun, String packName) {
		Path subPackPath = output
				.resolvePath(DataOutput.OutputType.DATA_PACK)
				.resolve("minecraft")
				.resolve("datapacks")
				.resolve(packName);
		return new Pack(shouldRun, packName, new DataOutput(subPackPath));
	}

	/**
	 * Контейнер провайдеров данных одного пака.
	 * Регистрирует провайдеры в родительском {@link DataGenerator}.
	 */
	public class Pack {

		private final boolean shouldRun;
		private final String packName;
		private final DataOutput output;

		Pack(boolean shouldRun, String packName, DataOutput output) {
			this.shouldRun = shouldRun;
			this.packName = packName;
			this.output = output;
		}

		public <T extends DataProvider> T addProvider(DataProvider.Factory<T> factory) {
			T dataProvider = factory.create(output);
			String qualifiedName = packName + "/" + dataProvider.getName();

			if (!DataGenerator.this.providerNames.add(qualifiedName)) {
				throw new IllegalStateException("Duplicate provider: " + qualifiedName);
			}

			if (shouldRun) {
				DataGenerator.this.runningProviders.put(qualifiedName, dataProvider);
			}

			return dataProvider;
		}
	}
}
