package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок приручает животное.
 */
public class TameAnimalCriterion extends AbstractCriterion<TameAnimalCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, AnimalEntity entity) {
		LootContext entityContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
		trigger(player, conditions -> conditions.matches(entityContext));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LootContextPredicate> entity
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("entity")
								.forGetter(Conditions::entity)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> any() {
			return Criteria.TAME_ANIMAL.create(new Conditions(Optional.empty(), Optional.empty()));
		}

		public static AdvancementCriterion<Conditions> create(EntityPredicate.Builder entity) {
			return Criteria.TAME_ANIMAL.create(new Conditions(
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(entity))
			));
		}

		public boolean matches(LootContext entityContext) {
			return entity.isEmpty() || entity.get().test(entityContext);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(entity, "entity");
		}
	}
}
