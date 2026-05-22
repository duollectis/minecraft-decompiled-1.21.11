package net.minecraft.client.gui.hud.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.tick.TickManager;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Запись отладочного HUD: TPS сервера, задержка тика, трафик пакетов.
 * Для интегрированного сервера показывает среднее время тика и режим спринта.
 */
@Environment(EnvType.CLIENT)
public class TpsDebugHudEntry implements DebugHudEntry {

	@Override
	public void render(
		DebugHudLines lines,
		@Nullable World world,
		@Nullable WorldChunk clientChunk,
		@Nullable WorldChunk chunk
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

		if (networkHandler == null || world == null) {
			return;
		}

		ClientConnection connection = networkHandler.getConnection();
		float packetsSent = connection.getAveragePacketsSent();
		float packetsReceived = connection.getAveragePacketsReceived();
		TickManager tickManager = world.getTickManager();
		IntegratedServer integratedServer = client.getServer();

		String tickStateLabel;
		if (tickManager.isStepping()) {
			tickStateLabel = " (frozen - stepping)";
		} else if (tickManager.isFrozen()) {
			tickStateLabel = " (frozen)";
		} else {
			tickStateLabel = "";
		}

		String serverLine;
		if (integratedServer != null) {
			ServerTickManager serverTickManager = integratedServer.getTickManager();
			boolean isSprinting = serverTickManager.isSprinting();

			if (isSprinting) {
				tickStateLabel = " (sprinting)";
			}

			String msPerTick = isSprinting ? "-" : String.format(Locale.ROOT, "%.1f", tickManager.getMillisPerTick());
			serverLine = String.format(
				Locale.ROOT,
				"Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx",
				integratedServer.getAverageTickTime(),
				msPerTick,
				tickStateLabel,
				packetsSent,
				packetsReceived
			);
		} else {
			serverLine = String.format(
				Locale.ROOT,
				"\"%s\" server%s, %.0f tx, %.0f rx",
				networkHandler.getBrand(),
				tickStateLabel,
				packetsSent,
				packetsReceived
			);
		}

		lines.addLine(serverLine);
	}

	@Override
	public boolean canShow(boolean reducedDebugInfo) {
		return true;
	}
}
