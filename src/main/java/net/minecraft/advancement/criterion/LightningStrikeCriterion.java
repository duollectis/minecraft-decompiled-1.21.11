package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Optional;

/**
 * Критерий: молния ударила рядом с игроком.
 * Проверяет саму молнию и наблюдателей (сущностей поблизости).
 */
public class LightningStrikeCriterion extends AbstractCriterion<LightningStrikeCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, LightningEntity lightning, List<Entity> bystanders) {
		List<LootContext> bystanderContexts = bystanders.stream()
				.map(bystander -> EntityPredicate.createAdvancementEntityLootContext(player, bystander))
				.toList();
		LootContext lightningContext = EntityPredicate.createAdvancementEntityLootContext(player, lightning);

		trigger(player, conditions -> conditions.test(lightningContext, bystanderContexts));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LootContextPredicate> lightning,
			Optional<LootContextPredicate> bystander
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("lightning")
								.forGetter(Conditions::lightning),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("bystander")
								.forGetter(Conditions::bystander)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(
				Optional<EntityPredicate> lightning,
				Optional<EntityPredicate> bystander
		) {
			return Criteria.LIGHTNING_STRIKE.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(lightning),
					EntityPredicate.contextPredicateFromEntityPredicate(bystander)
			));
		}

		public boolean test(LootContext lightningContext, List<LootContext> bystanderContexts) {
			if (lightning.isPresent() && !lightning.get().test(lightningContext)) {
				return false;
			}

			return bystander.isEmpty() || bystanderContexts.stream().anyMatch(bystander.get()::test);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(lightning, "lightning");
			validator.validateEntityPredicate(bystander, "bystander");
		}
	}
}
