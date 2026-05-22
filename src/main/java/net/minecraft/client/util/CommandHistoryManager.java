package net.minecraft.client.util;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.collection.ArrayListDeque;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Менеджер истории команд чата клиента.
 * Хранит до {@code MAX_HISTORY_SIZE} последних уникальных команд и персистирует их в файл.
 */
@Environment(EnvType.CLIENT)
public class CommandHistoryManager {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_HISTORY_SIZE = 50;
	private static final String HISTORY_FILENAME = "command_history.txt";

	private final Path historyFilePath;
	private final ArrayListDeque<String> history = new ArrayListDeque<>(MAX_HISTORY_SIZE);

	public CommandHistoryManager(Path directoryPath) {
		historyFilePath = directoryPath.resolve(HISTORY_FILENAME);
		if (Files.exists(historyFilePath)) {
			try (BufferedReader reader = Files.newBufferedReader(historyFilePath, StandardCharsets.UTF_8)) {
				history.addAll(reader.lines().toList());
			} catch (Exception exception) {
				LOGGER.error("Failed to read {}, command history will be missing", HISTORY_FILENAME, exception);
			}
		}
	}

	/**
	 * Добавляет команду в историю, если она отличается от последней.
	 * При превышении лимита удаляет самую старую запись.
	 *
	 * @param command команда для добавления
	 */
	public void add(String command) {
		if (command.equals(history.peekLast())) {
			return;
		}

		if (history.size() >= MAX_HISTORY_SIZE) {
			history.removeFirst();
		}

		history.addLast(command);
		write();
	}

	private void write() {
		try (BufferedWriter writer = Files.newBufferedWriter(historyFilePath, StandardCharsets.UTF_8)) {
			for (String command : history) {
				writer.write(command);
				writer.newLine();
			}
		} catch (IOException exception) {
			LOGGER.error("Failed to write {}, command history will be missing", HISTORY_FILENAME, exception);
		}
	}

	public Collection<String> getHistory() {
		return history;
	}
}
