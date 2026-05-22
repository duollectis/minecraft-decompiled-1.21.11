package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Критерий выполняется, когда брошенный игроком предмет подбирает сущность или сам игрок.
 * Используется для двух сценариев: подбор сущностью и подбор игроком.
 */
public class ThrownItemPickedUpByEntityCriterion extends AbstractCriterion<ThrownItemPickedUpByEntityCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, ItemStack stack, @Nullable Entity entity) {
		LootContext entityContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
		trigger(player, conditions -> conditions.test(player, stack, entityContext));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<ItemPredicate> item,
			Optional<LootContextPredicate> entity
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						ItemPredicate.CODEC
								.optionalFieldOf("item")
								.forGetter(Conditions::item),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("entity")
								.forGetter(Conditions::entity)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> createThrownItemPickedUpByEntity(
				LootContextPredicate player, Optional<ItemPredicate> item, Optional<LootContextPredicate> entity
		) {
			return Criteria.THROWN_ITEM_PICKED_UP_BY_ENTITY.create(new Conditions(
					Optional.of(player),
					item,
					entity
			));
		}

		public static AdvancementCriterion<Conditions> createThrownItemPickedUpByPlayer(
				Optional<LootContextPredicate> playerPredicate,
				Optional<ItemPredicate> item,
				Optional<LootContextPredicate> entity
		) {
			return Criteria.THROWN_ITEM_PICKED_UP_BY_PLAYER.create(new Conditions(
					playerPredicate,
					item,
					entity
			));
		}

		public boolean test(ServerPlayerEntity player, ItemStack stack, LootContext entityContext) {
			if (item.isPresent() && !item.get().test(stack)) {
				return false;
			}

			return entity.isEmpty() || entity.get().test(entityContext);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(entity, "entity");
		}
	}
}
