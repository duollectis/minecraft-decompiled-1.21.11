package net.minecraft.server.dedicated.management.handler;

import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * {@code ServerManagementHandler}.
 */
public interface ServerManagementHandler {

	boolean isLoading();

	boolean save(boolean suppressLogs, boolean flush, boolean force, ManagementConnectionId remote);

	void stop(boolean waitForShutdown, ManagementConnectionId remote);

	void broadcastMessage(Text message, ManagementConnectionId remote);

	void sendMessageTo(
			Text message,
			boolean overlay,
			Collection<ServerPlayerEntity> players,
			ManagementConnectionId remote
	);

	void broadcastMessage(Text message, boolean overlay, ManagementConnectionId remote);
}
