package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Критерий: игрок скрестил двух животных.
 * Поддерживает проверку родителей и потомка по отдельности.
 * Родители взаимозаменяемы — порядок передачи не важен.
 */
public class BredAnimalsCriterion extends AbstractCriterion<BredAnimalsCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(
			ServerPlayerEntity player,
			AnimalEntity parent,
			AnimalEntity partner,
			@Nullable PassiveEntity child
	) {
		LootContext parentContext = EntityPredicate.createAdvancementEntityLootContext(player, parent);
		LootContext partnerContext = EntityPredicate.createAdvancementEntityLootContext(player, partner);
		LootContext childContext = child != null
				? EntityPredicate.createAdvancementEntityLootContext(player, child)
				: null;
		trigger(player, conditions -> conditions.matches(parentContext, partnerContext, childContext));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LootContextPredicate> parent,
			Optional<LootContextPredicate> partner,
			Optional<LootContextPredicate> child
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("parent")
								.forGetter(Conditions::parent),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("partner")
								.forGetter(Conditions::partner),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("child")
								.forGetter(Conditions::child)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> any() {
			return Criteria.BRED_ANIMALS.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					Optional.empty(),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> create(EntityPredicate.Builder child) {
			return Criteria.BRED_ANIMALS.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(child))
			));
		}

		public static AdvancementCriterion<Conditions> create(
				Optional<EntityPredicate> parent,
				Optional<EntityPredicate> partner,
				Optional<EntityPredicate> child
		) {
			return Criteria.BRED_ANIMALS.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(parent),
					EntityPredicate.contextPredicateFromEntityPredicate(partner),
					EntityPredicate.contextPredicateFromEntityPredicate(child)
			));
		}

		/**
		 * Проверяет совпадение. Родители взаимозаменяемы: проверяются оба варианта
		 * (parent=A, partner=B) и (parent=B, partner=A).
		 */
		public boolean matches(
				LootContext parentContext,
				LootContext partnerContext,
				@Nullable LootContext childContext
		) {
			if (child.isPresent() && (childContext == null || !child.get().test(childContext))) {
				return false;
			}

			return parentMatches(parent, parentContext) && parentMatches(partner, partnerContext)
					|| parentMatches(parent, partnerContext) && parentMatches(partner, parentContext);
		}

		private static boolean parentMatches(Optional<LootContextPredicate> predicate, LootContext context) {
			return predicate.isEmpty() || predicate.get().test(context);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(parent, "parent");
			validator.validateEntityPredicate(partner, "partner");
			validator.validateEntityPredicate(child, "child");
		}
	}
}
