package net.minecraft.world.level;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.world.*;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.timer.Timer;

import java.util.Optional;
import java.util.UUID;

/**
 * Декоратор над {@link ServerWorldProperties}, блокирующий все мутирующие операции.
 * Используется для дочерних измерений (Nether, End), которые читают глобальные
 * настройки из {@link SaveProperties} главного мира, но не должны их изменять.
 *
 * <p>Все сеттеры — пустые заглушки. Геттеры делегируют либо в {@code saveProperties}
 * (глобальные настройки: режим, сложность, правила), либо в {@code worldProperties}
 * (локальное состояние: время, погода, граница мира).
 */
public class UnmodifiableLevelProperties implements ServerWorldProperties {

	private final SaveProperties saveProperties;
	private final ServerWorldProperties worldProperties;

	public UnmodifiableLevelProperties(SaveProperties saveProperties, ServerWorldProperties worldProperties) {
		this.saveProperties = saveProperties;
		this.worldProperties = worldProperties;
	}

	@Override
	public WorldProperties.SpawnPoint getSpawnPoint() {
		return worldProperties.getSpawnPoint();
	}

	@Override
	public long getTime() {
		return worldProperties.getTime();
	}

	@Override
	public long getTimeOfDay() {
		return worldProperties.getTimeOfDay();
	}

	@Override
	public String getLevelName() {
		return saveProperties.getLevelName();
	}

	@Override
	public int getClearWeatherTime() {
		return worldProperties.getClearWeatherTime();
	}

	@Override
	public void setClearWeatherTime(int clearWeatherTime) {
	}

	@Override
	public boolean isThundering() {
		return worldProperties.isThundering();
	}

	@Override
	public int getThunderTime() {
		return worldProperties.getThunderTime();
	}

	@Override
	public boolean isRaining() {
		return worldProperties.isRaining();
	}

	@Override
	public int getRainTime() {
		return worldProperties.getRainTime();
	}

	@Override
	public GameMode getGameMode() {
		return saveProperties.getGameMode();
	}

	@Override
	public void setTime(long time) {
	}

	@Override
	public void setTimeOfDay(long timeOfDay) {
	}

	@Override
	public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {
		worldProperties.setSpawnPoint(spawnPoint);
	}

	@Override
	public void setThundering(boolean thundering) {
	}

	@Override
	public void setThunderTime(int thunderTime) {
	}

	@Override
	public void setRaining(boolean raining) {
	}

	@Override
	public void setRainTime(int rainTime) {
	}

	@Override
	public void setGameMode(GameMode gameMode) {
	}

	@Override
	public boolean isHardcore() {
		return saveProperties.isHardcore();
	}

	@Override
	public boolean areCommandsAllowed() {
		return saveProperties.areCommandsAllowed();
	}

	@Override
	public boolean isInitialized() {
		return worldProperties.isInitialized();
	}

	@Override
	public void setInitialized(boolean initialized) {
	}

	@Override
	public GameRules getGameRules() {
		return saveProperties.getGameRules();
	}

	@Override
	public Optional<WorldBorder.Properties> getWorldBorder() {
		return worldProperties.getWorldBorder();
	}

	@Override
	public void setWorldBorder(Optional<WorldBorder.Properties> worldBorder) {
	}

	@Override
	public Difficulty getDifficulty() {
		return saveProperties.getDifficulty();
	}

	@Override
	public boolean isDifficultyLocked() {
		return saveProperties.isDifficultyLocked();
	}

	@Override
	public Timer<MinecraftServer> getScheduledEvents() {
		return worldProperties.getScheduledEvents();
	}

	@Override
	public int getWanderingTraderSpawnDelay() {
		return 0;
	}

	@Override
	public void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay) {
	}

	@Override
	public int getWanderingTraderSpawnChance() {
		return 0;
	}

	@Override
	public void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance) {
	}

	@Override
	public UUID getWanderingTraderId() {
		return null;
	}

	@Override
	public void setWanderingTraderId(UUID wanderingTraderId) {
	}

	@Override
	public void populateCrashReport(CrashReportSection reportSection, HeightLimitView world) {
		reportSection.add("Derived", true);
		worldProperties.populateCrashReport(reportSection, world);
	}
}
