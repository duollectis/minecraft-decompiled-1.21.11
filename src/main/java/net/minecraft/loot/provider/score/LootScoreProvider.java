package net.minecraft.loot.provider.score;

import net.minecraft.loot.context.LootContext;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.util.context.ContextParameter;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Стратегия разрешения держателя очков ({@link ScoreHolder}) в контексте таблицы лута.
 * Используется в условиях и функциях лута, работающих со счётом на табло.
 */
public interface LootScoreProvider {

	@Nullable ScoreHolder getScoreHolder(LootContext context);

	LootScoreProviderType getType();

	Set<ContextParameter<?>> getRequiredParameters();
}
