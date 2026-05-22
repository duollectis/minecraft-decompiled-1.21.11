package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Критерий, срабатывающий при поражении молнией через трезубец нескольких существ.
 */
public class ChanneledLightningCriterion extends AbstractCriterion<ChanneledLightningCriterion.Conditions> {

	@Override
	public Codec<ChanneledLightningCriterion.Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, Collection<? extends Entity> victims) {
		List<LootContext> victimContexts = victims
				.stream()
				.map(entity -> EntityPredicate.createAdvancementEntityLootContext(player, entity))
				.toList();
		trigger(player, conditions -> conditions.matches(victimContexts));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			List<LootContextPredicate> victims
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.listOf()
								.optionalFieldOf("victims", List.of())
								.forGetter(Conditions::victims)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(EntityPredicate.Builder... victims) {
			return Criteria.CHANNELED_LIGHTNING.create(new Conditions(
					Optional.empty(),
					EntityPredicate.contextPredicateFromEntityPredicates(victims)
			));
		}

		/**
		 * Проверяет, что каждый предикат жертвы соответствует хотя бы одному существу из списка.
		 * Реализует логику «все условия должны быть выполнены».
		 */
		public boolean matches(Collection<? extends LootContext> victims) {
			for (LootContextPredicate predicate : this.victims) {
				boolean matched = false;

				for (LootContext victimContext : victims) {
					if (predicate.test(victimContext)) {
						matched = true;
						break;
					}
				}

				if (!matched) {
					return false;
				}
			}

			return true;
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicates(victims, "victims");
		}
	}
}
