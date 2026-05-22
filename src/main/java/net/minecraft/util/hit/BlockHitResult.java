package net.minecraft.util.hit;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Результат трассировки луча, попавшего в блок (или промахнувшегося).
 * Иммутабелен: методы {@link #withSide}, {@link #withBlockPos} и {@link #againstWorldBorder}
 * возвращают новый экземпляр с изменённым полем.
 */
public class BlockHitResult extends HitResult {

	private final Direction side;
	private final BlockPos blockPos;
	private final boolean missed;
	private final boolean insideBlock;
	private final boolean againstWorldBorder;

	/** Создаёт результат промаха — луч не попал ни в один блок. */
	public static BlockHitResult createMissed(Vec3d pos, Direction side, BlockPos blockPos) {
		return new BlockHitResult(true, pos, side, blockPos, false, false);
	}

	public BlockHitResult(Vec3d pos, Direction side, BlockPos blockPos, boolean insideBlock) {
		this(false, pos, side, blockPos, insideBlock, false);
	}

	public BlockHitResult(
		Vec3d pos,
		Direction side,
		BlockPos blockPos,
		boolean insideBlock,
		boolean againstWorldBorder
	) {
		this(false, pos, side, blockPos, insideBlock, againstWorldBorder);
	}

	private BlockHitResult(
		boolean missed,
		Vec3d pos,
		Direction side,
		BlockPos blockPos,
		boolean insideBlock,
		boolean againstWorldBorder
	) {
		super(pos);
		this.missed = missed;
		this.side = side;
		this.blockPos = blockPos;
		this.insideBlock = insideBlock;
		this.againstWorldBorder = againstWorldBorder;
	}

	public BlockHitResult withSide(Direction side) {
		return new BlockHitResult(missed, pos, side, blockPos, insideBlock, againstWorldBorder);
	}

	public BlockHitResult withBlockPos(BlockPos blockPos) {
		return new BlockHitResult(missed, pos, side, blockPos, insideBlock, againstWorldBorder);
	}

	public BlockHitResult againstWorldBorder() {
		return new BlockHitResult(missed, pos, side, blockPos, insideBlock, true);
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public Direction getSide() {
		return side;
	}

	@Override
	public Type getType() {
		return missed ? Type.MISS : Type.BLOCK;
	}

	public boolean isInsideBlock() {
		return insideBlock;
	}

	public boolean isAgainstWorldBorder() {
		return againstWorldBorder;
	}
}
