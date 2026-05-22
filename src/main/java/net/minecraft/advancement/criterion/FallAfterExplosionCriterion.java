package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Критерий: игрок упал после взрыва.
 * Проверяет начальную позицию, дистанцию падения и источник взрыва.
 */
public class FallAfterExplosionCriterion extends AbstractCriterion<FallAfterExplosionCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, Vec3d startPosition, @Nullable Entity cause) {
		Vec3d endPos = player.getEntityPos();
		LootContext causeContext = cause == null
				? null
				: EntityPredicate.createAdvancementEntityLootContext(player, cause);

		trigger(player, conditions -> conditions.matches(player.getEntityWorld(), startPosition, endPos, causeContext));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LocationPredicate> startPosition,
			Optional<DistancePredicate> distance,
			Optional<LootContextPredicate> cause
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
								.forGetter(Conditions::distance),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("cause")
								.forGetter(Conditions::cause)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(
				DistancePredicate distance,
				EntityPredicate.Builder cause
		) {
			return Criteria.FALL_AFTER_EXPLOSION.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					Optional.of(distance),
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(cause))
			));
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(cause(), "cause");
		}

		public boolean matches(ServerWorld world, Vec3d startPos, Vec3d endPos, @Nullable LootContext causeContext) {
			if (startPosition.isPresent() && !startPosition.get().test(world, startPos.x, startPos.y, startPos.z)) {
				return false;
			}

			if (distance.isPresent() && !distance.get().test(
					startPos.x, startPos.y, startPos.z,
					endPos.x, endPos.y, endPos.z
			)) {
				return false;
			}

			return cause.isEmpty() || (causeContext != null && cause.get().test(causeContext));
		}
	}
}
