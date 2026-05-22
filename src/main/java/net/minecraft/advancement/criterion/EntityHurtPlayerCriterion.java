package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.predicate.DamagePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий: сущность нанесла урон игроку.
 * Проверяет параметры полученного урона через {@link DamagePredicate}.
 */
public class EntityHurtPlayerCriterion extends AbstractCriterion<EntityHurtPlayerCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, DamageSource source, float dealt, float taken, boolean blocked) {
		trigger(player, conditions -> conditions.matches(player, source, dealt, taken, blocked));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<DamagePredicate> damage
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						DamagePredicate.CODEC
								.optionalFieldOf("damage")
								.forGetter(Conditions::damage)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create() {
			return Criteria.ENTITY_HURT_PLAYER.create(new Conditions(Optional.empty(), Optional.empty()));
		}

		public static AdvancementCriterion<Conditions> create(DamagePredicate predicate) {
			return Criteria.ENTITY_HURT_PLAYER.create(new Conditions(Optional.empty(), Optional.of(predicate)));
		}

		public static AdvancementCriterion<Conditions> create(DamagePredicate.Builder damageBuilder) {
			return Criteria.ENTITY_HURT_PLAYER.create(new Conditions(Optional.empty(), Optional.of(damageBuilder.build())));
		}

		public boolean matches(
				ServerPlayerEntity player,
				DamageSource damageSource,
				float dealt,
				float taken,
				boolean blocked
		) {
			return damage.isEmpty() || damage.get().test(player, damageSource, dealt, taken, blocked);
		}
	}
}
