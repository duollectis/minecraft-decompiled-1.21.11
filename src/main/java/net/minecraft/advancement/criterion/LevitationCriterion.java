package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.DistancePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Критерий: игрок левитировал на определённое расстояние за определённое время.
 */
public class LevitationCriterion extends AbstractCriterion<LevitationCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, Vec3d startPos, int duration) {
		trigger(player, conditions -> conditions.matches(player, startPos, duration));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<DistancePredicate> distance,
			NumberRange.IntRange duration
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						DistancePredicate.CODEC
								.optionalFieldOf("distance")
								.forGetter(Conditions::distance),
						NumberRange.IntRange.CODEC
								.optionalFieldOf("duration", NumberRange.IntRange.ANY)
								.forGetter(Conditions::duration)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(DistancePredicate distance) {
			return Criteria.LEVITATION.create(new Conditions(
					Optional.empty(),
					Optional.of(distance),
					NumberRange.IntRange.ANY
			));
		}

		public boolean matches(ServerPlayerEntity player, Vec3d startPos, int duration) {
			if (this.distance.isPresent()
					&& !this.distance.get().test(startPos.x, startPos.y, startPos.z, player.getX(), player.getY(), player.getZ())
			) {
				return false;
			}

			return this.duration.test(duration);
		}
	}
}
