package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

/**
 * Предикат, проверяющий что позиция со смещением находится в пределах высот мира.
 */
public class InsideWorldBoundsBlockPredicate implements BlockPredicate {

	public static final MapCodec<InsideWorldBoundsBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(Vec3i.createOffsetCodec(16).optionalFieldOf("offset", BlockPos.ORIGIN).forGetter(p -> p.offset))
			.apply(instance, InsideWorldBoundsBlockPredicate::new)
	);

	private final Vec3i offset;

	public InsideWorldBoundsBlockPredicate(Vec3i offset) {
		this.offset = offset;
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		return !world.isOutOfHeightLimit(pos.add(offset));
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.INSIDE_WORLD_BOUNDS;
	}
}
