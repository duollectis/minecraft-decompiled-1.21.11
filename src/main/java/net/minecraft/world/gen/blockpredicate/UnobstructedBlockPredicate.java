package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.StructureWorldAccess;

/**
 * Предикат, проверяющий, что в позиции с учётом смещения нет пересекающихся сущностей
 * (т.е. пространство не занято). Используется при размещении структур и фич.
 */
record UnobstructedBlockPredicate(Vec3i offset) implements BlockPredicate {

	public static final MapCodec<UnobstructedBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				Vec3i.CODEC
					.optionalFieldOf("offset", Vec3i.ZERO)
					.forGetter(UnobstructedBlockPredicate::offset)
			)
			.apply(instance, UnobstructedBlockPredicate::new)
	);

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.UNOBSTRUCTED;
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		return world.doesNotIntersectEntities(null, VoxelShapes.fullCube().offset(pos));
	}
}
