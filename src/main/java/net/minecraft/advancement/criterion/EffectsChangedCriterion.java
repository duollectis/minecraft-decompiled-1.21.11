package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityEffectPredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Критерий, срабатывающий при изменении эффектов у игрока.
 */
public class EffectsChangedCriterion extends AbstractCriterion<EffectsChangedCriterion.Conditions> {

	@Override
	public Codec<EffectsChangedCriterion.Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, @Nullable Entity source) {
		LootContext sourceContext = source == null
				? null
				: EntityPredicate.createAdvancementEntityLootContext(player, source);
		trigger(player, conditions -> conditions.matches(player, sourceContext));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<EntityEffectPredicate> effects,
			Optional<LootContextPredicate> source
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityEffectPredicate.CODEC
								.optionalFieldOf("effects")
								.forGetter(Conditions::effects),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("source")
								.forGetter(Conditions::source)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(EntityEffectPredicate.Builder effects) {
			return Criteria.EFFECTS_CHANGED.create(new Conditions(Optional.empty(), effects.build(), Optional.empty()));
		}

		public static AdvancementCriterion<Conditions> create(EntityPredicate.Builder source) {
			return Criteria.EFFECTS_CHANGED.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					Optional.of(EntityPredicate.asLootContextPredicate(source.build()))
			));
		}

		public boolean matches(ServerPlayerEntity player, @Nullable LootContext sourceContext) {
			if (effects.isPresent() && !effects.get().test((LivingEntity) player)) {
				return false;
			}

			return source.isEmpty() || sourceContext != null && source.get().test(sourceContext);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(source, "source");
		}
	}
}
