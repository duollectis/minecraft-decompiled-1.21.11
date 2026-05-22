package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий, срабатывающий при зачаровании предмета игроком.
 */
public class EnchantedItemCriterion extends AbstractCriterion<EnchantedItemCriterion.Conditions> {

	@Override
	public Codec<EnchantedItemCriterion.Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, ItemStack stack, int levels) {
		trigger(player, conditions -> conditions.matches(stack, levels));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<ItemPredicate> item,
			NumberRange.IntRange levels
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						ItemPredicate.CODEC.optionalFieldOf("item").forGetter(Conditions::item),
						NumberRange.IntRange.CODEC
								.optionalFieldOf("levels", NumberRange.IntRange.ANY)
								.forGetter(Conditions::levels)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> any() {
			return Criteria.ENCHANTED_ITEM.create(new Conditions(
					Optional.empty(), Optional.empty(), NumberRange.IntRange.ANY
			));
		}

		public boolean matches(ItemStack stack, int levels) {
			if (item.isPresent() && !item.get().test(stack)) {
				return false;
			}

			return this.levels.test(levels);
		}
	}
}
