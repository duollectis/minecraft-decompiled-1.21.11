package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок использует Глаз Эндера.
 * Проверяет расстояние до крепости по квадрату дистанции (без извлечения корня для производительности).
 */
public class UsedEnderEyeCriterion extends AbstractCriterion<UsedEnderEyeCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, BlockPos strongholdPos) {
		double deltaX = player.getX() - strongholdPos.getX();
		double deltaZ = player.getZ() - strongholdPos.getZ();
		double distanceSq = deltaX * deltaX + deltaZ * deltaZ;
		trigger(player, conditions -> conditions.matches(distanceSq));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			NumberRange.DoubleRange distance
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						NumberRange.DoubleRange.CODEC
								.optionalFieldOf("distance", NumberRange.DoubleRange.ANY)
								.forGetter(Conditions::distance)
				).apply(instance, Conditions::new)
		);

		public boolean matches(double distanceSq) {
			return distance.testSqrt(distanceSq);
		}
	}
}
