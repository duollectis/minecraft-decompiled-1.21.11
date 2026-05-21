package net.minecraft.loot.condition;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.context.ContextParameter;

import java.util.Optional;
import java.util.Set;

/**
 * {@code BlockStatePropertyLootCondition}.
 */
public record BlockStatePropertyLootCondition(
		RegistryEntry<Block> block,
		Optional<StatePredicate> properties
) implements LootCondition {

	@SuppressWarnings("unchecked")
	public static final MapCodec<BlockStatePropertyLootCondition>
			CODEC =
			(MapCodec<BlockStatePropertyLootCondition>) RecordCodecBuilder.<BlockStatePropertyLootCondition>mapCodec(
					                                                              instance -> instance.group(
							                                                                                  Registries.BLOCK
									                                                                                  .getEntryCodec()
									                                                                                  .fieldOf("block")
									                                                                                  .forGetter(BlockStatePropertyLootCondition::block),
							                                                                                  StatePredicate.CODEC
									                                                                                  .optionalFieldOf("properties")
									                                                                                  .forGetter(BlockStatePropertyLootCondition::properties)
					                                                                                  )
					                                                                                  .apply(instance, BlockStatePropertyLootCondition::new)
			                                                              )
			                                                              .validate(condition -> BlockStatePropertyLootCondition.validateHasProperties(
					                                                              (BlockStatePropertyLootCondition) condition));

	@SuppressWarnings("unchecked")
	private static DataResult<BlockStatePropertyLootCondition> validateHasProperties(BlockStatePropertyLootCondition condition) {
		return condition.properties()
		                .flatMap(predicate -> predicate.findMissing(condition.block().value().getStateManager()))
		                .map(property -> DataResult.<BlockStatePropertyLootCondition>error(() -> "Block "
				                + condition.block() + " has no property" + property))
		                .orElse(DataResult.success(condition));
	}

	@Override
	public LootConditionType getType() {
		return LootConditionTypes.BLOCK_STATE_PROPERTY;
	}

	@Override
	public Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of(LootContextParameters.BLOCK_STATE);
	}

	/**
	 * Test.
	 *
	 * @param lootContext loot context
	 *
	 * @return boolean — результат операции
	 */
	public boolean test(LootContext lootContext) {
		BlockState blockState = lootContext.get(LootContextParameters.BLOCK_STATE);
		return blockState != null && blockState.isOf(this.block) && (this.properties.isEmpty() || this.properties
				.get()
				.test(blockState)
		);
	}

	public static BlockStatePropertyLootCondition.Builder builder(Block block) {
		return new BlockStatePropertyLootCondition.Builder(block);
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder implements LootCondition.Builder {

		private final RegistryEntry<Block> block;
		private Optional<StatePredicate> propertyValues = Optional.empty();

		public Builder(Block block) {
			this.block = block.getRegistryEntry();
		}

		public BlockStatePropertyLootCondition.Builder properties(StatePredicate.Builder builder) {
			this.propertyValues = builder.build();
			return this;
		}

		@Override
		public LootCondition build() {
			return new BlockStatePropertyLootCondition(this.block, this.propertyValues);
		}
	}
}
