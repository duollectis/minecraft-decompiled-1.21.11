package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок поражает трезубцем заданное количество мобов.
 */
public class SpearMobsCriterion extends AbstractCriterion<SpearMobsCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, int count) {
		trigger(player, conditions -> conditions.test(count));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<Integer> count
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						Codecs.POSITIVE_INT
								.optionalFieldOf("count")
								.forGetter(Conditions::count)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(int count) {
			return Criteria.SPEAR_MOBS.create(new Conditions(Optional.empty(), Optional.of(count)));
		}

		public boolean test(int count) {
			return this.count.isEmpty() || count >= this.count.get();
		}
	}
}
