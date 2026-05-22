package net.minecraft.server.chase;

import com.mojang.logging.LogUtils;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ChaseCommand;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Клиент системы Chase — подключается к {@link ChaseServer} и непрерывно выполняет
 * команды телепортации, транслируемые сервером. Используется для синхронизации позиции
 * игрока между несколькими серверами в режиме разработки.
 */
public class ChaseClient {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int RETRY_INTERVAL_MS = 5000;
	private static final int RETRY_INTERVAL_SECONDS = 5;

	private final String ip;
	private final int port;
	private final MinecraftServer minecraftServer;
	private volatile boolean running;
	private @Nullable Socket socket;
	private @Nullable Thread thread;

	public ChaseClient(String ip, int port, MinecraftServer minecraftServer) {
		this.ip = ip;
		this.port = port;
		this.minecraftServer = minecraftServer;
	}

	public void start() {
		if (thread != null && thread.isAlive()) {
			LOGGER.warn("Remote control client was asked to start, but it is already running. Will ignore.");
		}

		running = true;
		thread = new Thread(this::run, "chase-client");
		thread.setDaemon(true);
		thread.start();
	}

	public void stop() {
		running = false;
		IOUtils.closeQuietly(socket);
		socket = null;
		thread = null;
	}

	public void run() {
		String address = ip + ":" + port;

		while (running) {
			try {
				LOGGER.info("Connecting to remote control server {}", address);
				socket = new Socket(ip, port);
				LOGGER.info("Connected to remote control server! Will continuously execute the command broadcasted by that server.");

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {
					while (running) {
						String line = reader.readLine();
						if (line == null) {
							LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", address, RETRY_INTERVAL_SECONDS);
							break;
						}

						parseMessage(line);
					}
				} catch (IOException exception) {
					LOGGER.warn("Lost connection to remote control server {}. Will retry in {}s.", address, RETRY_INTERVAL_SECONDS);
				}
			} catch (IOException exception) {
				LOGGER.warn("Failed to connect to remote control server {}. Will retry in {}s.", address, RETRY_INTERVAL_SECONDS);
			}

			if (running) {
				try {
					Thread.sleep(RETRY_INTERVAL_MS);
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	private void parseMessage(String message) {
		try (Scanner scanner = new Scanner(new StringReader(message))) {
			scanner.useLocale(Locale.ROOT);
			String type = scanner.next();
			if ("t".equals(type)) {
				executeTeleportCommand(scanner);
			} else {
				LOGGER.warn("Unknown message type '{}'", type);
			}
		} catch (NoSuchElementException exception) {
			LOGGER.warn("Could not parse message '{}', ignoring", message);
		}
	}

	private void executeTeleportCommand(Scanner scanner) {
		getTeleportPos(scanner).ifPresent(pos -> executeCommand(
				String.format(
						Locale.ROOT,
						"execute in %s run tp @s %.3f %.3f %.3f %.3f %.3f",
						pos.dimension.getValue(),
						pos.pos.x,
						pos.pos.y,
						pos.pos.z,
						pos.rot.y,
						pos.rot.x
				)
		));
	}

	@SuppressWarnings("unchecked")
	private Optional<TeleportPos> getTeleportPos(Scanner scanner) {
		RegistryKey<World> dimension = (RegistryKey<World>) ChaseCommand.DIMENSIONS.get(scanner.next());
		if (dimension == null) {
			return Optional.empty();
		}

		float x = scanner.nextFloat();
		float y = scanner.nextFloat();
		float z = scanner.nextFloat();
		float yaw = scanner.nextFloat();
		float pitch = scanner.nextFloat();
		return Optional.of(new TeleportPos(dimension, new Vec3d(x, y, z), new Vec2f(pitch, yaw)));
	}

	private void executeCommand(String command) {
		minecraftServer.execute(() -> {
			List<ServerPlayerEntity> players = minecraftServer.getPlayerManager().getPlayerList();
			if (players.isEmpty()) {
				return;
			}

			ServerPlayerEntity player = players.get(0);
			ServerWorld world = minecraftServer.getOverworld();
			ServerCommandSource source = new ServerCommandSource(
					player.getCommandOutput(),
					Vec3d.of(world.getSpawnPoint().getPos()),
					Vec2f.ZERO,
					world,
					LeveledPermissionPredicate.OWNERS,
					"",
					ScreenTexts.EMPTY,
					minecraftServer,
					player
			);
			minecraftServer.getCommandManager().parseAndExecute(source, command);
		});
	}

	record TeleportPos(RegistryKey<World> dimension, Vec3d pos, Vec2f rot) {
	}
}
