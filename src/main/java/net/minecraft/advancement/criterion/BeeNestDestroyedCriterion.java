package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий: игрок уничтожил улей или гнездо пчёл.
 */
public class BeeNestDestroyedCriterion extends AbstractCriterion<BeeNestDestroyedCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, BlockState state, ItemStack stack, int beeCount) {
		trigger(player, conditions -> conditions.test(state, stack, beeCount));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<RegistryEntry<Block>> block,
			Optional<ItemPredicate> item,
			NumberRange.IntRange beesInside
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						Registries.BLOCK
								.getEntryCodec()
								.optionalFieldOf("block")
								.forGetter(Conditions::block),
						ItemPredicate.CODEC
								.optionalFieldOf("item")
								.forGetter(Conditions::item),
						NumberRange.IntRange.CODEC
								.optionalFieldOf("num_bees_inside", NumberRange.IntRange.ANY)
								.forGetter(Conditions::beesInside)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> create(
				Block block,
				ItemPredicate.Builder itemPredicateBuilder,
				NumberRange.IntRange beeCountRange
		) {
			return Criteria.BEE_NEST_DESTROYED.create(new Conditions(
					Optional.empty(),
					Optional.of(block.getRegistryEntry()),
					Optional.of(itemPredicateBuilder.build()),
					beeCountRange
			));
		}

		public boolean test(BlockState state, ItemStack stack, int count) {
			if (block.isPresent() && !state.isOf(block.get())) {
				return false;
			}

			if (item.isPresent() && !item.get().test(stack)) {
				return false;
			}

			return beesInside.test(count);
		}
	}
}
