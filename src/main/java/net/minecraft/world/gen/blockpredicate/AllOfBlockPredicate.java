package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.List;

/**
 * {@code AllOfBlockPredicate}.
 */
class AllOfBlockPredicate extends CombinedBlockPredicate {

	public static final MapCodec<AllOfBlockPredicate> CODEC = buildCodec(AllOfBlockPredicate::new);

	public AllOfBlockPredicate(List<BlockPredicate> list) {
		super(list);
	}

	/**
	 * Test.
	 *
	 * @param structureWorldAccess structure world access
	 * @param blockPos block pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean test(StructureWorldAccess structureWorldAccess, BlockPos blockPos) {
		for (BlockPredicate blockPredicate : this.predicates) {
			if (!blockPredicate.test(structureWorldAccess, blockPos)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.ALL_OF;
	}
}
