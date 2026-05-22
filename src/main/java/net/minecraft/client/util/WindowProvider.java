package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowSettings;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class WindowProvider implements AutoCloseable {

	private final MinecraftClient client;
	private final MonitorTracker monitorTracker;

	public WindowProvider(MinecraftClient client) {
		this.client = client;
		monitorTracker = new MonitorTracker(Monitor::new);
	}

	public Window createWindow(WindowSettings settings, @Nullable String videoMode, String title) {
		return new Window(client, monitorTracker, settings, videoMode, title);
	}

	@Override
	public void close() {
		monitorTracker.stop();
	}
}
