package net.minecraft.world;

import com.mojang.serialization.Lifecycle;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Set;

/**
 * Свойства сохранения мира (уровня).
 * Содержит метаданные уровня: режим игры, сложность, правила, бренды серверов и т.д.
 */
public interface SaveProperties {

	int ANVIL_FORMAT_ID = 19133;
	int MCREGION_FORMAT_ID = 19132;

	DataConfiguration getDataConfiguration();

	void updateLevelInfo(DataConfiguration dataConfiguration);

	boolean isModded();

	Set<String> getServerBrands();

	Set<String> getRemovedFeatures();

	void addServerBrand(String brand, boolean modded);

	default void populateCrashReport(CrashReportSection section) {
		section.add("Known server brands", () -> String.join(", ", getServerBrands()));
		section.add("Removed feature flags", () -> String.join(", ", getRemovedFeatures()));
		section.add("Level was modded", () -> Boolean.toString(isModded()));
		section.add("Level storage version", () -> {
			int version = getVersion();
			return String.format(Locale.ROOT, "0x%05X - %s", version, getFormatName(version));
		});
	}

	default String getFormatName(int id) {
		return switch (id) {
			case MCREGION_FORMAT_ID -> "McRegion";
			case ANVIL_FORMAT_ID -> "Anvil";
			default -> "Unknown?";
		};
	}

	@Nullable NbtCompound getCustomBossEvents();

	void setCustomBossEvents(@Nullable NbtCompound customBossEvents);

	ServerWorldProperties getMainWorldProperties();

	LevelInfo getLevelInfo();

	NbtCompound cloneWorldNbt(DynamicRegistryManager registryManager, @Nullable NbtCompound playerNbt);

	boolean isHardcore();

	int getVersion();

	String getLevelName();

	GameMode getGameMode();

	void setGameMode(GameMode gameMode);

	boolean areCommandsAllowed();

	Difficulty getDifficulty();

	void setDifficulty(Difficulty difficulty);

	boolean isDifficultyLocked();

	void setDifficultyLocked(boolean difficultyLocked);

	GameRules getGameRules();

	@Nullable NbtCompound getPlayerData();

	EnderDragonFight.Data getDragonFight();

	void setDragonFight(EnderDragonFight.Data dragonFight);

	GeneratorOptions getGeneratorOptions();

	boolean isFlatWorld();

	boolean isDebugWorld();

	Lifecycle getLifecycle();

	default FeatureSet getEnabledFeatures() {
		return getDataConfiguration().enabledFeatures();
	}
}
