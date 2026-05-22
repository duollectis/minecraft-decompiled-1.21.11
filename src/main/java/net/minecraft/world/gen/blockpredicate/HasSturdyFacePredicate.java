package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

/**
 * Предикат, проверяющий наличие прочной грани блока в заданном направлении.
 */
public class HasSturdyFacePredicate implements BlockPredicate {

	public static final MapCodec<HasSturdyFacePredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			Vec3i.createOffsetCodec(16).optionalFieldOf("offset", Vec3i.ZERO).forGetter(p -> p.offset),
			Direction.CODEC.fieldOf("direction").forGetter(p -> p.face)
		).apply(instance, HasSturdyFacePredicate::new)
	);

	private final Vec3i offset;
	private final Direction face;

	public HasSturdyFacePredicate(Vec3i offset, Direction face) {
		this.offset = offset;
		this.face = face;
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		BlockPos targetPos = pos.add(offset);
		return world.getBlockState(targetPos).isSideSolidFullSquare(world, targetPos, face);
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.HAS_STURDY_FACE;
	}
}
