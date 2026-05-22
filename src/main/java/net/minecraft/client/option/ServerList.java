package net.minecraft.client.option;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Util;
import net.minecraft.util.thread.SimpleConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Управляет списком серверов мультиплеера, хранящимся в файле {@code servers.dat}.
 * <p>
 * Поддерживает два списка: видимые серверы ({@link #servers}) и скрытые ({@link #hiddenServers}).
 * Скрытые серверы используются для кэширования недавно посещённых серверов без отображения
 * в основном списке. Количество скрытых записей ограничено {@link #MAX_HIDDEN_ENTRIES}.
 * <p>
 * Операции записи на диск выполняются асинхронно через {@link #IO_EXECUTOR}.
 */
@Environment(EnvType.CLIENT)
public class ServerList {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final SimpleConsecutiveExecutor IO_EXECUTOR =
			new SimpleConsecutiveExecutor(Util.getMainWorkerExecutor(), "server-list-io");
	private static final int MAX_HIDDEN_ENTRIES = 16;

	private final MinecraftClient client;
	private final List<ServerInfo> servers = Lists.newArrayList();
	private final List<ServerInfo> hiddenServers = Lists.newArrayList();

	public ServerList(MinecraftClient client) {
		this.client = client;
	}

	/**
	 * Загружает список серверов из файла {@code servers.dat} в директории игры.
	 * Разделяет записи на видимые и скрытые по флагу {@code "hidden"} в NBT.
	 */
	public void loadFile() {
		try {
			servers.clear();
			hiddenServers.clear();
			NbtCompound root = NbtIo.read(client.runDirectory.toPath().resolve("servers.dat"));

			if (root == null) {
				return;
			}

			root.getListOrEmpty("servers").streamCompounds().forEach(nbt -> {
				ServerInfo serverInfo = ServerInfo.fromNbt(nbt);

				if (nbt.getBoolean("hidden", false)) {
					hiddenServers.add(serverInfo);
				}
				else {
					servers.add(serverInfo);
				}
			});
		}
		catch (Exception exception) {
			LOGGER.error("Couldn't load server list", exception);
		}
	}

	/**
	 * Сохраняет список серверов в файл {@code servers.dat} атомарно через временный файл.
	 * Использует {@link Util#backupAndReplace} для безопасной замены файла.
	 */
	public void saveFile() {
		try {
			NbtList nbtList = new NbtList();

			for (ServerInfo serverInfo : servers) {
				NbtCompound nbt = serverInfo.toNbt();
				nbt.putBoolean("hidden", false);
				nbtList.add(nbt);
			}

			for (ServerInfo serverInfo : hiddenServers) {
				NbtCompound nbt = serverInfo.toNbt();
				nbt.putBoolean("hidden", true);
				nbtList.add(nbt);
			}

			NbtCompound root = new NbtCompound();
			root.put("servers", nbtList);

			Path runDir = client.runDirectory.toPath();
			Path tempFile = Files.createTempFile(runDir, "servers", ".dat");
			NbtIo.write(root, tempFile);

			Path backupFile = runDir.resolve("servers.dat_old");
			Path targetFile = runDir.resolve("servers.dat");
			Util.backupAndReplace(targetFile, tempFile, backupFile);
		}
		catch (Exception exception) {
			LOGGER.error("Couldn't save server list", exception);
		}
	}

	public ServerInfo get(int index) {
		return servers.get(index);
	}

	public @Nullable ServerInfo get(String address) {
		for (ServerInfo serverInfo : servers) {
			if (serverInfo.address.equals(address)) {
				return serverInfo;
			}
		}

		for (ServerInfo serverInfo : hiddenServers) {
			if (serverInfo.address.equals(address)) {
				return serverInfo;
			}
		}

		return null;
	}

	/**
	 * Перемещает сервер из скрытого списка в видимый по адресу.
	 *
	 * @param address адрес сервера для разскрытия
	 * @return перемещённый {@link ServerInfo} или {@code null}, если сервер не найден в скрытых
	 */
	public @Nullable ServerInfo tryUnhide(String address) {
		for (int index = 0; index < hiddenServers.size(); index++) {
			ServerInfo serverInfo = hiddenServers.get(index);

			if (serverInfo.address.equals(address)) {
				hiddenServers.remove(index);
				servers.add(serverInfo);
				return serverInfo;
			}
		}

		return null;
	}

	public void remove(ServerInfo serverInfo) {
		if (!servers.remove(serverInfo)) {
			hiddenServers.remove(serverInfo);
		}
	}

	/**
	 * Добавляет сервер в список. Скрытые серверы добавляются в начало списка скрытых
	 * и автоматически вытесняют старые записи при превышении {@link #MAX_HIDDEN_ENTRIES}.
	 */
	public void add(ServerInfo serverInfo, boolean hidden) {
		if (hidden) {
			hiddenServers.add(0, serverInfo);

			while (hiddenServers.size() > MAX_HIDDEN_ENTRIES) {
				hiddenServers.remove(hiddenServers.size() - 1);
			}
		}
		else {
			servers.add(serverInfo);
		}
	}

	public int size() {
		return servers.size();
	}

	public void swapEntries(int index1, int index2) {
		ServerInfo temp = get(index1);
		servers.set(index1, get(index2));
		servers.set(index2, temp);
		saveFile();
	}

	public void set(int index, ServerInfo serverInfo) {
		servers.set(index, serverInfo);
	}

	private static boolean replace(ServerInfo serverInfo, List<ServerInfo> serverInfos) {
		for (int index = 0; index < serverInfos.size(); index++) {
			ServerInfo existing = serverInfos.get(index);

			if (Objects.equals(existing.name, serverInfo.name) && existing.address.equals(serverInfo.address)) {
				serverInfos.set(index, serverInfo);
				return true;
			}
		}

		return false;
	}

	/**
	 * Асинхронно обновляет запись сервера в файле {@code servers.dat}.
	 * Ищет совпадение по имени и адресу в обоих списках (видимом и скрытом).
	 */
	public static void updateServerListEntry(ServerInfo serverInfo) {
		IO_EXECUTOR.send(() -> {
			ServerList serverList = new ServerList(MinecraftClient.getInstance());
			serverList.loadFile();

			if (!replace(serverInfo, serverList.servers)) {
				replace(serverInfo, serverList.hiddenServers);
			}

			serverList.saveFile();
		});
	}
}
