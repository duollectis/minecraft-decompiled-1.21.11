package net.minecraft.scoreboard;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Контейнер очков одного держателя по всем целям скорборда.
 * Использует {@link Reference2ObjectOpenHashMap} для быстрого поиска по ссылке на цель.
 */
class Scores {

	private final Reference2ObjectOpenHashMap<ScoreboardObjective, ScoreboardScore> scores =
			new Reference2ObjectOpenHashMap<>(16, 0.5F);

	public @Nullable ScoreboardScore get(ScoreboardObjective objective) {
		return scores.get(objective);
	}

	/**
	 * Возвращает существующее очко по цели или создаёт новое,
	 * уведомляя переданный {@code onCreate} при создании.
	 */
	public ScoreboardScore getOrCreate(ScoreboardObjective objective, Consumer<ScoreboardScore> onCreate) {
		return scores.computeIfAbsent(objective, key -> {
			ScoreboardScore score = new ScoreboardScore();
			onCreate.accept(score);
			return score;
		});
	}

	public boolean remove(ScoreboardObjective objective) {
		return scores.remove(objective) != null;
	}

	public boolean hasScores() {
		return !scores.isEmpty();
	}

	/**
	 * Возвращает карту «цель → числовое значение очка» для внешнего использования.
	 */
	public Object2IntMap<ScoreboardObjective> getScoresAsIntMap() {
		Object2IntMap<ScoreboardObjective> result = new Object2IntOpenHashMap<>();
		scores.forEach((objective, score) -> result.put(objective, score.getScore()));
		return result;
	}

	void put(ScoreboardObjective objective, ScoreboardScore score) {
		scores.put(objective, score);
	}

	Map<ScoreboardObjective, ScoreboardScore> getScores() {
		return Collections.unmodifiableMap(scores);
	}
}
