package net.minecraft.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * Критерий, срабатывающий при вхождении игрока в блок.
 */
public class EnterBlockCriterion extends AbstractCriterion<EnterBlockCriterion.Conditions> {

	@Override
	public Codec<EnterBlockCriterion.Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, BlockState state) {
		trigger(player, conditions -> conditions.matches(state));
	}

	public record Conditions(
			Optional<LootContextPredicate> player,
			Optional<RegistryEntry<Block>> block,
			Optional<StatePredicate> state
	) implements AbstractCriterion.Conditions {

		public static final Codec<Conditions> CODEC = RecordCodecBuilder.<Conditions>create(
				instance -> instance.group(
						EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC
								.optionalFieldOf("player")
								.forGetter(Conditions::player),
						Registries.BLOCK.getEntryCodec()
								.optionalFieldOf("block")
								.forGetter(c -> c.block()),
						StatePredicate.CODEC
								.optionalFieldOf("state")
								.forGetter(Conditions::state)
				).apply(instance, Conditions::new)
		).validate(Conditions::validate);

		private static DataResult<Conditions> validate(Conditions conditions) {
			return conditions.block
					.<DataResult<Conditions>>flatMap(
							block -> conditions.state
									.<String>flatMap(state -> state.findMissing(((Block) block.value()).getStateManager()))
									.map(property -> DataResult.error(() -> "Block" + block + " has no property " + property))
					)
					.orElseGet(() -> DataResult.success(conditions));
		}

		public static AdvancementCriterion<Conditions> block(Block block) {
			return Criteria.ENTER_BLOCK.create(new Conditions(
					Optional.empty(), Optional.of(block.getRegistryEntry()), Optional.empty()
			));
		}

		public boolean matches(BlockState blockState) {
			if (block.isPresent() && !blockState.isOf(block.get())) {
				return false;
			}

			return state.isEmpty() || state.get().test(blockState);
		}
	}
}
