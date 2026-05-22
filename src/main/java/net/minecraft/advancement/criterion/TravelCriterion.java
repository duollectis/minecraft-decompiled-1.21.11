package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.predicate.entity.DistancePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок перемещается на определённое расстояние.
 * Используется для путешествий в Нижнем мире, падений с высоты и езды в лаве.
 */
public class TravelCriterion extends AbstractCriterion<TravelCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, Vec3d startPos) {
		Vec3d currentPos = player.getEntityPos();
		trigger(player, conditions -> conditions.matches(player.getEntityWorld(), startPos, currentPos));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LocationPredicate> startPosition,
			Optional<DistancePredicate> distance
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						LocationPredicate.CODEC
								.optionalFieldOf("start_position")
								.forGetter(Conditions::startPosition),
						DistancePredicate.CODEC
								.optionalFieldOf("distance")
								.forGetter(Conditions::distance)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> fallFromHeight(
				EntityPredicate.Builder entity, DistancePredicate distance, LocationPredicate.Builder startPos
		) {
			return Criteria.FALL_FROM_HEIGHT.create(new Conditions(
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(entity)),
					Optional.of(startPos.build()),
					Optional.of(distance)
			));
		}

		public static AdvancementCriterion<Conditions> rideEntityInLava(
				EntityPredicate.Builder entity,
				DistancePredicate distance
		) {
			return Criteria.RIDE_ENTITY_IN_LAVA.create(new Conditions(
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(entity)),
					Optional.empty(),
					Optional.of(distance)
			));
		}

		public static AdvancementCriterion<Conditions> netherTravel(DistancePredicate distance) {
			return Criteria.NETHER_TRAVEL.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					Optional.of(distance)
			));
		}

		public boolean matches(ServerWorld world, Vec3d pos, Vec3d endPos) {
			if (startPosition.isPresent() && !startPosition.get().test(world, pos.x, pos.y, pos.z)) {
				return false;
			}

			return distance.isEmpty() || distance.get().test(pos.x, pos.y, pos.z, endPos.x, endPos.y, endPos.z);
		}
	}
}
