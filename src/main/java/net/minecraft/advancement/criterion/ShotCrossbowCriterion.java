package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий выполняется, когда игрок производит выстрел из арбалета.
 */
public class ShotCrossbowCriterion extends AbstractCriterion<ShotCrossbowCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, ItemStack stack) {
		trigger(player, conditions -> conditions.matches(stack));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<ItemPredicate> item
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						ItemPredicate.CODEC
								.optionalFieldOf("item")
								.forGetter(Conditions::item)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(Optional<ItemPredicate> item) {
			return Criteria.SHOT_CROSSBOW.create(new Conditions(Optional.empty(), item));
		}

		public static AdvancementCriterion<Conditions> create(
				RegistryEntryLookup<Item> itemRegistry,
				ItemConvertible item
		) {
			return Criteria.SHOT_CROSSBOW.create(new Conditions(
					Optional.empty(),
					Optional.of(ItemPredicate.Builder.create().items(itemRegistry, item).build())
			));
		}

		public boolean matches(ItemStack stack) {
			return item.isEmpty() || item.get().test(stack);
		}
	}
}
