package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

/**
 * Предикат-инвертор: возвращает {@code true} если вложенный предикат возвращает {@code false}.
 */
class NotBlockPredicate implements BlockPredicate {

	public static final MapCodec<NotBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(BlockPredicate.BASE_CODEC.fieldOf("predicate").forGetter(p -> p.predicate))
			.apply(instance, NotBlockPredicate::new)
	);

	private final BlockPredicate predicate;

	public NotBlockPredicate(BlockPredicate predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		return !predicate.test(world, pos);
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.NOT;
	}
}
