package net.minecraft.world.gen.blockpredicate;

import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

/**
 * {@code OffsetPredicate}.
 */
public abstract class OffsetPredicate implements BlockPredicate {

	protected final Vec3i offset;

	/**
	 * Регистрирует offset field.
	 *
	 * @param instance instance
	 *
	 * @return P1, Vec3i> — результат операции
	 */
	protected static <P extends OffsetPredicate> P1<Mu<P>, Vec3i> registerOffsetField(Instance<P> instance) {
		return instance.group(Vec3i
				.createOffsetCodec(16)
				.optionalFieldOf("offset", Vec3i.ZERO)
				.forGetter(predicate -> predicate.offset));
	}

	protected OffsetPredicate(Vec3i offset) {
		this.offset = offset;
	}

	/**
	 * Test.
	 *
	 * @param structureWorldAccess structure world access
	 * @param blockPos block pos
	 *
	 * @return boolean — результат операции
	 */
	public final boolean test(StructureWorldAccess structureWorldAccess, BlockPos blockPos) {
		return this.test(structureWorldAccess.getBlockState(blockPos.add(this.offset)));
	}

	/**
	 * Test.
	 *
	 * @param state state
	 *
	 * @return boolean — результат операции
	 */
	protected abstract boolean test(BlockState state);
}
