package net.minecraft.world;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

/**
 * Контекст для рейкаста по состояниям блоков.
 * Содержит начальную и конечную точки луча, а также предикат,
 * определяющий, какие состояния блоков считаются «непрозрачными» для луча.
 */
public class BlockStateRaycastContext {

	private final Vec3d start;
	private final Vec3d end;
	private final Predicate<BlockState> statePredicate;

	public BlockStateRaycastContext(Vec3d start, Vec3d end, Predicate<BlockState> statePredicate) {
		this.start = start;
		this.end = end;
		this.statePredicate = statePredicate;
	}

	public Vec3d getStart() {
		return start;
	}

	public Vec3d getEnd() {
		return end;
	}

	public Predicate<BlockState> getStatePredicate() {
		return statePredicate;
	}
}
