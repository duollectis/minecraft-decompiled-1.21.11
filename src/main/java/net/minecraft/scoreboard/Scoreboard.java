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
 * Центральное хранилище данных системы скорборда.
 * Управляет целями ({@link ScoreboardObjective}), командами ({@link Team})
 * и очками ({@link ScoreboardScore}) всех держателей.
 * <p>
 * Базовая реализация не выполняет сетевую синхронизацию —
 * для серверной логики используется {@link ServerScoreboard}.
 */
public class Scoreboard {

	public static final String TEAM_SCORE_PREFIX = "#";

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Object2ObjectMap<String, ScoreboardObjective> objectives = new Object2ObjectOpenHashMap<>(16, 0.5F);
	private final Reference2ObjectMap<ScoreboardCriterion, List<ScoreboardObjective>> objectivesByCriterion =
			new Reference2ObjectOpenHashMap<>();
	private final Map<String, Scores> scores = new Object2ObjectOpenHashMap<>(16, 0.5F);
	private final Map<ScoreboardDisplaySlot, ScoreboardObjective> objectiveSlots = new EnumMap<>(ScoreboardDisplaySlot.class);
	private final Object2ObjectMap<String, Team> teams = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectMap<String, Team> teamsByScoreHolder = new Object2ObjectOpenHashMap<>();

	/**
	 * Возвращает цель скорборда по имени или {@code null}, если она не зарегистрирована.
	 */
	public @Nullable ScoreboardObjective getNullableObjective(@Nullable String name) {
		return objectives.get(name);
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
		if (objectives.containsKey(name)) {
			throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
		}

		ScoreboardObjective objective = new ScoreboardObjective(
				this,
				name,
				criterion,
				displayName,
				renderType,
				displayAutoUpdate,
				numberFormat
		);

		((List<ScoreboardObjective>) objectivesByCriterion.computeIfAbsent(criterion, c -> Lists.newArrayList()))
				.add(objective);
		objectives.put(name, objective);
		updateObjective(objective);

		return objective;
	}

	/**
	 * Применяет действие ко всем очкам держателя по всем целям с заданным критерием.
	 * Используется для автоматического обновления read-only критериев (здоровье, еда и т.д.).
	 */
	public final void forEachScore(ScoreboardCriterion criterion, ScoreHolder scoreHolder, Consumer<ScoreAccess> action) {
		((List<ScoreboardObjective>) objectivesByCriterion.getOrDefault(criterion, Collections.emptyList()))
				.forEach(objective -> action.accept(getOrCreateScore(scoreHolder, objective, true)));
	}

	private Scores getScores(String scoreHolderName) {
		return scores.computeIfAbsent(scoreHolderName, name -> new Scores());
	}

