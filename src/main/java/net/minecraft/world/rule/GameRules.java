package net.minecraft.world.rule;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Реестр всех правил игры и контейнер их текущих значений для конкретного мира.
 * Правила регистрируются статически при загрузке класса и хранятся в {@link Registries#GAME_RULE}.
 * Каждый экземпляр {@link GameRules} содержит набор значений, привязанных к конкретному миру.
 */
public class GameRules {

	public static final GameRule<Boolean> ADVANCE_TIME =
		registerBooleanRule("advance_time", GameRuleCategory.UPDATES, !SharedConstants.WORLD_RECREATE);
	public static final GameRule<Boolean> ADVANCE_WEATHER =
		registerBooleanRule("advance_weather", GameRuleCategory.UPDATES, !SharedConstants.WORLD_RECREATE);
	public static final GameRule<Boolean> ALLOW_ENTERING_NETHER_USING_PORTALS =
		registerBooleanRule("allow_entering_nether_using_portals", GameRuleCategory.MISC, true);
	public static final GameRule<Boolean> DO_TILE_DROPS =
		registerBooleanRule("block_drops", GameRuleCategory.DROPS, true);
	public static final GameRule<Boolean> BLOCK_EXPLOSION_DROP_DECAY =
		registerBooleanRule("block_explosion_drop_decay", GameRuleCategory.DROPS, true);
	public static final GameRule<Boolean> COMMAND_BLOCKS_WORK =
		registerBooleanRule("command_blocks_work", GameRuleCategory.MISC, true);
	public static final GameRule<Boolean> COMMAND_BLOCK_OUTPUT =
		registerBooleanRule("command_block_output", GameRuleCategory.CHAT, true);
	public static final GameRule<Boolean> DROWNING_DAMAGE =
		registerBooleanRule("drowning_damage", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> ELYTRA_MOVEMENT_CHECK =
		registerBooleanRule("elytra_movement_check", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> ENDER_PEARLS_VANISH_ON_DEATH =
		registerBooleanRule("ender_pearls_vanish_on_death", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> ENTITY_DROPS =
		registerBooleanRule("entity_drops", GameRuleCategory.DROPS, true);
	public static final GameRule<Boolean> FALL_DAMAGE =
		registerBooleanRule("fall_damage", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> FIRE_DAMAGE =
		registerBooleanRule("fire_damage", GameRuleCategory.PLAYER, true);
	public static final GameRule<Integer> FIRE_SPREAD_RADIUS_AROUND_PLAYER =
		registerIntRule("fire_spread_radius_around_player", GameRuleCategory.UPDATES, 128, -1);
	public static final GameRule<Boolean> FORGIVE_DEAD_PLAYERS =
		registerBooleanRule("forgive_dead_players", GameRuleCategory.MOBS, true);
	public static final GameRule<Boolean> FREEZE_DAMAGE =
		registerBooleanRule("freeze_damage", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> GLOBAL_SOUND_EVENTS =
		registerBooleanRule("global_sound_events", GameRuleCategory.MISC, true);
	public static final GameRule<Boolean> DO_IMMEDIATE_RESPAWN =
		registerBooleanRule("immediate_respawn", GameRuleCategory.PLAYER, false);
	public static final GameRule<Boolean> KEEP_INVENTORY =
		registerBooleanRule("keep_inventory", GameRuleCategory.PLAYER, false);
	public static final GameRule<Boolean> LAVA_SOURCE_CONVERSION =
		registerBooleanRule("lava_source_conversion", GameRuleCategory.UPDATES, false);
	public static final GameRule<Boolean> LIMITED_CRAFTING =
		registerBooleanRule("limited_crafting", GameRuleCategory.PLAYER, false);
	public static final GameRule<Boolean> LOCATOR_BAR =
		registerBooleanRule("locator_bar", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> LOG_ADMIN_COMMANDS =
		registerBooleanRule("log_admin_commands", GameRuleCategory.CHAT, true);
	public static final GameRule<Integer> MAX_BLOCK_MODIFICATIONS =
		registerIntRule("max_block_modifications", GameRuleCategory.MISC, 32768, 1);
	public static final GameRule<Integer> MAX_COMMAND_FORKS =
		registerIntRule("max_command_forks", GameRuleCategory.MISC, 65536, 0);
	public static final GameRule<Integer> MAX_COMMAND_SEQUENCE_LENGTH =
		registerIntRule("max_command_sequence_length", GameRuleCategory.MISC, 65536, 0);
	public static final GameRule<Integer> MAX_ENTITY_CRAMMING =
		registerIntRule("max_entity_cramming", GameRuleCategory.MOBS, 24, 0);
	public static final GameRule<Integer> MAX_MINECART_SPEED = registerIntRule(
		"max_minecart_speed", GameRuleCategory.MISC, 8, 1, 1000, FeatureSet.of(FeatureFlags.MINECART_IMPROVEMENTS)
	);
	public static final GameRule<Integer> MAX_SNOW_ACCUMULATION_HEIGHT =
		registerIntRule("max_snow_accumulation_height", GameRuleCategory.UPDATES, 1, 0, 8);
	public static final GameRule<Boolean> DO_MOB_LOOT =
		registerBooleanRule("mob_drops", GameRuleCategory.DROPS, true);
	public static final GameRule<Boolean> MOB_EXPLOSION_DROP_DECAY =
		registerBooleanRule("mob_explosion_drop_decay", GameRuleCategory.DROPS, true);
	public static final GameRule<Boolean> DO_MOB_GRIEFING =
		registerBooleanRule("mob_griefing", GameRuleCategory.MOBS, true);
	public static final GameRule<Boolean> NATURAL_HEALTH_REGENERATION =
		registerBooleanRule("natural_health_regeneration", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> PLAYER_MOVEMENT_CHECK =
		registerBooleanRule("player_movement_check", GameRuleCategory.PLAYER, true);
	public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_CREATIVE_DELAY =
		registerIntRule("players_nether_portal_creative_delay", GameRuleCategory.PLAYER, 0, 0);
	public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_DEFAULT_DELAY =
		registerIntRule("players_nether_portal_default_delay", GameRuleCategory.PLAYER, 80, 0);
	public static final GameRule<Integer> PLAYERS_SLEEPING_PERCENTAGE =
		registerIntRule("players_sleeping_percentage", GameRuleCategory.PLAYER, 100, 0);
	public static final GameRule<Boolean> PROJECTILES_CAN_BREAK_BLOCKS =
		registerBooleanRule("projectiles_can_break_blocks", GameRuleCategory.DROPS, true);
	public static final GameRule<Boolean> PVP =
		registerBooleanRule("pvp", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> DISABLE_RAIDS =
		registerBooleanRule("raids", GameRuleCategory.MOBS, true);
	public static final GameRule<Integer> RANDOM_TICK_SPEED =
		registerIntRule("random_tick_speed", GameRuleCategory.UPDATES, 3, 0);
	public static final GameRule<Boolean> REDUCED_DEBUG_INFO =
		registerBooleanRule("reduced_debug_info", GameRuleCategory.MISC, false);
	public static final GameRule<Integer> RESPAWN_RADIUS =
		registerIntRule("respawn_radius", GameRuleCategory.PLAYER, 10, 0);
	public static final GameRule<Boolean> SEND_COMMAND_FEEDBACK =
		registerBooleanRule("send_command_feedback", GameRuleCategory.CHAT, true);
	public static final GameRule<Boolean> ANNOUNCE_ADVANCEMENTS =
		registerBooleanRule("show_advancement_messages", GameRuleCategory.CHAT, true);
	public static final GameRule<Boolean> SHOW_DEATH_MESSAGES =
		registerBooleanRule("show_death_messages", GameRuleCategory.CHAT, true);
	public static final GameRule<Boolean> SPAWNER_BLOCKS_WORK =
		registerBooleanRule("spawner_blocks_work", GameRuleCategory.MISC, true);
	public static final GameRule<Boolean> DO_MOB_SPAWNING =
		registerBooleanRule("spawn_mobs", GameRuleCategory.SPAWNING, true);
	public static final GameRule<Boolean> SPAWN_MONSTERS =
		registerBooleanRule("spawn_monsters", GameRuleCategory.SPAWNING, true);
	public static final GameRule<Boolean> SPAWN_PATROLS =
		registerBooleanRule("spawn_patrols", GameRuleCategory.SPAWNING, true);
	public static final GameRule<Boolean> SPAWN_PHANTOMS =
		registerBooleanRule("spawn_phantoms", GameRuleCategory.SPAWNING, true);
	public static final GameRule<Boolean> SPAWN_WANDERING_TRADERS =
		registerBooleanRule("spawn_wandering_traders", GameRuleCategory.SPAWNING, true);
	public static final GameRule<Boolean> SPAWN_WARDENS =
		registerBooleanRule("spawn_wardens", GameRuleCategory.SPAWNING, true);
	public static final GameRule<Boolean> SPECTATORS_GENERATE_CHUNKS =
		registerBooleanRule("spectators_generate_chunks", GameRuleCategory.PLAYER, true);
	public static final GameRule<Boolean> SPREAD_VINES =
		registerBooleanRule("spread_vines", GameRuleCategory.UPDATES, true);
	public static final GameRule<Boolean> TNT_EXPLODES =
		registerBooleanRule("tnt_explodes", GameRuleCategory.MISC, true);
	public static final GameRule<Boolean> TNT_EXPLOSION_DROP_DECAY =
		registerBooleanRule("tnt_explosion_drop_decay", GameRuleCategory.DROPS, false);
	public static final GameRule<Boolean> UNIVERSAL_ANGER =
		registerBooleanRule("universal_anger", GameRuleCategory.MOBS, false);
	public static final GameRule<Boolean> WATER_SOURCE_CONVERSION =
		registerBooleanRule("water_source_conversion", GameRuleCategory.UPDATES, true);

	private final ServerGameRules rules;

	/**
	 * Создаёт Codec для сериализации/десериализации {@link GameRules} с учётом включённых фич.
	 *
	 * @param featureSet набор включённых фич
	 * @return Codec для {@link GameRules}
	 */
	public static Codec<GameRules> createCodec(FeatureSet featureSet) {
		return ServerGameRules.CODEC.xmap(
			rules -> new GameRules(featureSet, rules),
			gameRules -> gameRules.rules
		);
	}

	public GameRules(FeatureSet enabledFeatures, ServerGameRules rules) {
		this(enabledFeatures);
		this.rules.copyFrom(rules, this.rules::contains);
	}

	public GameRules(FeatureSet enabledFeatures) {
		rules = ServerGameRules.ofDefault(
			Registries.GAME_RULE
				.withFeatureFilter(enabledFeatures)
				.streamEntries()
				.map(RegistryEntry::value)
		);
	}

	public Stream<GameRule<?>> streamRules() {
		return rules.keySet().stream();
	}

	/**
	 * Возвращает текущее значение правила.
	 *
	 * @param rule правило игры
	 * @return текущее значение
	 * @throws IllegalArgumentException если правило не зарегистрировано в данном экземпляре
	 */
	public <T> T getValue(GameRule<T> rule) {
		T value = rules.get(rule);

		if (value == null) {
			throw new IllegalArgumentException("Tried to access invalid game rule");
		}

		return value;
	}

	/**
	 * Устанавливает новое значение правила и уведомляет сервер об изменении.
	 *
	 * @param rule   правило игры
	 * @param value  новое значение
	 * @param server сервер для уведомления (может быть {@code null})
	 * @throws IllegalArgumentException если правило не зарегистрировано в данном экземпляре
	 */
	public <T> void setValue(GameRule<T> rule, T value, @Nullable MinecraftServer server) {
		if (!rules.contains(rule)) {
			throw new IllegalArgumentException("Tried to set invalid game rule");
		}

		rules.put(rule, value);

		if (server != null) {
			server.onGameRuleUpdated(rule, value);
		}
	}

	/**
	 * Создаёт новый экземпляр {@link GameRules} с теми же значениями, но для другого набора фич.
	 *
	 * @param enabledFeatures новый набор включённых фич
	 * @return новый экземпляр с применёнными значениями
	 */
	public GameRules withEnabledFeatures(FeatureSet enabledFeatures) {
		return new GameRules(enabledFeatures, rules);
	}

	/**
	 * Копирует значения правил из другого экземпляра {@link GameRules}.
	 *
	 * @param source источник значений
	 * @param server сервер для уведомления об изменениях (может быть {@code null})
	 */
	public void copyFrom(GameRules source, @Nullable MinecraftServer server) {
		copyFrom(source.rules, server);
	}

	/**
	 * Копирует значения правил из {@link ServerGameRules}.
	 *
	 * @param source источник значений
	 * @param server сервер для уведомления об изменениях (может быть {@code null})
	 */
	public void copyFrom(ServerGameRules source, @Nullable MinecraftServer server) {
		source.keySet().forEach(rule -> copyFrom(source, (GameRule<?>) rule, server));
	}

	private <T> void copyFrom(ServerGameRules source, GameRule<T> rule, @Nullable MinecraftServer server) {
		setValue(rule, Objects.requireNonNull(source.get(rule)), server);
	}

	/**
	 * Обходит все правила через переданный посетитель.
	 *
	 * @param visitor посетитель правил
	 */
	public void accept(GameRuleVisitor visitor) {
		rules.keySet().forEach(rule -> {
			visitor.visit((GameRule<?>) rule);
			rule.accept(visitor);
		});
	}

	private static GameRule<Boolean> registerBooleanRule(
		String name,
		GameRuleCategory category,
		boolean defaultValue
	) {
		return register(
			name,
			category,
			GameRuleType.BOOL,
			BoolArgumentType.bool(),
			Codec.BOOL,
			defaultValue,
			FeatureSet.empty(),
			GameRuleVisitor::visitBoolean,
			value -> value ? 1 : 0
		);
	}

	private static GameRule<Integer> registerIntRule(
		String name,
		GameRuleCategory category,
		int defaultValue,
		int minValue
	) {
		return registerIntRule(name, category, defaultValue, minValue, Integer.MAX_VALUE, FeatureSet.empty());
	}

	private static GameRule<Integer> registerIntRule(
		String name,
		GameRuleCategory category,
		int defaultValue,
		int minValue,
		int maxValue
	) {
		return registerIntRule(name, category, defaultValue, minValue, maxValue, FeatureSet.empty());
	}

	private static GameRule<Integer> registerIntRule(
		String name,
		GameRuleCategory category,
		int defaultValue,
		int minValue,
		int maxValue,
		FeatureSet requiredFeatures
	) {
		return register(
			name,
			category,
			GameRuleType.INT,
			IntegerArgumentType.integer(minValue, maxValue),
			Codec.intRange(minValue, maxValue),
			defaultValue,
			requiredFeatures,
			GameRuleVisitor::visitInt,
			value -> value
		);
	}

	private static <T> GameRule<T> register(
		String name,
		GameRuleCategory category,
		GameRuleType type,
		ArgumentType<T> argumentType,
		Codec<T> codec,
		T defaultValue,
		FeatureSet requiredFeatures,
		Acceptor<T> acceptor,
		ToIntFunction<T> commandResultSupplier
	) {
		return Registry.register(
			Registries.GAME_RULE,
			name,
			new GameRule<>(
				category,
				type,
				argumentType,
				acceptor,
				codec,
				commandResultSupplier,
				defaultValue,
				requiredFeatures
			)
		);
	}

	/**
	 * Регистрирует все стандартные правила и возвращает первое из них.
	 * Вызывается при инициализации реестра.
	 *
	 * @param registry реестр правил игры
	 * @return первое зарегистрированное правило ({@link #ADVANCE_TIME})
	 */
	public static GameRule<?> registerAndGetDefault(Registry<GameRule<?>> registry) {
		return ADVANCE_TIME;
	}

	public <T> String getRuleValueName(GameRule<T> rule) {
		return rule.getValueName(getValue(rule));
	}

	/**
	 * Функциональный интерфейс для диспетчеризации вызова посетителя по типу правила.
	 *
	 * @param <T> тип значения правила
	 */
	public interface Acceptor<T> {

		void call(GameRuleVisitor consumer, GameRule<T> rule);
	}
}
