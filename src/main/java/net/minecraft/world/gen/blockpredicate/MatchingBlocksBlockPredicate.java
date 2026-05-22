package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.Vec3i;

/**
 * Предикат, проверяющий, принадлежит ли блок по смещённой позиции одному из заданных блоков.
 */
class MatchingBlocksBlockPredicate extends OffsetPredicate {

	public static final MapCodec<MatchingBlocksBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> registerOffsetField(instance)
			.and(
				RegistryCodecs
					.entryList(RegistryKeys.BLOCK)
					.fieldOf("blocks")
					.forGetter(predicate -> predicate.blocks)
			)
			.apply(instance, MatchingBlocksBlockPredicate::new)
	);

	private final RegistryEntryList<Block> blocks;

	public MatchingBlocksBlockPredicate(Vec3i offset, RegistryEntryList<Block> blocks) {
		super(offset);
		this.blocks = blocks;
	}

	@Override
	protected boolean test(BlockState state) {
		return state.isIn(blocks);
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.MATCHING_BLOCKS;
	}
}
