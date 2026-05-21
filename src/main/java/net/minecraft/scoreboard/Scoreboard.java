package net.minecraft.scoreboard;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;

/**
 * Таблица очков: управляет целями, командами и очками игроков.
 * Является центральным хранилищем данных системы скорборда.
 */
public class Scoreboard {

	public static final String TEAM_SCORE_PREFIX = "#";
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Object2ObjectMap<String, ScoreboardObjective> objectives = new Object2ObjectOpenHashMap(16, 0.5F);
	private final Reference2ObjectMap<ScoreboardCriterion, List<ScoreboardObjective>>
			objectivesByCriterion =
			new Reference2ObjectOpenHashMap();
	private final Map<String, Scores> scores = new Object2ObjectOpenHashMap(16, 0.5F);
	private final Map<ScoreboardDisplaySlot, ScoreboardObjective>
			objectiveSlots =
			new EnumMap<>(ScoreboardDisplaySlot.class);
	private final Object2ObjectMap<String, Team> teams = new Object2ObjectOpenHashMap();
	private final Object2ObjectMap<String, Team> teamsByScoreHolder = new Object2ObjectOpenHashMap();

	/**
	 * Возвращает цель скорборда по имени или {@code null}, если она не существует.
	 */
	public @Nullable ScoreboardObjective getNullableObjective(@Nullable String name) {
		return (ScoreboardObjective) this.objectives.get(name);
	}

	/**
	 * Создаёт и регистрирует новую цель скорборда.
	 *
	 * @throws IllegalArgumentException если цель с таким именем уже существует
	 */
	public ScoreboardObjective addObjective(
			String name,
			ScoreboardCriterion criterion,
			Text displayName,
			ScoreboardCriterion.RenderType renderType,
			boolean displayAutoUpdate,
			@Nullable NumberFormat numberFormat
	) {
		if (this.objectives.containsKey(name)) {
			throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
		}
		else {
			ScoreboardObjective
					scoreboardObjective =
					new ScoreboardObjective(
							this,
							name,
							criterion,
							displayName,
							renderType,
							displayAutoUpdate,
							numberFormat
					);
			((List) this.objectivesByCriterion.computeIfAbsent(criterion, criterion2 -> Lists.newArrayList())).add(
					scoreboardObjective);
			this.objectives.put(name, scoreboardObjective);
			this.updateObjective(scoreboardObjective);
			return scoreboardObjective;
		}
	}

	/**
	 * Применяет действие ко всем очкам держателя по заданному критерию.
	 */
	public final void forEachScore(
			ScoreboardCriterion criterion,
			ScoreHolder scoreHolder,
			Consumer<ScoreAccess> action
	) {
		((List<ScoreboardObjective>) this.objectivesByCriterion.getOrDefault(criterion, Collections.emptyList()))
				.forEach(objective -> action.accept(this.getOrCreateScore(scoreHolder, objective, true)));
	}

	/**
	 * Возвращает или создаёт контейнер очков для держателя с указанным именем.
	 */
	private Scores getScores(String scoreHolderName) {
		return this.scores.computeIfAbsent(scoreHolderName, name -> new Scores());
	}

