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

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок взаимодействует с сущностью (правый клик).
 * Используется также для стрижки снаряжения с сущностей.
 */
public class PlayerInteractedWithEntityCriterion extends AbstractCriterion<PlayerInteractedWithEntityCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, ItemStack stack, Entity entity) {
		LootContext entityContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
		trigger(player, conditions -> conditions.test(stack, entityContext));
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

		public static AdvancementCriterion<Conditions> create(
				Optional<LootContextPredicate> playerPredicate,
				ItemPredicate.Builder item,
				Optional<LootContextPredicate> entity
		) {
			return Criteria.PLAYER_INTERACTED_WITH_ENTITY.create(new Conditions(
					playerPredicate,
					Optional.of(item.build()),
					entity
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerShearedEquipment(
				Optional<LootContextPredicate> playerPredicate,
				ItemPredicate.Builder item,
				Optional<LootContextPredicate> entity
		) {
			return Criteria.PLAYER_SHEARED_EQUIPMENT.create(new Conditions(
					playerPredicate,
					Optional.of(item.build()),
					entity
			));
		}

		public static AdvancementCriterion<Conditions> createPlayerShearedEquipment(
				ItemPredicate.Builder item, Optional<LootContextPredicate> entity
		) {
			return Criteria.PLAYER_SHEARED_EQUIPMENT.create(new Conditions(
					Optional.empty(),
					Optional.of(item.build()),
					entity
			));
		}

		public static AdvancementCriterion<Conditions> create(
				ItemPredicate.Builder item, Optional<LootContextPredicate> entity
		) {
			return create(Optional.empty(), item, entity);
		}

		public boolean test(ItemStack stack, LootContext entityContext) {
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
