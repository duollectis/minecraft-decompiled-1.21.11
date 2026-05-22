package net.minecraft.client.util.tracy;

import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogListeners;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.event.Level;

@Environment(EnvType.CLIENT)
public class TracyLoader {

	private static final int COLOR_DEBUG = 0xAAAAAA;
	private static final int COLOR_WARN = 0xFFFFAA;
	private static final int COLOR_ERROR = 0xFFAAAA;
	private static final int COLOR_DEFAULT = 0xFFFFFF;

	private static boolean loaded;

	public static void load() {
		if (loaded) {
			return;
		}

		TracyClient.load();

		if (TracyClient.isAvailable()) {
			LogListeners.addListener("Tracy", (message, level) -> TracyClient.message(message, getColor(level)));
			loaded = true;
		}
	}

	private static int getColor(Level level) {
		return switch (level) {
			case DEBUG -> COLOR_DEBUG;
			case WARN -> COLOR_WARN;
			case ERROR -> COLOR_ERROR;
			default -> COLOR_DEFAULT;
		};
	}
}
