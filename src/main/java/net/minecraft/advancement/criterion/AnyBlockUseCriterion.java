package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.entity.LootContextPredicateValidator;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Критерий, срабатывающий при использовании игроком любого блока.
 * Проверяет местоположение и инструмент взаимодействия.
 */
public class AnyBlockUseCriterion extends AbstractCriterion<AnyBlockUseCriterion.Conditions> {

	@Override
	public Codec<AnyBlockUseCriterion.Conditions> getConditionsCodec() {
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

		public boolean test(LootContext locationContext) {
			return location.isEmpty() || location.get().test(locationContext);
		}

		@Override
		public void validate(LootContextPredicateValidator validator) {
			AbstractCriterion.Conditions.super.validate(validator);
			location.ifPresent(loc -> validator.validate(
					loc,
					LootContextTypes.ADVANCEMENT_LOCATION,
					"location"
			));
		}
	}
}
