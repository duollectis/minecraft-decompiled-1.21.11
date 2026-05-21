package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

/**
 * {@code AlwaysTrueBlockPredicate}.
 */
class AlwaysTrueBlockPredicate implements BlockPredicate {

	public static AlwaysTrueBlockPredicate instance = new AlwaysTrueBlockPredicate();
	public static final MapCodec<AlwaysTrueBlockPredicate> CODEC = MapCodec.unit(() -> instance);

	private AlwaysTrueBlockPredicate() {
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
		return true;
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.TRUE;
	}
}
