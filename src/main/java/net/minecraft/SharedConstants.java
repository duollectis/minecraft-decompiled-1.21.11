package net.minecraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import net.minecraft.command.TranslatableBuiltInExceptions;
import net.minecraft.util.annotation.SuppressLinter;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Глобальные константы и отладочные флаги игры Minecraft.
 * <p>
 * Содержит версионные константы, параметры мира, отладочные переключатели
 * (управляемые через системные свойства {@code MC_DEBUG_*}) и прочие
 * общие настройки, разделяемые между клиентом и сервером.
 * <p>
 * Поля, помеченные {@link Deprecated}, дублируются в {@link GameVersion}
 * и должны использоваться только через неё.
 */
@SuppressLinter(reason = "System.out needed before bootstrap")
public class SharedConstants {

	// -------------------------------------------------------------------------
	// Версионные константы (устаревшие — используй GameVersion)
	// -------------------------------------------------------------------------

	@Deprecated
	public static final boolean IS_DEVELOPMENT_VERSION = false;
	@Deprecated
	public static final int WORLD_VERSION = 4671;
	@Deprecated
	public static final String CURRENT_SERIES = "main";
	@Deprecated
	public static final int RELEASE_TARGET_PROTOCOL_VERSION = 774;
	@Deprecated
	public static final int NBT_PROTOCOL_VERSION = 286;
	@Deprecated
	public static final int RESOURCE_PACK_VERSION = 75;
	@Deprecated
	public static final int MIN_RESOURCE_PACK_VERSION = 0;
	@Deprecated
	public static final int DATA_PACK_VERSION = 94;
	@Deprecated
	public static final int MIN_DATA_PACK_VERSION = 1;
	@Deprecated
	public static final int MIN_WORLD_VERSION = 1;

	// -------------------------------------------------------------------------
	// Константы мира и сети
	// -------------------------------------------------------------------------

	public static final int DEFAULT_PORT = 25565;
	public static final int CHUNK_WIDTH = 16;
	public static final int CHUNK_SECTION_SIZE = 16;
	public static final int DEFAULT_WORLD_HEIGHT = 256;
	public static final int MAX_RENDER_DISTANCE = 32;
	public static final int MAX_SIMULATION_DISTANCE = 128;
	public static final int MIN_SIMULATION_DISTANCE = 3;
	public static final int MAX_CHUNK_RADIUS = 64;
	private static final int MAX_RENDER_DISTANCE_CHUNKS = 30;

	// -------------------------------------------------------------------------
	// Константы тиков и времени
	// -------------------------------------------------------------------------

	public static final int TICKS_PER_SECOND = 20;
	public static final int MILLIS_PER_TICK = 50;
	public static final int TICKS_PER_MINUTE = 1200;
	public static final int TICKS_PER_IN_GAME_DAY = 24000;
	public static final float MILLIS_PER_HOUR = 3600000.0F;
	public static final long TICK_OVERTIME_THRESHOLD_NS = Duration.ofMillis(300L).toNanos();

	// -------------------------------------------------------------------------
	// Константы команд и данных
	// -------------------------------------------------------------------------

	public static final int COMMAND_MAX_LENGTH = 32500;
	public static final int EXPANDED_MACRO_COMMAND_MAX_LENGTH = 2000000;
	public static final int MAX_COMMAND_FEEDBACK_LENGTH = 1000000;
	public static final int WORLD_VERSION_STEP = 1;
	public static final int SNBT_TOO_OLD_THRESHOLD = 4650;
	public static final String DATA_VERSION_KEY = "DataVersion";
	public static final String CHAT_FORMAT_VERSION = "2.0.0";

	// -------------------------------------------------------------------------
	// Константы проекции
	// -------------------------------------------------------------------------

	public static final float PROJECTION_SCALE = 1365.3334F;
	public static final float PROJECTION_NEAR_FACTOR = 0.87890625F;
	public static final float PROJECTION_FAR_FACTOR = 17.578125F;

	// -------------------------------------------------------------------------
	// Прочие константы
	// -------------------------------------------------------------------------

