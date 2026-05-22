package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.potion.Potion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий, срабатывающий при варке зелья игроком.
 */
public class BrewedPotionCriterion extends AbstractCriterion<BrewedPotionCriterion.Conditions> {

	@Override
	public Codec<BrewedPotionCriterion.Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, RegistryEntry<Potion> potion) {
		trigger(player, conditions -> conditions.matches(potion));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<RegistryEntry<Potion>> potion
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						Potion.CODEC.optionalFieldOf("potion").forGetter(Conditions::potion)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> any() {
			return Criteria.BREWED_POTION.create(new Conditions(Optional.empty(), Optional.empty()));
		}

		public boolean matches(RegistryEntry<Potion> potion) {
			return this.potion.isEmpty() || this.potion.get().equals(potion);
		}
	}
}
