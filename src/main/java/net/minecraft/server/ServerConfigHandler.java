package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.logging.LogUtils;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.dedicated.management.listener.BlankManagementListener;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Uuids;
import net.minecraft.util.WorldSavePath;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * Утилитарный класс для конвертации устаревших текстовых конфигурационных файлов сервера
 * (banned-players.txt, banned-ips.txt, ops.txt, white-list.txt) в современный JSON-формат.
 * Выполняет однократную миграцию при первом запуске сервера после обновления.
 */
public class ServerConfigHandler {

	static final Logger LOGGER = LogUtils.getLogger();
	public static final File BANNED_IPS_FILE = new File("banned-ips.txt");
	public static final File BANNED_PLAYERS_FILE = new File("banned-players.txt");
	public static final File OPERATORS_FILE = new File("ops.txt");
	public static final File WHITE_LIST_FILE = new File("white-list.txt");

	static List<String> processSimpleListFile(File file, Map<String, String[]> valueMap) throws IOException {
		List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);

		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith("#") || trimmed.isEmpty()) {
				continue;
			}

			String[] parts = trimmed.split("\\|");
			valueMap.put(parts[0].toLowerCase(Locale.ROOT), parts);
		}

		return lines;
	}

	private static void lookupProfile(
			MinecraftServer server,
			Collection<String> playerNames,
			ProfileLookupCallback callback
	) {
		String[] names = playerNames
				.stream()
				.filter(name -> !StringHelper.isEmpty(name))
				.toArray(String[]::new);

		if (server.isOnlineMode()) {
			server.getApiServices().profileRepository().findProfilesByNames(names, callback);
		} else {
			for (String name : names) {
				callback.onProfileLookupSucceeded(name, Uuids.getOfflinePlayerUuid(name));
			}
		}
	}

	/**
	 * Конвертирует устаревший текстовый файл забаненных игроков в JSON-формат.
	 * Выполняет поиск UUID по имени через Mojang API (онлайн-режим) или генерирует
	 * офлайн-UUID, затем сохраняет результат в {@link BannedPlayerList}.
	 *
	 * @param server экземпляр сервера для доступа к API-сервисам
	 * @return {@code true} если конвертация прошла успешно или файл не существует
	 */
	public static boolean convertBannedPlayers(MinecraftServer server) {
		final BannedPlayerList bannedPlayerList = new BannedPlayerList(
				PlayerManager.BANNED_PLAYERS_FILE,
				new BlankManagementListener()
		);

		if (!BANNED_PLAYERS_FILE.exists() || !BANNED_PLAYERS_FILE.isFile()) {
			return true;
		}

		if (bannedPlayerList.getFile().exists()) {
			try {
				bannedPlayerList.load();
			} catch (IOException exception) {
				LOGGER.warn("Could not load existing file {}", bannedPlayerList.getFile().getName(), exception);
			}
		}

		try {
			final Map<String, String[]> banMap = Maps.newHashMap();
			processSimpleListFile(BANNED_PLAYERS_FILE, banMap);

			ProfileLookupCallback callback = new ProfileLookupCallback() {
				public void onProfileLookupSucceeded(String name, UUID uuid) {
					PlayerConfigEntry player = new PlayerConfigEntry(uuid, name);
					server.getApiServices().nameToIdCache().add(player);

					String[] parts = banMap.get(player.name().toLowerCase(Locale.ROOT));
					if (parts == null) {
						ServerConfigHandler.LOGGER.warn("Could not convert user banlist entry for {}", player.name());
						throw new ServerConfigHandler.ServerConfigException("Profile not in the conversionlist");
					}

					Date banStart = parts.length > 1 ? ServerConfigHandler.parseDate(parts[1], null) : null;
					String bannedBy = parts.length > 2 ? parts[2] : null;
					Date banExpiry = parts.length > 3 ? ServerConfigHandler.parseDate(parts[3], null) : null;
					String banReason = parts.length > 4 ? parts[4] : null;
					bannedPlayerList.add(new BannedPlayerEntry(player, banStart, bannedBy, banExpiry, banReason));
				}

				public void onProfileLookupFailed(String name, Exception exception) {
					ServerConfigHandler.LOGGER.warn("Could not lookup user banlist entry for {}", name, exception);
					if (!(exception instanceof ProfileNotFoundException)) {
						throw new ServerConfigHandler.ServerConfigException(
								"Could not request user " + name + " from backend systems",
								exception
						);
					}
				}
			};

			lookupProfile(server, banMap.keySet(), callback);
			bannedPlayerList.save();
			markFileConverted(BANNED_PLAYERS_FILE);
			return true;
		} catch (IOException exception) {
			LOGGER.warn("Could not read old user banlist to convert it!", exception);
			return false;
		} catch (ServerConfigHandler.ServerConfigException exception) {
			LOGGER.error("Conversion failed, please try again later", exception);
			return false;
		}
	}

	/**
	 * Конвертирует устаревший текстовый файл забаненных IP-адресов в JSON-формат.
	 *
	 * @param server экземпляр сервера для доступа к файловой системе
	 * @return {@code true} если конвертация прошла успешно или файл не существует
	 */
	public static boolean convertBannedIps(MinecraftServer server) {
		BannedIpList bannedIpList = new BannedIpList(PlayerManager.BANNED_IPS_FILE, new BlankManagementListener());

		if (!BANNED_IPS_FILE.exists() || !BANNED_IPS_FILE.isFile()) {
			return true;
		}

		if (bannedIpList.getFile().exists()) {
			try {
				bannedIpList.load();
			} catch (IOException exception) {
				LOGGER.warn("Could not load existing file {}", bannedIpList.getFile().getName(), exception);
			}
		}

		try {
			Map<String, String[]> banMap = Maps.newHashMap();
			processSimpleListFile(BANNED_IPS_FILE, banMap);

			for (Map.Entry<String, String[]> entry : banMap.entrySet()) {
				String ip = entry.getKey();
				String[] parts = entry.getValue();
				Date banStart = parts.length > 1 ? parseDate(parts[1], null) : null;
				String bannedBy = parts.length > 2 ? parts[2] : null;
				Date banExpiry = parts.length > 3 ? parseDate(parts[3], null) : null;
				String banReason = parts.length > 4 ? parts[4] : null;
				bannedIpList.add(new BannedIpEntry(ip, banStart, bannedBy, banExpiry, banReason));
			}

			bannedIpList.save();
			markFileConverted(BANNED_IPS_FILE);
			return true;
		} catch (IOException exception) {
			LOGGER.warn("Could not parse old ip banlist to convert it!", exception);
			return false;
		}
	}

	/**
	 * Конвертирует устаревший текстовый файл операторов в JSON-формат.
	 *
	 * @param server экземпляр сервера для доступа к API-сервисам
	 * @return {@code true} если конвертация прошла успешно или файл не существует
	 */
	public static boolean convertOperators(MinecraftServer server) {
		final OperatorList operatorList = new OperatorList(PlayerManager.OPERATORS_FILE, new BlankManagementListener());

		if (!OPERATORS_FILE.exists() || !OPERATORS_FILE.isFile()) {
			return true;
		}

		if (operatorList.getFile().exists()) {
			try {
				operatorList.load();
			} catch (IOException exception) {
				LOGGER.warn("Could not load existing file {}", operatorList.getFile().getName(), exception);
			}
		}

		try {
			List<String> names = Files.readLines(OPERATORS_FILE, StandardCharsets.UTF_8);

			ProfileLookupCallback callback = new ProfileLookupCallback() {
				public void onProfileLookupSucceeded(String name, UUID uuid) {
					PlayerConfigEntry player = new PlayerConfigEntry(uuid, name);
					server.getApiServices().nameToIdCache().add(player);
					operatorList.add(new OperatorEntry(player, server.getOpPermissionLevel(), false));
				}

				public void onProfileLookupFailed(String name, Exception exception) {
					ServerConfigHandler.LOGGER.warn("Could not lookup oplist entry for {}", name, exception);
					if (!(exception instanceof ProfileNotFoundException)) {
						throw new ServerConfigHandler.ServerConfigException(
								"Could not request user " + name + " from backend systems",
								exception
						);
					}
				}
			};

			lookupProfile(server, names, callback);
			operatorList.save();
			markFileConverted(OPERATORS_FILE);
			return true;
		} catch (IOException exception) {
			LOGGER.warn("Could not read old oplist to convert it!", exception);
			return false;
		} catch (ServerConfigHandler.ServerConfigException exception) {
			LOGGER.error("Conversion failed, please try again later", exception);
			return false;
		}
	}

	/**
	 * Конвертирует устаревший текстовый файл белого списка в JSON-формат.
	 *
	 * @param server экземпляр сервера для доступа к API-сервисам
	 * @return {@code true} если конвертация прошла успешно или файл не существует
	 */
	public static boolean convertWhitelist(MinecraftServer server) {
		final Whitelist whitelist = new Whitelist(PlayerManager.WHITELIST_FILE, new BlankManagementListener());

		if (!WHITE_LIST_FILE.exists() || !WHITE_LIST_FILE.isFile()) {
			return true;
		}

		if (whitelist.getFile().exists()) {
			try {
				whitelist.load();
			} catch (IOException exception) {
				LOGGER.warn("Could not load existing file {}", whitelist.getFile().getName(), exception);
			}
		}

		try {
			List<String> names = Files.readLines(WHITE_LIST_FILE, StandardCharsets.UTF_8);

			ProfileLookupCallback callback = new ProfileLookupCallback() {
				public void onProfileLookupSucceeded(String name, UUID uuid) {
					PlayerConfigEntry player = new PlayerConfigEntry(uuid, name);
					server.getApiServices().nameToIdCache().add(player);
					whitelist.add(new WhitelistEntry(player));
				}

				public void onProfileLookupFailed(String name, Exception exception) {
					ServerConfigHandler.LOGGER.warn(
							"Could not lookup user whitelist entry for {}",
							name,
							exception
					);
					if (!(exception instanceof ProfileNotFoundException)) {
						throw new ServerConfigHandler.ServerConfigException(
								"Could not request user " + name + " from backend systems",
								exception
						);
					}
				}
			};

			lookupProfile(server, names, callback);
			whitelist.save();
			markFileConverted(WHITE_LIST_FILE);
			return true;
		} catch (IOException exception) {
			LOGGER.warn("Could not read old whitelist to convert it!", exception);
			return false;
		} catch (ServerConfigHandler.ServerConfigException exception) {
			LOGGER.error("Conversion failed, please try again later", exception);
			return false;
		}
	}

	/**
	 * Ищет UUID игрока по имени. Если имя короче 16 символов — ищет через кэш или Mojang API.
	 * Если имя длиннее — пытается распарсить как UUID напрямую (для команд с UUID-аргументом).
	 *
	 * @param server экземпляр сервера
	 * @param name   имя игрока или строковое представление UUID
	 * @return UUID игрока или {@code null} если не найден
	 */
	public static @Nullable UUID getPlayerUuidByName(MinecraftServer server, String name) {
		if (StringHelper.isEmpty(name) || name.length() > 16) {
			try {
				return UUID.fromString(name);
			} catch (IllegalArgumentException exception) {
				return null;
			}
		}

		Optional<UUID> cached = server.getApiServices().nameToIdCache().findByName(name).map(PlayerConfigEntry::id);
		if (cached.isPresent()) {
			return cached.get();
		}

		if (!server.isSingleplayer() && server.isOnlineMode()) {
			final List<PlayerConfigEntry> found = new ArrayList<>();

			ProfileLookupCallback callback = new ProfileLookupCallback() {
				public void onProfileLookupSucceeded(String playerName, UUID uuid) {
					PlayerConfigEntry player = new PlayerConfigEntry(uuid, playerName);
					server.getApiServices().nameToIdCache().add(player);
					found.add(player);
				}

				public void onProfileLookupFailed(String playerName, Exception exception) {
					ServerConfigHandler.LOGGER.warn(
							"Could not lookup user whitelist entry for {}",
							playerName,
							exception
					);
				}
			};

			lookupProfile(server, Lists.newArrayList(name), callback);
			return found.isEmpty() ? null : found.getFirst().id();
		}

		return Uuids.getOfflinePlayerUuid(name);
	}

	/**
	 * Конвертирует устаревшие файлы данных игроков из папки {@code players/} в новый формат
	 * {@code playerdata/} с именами файлов по UUID. Неизвестные профили перемещаются в {@code unknownplayers/}.
	 *
	 * @param minecraftServer экземпляр выделенного сервера
	 * @return {@code true} если конвертация прошла успешно или папка не существует
	 */
	public static boolean convertPlayerFiles(MinecraftDedicatedServer minecraftServer) {
		final File playersFolder = getLevelPlayersFolder(minecraftServer);
		final File playerDataFolder = new File(playersFolder.getParentFile(), "playerdata");
		final File unknownPlayersFolder = new File(playersFolder.getParentFile(), "unknownplayers");

		if (!playersFolder.exists() || !playersFolder.isDirectory()) {
			return true;
		}

		File[] files = playersFolder.listFiles();
		List<String> playerNames = Lists.newArrayList();

		for (File playerFile : files) {
			String fileName = playerFile.getName();
			if (!fileName.toLowerCase(Locale.ROOT).endsWith(".dat")) {
				continue;
			}

			String baseName = fileName.substring(0, fileName.length() - ".dat".length());
			if (!baseName.isEmpty()) {
				playerNames.add(baseName);
			}
		}

		try {
			final String[] nameArray = playerNames.toArray(new String[0]);

			ProfileLookupCallback callback = new ProfileLookupCallback() {
				public void onProfileLookupSucceeded(String name, UUID uuid) {
					PlayerConfigEntry player = new PlayerConfigEntry(uuid, name);
					minecraftServer.getApiServices().nameToIdCache().add(player);
					movePlayerFile(playerDataFolder, findFileName(name), uuid.toString());
				}

				public void onProfileLookupFailed(String name, Exception exception) {
					ServerConfigHandler.LOGGER.warn("Could not lookup user uuid for {}", name, exception);
					if (exception instanceof ProfileNotFoundException) {
						String fileName = findFileName(name);
						movePlayerFile(unknownPlayersFolder, fileName, fileName);
					} else {
						throw new ServerConfigHandler.ServerConfigException(
								"Could not request user " + name + " from backend systems",
								exception
						);
					}
				}

				private void movePlayerFile(File targetFolder, String sourceName, String targetName) {
					File source = new File(playersFolder, sourceName + ".dat");
					File target = new File(targetFolder, targetName + ".dat");
					ServerConfigHandler.createDirectory(targetFolder);
					if (!source.renameTo(target)) {
						throw new ServerConfigHandler.ServerConfigException("Could not convert file for " + sourceName);
					}
				}

				private String findFileName(String name) {
					for (String candidate : nameArray) {
						if (candidate != null && candidate.equalsIgnoreCase(name)) {
							return candidate;
						}
					}

					throw new ServerConfigHandler.ServerConfigException(
							"Could not find the filename for " + name + " anymore"
					);
				}
			};

			lookupProfile(minecraftServer, Lists.newArrayList(nameArray), callback);
			return true;
		} catch (ServerConfigHandler.ServerConfigException exception) {
			LOGGER.error("Conversion failed, please try again later", exception);
			return false;
		}
	}

	static void createDirectory(File directory) {
		if (directory.exists()) {
			if (!directory.isDirectory()) {
				throw new ServerConfigHandler.ServerConfigException(
						"Can't create directory " + directory.getName() + " in world save directory.");
			}
		}
		else if (!directory.mkdirs()) {
			throw new ServerConfigHandler.ServerConfigException(
					"Can't create directory " + directory.getName() + " in world save directory.");
		}
	}

	/**
	 * Проверяет, что все конвертации конфигурационных файлов завершились успешно.
	 * Возвращает {@code false} если хотя бы один из устаревших файлов всё ещё существует.
	 *
	 * @param server экземпляр сервера
	 * @return {@code true} если конвертация полностью завершена
	 */
	public static boolean checkSuccess(MinecraftServer server) {
		return checkListConversionSuccess() && checkPlayerConversionSuccess(server);
	}

	private static boolean checkListConversionSuccess() {
		boolean bannedPlayersRemains = BANNED_PLAYERS_FILE.exists() && BANNED_PLAYERS_FILE.isFile();
		boolean bannedIpsRemains = BANNED_IPS_FILE.exists() && BANNED_IPS_FILE.isFile();
		boolean operatorsRemains = OPERATORS_FILE.exists() && OPERATORS_FILE.isFile();
		boolean whitelistRemains = WHITE_LIST_FILE.exists() && WHITE_LIST_FILE.isFile();

		if (!bannedPlayersRemains && !bannedIpsRemains && !operatorsRemains && !whitelistRemains) {
			return true;
		}

		LOGGER.warn("**** FAILED TO START THE SERVER AFTER ACCOUNT CONVERSION!");
		LOGGER.warn("** please remove the following files and restart the server:");

		if (bannedPlayersRemains) {
			LOGGER.warn("* {}", BANNED_PLAYERS_FILE.getName());
		}

		if (bannedIpsRemains) {
			LOGGER.warn("* {}", BANNED_IPS_FILE.getName());
		}

		if (operatorsRemains) {
			LOGGER.warn("* {}", OPERATORS_FILE.getName());
		}

		if (whitelistRemains) {
			LOGGER.warn("* {}", WHITE_LIST_FILE.getName());
		}

		return false;
	}

	private static boolean checkPlayerConversionSuccess(MinecraftServer server) {
		File file = getLevelPlayersFolder(server);
		if (!file.exists() || !file.isDirectory() || file.list().length <= 0 && file.delete()) {
			return true;
		}
		else {
			LOGGER.warn("**** DETECTED OLD PLAYER DIRECTORY IN THE WORLD SAVE");
			LOGGER.warn("**** THIS USUALLY HAPPENS WHEN THE AUTOMATIC CONVERSION FAILED IN SOME WAY");
			LOGGER.warn(
					"** please restart the server and if the problem persists, remove the directory '{}'",
					file.getPath()
			);
			return false;
		}
	}

	private static File getLevelPlayersFolder(MinecraftServer server) {
		return server.getSavePath(WorldSavePath.PLAYERS).toFile();
	}

	private static void markFileConverted(File file) {
		File converted = new File(file.getName() + ".converted");
		file.renameTo(converted);
	}

	static Date parseDate(String dateString, Date fallback) {
		try {
			return BanEntry.DATE_FORMAT.parse(dateString);
		} catch (ParseException exception) {
			return fallback;
		}
	}

	/**
	 * Внутреннее исключение, сигнализирующее о сбое в процессе конвертации конфигурационных файлов.
	 * Используется для прерывания цепочки обратных вызовов {@link ProfileLookupCallback}.
	 */
	static class ServerConfigException extends RuntimeException {

		ServerConfigException(String message, Throwable cause) {
			super(message, cause);
		}

		ServerConfigException(String message) {
			super(message);
		}
	}
}
