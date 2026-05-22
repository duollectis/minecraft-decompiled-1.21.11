package net.minecraft.server.chase;

import com.mojang.logging.LogUtils;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ChaseCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Сервер системы Chase — принимает подключения от {@link ChaseClient} и периодически
 * транслирует им текущую позицию первого игрока на сервере. Используется для синхронизации
 * позиции между несколькими серверами в режиме разработки.
 */
public class ChaseServer {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int BACKLOG = 50;

	private final String ip;
	private final int port;
	private final PlayerManager playerManager;
	private final int interval;
	private volatile boolean running;
	private @Nullable ServerSocket socket;
	private final CopyOnWriteArrayList<Socket> clientSockets = new CopyOnWriteArrayList<>();

	public ChaseServer(String ip, int port, PlayerManager playerManager, int interval) {
		this.ip = ip;
		this.port = port;
		this.playerManager = playerManager;
		this.interval = interval;
	}

	public void start() throws IOException {
		if (socket != null && !socket.isClosed()) {
			LOGGER.warn("Remote control server was asked to start, but it is already running. Will ignore.");
			return;
		}

		running = true;
		socket = new ServerSocket(port, BACKLOG, InetAddress.getByName(ip));

		Thread acceptor = new Thread(this::runAcceptor, "chase-server-acceptor");
		acceptor.setDaemon(true);
		acceptor.start();

		Thread sender = new Thread(this::runSender, "chase-server-sender");
		sender.setDaemon(true);
		sender.start();
	}

	public void stop() {
		running = false;
		IOUtils.closeQuietly(socket);
		socket = null;
	}

	private void runSender() {
		TeleportPos lastPos = null;

		while (running) {
			if (!clientSockets.isEmpty()) {
				TeleportPos currentPos = getTeleportPosition();
				if (currentPos != null && !currentPos.equals(lastPos)) {
					lastPos = currentPos;
					byte[] payload = currentPos.getTeleportCommand().getBytes(StandardCharsets.US_ASCII);

					for (Socket clientSocket : clientSockets) {
						if (clientSocket.isClosed()) {
							continue;
						}

						Util.getIoWorkerExecutor().execute(() -> {
							try {
								OutputStream out = clientSocket.getOutputStream();
								out.write(payload);
								out.flush();
							} catch (IOException exception) {
								LOGGER.info("Remote control client socket got an IO exception and will be closed", exception);
								IOUtils.closeQuietly(clientSocket);
							}
						});
					}
				}

				List<Socket> closed = clientSockets.stream().filter(Socket::isClosed).collect(Collectors.toList());
				clientSockets.removeAll(closed);
			}

			if (running) {
				try {
					Thread.sleep(interval);
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	private void runAcceptor() {
		try {
			while (running) {
				if (socket == null) {
					continue;
				}

				LOGGER.info("Remote control server is listening for connections on port {}", port);
				Socket clientSocket = socket.accept();
				LOGGER.info("Remote control server received client connection on port {}", clientSocket.getPort());
				clientSockets.add(clientSocket);
			}
		} catch (ClosedByInterruptException exception) {
			if (running) {
				LOGGER.info("Remote control server closed by interrupt");
			}
		} catch (IOException exception) {
			if (running) {
				LOGGER.error("Remote control server closed because of an IO exception", exception);
			}
		} finally {
			IOUtils.closeQuietly(socket);
		}

		LOGGER.info("Remote control server is now stopped");
		running = false;
	}

	@SuppressWarnings("unchecked")
	private @Nullable TeleportPos getTeleportPosition() {
		List<ServerPlayerEntity> players = playerManager.getPlayerList();
		if (players.isEmpty()) {
			return null;
		}

		ServerPlayerEntity player = players.get(0);
		String dimensionName = (String) ChaseCommand.DIMENSIONS.inverse().get(player.getEntityWorld().getRegistryKey());
		return dimensionName == null
				? null
				: new TeleportPos(dimensionName, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
	}

	record TeleportPos(String dimensionName, double x, double y, double z, float yaw, float pitch) {

		String getTeleportCommand() {
			return String.format(
					Locale.ROOT,
					"t %s %.2f %.2f %.2f %.2f %.2f\n",
					dimensionName,
					x,
					y,
					z,
					yaw,
					pitch
			);
		}
	}
}
