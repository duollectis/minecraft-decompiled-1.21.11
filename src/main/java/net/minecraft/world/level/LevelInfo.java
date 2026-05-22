package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRules;
import org.slf4j.Logger;

/**
 * Неизменяемый контейнер базовых настроек уровня: имя, режим игры, сложность,
 * правила игры и конфигурация датапаков. Все мутирующие операции возвращают
 * новый экземпляр (copy-on-write), что гарантирует потокобезопасность при чтении.
 */
public final class LevelInfo {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final String name;
	private final GameMode gameMode;
	private final boolean hardcore;
	private final Difficulty difficulty;
	private final boolean allowCommands;
	private final GameRules gameRules;
	private final DataConfiguration dataConfiguration;

	public LevelInfo(
		String name,
		GameMode gameMode,
		boolean hardcore,
		Difficulty difficulty,
		boolean allowCommands,
		GameRules gameRules,
		DataConfiguration dataConfiguration
	) {
		this.name = name;
		this.gameMode = gameMode;
		this.hardcore = hardcore;
		this.difficulty = difficulty;
		this.allowCommands = allowCommands;
		this.gameRules = gameRules;
		this.dataConfiguration = dataConfiguration;
	}

	/**
	 * Десериализует {@code LevelInfo} из NBT-совместимого {@link Dynamic}.
	 * Сложность читается как байт через {@code Difficulty.byId}; если поле отсутствует —
	 * используется {@link Difficulty#NORMAL}. Правила игры парсятся через кодек,
	 * привязанный к набору включённых фич из {@code dataConfiguration}.
	 *
	 * @param dynamic           динамическое NBT-представление данных уровня
	 * @param dataConfiguration конфигурация датапаков, определяющая доступные фичи
	 * @return десериализованный экземпляр {@code LevelInfo}
	 */
	public static LevelInfo fromDynamic(Dynamic<?> dynamic, DataConfiguration dataConfiguration) {
		GameMode gameMode = GameMode.byIndex(dynamic.get("GameType").asInt(0));

		return new LevelInfo(
			dynamic.get("LevelName").asString(""),
			gameMode,
			dynamic.get("hardcore").asBoolean(false),
			dynamic.get("Difficulty")
				.asNumber()
				.map(difficulty -> Difficulty.byId(difficulty.byteValue()))
				.result()
				.orElse(Difficulty.NORMAL),
			dynamic.get("allowCommands").asBoolean(gameMode == GameMode.CREATIVE),
			GameRules.createCodec(dataConfiguration.enabledFeatures())
				.parse(dynamic.get("game_rules").orElseEmptyMap())
				.resultOrPartial(LOGGER::warn)
				.orElseThrow(),
			dataConfiguration
		);
	}

	public String getLevelName() {
		return name;
	}

	public GameMode getGameMode() {
		return gameMode;
	}

	public boolean isHardcore() {
		return hardcore;
	}

	public Difficulty getDifficulty() {
		return difficulty;
	}

	public boolean areCommandsAllowed() {
		return allowCommands;
	}

	public GameRules getGameRules() {
		return gameRules;
	}

	public DataConfiguration getDataConfiguration() {
		return dataConfiguration;
	}

	/** Возвращает копию с изменённым режимом игры. */
	public LevelInfo withGameMode(GameMode mode) {
		return new LevelInfo(name, mode, hardcore, difficulty, allowCommands, gameRules, dataConfiguration);
	}

	/** Возвращает копию с изменённой сложностью. */
	public LevelInfo withDifficulty(Difficulty newDifficulty) {
		return new LevelInfo(name, gameMode, hardcore, newDifficulty, allowCommands, gameRules, dataConfiguration);
	}

	/** Возвращает копию с изменённой конфигурацией датапаков. */
	public LevelInfo withDataConfiguration(DataConfiguration newDataConfiguration) {
		return new LevelInfo(name, gameMode, hardcore, difficulty, allowCommands, gameRules, newDataConfiguration);
	}

	/**
	 * Возвращает копию, в которой правила игры пересозданы с учётом текущего набора
	 * включённых фич из {@code dataConfiguration}. Используется при создании нового
	 * мира, чтобы правила соответствовали активным датапакам.
	 */
	public LevelInfo withCopiedGameRules() {
		return new LevelInfo(
			name,
			gameMode,
			hardcore,
			difficulty,
			allowCommands,
			gameRules.withEnabledFeatures(dataConfiguration.enabledFeatures()),
			dataConfiguration
		);
	}
}
