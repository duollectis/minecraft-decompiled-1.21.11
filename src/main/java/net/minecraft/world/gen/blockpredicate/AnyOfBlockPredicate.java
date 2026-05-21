package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.List;

/**
 * {@code AnyOfBlockPredicate}.
 */
class AnyOfBlockPredicate extends CombinedBlockPredicate {

	public static final MapCodec<AnyOfBlockPredicate> CODEC = buildCodec(AnyOfBlockPredicate::new);

	public AnyOfBlockPredicate(List<BlockPredicate> list) {
		super(list);
	}

	public boolean test(StructureWorldAccess structureWorldAccess, BlockPos blockPos) {
		for (BlockPredicate blockPredicate : this.predicates) {
			if (blockPredicate.test(structureWorldAccess, blockPos)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.ANY_OF;
	}
}
