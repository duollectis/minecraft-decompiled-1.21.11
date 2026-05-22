package net.minecraft.loot.condition;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.context.ContextParameter;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Условие, проверяющее очки сущности на табло по нескольким целям.
 *
 * <p>Все указанные цели должны соответствовать своим диапазонам — условие работает как AND.</p>
 */
public record EntityScoresLootCondition(
	Map<String, BoundedIntUnaryOperator> scores,
	LootContext.EntityReference entity
) implements LootCondition {

	public static final MapCodec<EntityScoresLootCondition> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, BoundedIntUnaryOperator.CODEC)
				.fieldOf("scores")
				.forGetter(EntityScoresLootCondition::scores),
			LootContext.EntityReference.CODEC.fieldOf("entity").forGetter(EntityScoresLootCondition::entity)
		)
		.apply(instance, EntityScoresLootCondition::new)
	);

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.ENTITY_SCORES;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Stream.concat(
			Stream.of(entity.contextParam()),
			scores.values().stream().flatMap(operator -> operator.getRequiredParameters().stream())
		).collect(ImmutableSet.toImmutableSet());
	}

	public boolean test(LootContext lootContext) {
		Entity target = lootContext.get(entity.contextParam());

		if (target == null) {
			return false;
		}

		Scoreboard scoreboard = lootContext.getWorld().getScoreboard();

		for (Entry<String, BoundedIntUnaryOperator> entry : scores.entrySet()) {
			if (!entityScoreIsInRange(lootContext, target, scoreboard, entry.getKey(), entry.getValue())) {
				return false;
			}
		}

		return true;
	}

	protected boolean entityScoreIsInRange(
		LootContext context,
		Entity target,
		Scoreboard scoreboard,
		String objectiveName,
		BoundedIntUnaryOperator range
	) {
		ScoreboardObjective objective = scoreboard.getNullableObjective(objectiveName);

		if (objective == null) {
			return false;
		}

		ReadableScoreboardScore score = scoreboard.getScore(target, objective);
		return score != null && range.test(context, score.getScore());
	}

	public static EntityScoresLootCondition.Builder create(LootContext.EntityReference target) {
		return new EntityScoresLootCondition.Builder(target);
	}

	public static class Builder implements LootCondition.Builder {

		private final ImmutableMap.Builder<String, BoundedIntUnaryOperator> scores = ImmutableMap.builder();
		private final LootContext.EntityReference target;

		public Builder(LootContext.EntityReference target) {
			this.target = target;
		}

		public EntityScoresLootCondition.Builder score(String name, BoundedIntUnaryOperator value) {
			scores.put(name, value);
			return this;
		}

		@Override
		public LootCondition build() {
			return new EntityScoresLootCondition(scores.build(), target);
		}
	}
}