	public static final char[] INVALID_CHARS_LEVEL_NAME = {
		'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'
	};
	public static final boolean CRASH_ON_UNCAUGHT_THREAD_EXCEPTION = false;
	public static final boolean DISABLE_CHAT_SIGNING = false;
	public static final boolean FORCE_UNICODE_FONT = false;
	public static final boolean DISABLE_REALMS_NOTIFICATIONS = false;
	public static final boolean FORCE_REALMS_PRODUCTION = false;
	public static final Level RESOURCE_LEAK_DETECTOR_DISABLED = Level.DISABLED;

	// -------------------------------------------------------------------------
	// Отладочные флаги (управляются через MC_DEBUG_* системные свойства)
	// -------------------------------------------------------------------------

	public static final String DEBUG_PREFIX = "MC_DEBUG_";
	public static final boolean DEBUG_ENABLED = propertyIsSet(getDebugPropertyName("ENABLED"));
	private static final boolean DEBUG_PRINT_PROPERTIES = propertyIsSet(getDebugPropertyName("PRINT_PROPERTIES"));

	public static final boolean OPEN_INCOMPATIBLE_WORLDS = debugProperty("OPEN_INCOMPATIBLE_WORLDS");
	public static final boolean ALLOW_LOW_SIM_DISTANCE = debugProperty("ALLOW_LOW_SIM_DISTANCE");
	public static final boolean HOTKEYS = debugProperty("HOTKEYS");
	public static final boolean UI_NARRATION = debugProperty("UI_NARRATION");
	public static final boolean SHUFFLE_UI_RENDERING_ORDER = debugProperty("SHUFFLE_UI_RENDERING_ORDER");
	public static final boolean SHUFFLE_MODELS = debugProperty("SHUFFLE_MODELS");
	public static final boolean RENDER_UI_LAYERING_RECTANGLES = debugProperty("RENDER_UI_LAYERING_RECTANGLES");
	public static final boolean PATHFINDING = debugProperty("PATHFINDING");
	public static final boolean SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES = debugProperty("SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES");
	public static final boolean SHAPES = debugProperty("SHAPES");
	public static final boolean NEIGHBORSUPDATE = debugProperty("NEIGHBORSUPDATE");
	public static final boolean EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER = debugProperty("EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER");
	public static final boolean STRUCTURES = debugProperty("STRUCTURES");
	public static final boolean GAME_EVENT_LISTENERS = debugProperty("GAME_EVENT_LISTENERS");
	public static final boolean DUMP_TEXTURE_ATLAS = debugProperty("DUMP_TEXTURE_ATLAS");
	public static final boolean DUMP_INTERPOLATED_TEXTURE_FRAMES = debugProperty("DUMP_INTERPOLATED_TEXTURE_FRAMES");
	public static final boolean STRUCTURE_EDIT_MODE = debugProperty("STRUCTURE_EDIT_MODE");
	public static final boolean SAVE_STRUCTURES_AS_SNBT = debugProperty("SAVE_STRUCTURES_AS_SNBT");
	public static final boolean SYNCHRONOUS_GL_LOGS = debugProperty("SYNCHRONOUS_GL_LOGS");
	public static final boolean VERBOSE_SERVER_EVENTS = debugProperty("VERBOSE_SERVER_EVENTS");
	public static final boolean NAMED_RUNNABLES = debugProperty("NAMED_RUNNABLES");
	public static final boolean GOAL_SELECTOR = debugProperty("GOAL_SELECTOR");
	public static final boolean VILLAGE_SECTIONS = debugProperty("VILLAGE_SECTIONS");
	public static final boolean BRAIN = debugProperty("BRAIN");
	public static final boolean POI = debugProperty("POI");
	public static final boolean BEES = debugProperty("BEES");
	public static final boolean RAIDS = debugProperty("RAIDS");
	public static final boolean BLOCK_BREAK = debugProperty("BLOCK_BREAK");
	public static final boolean MONITOR_TICK_TIMES = debugProperty("MONITOR_TICK_TIMES");
	public static final boolean KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN = debugProperty("KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN");
	public static final boolean DONT_SAVE_WORLD = debugProperty("DONT_SAVE_WORLD");
	public static final boolean LARGE_DRIPSTONE = debugProperty("LARGE_DRIPSTONE");
	public static final boolean CARVERS = debugProperty("CARVERS");
	public static final boolean ORE_VEINS = debugProperty("ORE_VEINS");
	public static final boolean SCULK_CATALYST = debugProperty("SCULK_CATALYST");
	public static final boolean BYPASS_REALMS_VERSION_CHECK = debugProperty("BYPASS_REALMS_VERSION_CHECK");
	public static final boolean SOCIAL_INTERACTIONS = debugProperty("SOCIAL_INTERACTIONS");
	public static final boolean VALIDATE_RESOURCE_PATH_CASE = debugProperty("VALIDATE_RESOURCE_PATH_CASE");
	public static final boolean UNLOCK_ALL_TRADES = debugProperty("UNLOCK_ALL_TRADES");
	public static final boolean BREEZE_MOB = debugProperty("BREEZE_MOB");
	public static final boolean TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS = debugProperty("TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS");
	public static final boolean VAULT_DETECTS_SHEEP_AS_PLAYERS = debugProperty("VAULT_DETECTS_SHEEP_AS_PLAYERS");
	public static final boolean FORCE_ONBOARDING_SCREEN = debugProperty("FORCE_ONBOARDING_SCREEN");
	public static final boolean CURSOR_POS = debugProperty("CURSOR_POS");
	public static final boolean DEFAULT_SKIN_OVERRIDE = debugProperty("DEFAULT_SKIN_OVERRIDE");
	public static final boolean PANORAMA_SCREENSHOT = debugProperty("PANORAMA_SCREENSHOT");
	public static final boolean CHASE_COMMAND = debugProperty("CHASE_COMMAND");
	public static final boolean VERBOSE_COMMAND_ERRORS = debugProperty("VERBOSE_COMMAND_ERRORS");
	public static final boolean DEV_COMMANDS = debugProperty("DEV_COMMANDS");
	public static final boolean ACTIVE_TEXT_AREAS = debugProperty("ACTIVE_TEXT_AREAS");
	public static final boolean IGNORE_LOCAL_MOB_CAP = debugProperty("IGNORE_LOCAL_MOB_CAP");
	public static final boolean DISABLE_LIQUID_SPREADING = debugProperty("DISABLE_LIQUID_SPREADING");
	public static final boolean AQUIFERS = debugProperty("AQUIFERS");
	public static final boolean JFR_PROFILING_ENABLE_LEVEL_LOADING = debugProperty("JFR_PROFILING_ENABLE_LEVEL_LOADING");
	public static final boolean ENTITY_BLOCK_INTERSECTION = debugProperty("ENTITY_BLOCK_INTERSECTION");
	public static final boolean ONLY_GENERATE_HALF_THE_WORLD = debugProperty("ONLY_GENERATE_HALF_THE_WORLD");
	public static final boolean DISABLE_FLUID_GENERATION = debugProperty("DISABLE_FLUID_GENERATION");
	public static final boolean DISABLE_AQUIFERS = debugProperty("DISABLE_AQUIFERS");
	public static final boolean DISABLE_SURFACE = debugProperty("DISABLE_SURFACE");
	public static final boolean DISABLE_CARVERS = debugProperty("DISABLE_CARVERS");
	public static final boolean DISABLE_STRUCTURES = debugProperty("DISABLE_STRUCTURES");
	public static final boolean DISABLE_FEATURES = debugProperty("DISABLE_FEATURES");
	public static final boolean DISABLE_ORE_VEINS = debugProperty("DISABLE_ORE_VEINS");
	public static final boolean DISABLE_BLENDING = debugProperty("DISABLE_BLENDING");
	public static final boolean DISABLE_BELOW_ZERO_RETROGENERATION = debugProperty("DISABLE_BELOW_ZERO_RETROGENERATION");
	public static final boolean SUBTITLES = debugProperty("SUBTITLES");
	public static final boolean COMMAND_STACK_TRACES = debugProperty("COMMAND_STACK_TRACES");
	public static final boolean WORLD_RECREATE = debugProperty("WORLD_RECREATE");
	public static final boolean SHOW_SERVER_DEBUG_VALUES = debugProperty("SHOW_SERVER_DEBUG_VALUES");
	public static final boolean FEATURE_COUNT = debugProperty("FEATURE_COUNT");
	public static final boolean FORCE_TELEMETRY = debugProperty("FORCE_TELEMETRY");
	public static final boolean DONT_SEND_TELEMETRY_TO_BACKEND = debugProperty("DONT_SEND_TELEMETRY_TO_BACKEND");