	/**
	 * Возвращает доступ к очку держателя по цели (только для чтения, если цель read-only).
	 */
	public ScoreAccess getOrCreateScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		return this.getOrCreateScore(scoreHolder, objective, false);
	}

	/**
	 * Возвращает доступ к очку держателя по цели с возможностью принудительной записи.
	 */
	public ScoreAccess getOrCreateScore(ScoreHolder scoreHolder, ScoreboardObjective objective, boolean forceWritable) {
		final boolean bl = forceWritable || !objective.getCriterion().isReadOnly();
		Scores holderScores = this.getScores(scoreHolder.getNameForScoreboard());
		final MutableBoolean mutableBoolean = new MutableBoolean();
		final ScoreboardScore scoreboardScore = holderScores.getOrCreate(objective, score -> mutableBoolean.setTrue());
		return new ScoreAccess() {
			@Override
			public int getScore() {
				return scoreboardScore.getScore();
			}

			@Override
			public void setScore(int score) {
				if (!bl) {
					throw new IllegalStateException("Cannot modify read-only score");
				}
				else {
					boolean blx = mutableBoolean.isTrue();
					if (objective.shouldDisplayAutoUpdate()) {
						Text text = scoreHolder.getDisplayName();
						if (text != null && !text.equals(scoreboardScore.getDisplayText())) {
							scoreboardScore.setDisplayText(text);
							blx = true;
						}
					}

					if (score != scoreboardScore.getScore()) {
						scoreboardScore.setScore(score);
						blx = true;
					}

					if (blx) {
						this.update();
					}
				}
			}

			@Override
			public @Nullable Text getDisplayText() {
				return scoreboardScore.getDisplayText();
			}

			@Override
			public void setDisplayText(@Nullable Text text) {
				if (mutableBoolean.isTrue() || !Objects.equals(text, scoreboardScore.getDisplayText())) {
					scoreboardScore.setDisplayText(text);
					this.update();
				}
			}

			@Override
			public void setNumberFormat(@Nullable NumberFormat numberFormat) {
				scoreboardScore.setNumberFormat(numberFormat);
				this.update();
			}

			@Override
			public boolean isLocked() {
				return scoreboardScore.isLocked();
			}

			@Override
			public void unlock() {
				this.setLocked(false);
			}

			@Override
			public void lock() {
				this.setLocked(true);
			}

			private void setLocked(boolean locked) {
				scoreboardScore.setLocked(locked);
				if (mutableBoolean.isTrue()) {
					this.update();
				}

				Scoreboard.this.resetScore(scoreHolder, objective);
			}

			private void update() {
				Scoreboard.this.updateScore(scoreHolder, objective, scoreboardScore);
				mutableBoolean.setFalse();
			}
		};
	}

	/**
	 * Возвращает очко держателя по цели или {@code null}, если оно не существует.
	 */
	public @Nullable ReadableScoreboardScore getScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		Scores holderScores = this.scores.get(scoreHolder.getNameForScoreboard());
		return holderScores != null ? holderScores.get(objective) : null;
	}

	/**
	 * Возвращает все записи скорборда для указанной цели.
	 */
	public Collection<ScoreboardEntry> getScoreboardEntries(ScoreboardObjective objective) {
		List<ScoreboardEntry> list = new ArrayList<>();
		this.scores.forEach((scoreHolderName, scores) -> {
			ScoreboardScore scoreboardScore = scores.get(objective);
			if (scoreboardScore != null) {
				list.add(new ScoreboardEntry(
						scoreHolderName,
						scoreboardScore.getScore(),
						scoreboardScore.getDisplayText(),
						scoreboardScore.getNumberFormat()
				));
			}
		});
		return list;
	}

	/**
	 * Возвращает все зарегистрированные цели скорборда.
	 */
	public Collection<ScoreboardObjective> getObjectives() {
		return this.objectives.values();
	}

	/**
	 * Возвращает имена всех зарегистрированных целей.
	 */
	public Collection<String> getObjectiveNames() {
		return this.objectives.keySet();
	}

	/**
	 * Возвращает всех известных держателей очков.
	 */
	public Collection<ScoreHolder> getKnownScoreHolders() {
		return this.scores.keySet().stream().map(ScoreHolder::fromName).toList();
	}

	/**
	 * Удаляет все очки держателя из скорборда.
	 */
	public void removeScores(ScoreHolder scoreHolder) {
		Scores holderScores = this.scores.remove(scoreHolder.getNameForScoreboard());
		if (holderScores != null) {
			this.onScoreHolderRemoved(scoreHolder);
		}
	}

	/**
	 * Удаляет очко держателя по конкретной цели.
	 */
	public void removeScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		Scores holderScores = this.scores.get(scoreHolder.getNameForScoreboard());
		if (holderScores != null) {
			boolean bl = holderScores.remove(objective);
			if (!holderScores.hasScores()) {
				Scores removedScores = this.scores.remove(scoreHolder.getNameForScoreboard());
				if (removedScores != null) {
					this.onScoreHolderRemoved(scoreHolder);
				}
			}
			else if (bl) {
				this.onScoreRemoved(scoreHolder, objective);
			}
		}
	}

	/**
	 * Возвращает карту очков держателя по всем целям.
	 */
	public Object2IntMap<ScoreboardObjective> getScoreHolderObjectives(ScoreHolder scoreHolder) {
		Scores holderScores = this.scores.get(scoreHolder.getNameForScoreboard());
		return holderScores != null ? holderScores.getScoresAsIntMap() : Object2IntMaps.emptyMap();
	}

	/**
	 * Удаляет цель скорборда и все связанные с ней очки.
	 */
	public void removeObjective(ScoreboardObjective objective) {
		this.objectives.remove(objective.getName());

		for (ScoreboardDisplaySlot scoreboardDisplaySlot : ScoreboardDisplaySlot.values()) {
			if (this.getObjectiveForSlot(scoreboardDisplaySlot) == objective) {
				this.setObjectiveSlot(scoreboardDisplaySlot, null);
			}
		}

		List<ScoreboardObjective>
				list =
				(List<ScoreboardObjective>) this.objectivesByCriterion.get(objective.getCriterion());
		if (list != null) {
			list.remove(objective);
		}

		for (Scores holderScores : this.scores.values()) {
			holderScores.remove(objective);
		}

		this.updateRemovedObjective(objective);
	}

	/**
	 * Назначает цель скорборда на указанный слот отображения.
	 */
	public void setObjectiveSlot(ScoreboardDisplaySlot slot, @Nullable ScoreboardObjective objective) {
		this.objectiveSlots.put(slot, objective);
	}

	/**
	 * Возвращает цель, назначенную на указанный слот отображения.
	 */
	public @Nullable ScoreboardObjective getObjectiveForSlot(ScoreboardDisplaySlot slot) {
		return this.objectiveSlots.get(slot);
	}

	/**
	 * Возвращает команду по имени или {@code null}, если она не существует.
	 */
	public @Nullable Team getTeam(String name) {
		return (Team) this.teams.get(name);
	}

	/**
	 * Создаёт новую команду с указанным именем или возвращает существующую с предупреждением.
	 */
	public Team addTeam(String name) {
		Team team = this.getTeam(name);
		if (team != null) {
			LOGGER.warn("Requested creation of existing team '{}'", name);
			return team;
		}
		else {
			team = new Team(this, name);
			this.teams.put(name, team);
			this.updateScoreboardTeamAndPlayers(team);
			return team;
		}
	}

	/**
	 * Удаляет команду и отвязывает всех её участников.
	 */
	public void removeTeam(Team team) {
		this.teams.remove(team.getName());

		for (String string : team.getPlayerList()) {
			this.teamsByScoreHolder.remove(string);
		}

		this.updateRemovedTeam(team);
	}

	/**
	 * Добавляет держателя очков в команду, предварительно удаляя его из текущей.
	 */
	public boolean addScoreHolderToTeam(String scoreHolderName, Team team) {
		if (this.getScoreHolderTeam(scoreHolderName) != null) {
			this.clearTeam(scoreHolderName);
		}

		this.teamsByScoreHolder.put(scoreHolderName, team);
		return team.getPlayerList().add(scoreHolderName);
	}

	/**
	 * Удаляет держателя очков из его текущей команды.
	 *
	 * @return {@code true}, если держатель был в команде
	 */
	public boolean clearTeam(String scoreHolderName) {
		Team team = this.getScoreHolderTeam(scoreHolderName);
		if (team != null) {
			this.removeScoreHolderFromTeam(scoreHolderName, team);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Удаляет держателя очков из указанной команды.
	 *
	 * @throws IllegalStateException если держатель не состоит в этой команде
	 */
	public void removeScoreHolderFromTeam(String scoreHolderName, Team team) {
		if (this.getScoreHolderTeam(scoreHolderName) != team) {
			throw new IllegalStateException(
					"Player is either on another team or not on any team. Cannot remove from team '" + team.getName()
							+ "'.");
		}
		else {
			this.teamsByScoreHolder.remove(scoreHolderName);
			team.getPlayerList().remove(scoreHolderName);
		}
	}

	/**
	 * Возвращает имена всех зарегистрированных команд.
	 */
	public Collection<String> getTeamNames() {
		return this.teams.keySet();
	}

	/**
	 * Возвращает все зарегистрированные команды.
	 */
	public Collection<Team> getTeams() {
		return this.teams.values();
	}

	/**
	 * Возвращает команду держателя очков или {@code null}, если он не в команде.
	 */
	public @Nullable Team getScoreHolderTeam(String scoreHolderName) {
		return (Team) this.teamsByScoreHolder.get(scoreHolderName);
	}

	public void updateObjective(ScoreboardObjective objective) {
	}

	public void updateExistingObjective(ScoreboardObjective objective) {
	}

	public void updateRemovedObjective(ScoreboardObjective objective) {
	}

	protected void updateScore(ScoreHolder scoreHolder, ScoreboardObjective objective, ScoreboardScore score) {
	}

	protected void resetScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
	}

	public void onScoreHolderRemoved(ScoreHolder scoreHolder) {
	}

	public void onScoreRemoved(ScoreHolder scoreHolder, ScoreboardObjective objective) {
	}

	public void updateScoreboardTeamAndPlayers(Team team) {
	}

	public void updateScoreboardTeam(Team team) {
	}

	public void updateRemovedTeam(Team team) {
	}

	/**
	 * Удаляет очки и команду мёртвой не-игровой сущности из скорборда.
	 */
	public void clearDeadEntity(Entity entity) {
		if (!(entity instanceof PlayerEntity) && !entity.isAlive()) {
			this.removeScores(entity);
			this.clearTeam(entity.getNameForScoreboard());
		}
	}

	protected List<Scoreboard.PackedEntry> pack() {
		return this.scores
				.entrySet()
				.stream()
				.flatMap(
						entry -> {
							String string = entry.getKey();
							return entry.getValue()
							            .getScores()
							            .entrySet()
							            .stream()
							            .map(entryx -> new Scoreboard.PackedEntry(
									            string,
									            entryx.getKey().getName(),
									            entryx.getValue().toPacked()
							            ));
						}
				)
				.toList();
	}

	protected void addEntry(Scoreboard.PackedEntry packedEntry) {
		ScoreboardObjective scoreboardObjective = this.getNullableObjective(packedEntry.objective);
		if (scoreboardObjective == null) {
			LOGGER.error("Unknown objective {} for name {}, ignoring", packedEntry.objective, packedEntry.owner);
		}
		else {
			this.getScores(packedEntry.owner).put(scoreboardObjective, new ScoreboardScore(packedEntry.score));
		}
	}

	protected List<Team.Packed> getPackedTeams() {
		return this.getTeams().stream().map(Team::pack).toList();
	}

	protected void addTeam(Team.Packed packedTeam) {
		Team team = this.addTeam(packedTeam.name());
		packedTeam.displayName().ifPresent(team::setDisplayName);
		packedTeam.color().ifPresent(team::setColor);
		team.setFriendlyFireAllowed(packedTeam.allowFriendlyFire());
		team.setShowFriendlyInvisibles(packedTeam.seeFriendlyInvisibles());
		team.setPrefix(packedTeam.memberNamePrefix());
		team.setSuffix(packedTeam.memberNameSuffix());
		team.setNameTagVisibilityRule(packedTeam.nameTagVisibility());
		team.setDeathMessageVisibilityRule(packedTeam.deathMessageVisibility());
		team.setCollisionRule(packedTeam.collisionRule());

		for (String string : packedTeam.players()) {
			this.addScoreHolderToTeam(string, team);
		}
	}

	protected List<ScoreboardObjective.Packed> getPackedObjectives() {
		return this.getObjectives().stream().map(ScoreboardObjective::pack).toList();
	}

	protected void addObjective(ScoreboardObjective.Packed packedObjective) {
		this.addObjective(
				packedObjective.name(),
				packedObjective.criteria(),
				packedObjective.displayName(),
				packedObjective.renderType(),
				packedObjective.displayAutoUpdate(),
				packedObjective.numberFormat().orElse(null)
		);
	}

	protected Map<ScoreboardDisplaySlot, String> getObjectivesBySlots() {
		Map<ScoreboardDisplaySlot, String> map = new EnumMap<>(ScoreboardDisplaySlot.class);

		for (ScoreboardDisplaySlot scoreboardDisplaySlot : ScoreboardDisplaySlot.values()) {
			ScoreboardObjective scoreboardObjective = this.getObjectiveForSlot(scoreboardDisplaySlot);
			if (scoreboardObjective != null) {
				map.put(scoreboardDisplaySlot, scoreboardObjective.getName());
			}
		}

		return map;
	}

	/**
	 * {@code PackedEntry}.
	 */
	public record PackedEntry(String owner, String objective, ScoreboardScore.Packed score) {

		public static final Codec<Scoreboard.PackedEntry> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						                    Codec.STRING.fieldOf("Name").forGetter(Scoreboard.PackedEntry::owner),
						                    Codec.STRING.fieldOf("Objective").forGetter(Scoreboard.PackedEntry::objective),
						                    ScoreboardScore.Packed.CODEC.forGetter(Scoreboard.PackedEntry::score)
				                    )
				                    .apply(instance, Scoreboard.PackedEntry::new)
		);
	}
}
