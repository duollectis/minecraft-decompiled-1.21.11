package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.DamagePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок наносит урон сущности.
 * Позволяет фильтровать по типу урона, параметрам урона и типу пострадавшей сущности.
 */
public class PlayerHurtEntityCriterion extends AbstractCriterion<PlayerHurtEntityCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(
			ServerPlayerEntity player,
			Entity entity,
			DamageSource damage,
			float dealt,
			float taken,
			boolean blocked
	) {
		LootContext entityContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
		trigger(player, conditions -> conditions.matches(player, entityContext, damage, dealt, taken, blocked));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<DamagePredicate> damage,
			Optional<LootContextPredicate> entity
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						DamagePredicate.CODEC
								.optionalFieldOf("damage")
								.forGetter(Conditions::damage),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("entity")
								.forGetter(Conditions::entity)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create() {
			return Criteria.PLAYER_HURT_ENTITY.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createDamage(Optional<DamagePredicate> damage) {
			return Criteria.PLAYER_HURT_ENTITY.create(new Conditions(
					Optional.empty(),
					damage,
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> create(DamagePredicate.Builder damage) {
			return Criteria.PLAYER_HURT_ENTITY.create(new Conditions(
					Optional.empty(),
					Optional.of(damage.build()),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createEntity(Optional<EntityPredicate> entity) {
			return Criteria.PLAYER_HURT_ENTITY.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(entity)
			));
		}

		public static AdvancementCriterion<Conditions> create(
				Optional<DamagePredicate> damage,
				Optional<EntityPredicate> entity
		) {
			return Criteria.PLAYER_HURT_ENTITY.create(new Conditions(
					Optional.empty(),
					damage,
					EntityPredicate.contextPredicateFromEntityPredicate(entity)
			));
		}

		public static AdvancementCriterion<Conditions> create(
				DamagePredicate.Builder damage,
				Optional<EntityPredicate> entity
		) {
			return Criteria.PLAYER_HURT_ENTITY.create(new Conditions(
					Optional.empty(),
					Optional.of(damage.build()),
					EntityPredicate.contextPredicateFromEntityPredicate(entity)
			));
		}

		public boolean matches(
				ServerPlayerEntity player,
				LootContext entityContext,
				DamageSource damageSource,
				float dealt,
				float taken,
				boolean blocked
		) {
			if (damage.isPresent() && !damage.get().test(player, damageSource, dealt, taken, blocked)) {
				return false;
			}

			return entity.isEmpty() || entity.get().test(entityContext);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(entity, "entity");
		}
	}
}
