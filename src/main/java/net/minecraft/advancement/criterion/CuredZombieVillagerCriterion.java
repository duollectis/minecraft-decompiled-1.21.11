package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий, срабатывающий при исцелении зомби-жителя игроком.
 */
public class CuredZombieVillagerCriterion extends AbstractCriterion<CuredZombieVillagerCriterion.Conditions> {

	@Override
	public Codec<CuredZombieVillagerCriterion.Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, ZombieEntity zombie, VillagerEntity villager) {
		LootContext zombieContext = EntityPredicate.createAdvancementEntityLootContext(player, zombie);
		LootContext villagerContext = EntityPredicate.createAdvancementEntityLootContext(player, villager);
		trigger(player, conditions -> conditions.matches(zombieContext, villagerContext));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LootContextPredicate> zombie,
			Optional<LootContextPredicate> villager
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("zombie")
								.forGetter(Conditions::zombie),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("villager")
								.forGetter(Conditions::villager)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> any() {
			return Criteria.CURED_ZOMBIE_VILLAGER.create(new Conditions(
					Optional.empty(), Optional.empty(), Optional.empty()
			));
		}

		public boolean matches(LootContext zombieContext, LootContext villagerContext) {
			if (zombie.isPresent() && !zombie.get().test(zombieContext)) {
				return false;
			}

			return villager.isEmpty() || villager.get().test(villagerContext);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(zombie, "zombie");
			validator.validateEntityPredicate(villager, "villager");
		}
	}
}
