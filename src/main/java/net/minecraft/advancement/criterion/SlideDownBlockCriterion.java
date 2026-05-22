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
 * Критерий выполняется, когда игрок скользит вниз по блоку (например, по мёду).
 * Поддерживает проверку конкретного типа блока и его состояния.
 */
public class SlideDownBlockCriterion extends AbstractCriterion<SlideDownBlockCriterion.Conditions> {

	@Override
	public Codec<Conditions> getConditionsCodec() {
		return Conditions.CODEC;
	}

	public void trigger(ServerPlayerEntity player, BlockState state) {
		trigger(player, conditions -> conditions.test(state));
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
								.forGetter(Conditions::block),
						StatePredicate.CODEC
								.optionalFieldOf("state")
								.forGetter(Conditions::state)
				).apply(instance, Conditions::new)
		).validate(Conditions::validate);

		/**
		 * Проверяет, что все свойства из предиката состояния существуют у указанного блока.
		 * Предотвращает создание условий с несуществующими свойствами блока.
		 */
		private static DataResult<Conditions> validate(Conditions conditions) {
			return conditions.block
					.<DataResult<Conditions>>flatMap(
							blockEntry -> conditions.state
									.<String>flatMap(statePredicate -> statePredicate.findMissing(blockEntry.value().getStateManager()))
									.map(property -> DataResult.error(() -> "Block " + blockEntry + " has no property " + property))
					)
					.orElseGet(() -> DataResult.success(conditions));
		}

		public static AdvancementCriterion<Conditions> create(Block block) {
			return Criteria.SLIDE_DOWN_BLOCK.create(new Conditions(
					Optional.empty(),
					Optional.of(block.getRegistryEntry()),
					Optional.empty()
			));
		}

		public boolean test(BlockState state) {
			if (block.isPresent() && !state.isOf(block.get())) {
				return false;
			}

			return this.state.isEmpty() || this.state.get().test(state);
		}
	}
}
