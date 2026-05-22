package net.minecraft.loot.provider.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.provider.score.ContextLootScoreProvider;
import net.minecraft.loot.provider.score.LootScoreProvider;
import net.minecraft.loot.provider.score.LootScoreProviderTypes;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.context.ContextParameter;

import java.util.Set;

/**
 * Провайдер числа, читающий значение очков таблицы результатов для указанной сущности.
 * Результат умножается на коэффициент масштабирования {@code scale}.
 */
public record ScoreLootNumberProvider(
	LootScoreProvider target,
	String score,
	float scale
) implements LootNumberProvider {

	public static final MapCodec<ScoreLootNumberProvider> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			LootScoreProviderTypes.CODEC.fieldOf("target").forGetter(ScoreLootNumberProvider::target),
			Codec.STRING.fieldOf("score").forGetter(ScoreLootNumberProvider::score),
			Codec.FLOAT.fieldOf("scale").orElse(1.0F).forGetter(ScoreLootNumberProvider::scale)
		)
		.apply(instance, ScoreLootNumberProvider::new)
	);

	@Override
	public LootNumberProviderType getType() {
		return LootNumberProviderTypes.SCORE;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return target.getRequiredParameters();
	}

	public static ScoreLootNumberProvider create(LootContext.EntityReference target, String score) {
		return create(target, score, 1.0F);
	}

	public static ScoreLootNumberProvider create(LootContext.EntityReference target, String score, float scale) {
		return new ScoreLootNumberProvider(ContextLootScoreProvider.create(target), score, scale);
	}

	@Override
	public float nextFloat(LootContext context) {
		ScoreHolder scoreHolder = target.getScoreHolder(context);
		if (scoreHolder == null) {
			return 0.0F;
		}

		Scoreboard scoreboard = context.getWorld().getScoreboard();
		ScoreboardObjective objective = scoreboard.getNullableObjective(score);
		if (objective == null) {
			return 0.0F;
		}

		ReadableScoreboardScore scoreEntry = scoreboard.getScore(scoreHolder, objective);
		return scoreEntry == null ? 0.0F : scoreEntry.getScore() * scale;
	}
}
