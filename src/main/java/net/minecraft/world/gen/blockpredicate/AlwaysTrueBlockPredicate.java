package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

/**
 * Предикат-синглтон, всегда возвращающий {@code true}.
 */
class AlwaysTrueBlockPredicate implements BlockPredicate {

	public static final AlwaysTrueBlockPredicate instance = new AlwaysTrueBlockPredicate();
	public static final MapCodec<AlwaysTrueBlockPredicate> CODEC = MapCodec.unit(() -> instance);

	private AlwaysTrueBlockPredicate() {
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		return true;
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.TRUE;
	}
}
