package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий, срабатывающий при постройке маяка игроком.
 */
public class ConstructBeaconCriterion extends AbstractCriterion<ConstructBeaconCriterion.Conditions> {

	@Override
	public Codec<ConstructBeaconCriterion.Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, int level) {
		trigger(player, conditions -> conditions.matches(level));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			NumberRange.IntRange level
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						NumberRange.IntRange.CODEC
								.optionalFieldOf("level", NumberRange.IntRange.ANY)
								.forGetter(Conditions::level)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create() {
			return Criteria.CONSTRUCT_BEACON.create(new Conditions(Optional.empty(), NumberRange.IntRange.ANY));
		}

		public static AdvancementCriterion<Conditions> level(NumberRange.IntRange level) {
			return Criteria.CONSTRUCT_BEACON.create(new Conditions(Optional.empty(), level));
		}

		public boolean matches(int level) {
			return this.level.test(level);
		}
	}
}
