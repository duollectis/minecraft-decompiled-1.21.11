package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок начинает ехать верхом на сущности.
 */
public class StartedRidingCriterion extends AbstractCriterion<StartedRidingCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player) {
		trigger(player, conditions -> true);
	}

	public record Conditions(Optional<LootContextPredicate> player) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance
						.group(EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player))
						.apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(EntityPredicate.Builder player) {
			return Criteria.STARTED_RIDING.create(new Conditions(
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(player))
			));
		}
	}
}
