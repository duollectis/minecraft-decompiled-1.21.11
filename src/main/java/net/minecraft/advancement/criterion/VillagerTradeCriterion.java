package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий, срабатывающий при совершении торговли с жителем деревни.
 */
public class VillagerTradeCriterion extends AbstractCriterion<VillagerTradeCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, MerchantEntity merchant, ItemStack stack) {
		LootContext villagerContext = EntityPredicate.createAdvancementEntityLootContext(player, merchant);
		trigger(player, conditions -> conditions.matches(villagerContext, stack));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LootContextPredicate> villager,
			Optional<ItemPredicate> item
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("villager")
								.forGetter(Conditions::villager),
						ItemPredicate.CODEC
								.optionalFieldOf("item")
								.forGetter(Conditions::item)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> any() {
			return Criteria.VILLAGER_TRADE.create(new Conditions(
					Optional.empty(),
					Optional.empty(),
					Optional.empty()
			));
		}

		public static AdvancementCriterion<Conditions> create(EntityPredicate.Builder playerPredicate) {
			return Criteria.VILLAGER_TRADE.create(new Conditions(
					Optional.of(EntityPredicate.contextPredicateFromEntityPredicate(playerPredicate)),
					Optional.empty(),
					Optional.empty()
			));
		}

		public boolean matches(LootContext villagerContext, ItemStack stack) {
			if (villager.isPresent() && !villager.get().test(villagerContext)) {
				return false;
			}

			return item.isEmpty() || item.get().test(stack);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(villager, "villager");
		}
	}
}
