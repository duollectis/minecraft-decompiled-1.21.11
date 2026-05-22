package net.minecraft.client.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
/**
 * {@code InactivityFpsLimiter}.
 */
public class InactivityFpsLimiter {

	private static final int IN_GUI_FPS = 60;
	private static final int MINIMIZED_FPS = 10;
	private static final int AFK_STAGE_1_FPS = 30;
	private static final int AFK_STAGE_2_FPS = 10;
	private static final long AFK_STAGE_1_THRESHOLD = 60000L;
	private static final long AFK_STAGE_2_THRESHOLD = 600000L;
	private final GameOptions options;
	private final MinecraftClient client;
	private int maxFps;
	private long lastInputTime;

	public InactivityFpsLimiter(GameOptions options, MinecraftClient client) {
		this.options = options;
		this.client = client;
		this.maxFps = options.getMaxFps().getValue();
	}

	/**
	 * Update.
	 *
	 * @return int — результат операции
	 */
	public int update() {
		return switch (this.getLimitReason()) {
			case NONE -> this.maxFps;
			case WINDOW_ICONIFIED -> 10;
			case LONG_AFK -> 10;
			case SHORT_AFK -> Math.min(this.maxFps, AFK_STAGE_1_FPS);
			case OUT_OF_LEVEL_MENU -> IN_GUI_FPS;
		};
	}

	public InactivityFpsLimiter.LimitReason getLimitReason() {
		InactivityFpsLimit inactivityFpsLimit = this.options.getInactivityFpsLimit().getValue();
		if (this.client.getWindow().isMinimized()) {
			return InactivityFpsLimiter.LimitReason.WINDOW_ICONIFIED;
		}
		else {
			if (inactivityFpsLimit == InactivityFpsLimit.AFK) {
				long l = Util.getMeasuringTimeMs() - this.lastInputTime;
				if (l > AFK_STAGE_2_THRESHOLD) {
					return InactivityFpsLimiter.LimitReason.LONG_AFK;
				}

				if (l > AFK_STAGE_1_THRESHOLD) {
					return InactivityFpsLimiter.LimitReason.SHORT_AFK;
				}
			}

			return this.client.world != null || this.client.currentScreen == null && this.client.getOverlay() == null
			       ? InactivityFpsLimiter.LimitReason.NONE
			       : InactivityFpsLimiter.LimitReason.OUT_OF_LEVEL_MENU;
		}
	}

	/**
	 * Определяет, следует ли disable profiler timeout.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldDisableProfilerTimeout() {
		InactivityFpsLimiter.LimitReason limitReason = this.getLimitReason();
		return limitReason == InactivityFpsLimiter.LimitReason.WINDOW_ICONIFIED
				|| limitReason == InactivityFpsLimiter.LimitReason.LONG_AFK;
	}

	public void setMaxFps(int maxFps) {
		this.maxFps = maxFps;
	}

	/**
	 * Обрабатывает событие input.
	 */
	public void onInput() {
		this.lastInputTime = Util.getMeasuringTimeMs();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LimitReason}.
	 */
	public static enum LimitReason {
		NONE,
		WINDOW_ICONIFIED,
		LONG_AFK,
		SHORT_AFK,
		OUT_OF_LEVEL_MENU;
	}
}
