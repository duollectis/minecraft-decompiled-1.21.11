package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.List;

/**
 * Предикат, возвращающий {@code true} если хотя бы один дочерний предикат истинен (логическое ИЛИ).
 */
class AnyOfBlockPredicate extends CombinedBlockPredicate {

	public static final MapCodec<AnyOfBlockPredicate> CODEC = buildCodec(AnyOfBlockPredicate::new);

	public AnyOfBlockPredicate(List<BlockPredicate> predicates) {
		super(predicates);
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		for (BlockPredicate predicate : predicates) {
			if (predicate.test(world, pos)) {
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
