package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.DamageSourcePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий: сущность убита (игроком или игрок убит сущностью).
 * Используется для нескольких событий: убийство игроком, убийство игрока, убийство у катализатора.
 */
public class OnKilledCriterion extends AbstractCriterion<OnKilledCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, Entity entity, DamageSource killingDamage) {
		LootContext entityContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
		trigger(player, conditions -> conditions.test(player, entityContext, killingDamage));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LootContextPredicate> entity,
			Optional<DamageSourcePredicate> killingBlow
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("entity")
								.forGetter(Conditions::entity),
						DamageSourcePredicate.CODEC
								.optionalFieldOf("killing_blow")
								.forGetter(Conditions::killingBlow)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> createPlayerKilledEntity(Optional<EntityPredicate> entity) {
			return Criteria.PLAYER_KILLED_ENTITY.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(entity),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerKilledEntity(EntityPredicate.Builder killedEntityPredicateBuilder) {
			return Criteria.PLAYER_KILLED_ENTITY.create(new Conditions(
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(killedEntityPredicateBuilder)),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerKilledEntity() {
			return Criteria.PLAYER_KILLED_ENTITY.create(new Conditions(
					Optional.empty(), Optional.empty(), Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerKilledEntity(
				Optional<EntityPredicate> entity, Optional<DamageSourcePredicate> killingBlow
		) {
			return Criteria.PLAYER_KILLED_ENTITY.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(entity),
					killingBlow
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerKilledEntity(
				EntityPredicate.Builder killedEntityPredicateBuilder, Optional<DamageSourcePredicate> killingBlow
		) {
			return Criteria.PLAYER_KILLED_ENTITY.create(new Conditions(
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(killedEntityPredicateBuilder)),
					killingBlow
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerKilledEntity(
				Optional<EntityPredicate> entity, DamageSourcePredicate.Builder damageSourcePredicateBuilder
		) {
			return Criteria.PLAYER_KILLED_ENTITY.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(entity),
					Optional.of(damageSourcePredicateBuilder.build())
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerKilledEntity(
				EntityPredicate.Builder killedEntityPredicateBuilder, DamageSourcePredicate.Builder killingBlowBuilder
		) {
			return Criteria.PLAYER_KILLED_ENTITY.create(new Conditions(
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(killedEntityPredicateBuilder)),
					Optional.of(killingBlowBuilder.build())
			));
		}

		public static AdvancementCriterion<Conditions> createKillMobNearSculkCatalyst() {
			return Criteria.KILL_MOB_NEAR_SCULK_CATALYST.create(new Conditions(
					Optional.empty(), Optional.empty(), Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createEntityKilledPlayer(Optional<EntityPredicate> entity) {
			return Criteria.ENTITY_KILLED_PLAYER.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(entity),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createEntityKilledPlayer(EntityPredicate.Builder killerEntityPredicateBuilder) {
			return Criteria.ENTITY_KILLED_PLAYER.create(new Conditions(
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(killerEntityPredicateBuilder)),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createEntityKilledPlayer() {
			return Criteria.ENTITY_KILLED_PLAYER.create(new Conditions(
					Optional.empty(), Optional.empty(), Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> createEntityKilledPlayer(
				Optional<EntityPredicate> entity, Optional<DamageSourcePredicate> killingBlow
		) {
			return Criteria.ENTITY_KILLED_PLAYER.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(entity),
					killingBlow
			));
		}

		public static AdvancementCriterion<Conditions> createEntityKilledPlayer(
				EntityPredicate.Builder killerEntityPredicateBuilder, Optional<DamageSourcePredicate> killingBlow
		) {
			return Criteria.ENTITY_KILLED_PLAYER.create(new Conditions(
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(killerEntityPredicateBuilder)),
					killingBlow
			));
		}

		public static AdvancementCriterion<Conditions> createEntityKilledPlayer(
				Optional<EntityPredicate> entity, DamageSourcePredicate.Builder damageSourcePredicateBuilder
		) {
			return Criteria.ENTITY_KILLED_PLAYER.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicate(entity),
					Optional.of(damageSourcePredicateBuilder.build())
			));
		}

		public static AdvancementCriterion<Conditions> createEntityKilledPlayer(
				EntityPredicate.Builder killerEntityPredicateBuilder,
				DamageSourcePredicate.Builder damageSourcePredicateBuilder
		) {
			return Criteria.ENTITY_KILLED_PLAYER.create(new Conditions(
					Optional.empty(),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(killerEntityPredicateBuilder)),
					Optional.of(damageSourcePredicateBuilder.build())
			));
		}

		public boolean test(ServerPlayerEntity player, LootContext entityContext, DamageSource killingBlow) {
			if (this.killingBlow.isPresent() && !this.killingBlow.get().test(player, killingBlow)) {
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
