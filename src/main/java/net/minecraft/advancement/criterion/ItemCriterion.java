package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.LocationCheckLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;

/**
 * Критерий: игрок использовал предмет на блоке или разместил блок.
 * Строит контекст лута с параметрами позиции, сущности, состояния блока и инструмента.
 */
public class ItemCriterion extends AbstractCriterion<ItemCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, BlockPos pos, ItemStack stack) {
		ServerWorld world = player.getEntityWorld();
		BlockState blockState = world.getBlockState(pos);
		LootWorldContext lootWorldContext = new LootWorldContext.Builder(world)
				.add(LootContextParameters.ORIGIN, pos.toCenterPos())
				.add(LootContextParameters.THIS_ENTITY, player)
				.add(LootContextParameters.BLOCK_STATE, blockState)
				.add(LootContextParameters.TOOL, stack)
				.build(LootContextTypes.ADVANCEMENT_LOCATION);
		LootContext lootContext = new LootContext.Builder(lootWorldContext).build(Optional.empty());

		trigger(player, conditions -> conditions.test(lootContext));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<LootContextPredicate> location
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						LootContextPredicate.CODEC
								.optionalFieldOf("location")
								.forGetter(Conditions::location)
				).apply(instance, Conditions::new)
		);

		public static AdvancementCriterion<Conditions> createPlacedBlock(Block block) {
			LootContextPredicate predicate = LootContextPredicate.create(
					BlockStatePropertyLootCondition.builder(block).build()
			);

			return Criteria.PLACED_BLOCK.create(new Conditions(Optional.empty(), Optional.of(predicate)));
		}

		public static AdvancementCriterion<Conditions> createPlacedBlock(LootCondition.Builder... locationConditions) {
			LootContextPredicate predicate = LootContextPredicate.create(
					Arrays.stream(locationConditions).map(LootCondition.Builder::build).toArray(LootCondition[]::new)
			);

			return Criteria.PLACED_BLOCK.create(new Conditions(Optional.empty(), Optional.of(predicate)));
		}

		public static <T extends Comparable<T>> AdvancementCriterion<Conditions> createPlacedWithState(
				Block block, Property<T> property, String value
		) {
			StatePredicate.Builder stateBuilder = StatePredicate.Builder.create().exactMatch(property, value);
			LootContextPredicate predicate = LootContextPredicate.create(
					BlockStatePropertyLootCondition.builder(block).properties(stateBuilder).build()
			);

			return Criteria.PLACED_BLOCK.create(new Conditions(Optional.empty(), Optional.of(predicate)));
		}

		public static AdvancementCriterion<Conditions> createPlacedWithState(
				Block block, Property<Boolean> property, boolean value
		) {
			return createPlacedWithState(block, property, String.valueOf(value));
		}

		public static AdvancementCriterion<Conditions> createPlacedWithState(
				Block block, Property<Integer> property, int value
		) {
			return createPlacedWithState(block, property, String.valueOf(value));
		}

		public static <T extends Comparable<T> & StringIdentifiable> AdvancementCriterion<Conditions> createPlacedWithState(
				Block block, Property<T> property, T value
		) {
			return createPlacedWithState(block, property, value.asString());
		}

		private static Conditions create(LocationPredicate.Builder location, ItemPredicate.Builder item) {
			LootContextPredicate predicate = LootContextPredicate.create(
					LocationCheckLootCondition.builder(location).build(),
					MatchToolLootCondition.builder(item).build()
			);

			return new Conditions(Optional.empty(), Optional.of(predicate));
		}

		public static AdvancementCriterion<Conditions> createItemUsedOnBlock(
				LocationPredicate.Builder location, ItemPredicate.Builder item
		) {
			return Criteria.ITEM_USED_ON_BLOCK.create(create(location, item));
		}

		public static AdvancementCriterion<Conditions> createAllayDropItemOnBlock(
				LocationPredicate.Builder location, ItemPredicate.Builder item
		) {
			return Criteria.ALLAY_DROP_ITEM_ON_BLOCK.create(create(location, item));
		}

		public boolean test(LootContext locationContext) {
			return location.isEmpty() || location.get().test(locationContext);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			location.ifPresent(loc -> validator.validate(loc, LootContextTypes.ADVANCEMENT_LOCATION, "location"));
		}
	}
}
