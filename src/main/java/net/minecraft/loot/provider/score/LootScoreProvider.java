package net.minecraft.loot.provider.score;

import java.util.Set;
import net.minecraft.loot.context.LootContext;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.util.context.ContextParameter;
import org.jspecify.annotations.Nullable;

public interface LootScoreProvider {
   @Nullable ScoreHolder getScoreHolder(LootContext context);

   LootScoreProviderType getType();

   Set<ContextParameter<?>> getRequiredParameters();
}