	public static final int FAKE_LATENCY_MS = debugIntProperty("FAKE_LATENCY_MS");
	public static final int FAKE_JITTER_MS = debugIntProperty("FAKE_JITTER_MS");

	// -------------------------------------------------------------------------
	// Изменяемые флаги состояния
	// -------------------------------------------------------------------------

	public static boolean DEBUG_BIOME_SOURCE = debugProperty("GENERATE_SQUARE_TERRAIN_WITHOUT_NOISE");
	public static boolean useChoiceTypeRegistrations = true;
	public static boolean isDevelopment;

	// -------------------------------------------------------------------------
	// Версия игры
	// -------------------------------------------------------------------------

	private static @Nullable GameVersion gameVersion;

	private static String getDebugPropertyName(String name) {
		return "MC_DEBUG_" + name;
	}

	private static boolean propertyIsSet(String name) {
		String value = System.getProperty(name);
		return value != null && (value.isEmpty() || Boolean.parseBoolean(value));
	}

	private static boolean debugProperty(String name) {
		if (!DEBUG_ENABLED) {
			return false;
		}

		String fullName = getDebugPropertyName(name);

		if (DEBUG_PRINT_PROPERTIES) {
			System.out.println("Debug property available: " + fullName + ": bool");
		}

		return propertyIsSet(fullName);
	}

