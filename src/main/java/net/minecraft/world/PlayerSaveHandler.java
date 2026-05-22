package net.minecraft.world;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.DateTimeFormatters;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Обработчик сохранения и загрузки данных игроков.
 * Хранит файлы в директории {@code playerdata} уровня.
 * При загрузке применяет DataFixer для миграции устаревших форматов.
 */
public class PlayerSaveHandler {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final File playerDataDir;
	protected final DataFixer dataFixer;

	public PlayerSaveHandler(LevelStorage.Session session, DataFixer dataFixer) {
		this.dataFixer = dataFixer;
		playerDataDir = session.getDirectory(WorldSavePath.PLAYERDATA).toFile();
		playerDataDir.mkdirs();
	}

	/**
	 * Сохраняет данные игрока на диск.
	 * Использует атомарную замену файла через временный файл и резервную копию.
	 *
	 * @param player игрок, данные которого нужно сохранить
	 */
	public void savePlayerData(PlayerEntity player) {
		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(player.getErrorReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, player.getRegistryManager());
			player.writeData(nbtWriteView);

			Path dataDir = playerDataDir.toPath();
			Path tempFile = Files.createTempFile(dataDir, player.getUuidAsString() + "-", ".dat");
			NbtIo.writeCompressed(nbtWriteView.getNbt(), tempFile);

			Path targetFile = dataDir.resolve(player.getUuidAsString() + ".dat");
			Path backupFile = dataDir.resolve(player.getUuidAsString() + ".dat_old");
			Util.backupAndReplace(targetFile, tempFile, backupFile);
		} catch (Exception e) {
			LOGGER.warn("Failed to save player data for {}", player.getStringifiedName());
		}
	}

	private void backupCorruptedPlayerData(PlayerConfigEntry playerConfigEntry, String extension) {
		Path dataDir = playerDataDir.toPath();
		String idString = playerConfigEntry.id().toString();
		Path sourceFile = dataDir.resolve(idString + extension);
		Path backupFile = dataDir.resolve(
			idString + "_corrupted_" + ZonedDateTime.now().format(DateTimeFormatters.MINUTES) + extension
		);

		if (!Files.isRegularFile(sourceFile)) {
			return;
		}

		try {
			Files.copy(sourceFile, backupFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		} catch (Exception e) {
			LOGGER.warn("Failed to copy the player.dat file for {}", playerConfigEntry.name(), e);
		}
	}

	private Optional<NbtCompound> loadPlayerData(PlayerConfigEntry playerConfigEntry, String extension) {
		File file = new File(playerDataDir, playerConfigEntry.id() + extension);

		if (!file.exists() || !file.isFile()) {
			return Optional.empty();
		}

		try {
			return Optional.of(NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes()));
		} catch (Exception e) {
			LOGGER.warn("Failed to load player data for {}", playerConfigEntry.name());
			return Optional.empty();
		}
	}

	/**
	 * Загружает данные игрока с диска.
	 * Сначала пробует основной файл {@code .dat}, при неудаче — резервный {@code .dat_old}.
	 * Применяет DataFixer для обновления формата данных.
	 *
	 * @param playerConfigEntry конфигурационная запись игрока
	 * @return NBT-данные игрока, если файл найден и успешно прочитан
	 */
	public Optional<NbtCompound> loadPlayerData(PlayerConfigEntry playerConfigEntry) {
		Optional<NbtCompound> primary = loadPlayerData(playerConfigEntry, ".dat");

		if (primary.isEmpty()) {
			backupCorruptedPlayerData(playerConfigEntry, ".dat");
		}

		return primary
			.or(() -> loadPlayerData(playerConfigEntry, ".dat_old"))
			.map(nbt -> {
				int dataVersion = NbtHelper.getDataVersion(nbt);
				return DataFixTypes.PLAYER.update(dataFixer, nbt, dataVersion);
			});
	}
}
