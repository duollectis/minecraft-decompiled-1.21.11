package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.List;

/**
 * Предикат, возвращающий {@code true} только если все дочерние предикаты истинны (логическое И).
 */
class AllOfBlockPredicate extends CombinedBlockPredicate {

	public static final MapCodec<AllOfBlockPredicate> CODEC = buildCodec(AllOfBlockPredicate::new);

	public AllOfBlockPredicate(List<BlockPredicate> predicates) {
		super(predicates);
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		for (BlockPredicate predicate : predicates) {
			if (!predicate.test(world, pos)) {
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
