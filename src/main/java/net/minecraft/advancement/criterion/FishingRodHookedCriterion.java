package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Optional;

/**
 * Критерий: игрок использовал удочку и что-то поймал.
 * Проверяет удочку, зацепленную сущность и пойманные предметы.
 */
public class FishingRodHookedCriterion extends AbstractCriterion<FishingRodHookedCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(
			ServerPlayerEntity player,
			ItemStack rod,
			FishingBobberEntity bobber,
			Collection<ItemStack> fishingLoots
	) {
		Entity hookedTarget = bobber.getHookedEntity() != null ? bobber.getHookedEntity() : bobber;
		LootContext hookedContext = EntityPredicate.createAdvancementEntityLootContext(player, hookedTarget);

		trigger(player, conditions -> conditions.matches(rod, hookedContext, fishingLoots));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<ItemPredicate> rod,
			Optional<LootContextPredicate> entity,
			Optional<ItemPredicate> item
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						ItemPredicate.CODEC
								.optionalFieldOf("rod")
								.forGetter(Conditions::rod),
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("entity")
								.forGetter(Conditions::entity),
						ItemPredicate.CODEC
								.optionalFieldOf("item")
								.forGetter(Conditions::item)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(
				Optional<ItemPredicate> rod,
				Optional<EntityPredicate> hookedEntity,
				Optional<ItemPredicate> caughtItem
		) {
			return Criteria.FISHING_ROD_HOOKED.create(new Conditions(
					Optional.empty(),
					rod,
					EntityPredicate.contextPredicateFromEntityPredicate(hookedEntity),
					caughtItem
			));
		}

		/**
		 * Проверяет соответствие удочки, зацепленной сущности и пойманных предметов.
		 * Для предиката предмета проверяется как сущность-предмет в крюке, так и список улова.
		 */
		public boolean matches(ItemStack rodStack, LootContext hookedEntity, Collection<ItemStack> fishingLoots) {
			if (rod.isPresent() && !rod.get().test(rodStack)) {
				return false;
			}

			if (entity.isPresent() && !entity.get().test(hookedEntity)) {
				return false;
			}

			if (item.isPresent()) {
				boolean itemFound = false;
				Entity hookedRaw = hookedEntity.get(LootContextParameters.THIS_ENTITY);

				if (hookedRaw instanceof ItemEntity itemEntity && item.get().test(itemEntity.getStack())) {
					itemFound = true;
				}

				if (!itemFound) {
					for (ItemStack lootStack : fishingLoots) {
						if (item.get().test(lootStack)) {
							itemFound = true;
							break;
						}
					}
				}

				if (!itemFound) {
					return false;
				}
			}

			return true;
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			validator.validateEntityPredicate(entity, "entity");
		}
	}
}
