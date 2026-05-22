package net.minecraft.world.gen.blockpredicate;

import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.StructureWorldAccess;

/**
 * Базовый класс для предикатов, проверяющих состояние блока со смещением от целевой позиции.
 */
public abstract class OffsetPredicate implements BlockPredicate {

	protected final Vec3i offset;

	protected OffsetPredicate(Vec3i offset) {
		this.offset = offset;
	}

	/**
	 * Регистрирует поле смещения в кодеке подкласса.
	 * Смещение ограничено диапазоном [-16, 16] по каждой оси.
	 */
	protected static <P extends OffsetPredicate> P1<Mu<P>, Vec3i> registerOffsetField(Instance<P> instance) {
		return instance.group(
			Vec3i.createOffsetCodec(16).optionalFieldOf("offset", Vec3i.ZERO).forGetter(p -> p.offset)
		);
	}

	@Override
	public final boolean test(StructureWorldAccess world, BlockPos pos) {
		return test(world.getBlockState(pos.add(offset)));
	}

	protected abstract boolean test(BlockState state);
}
