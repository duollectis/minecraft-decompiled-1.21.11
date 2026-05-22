package net.minecraft.world.gen.blockpredicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3i;

/**
 * Предикат, проверяющий, является ли блок по смещённой позиции твёрдым (solid).
 *
 * @deprecated Используйте {@link HasSturdyFacePredicate} для более точной проверки опоры.
 */
@Deprecated
public class SolidBlockPredicate extends OffsetPredicate {

	public static final MapCodec<SolidBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(
		instance -> registerOffsetField(instance).apply(instance, SolidBlockPredicate::new)
	);

	public SolidBlockPredicate(Vec3i offset) {
		super(offset);
	}

	@Override
	protected boolean test(BlockState state) {
		return state.isSolid();
	}

	@Override
	public BlockPredicateType<?> getType() {
		return BlockPredicateType.SOLID;
	}
}
