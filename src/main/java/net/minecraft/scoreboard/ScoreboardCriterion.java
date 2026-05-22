package net.minecraft.scoreboard;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.registry.Registries;
import net.minecraft.stat.StatType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Критерий скорборда — определяет, как и когда обновляется значение очка.
 * <p>
 * Простые критерии (dummy, trigger, deathCount и т.д.) регистрируются статически.
 * Статистические критерии создаются динамически по имени вида {@code stat_type:stat_id}.
 * Read-only критерии (здоровье, еда, воздух и т.д.) обновляются игровым движком
 * и не могут быть изменены командами.
 */
public class ScoreboardCriterion {

	private static final Map<String, ScoreboardCriterion> SIMPLE_CRITERIA = Maps.newHashMap();
	private static final Map<String, ScoreboardCriterion> CRITERIA = Maps.newHashMap();

	/**
	 * Кодек для сериализации критерия по его строковому имени.
	 * При десериализации пытается найти существующий или создать статистический критерий.
	 */
	public static final Codec<ScoreboardCriterion> CODEC = Codec.STRING.comapFlatMap(
			name -> getOrCreateStatCriterion(name)
					.<DataResult<ScoreboardCriterion>>map(DataResult::success)
					.orElse(DataResult.error(() -> "No scoreboard criteria with name: " + name)),
			ScoreboardCriterion::getName
	);

	public static final ScoreboardCriterion DUMMY = create("dummy");
	public static final ScoreboardCriterion TRIGGER = create("trigger");
	public static final ScoreboardCriterion DEATH_COUNT = create("deathCount");
	public static final ScoreboardCriterion PLAYER_KILL_COUNT = create("playerKillCount");
	public static final ScoreboardCriterion TOTAL_KILL_COUNT = create("totalKillCount");
	public static final ScoreboardCriterion HEALTH = create("health", true, RenderType.HEARTS);
	public static final ScoreboardCriterion FOOD = create("food", true, RenderType.INTEGER);
	public static final ScoreboardCriterion AIR = create("air", true, RenderType.INTEGER);
	public static final ScoreboardCriterion ARMOR = create("armor", true, RenderType.INTEGER);
	public static final ScoreboardCriterion XP = create("xp", true, RenderType.INTEGER);
	public static final ScoreboardCriterion LEVEL = create("level", true, RenderType.INTEGER);

	public static final ScoreboardCriterion[] TEAM_KILLS = new ScoreboardCriterion[]{
			create("teamkill." + Formatting.BLACK.getName()),
			create("teamkill." + Formatting.DARK_BLUE.getName()),
			create("teamkill." + Formatting.DARK_GREEN.getName()),
			create("teamkill." + Formatting.DARK_AQUA.getName()),
			create("teamkill." + Formatting.DARK_RED.getName()),
			create("teamkill." + Formatting.DARK_PURPLE.getName()),
			create("teamkill." + Formatting.GOLD.getName()),
			create("teamkill." + Formatting.GRAY.getName()),
			create("teamkill." + Formatting.DARK_GRAY.getName()),
			create("teamkill." + Formatting.BLUE.getName()),
			create("teamkill." + Formatting.GREEN.getName()),
			create("teamkill." + Formatting.AQUA.getName()),
			create("teamkill." + Formatting.RED.getName()),
			create("teamkill." + Formatting.LIGHT_PURPLE.getName()),
			create("teamkill." + Formatting.YELLOW.getName()),
			create("teamkill." + Formatting.WHITE.getName())
	};

	public static final ScoreboardCriterion[] KILLED_BY_TEAMS = new ScoreboardCriterion[]{
			create("killedByTeam." + Formatting.BLACK.getName()),
			create("killedByTeam." + Formatting.DARK_BLUE.getName()),
			create("killedByTeam." + Formatting.DARK_GREEN.getName()),
			create("killedByTeam." + Formatting.DARK_AQUA.getName()),
			create("killedByTeam." + Formatting.DARK_RED.getName()),
			create("killedByTeam." + Formatting.DARK_PURPLE.getName()),
			create("killedByTeam." + Formatting.GOLD.getName()),
			create("killedByTeam." + Formatting.GRAY.getName()),
			create("killedByTeam." + Formatting.DARK_GRAY.getName()),
			create("killedByTeam." + Formatting.BLUE.getName()),
			create("killedByTeam." + Formatting.GREEN.getName()),
			create("killedByTeam." + Formatting.AQUA.getName()),
			create("killedByTeam." + Formatting.RED.getName()),
			create("killedByTeam." + Formatting.LIGHT_PURPLE.getName()),
			create("killedByTeam." + Formatting.YELLOW.getName()),
			create("killedByTeam." + Formatting.WHITE.getName())
	};

	private final String name;
	private final boolean readOnly;
	private final RenderType defaultRenderType;

	public static ScoreboardCriterion create(String name, boolean readOnly, RenderType defaultRenderType) {
		ScoreboardCriterion criterion = new ScoreboardCriterion(name, readOnly, defaultRenderType);
		SIMPLE_CRITERIA.put(name, criterion);
		return criterion;
	}

	public static ScoreboardCriterion create(String name) {
		return create(name, false, RenderType.INTEGER);
	}

	protected ScoreboardCriterion(String name) {
		this(name, false, RenderType.INTEGER);
	}

	protected ScoreboardCriterion(String name, boolean readOnly, RenderType defaultRenderType) {
		this.name = name;
		this.readOnly = readOnly;
		this.defaultRenderType = defaultRenderType;
		CRITERIA.put(name, this);
	}

	public static Set<String> getAllSimpleCriteria() {
		return ImmutableSet.copyOf(SIMPLE_CRITERIA.keySet());
	}

	/**
	 * Ищет существующий критерий по имени или создаёт статистический критерий
	 * из строки формата {@code stat_type:stat_id} (разделитель — двоеточие, код 58).
	 *
	 * @param name строковое имя критерия
	 * @return {@link Optional} с найденным или созданным критерием
	 */
	public static Optional<ScoreboardCriterion> getOrCreateStatCriterion(String name) {
		ScoreboardCriterion existing = CRITERIA.get(name);
		if (existing != null) {
			return Optional.of(existing);
		}

		int colonIndex = name.indexOf(':');
		return colonIndex < 0
				? Optional.empty()
				: Registries.STAT_TYPE
						.getOptionalValue(Identifier.splitOn(name.substring(0, colonIndex), '.'))
						.flatMap(type -> getOrCreateStatCriterion(
								(StatType<?>) type,
								Identifier.splitOn(name.substring(colonIndex + 1), '.')
						));
	}

	private static <T> Optional<ScoreboardCriterion> getOrCreateStatCriterion(StatType<T> statType, Identifier id) {
		return statType.getRegistry().getOptionalValue(id).map(statType::getOrCreateStat);
	}

	public String getName() {
		return name;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public RenderType getDefaultRenderType() {
		return defaultRenderType;
	}

	/**
	 * Тип отображения значения очка в интерфейсе скорборда.
	 * {@code INTEGER} — числовое значение, {@code HEARTS} — иконки сердец (для здоровья).
	 */
	public enum RenderType implements StringIdentifiable {
		INTEGER("integer"),
		HEARTS("hearts");

		public static final StringIdentifiable.EnumCodec<RenderType> CODEC =
				StringIdentifiable.createCodec(RenderType::values);

		private final String name;

		RenderType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String asString() {
			return name;
		}

		public static RenderType getType(String name) {
			return CODEC.byId(name, INTEGER);
		}
	}
}