	/**
	 * Возвращает доступ к очку держателя по цели.
	 * Если цель read-only, запись будет запрещена.
	 */
	public ScoreAccess getOrCreateScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		return getOrCreateScore(scoreHolder, objective, false);
	}

	/**
	 * Возвращает доступ к очку держателя по цели с возможностью принудительного разрешения записи.
	 * Анонимная реализация {@link ScoreAccess} отслеживает факт создания нового очка
	 * через {@link MutableBoolean}, чтобы гарантировать отправку обновления при первой записи.
	 *
	 * @param forceWritable если {@code true} — запись разрешена даже для read-only критериев
	 */
	public ScoreAccess getOrCreateScore(ScoreHolder scoreHolder, ScoreboardObjective objective, boolean forceWritable) {
		final boolean writable = forceWritable || !objective.getCriterion().isReadOnly();
		Scores holderScores = getScores(scoreHolder.getNameForScoreboard());
		final MutableBoolean justCreated = new MutableBoolean();
		final ScoreboardScore score = holderScores.getOrCreate(objective, s -> justCreated.setTrue());

		return new ScoreAccess() {
			@Override
			public int getScore() {
				return score.getScore();
			}

			@Override
			public void setScore(int value) {
				if (!writable) {
					throw new IllegalStateException("Cannot modify read-only score");
				}

				boolean changed = justCreated.isTrue();

				if (objective.shouldDisplayAutoUpdate()) {
					Text displayName = scoreHolder.getDisplayName();
					if (displayName != null && !displayName.equals(score.getDisplayText())) {
						score.setDisplayText(displayName);
						changed = true;
					}
				}

				if (value != score.getScore()) {
					score.setScore(value);
					changed = true;
				}

				if (changed) {
					update();
				}
			}

			@Override
			public @Nullable Text getDisplayText() {
				return score.getDisplayText();
			}

			@Override
			public void setDisplayText(@Nullable Text text) {
				if (justCreated.isTrue() || !Objects.equals(text, score.getDisplayText())) {
					score.setDisplayText(text);
					update();
				}
			}

			@Override
			public void setNumberFormat(@Nullable NumberFormat numberFormat) {
				score.setNumberFormat(numberFormat);
				update();
			}

			@Override
			public boolean isLocked() {
				return score.isLocked();
			}

			@Override
			public void unlock() {
				setLocked(false);
			}

			@Override
			public void lock() {
				setLocked(true);
			}

			private void setLocked(boolean locked) {
				score.setLocked(locked);
				if (justCreated.isTrue()) {
					update();
				}

				Scoreboard.this.resetScore(scoreHolder, objective);
			}

			private void update() {
				Scoreboard.this.updateScore(scoreHolder, objective, score);
				justCreated.setFalse();
			}
		};
	}

	/**
	 * Возвращает очко держателя по цели только для чтения, или {@code null}, если оно не существует.
	 */
	public @Nullable ReadableScoreboardScore getScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		Scores holderScores = scores.get(scoreHolder.getNameForScoreboard());
		return holderScores != null ? holderScores.get(objective) : null;
	}

	/**
	 * Возвращает все записи скорборда для указанной цели в виде снимка.
	 */
	public Collection<ScoreboardEntry> getScoreboardEntries(ScoreboardObjective objective) {
		List<ScoreboardEntry> entries = new ArrayList<>();
		scores.forEach((holderName, holderScores) -> {
			ScoreboardScore holderScore = holderScores.get(objective);
			if (holderScore != null) {
				entries.add(new ScoreboardEntry(
						holderName,
						holderScore.getScore(),
						holderScore.getDisplayText(),
						holderScore.getNumberFormat()
				));
			}
		});
		return entries;
	}

	public Collection<ScoreboardObjective> getObjectives() {
		return objectives.values();
	}

	public Collection<String> getObjectiveNames() {
		return objectives.keySet();
	}

	public Collection<ScoreHolder> getKnownScoreHolders() {
		return scores.keySet().stream().map(ScoreHolder::fromName).toList();
	}

	/**
	 * Удаляет все очки держателя из скорборда и уведомляет об этом подписчиков.
	 */
	public void removeScores(ScoreHolder scoreHolder) {
		Scores removed = scores.remove(scoreHolder.getNameForScoreboard());
		if (removed != null) {
			onScoreHolderRemoved(scoreHolder);
		}
	}

	/**
	 * Удаляет очко держателя по конкретной цели.
	 * Если после удаления у держателя не осталось очков — удаляет его полностью.
	 */
	public void removeScore(ScoreHolder scoreHolder, ScoreboardObjective objective) {
		Scores holderScores = scores.get(scoreHolder.getNameForScoreboard());
		if (holderScores == null) {
			return;
		}

		boolean removed = holderScores.remove(objective);
		if (!holderScores.hasScores()) {
			Scores removedScores = scores.remove(scoreHolder.getNameForScoreboard());
			if (removedScores != null) {
				onScoreHolderRemoved(scoreHolder);
			}
		} else if (removed) {
			onScoreRemoved(scoreHolder, objective);
		}
	}

	/**
	 * Возвращает карту «цель → значение очка» для указанного держателя.
	 */
	public Object2IntMap<ScoreboardObjective> getScoreHolderObjectives(ScoreHolder scoreHolder) {
		Scores holderScores = scores.get(scoreHolder.getNameForScoreboard());
		return holderScores != null ? holderScores.getScoresAsIntMap() : Object2IntMaps.emptyMap();
	}

	/**
	 * Удаляет цель скорборда, очищает все слоты отображения и связанные очки.
	 */
	public void removeObjective(ScoreboardObjective objective) {
		objectives.remove(objective.getName());

		for (ScoreboardDisplaySlot slot : ScoreboardDisplaySlot.values()) {
			if (getObjectiveForSlot(slot) == objective) {
				setObjectiveSlot(slot, null);
			}
		}

		List<ScoreboardObjective> criterionObjectives = objectivesByCriterion.get(objective.getCriterion());
		if (criterionObjectives != null) {
			criterionObjectives.remove(objective);
		}

		for (Scores holderScores : scores.values()) {
			holderScores.remove(objective);
		}

		updateRemovedObjective(objective);
	}

	/**
	 * Назначает цель скорборда на указанный слот отображения.
	 * Передача {@code null} очищает слот.
	 */
	public void setObjectiveSlot(ScoreboardDisplaySlot slot, @Nullable ScoreboardObjective objective) {
		objectiveSlots.put(slot, objective);
	}

	public @Nullable ScoreboardObjective getObjectiveForSlot(ScoreboardDisplaySlot slot) {
		return objectiveSlots.get(slot);
	}

	public @Nullable Team getTeam(String name) {
		return teams.get(name);
	}

	/**
	 * Создаёт новую команду с указанным именем.
	 * Если команда уже существует — возвращает её с предупреждением в лог.
	 */
	public Team addTeam(String name) {
		Team existing = getTeam(name);
		if (existing != null) {
			LOGGER.warn("Requested creation of existing team '{}'", name);
			return existing;
		}

		Team team = new Team(this, name);
		teams.put(name, team);
		updateScoreboardTeamAndPlayers(team);
		return team;
	}

	/**
	 * Удаляет команду и отвязывает всех её участников из индекса {@code teamsByScoreHolder}.
	 */
	public void removeTeam(Team team) {
		teams.remove(team.getName());

		for (String playerName : team.getPlayerList()) {
			teamsByScoreHolder.remove(playerName);
		}

		updateRemovedTeam(team);
	}

	/**
	 * Добавляет держателя очков в команду, предварительно удаляя его из текущей команды.
	 *
	 * @return {@code true}, если держатель успешно добавлен в список участников команды
	 */
	public boolean addScoreHolderToTeam(String scoreHolderName, Team team) {
		if (getScoreHolderTeam(scoreHolderName) != null) {
			clearTeam(scoreHolderName);
		}

		teamsByScoreHolder.put(scoreHolderName, team);
		return team.getPlayerList().add(scoreHolderName);
	}

	/**
	 * Удаляет держателя очков из его текущей команды.
	 *
	 * @return {@code true}, если держатель был в команде и успешно удалён
	 */
	public boolean clearTeam(String scoreHolderName) {
		Team team = getScoreHolderTeam(scoreHolderName);
		if (team == null) {
			return false;
		}

		removeScoreHolderFromTeam(scoreHolderName, team);
		return true;
	}

	/**
	 * Удаляет держателя очков из указанной команды.
	 *
	 * @throws IllegalStateException если держатель не состоит в этой команде
	 */
	public void removeScoreHolderFromTeam(String scoreHolderName, Team team) {
		if (getScoreHolderTeam(scoreHolderName) != team) {
			throw new IllegalStateException(
					"Player is either on another team or not on any team. Cannot remove from team '" + team.getName() + "'."
			);
		}

		teamsByScoreHolder.remove(scoreHolderName);
		team.getPlayerList().remove(scoreHolderName);
	}

	public Collection<String> getTeamNames() {
		return teams.keySet();
	}

	public Collection<Team> getTeams() {
		return teams.values();
	}

	public @Nullable Team getScoreHolderTeam(String scoreHolderName) {
		return teamsByScoreHolder.get(scoreHolderName);
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
	 * Игроки не удаляются автоматически — их данные сохраняются между сессиями.
	 */
	public void clearDeadEntity(Entity entity) {
		if (entity instanceof PlayerEntity || entity.isAlive()) {
			return;
		}

		removeScores(entity);
		clearTeam(entity.getNameForScoreboard());
	}

	protected List<PackedEntry> pack() {
		return scores.entrySet()
				.stream()
				.flatMap(entry -> entry.getValue()
						.getScores()
						.entrySet()
						.stream()
						.map(scoreEntry -> new PackedEntry(
								entry.getKey(),
								scoreEntry.getKey().getName(),
								scoreEntry.getValue().toPacked()
						))
				)
				.toList();
	}

	protected void addEntry(PackedEntry packedEntry) {
		ScoreboardObjective objective = getNullableObjective(packedEntry.objective);
		if (objective == null) {
			LOGGER.error("Unknown objective {} for name {}, ignoring", packedEntry.objective, packedEntry.owner);
			return;
		}

		getScores(packedEntry.owner).put(objective, new ScoreboardScore(packedEntry.score));
	}

	protected List<Team.Packed> getPackedTeams() {
		return getTeams().stream().map(Team::pack).toList();
	}

	protected void addTeam(Team.Packed packedTeam) {
		Team team = addTeam(packedTeam.name());
		packedTeam.displayName().ifPresent(team::setDisplayName);
		packedTeam.color().ifPresent(team::setColor);
		team.setFriendlyFireAllowed(packedTeam.allowFriendlyFire());
		team.setShowFriendlyInvisibles(packedTeam.seeFriendlyInvisibles());
		team.setPrefix(packedTeam.memberNamePrefix());
		team.setSuffix(packedTeam.memberNameSuffix());
		team.setNameTagVisibilityRule(packedTeam.nameTagVisibility());
		team.setDeathMessageVisibilityRule(packedTeam.deathMessageVisibility());
		team.setCollisionRule(packedTeam.collisionRule());

		for (String playerName : packedTeam.players()) {
			addScoreHolderToTeam(playerName, team);
		}
	}

	protected List<ScoreboardObjective.Packed> getPackedObjectives() {
		return getObjectives().stream().map(ScoreboardObjective::pack).toList();
	}

	protected void addObjective(ScoreboardObjective.Packed packedObjective) {
		addObjective(
				packedObjective.name(),
				packedObjective.criteria(),
				packedObjective.displayName(),
				packedObjective.renderType(),
				packedObjective.displayAutoUpdate(),
				packedObjective.numberFormat().orElse(null)
		);
	}

	protected Map<ScoreboardDisplaySlot, String> getObjectivesBySlots() {
		Map<ScoreboardDisplaySlot, String> slotMap = new EnumMap<>(ScoreboardDisplaySlot.class);

		for (ScoreboardDisplaySlot slot : ScoreboardDisplaySlot.values()) {
			ScoreboardObjective objective = getObjectiveForSlot(slot);
			if (objective != null) {
				slotMap.put(slot, objective.getName());
			}
		}

		return slotMap;
	}

	/**
	 * Упакованная запись очка для сериализации в NBT/JSON.
	 * Хранит имя владельца, имя цели и упакованные данные очка.
	 */
	public record PackedEntry(String owner, String objective, ScoreboardScore.Packed score) {

		public static final Codec<PackedEntry> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codec.STRING.fieldOf("Name").forGetter(PackedEntry::owner),
						Codec.STRING.fieldOf("Objective").forGetter(PackedEntry::objective),
						ScoreboardScore.Packed.CODEC.forGetter(PackedEntry::score)
				).apply(instance, PackedEntry::new)
		);
	}
}
