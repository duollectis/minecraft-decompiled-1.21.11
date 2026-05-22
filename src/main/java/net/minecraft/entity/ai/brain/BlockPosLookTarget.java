package net.minecraft.entity.ai.brain;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Реализация {@link LookTarget}, привязанная к фиксированной позиции блока.
 * Всегда считается видимой для любого наблюдателя, так как блоки не могут «прятаться».
 */
public class BlockPosLookTarget implements LookTarget {

	private final BlockPos blockPos;
	private final Vec3d pos;

	public BlockPosLookTarget(BlockPos blockPos) {
		this.blockPos = blockPos.toImmutable();
		this.pos = Vec3d.ofCenter(blockPos);
	}

	public BlockPosLookTarget(Vec3d pos) {
		this.blockPos = BlockPos.ofFloored(pos);
		this.pos = pos;
	}

	@Override
	public Vec3d getPos() {
		return pos;
	}

	@Override
	public BlockPos getBlockPos() {
		return blockPos;
	}

	@Override
	public boolean isSeenBy(LivingEntity entity) {
		return true;
	}

	@Override
	public String toString() {
		return "BlockPosTracker{blockPos=" + blockPos + ", centerPosition=" + pos + "}";
	}
}