	private static int debugIntProperty(String name) {
		if (!DEBUG_ENABLED) {
			return 0;
		}

		String fullName = getDebugPropertyName(name);

		if (DEBUG_PRINT_PROPERTIES) {
			System.out.println("Debug property available: " + fullName + ": int");
		}

		return Integer.parseInt(System.getProperty(fullName, "0"));
	}

	/**
	 * Устанавливает версию игры. Повторная установка той же версии допустима,
	 * но попытка заменить версию другим объектом бросает исключение.
	 *
	 * @param version версия игры для установки
	 * @throws IllegalStateException если версия уже установлена и отличается от переданной
	 */
	public static void setGameVersion(GameVersion version) {
		if (gameVersion == null) {
			gameVersion = version;
			return;
		}

		if (version != gameVersion) {
			throw new IllegalStateException("Cannot override the current game version!");
		}
	}

	/**
	 * Создаёт версию игры из файла {@code version.json}, если она ещё не установлена.
	 */
	public static void createGameVersion() {
		if (gameVersion == null) {
			gameVersion = MinecraftVersion.create();
		}
	}

	/**
	 * Возвращает текущую версию игры.
	 *
	 * @return текущая версия игры
	 * @throws IllegalStateException если версия ещё не была установлена
	 */
	public static GameVersion getGameVersion() {
		if (gameVersion == null) {
			throw new IllegalStateException("Game version not set");
		}

		return gameVersion;
	}

	public static int getProtocolVersion() {
		return RELEASE_TARGET_PROTOCOL_VERSION;
	}

	/**
	 * Проверяет, находится ли чанк за пределами допустимой области генерации.
	 * Используется в отладочных режимах для ограничения генерации мира.
	 *
	 * @param pos позиция чанка
	 * @return {@code true}, если чанк находится вне области генерации
	 */
	public static boolean isOutsideGenerationArea(ChunkPos pos) {
		int startX = pos.getStartX();
		int startZ = pos.getStartZ();

		if (ONLY_GENERATE_HALF_THE_WORLD) {
			return startZ < 0;
		}

		return DEBUG_BIOME_SOURCE && (startX > 8192 || startX < 0 || startZ > 1024 || startZ < 0);
	}

	static {
		ResourceLeakDetector.setLevel(RESOURCE_LEAK_DETECTOR_DISABLED);
		CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES = COMMAND_STACK_TRACES;
		CommandSyntaxException.BUILT_IN_EXCEPTIONS = new TranslatableBuiltInExceptions();
	}
}
