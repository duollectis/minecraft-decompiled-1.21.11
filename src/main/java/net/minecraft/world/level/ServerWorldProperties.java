package net.minecraft.world.level;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.world.GameMode;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.timer.Timer;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Расширение {@link MutableWorldProperties} для серверного контекста.
 * Добавляет управление погодой, блуждающим торговцем, игровым режимом,
 * границей мира и таймером запланированных событий.
 *
 * <p>Реализация по умолчанию {@link #populateCrashReport} дополняет отчёт
 * о сбое именем уровня, режимом игры и текущим состоянием погоды.
 */
public interface ServerWorldProperties extends MutableWorldProperties {

	String getLevelName();

	void setThundering(boolean thundering);

	int getRainTime();

	void setRainTime(int rainTime);

	void setThunderTime(int thunderTime);

	int getThunderTime();

	@Override
	default void populateCrashReport(CrashReportSection reportSection, HeightLimitView world) {
		MutableWorldProperties.super.populateCrashReport(reportSection, world);
		reportSection.add("Level name", this::getLevelName);
		reportSection.add(
			"Level game mode",
			() -> String.format(
				Locale.ROOT,
				"Game mode: %s (ID %d). Hardcore: %b. Commands: %b",
				getGameMode().getId(),
				getGameMode().getIndex(),
				isHardcore(),
				areCommandsAllowed()
			)
		);
		reportSection.add(
			"Level weather",
			() -> String.format(
				Locale.ROOT,
				"Rain time: %d (now: %b), thunder time: %d (now: %b)",
				getRainTime(),
				isRaining(),
				getThunderTime(),
				isThundering()
			)
		);
	}

	int getClearWeatherTime();

	void setClearWeatherTime(int clearWeatherTime);

	int getWanderingTraderSpawnDelay();

	void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay);

	int getWanderingTraderSpawnChance();

	void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance);

	@Nullable UUID getWanderingTraderId();

	void setWanderingTraderId(UUID wanderingTraderId);

	GameMode getGameMode();

	@Deprecated
	Optional<WorldBorder.Properties> getWorldBorder();

	@Deprecated
	void setWorldBorder(Optional<WorldBorder.Properties> worldBorder);

	boolean isInitialized();

	void setInitialized(boolean initialized);

	boolean areCommandsAllowed();

	void setGameMode(GameMode gameMode);

	Timer<MinecraftServer> getScheduledEvents();

	void setTime(long time);

	void setTimeOfDay(long timeOfDay);

	GameRules getGameRules();
}
