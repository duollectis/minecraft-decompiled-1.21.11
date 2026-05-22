package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

/**
 * Предикат, проверяющий, может ли заданный {@link BlockState} выжить (canPlaceAt)
 * в позиции с учётом смещения. Используется при условном размещении растений и декораций.
 */
public class WouldSurviveBlockPredicate implements BlockPredicate {

	public static final MapCodec<WouldSurviveBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(
				Vec3i
					.createOffsetCodec(16)
					.optionalFieldOf("offset", Vec3i.ZERO)
					.forGetter(predicate -> predicate.offset),
				BlockState.CODEC
					.fieldOf("state")
					.forGetter(predicate -> predicate.state)
			)
			.apply(instance, WouldSurviveBlockPredicate::new)
	);

	private final Vec3i offset;
	private final BlockState state;

	protected WouldSurviveBlockPredicate(Vec3i offset, BlockState state) {
		this.offset = offset;
		this.state = state;
	}

	@Override
	public boolean test(StructureWorldAccess world, BlockPos pos) {
		return state.canPlaceAt(world, pos.add(offset));
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.WOULD_SURVIVE;
	}
}
