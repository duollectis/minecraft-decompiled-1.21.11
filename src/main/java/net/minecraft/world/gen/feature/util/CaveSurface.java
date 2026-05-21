package net.minecraft.world.gen.feature.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.TestableWorld;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * {@code CaveSurface}.
 */
public abstract class CaveSurface {

	public static CaveSurface.Bounded createBoundedExpanded(int floor, int ceiling) {
		return new CaveSurface.Bounded(floor - 1, ceiling + 1);
	}

	public static CaveSurface.Bounded createBounded(int floor, int ceiling) {
		return new CaveSurface.Bounded(floor, ceiling);
	}

	/**
	 * Создаёт half with ceiling.
	 *
	 * @param ceiling ceiling
	 *
	 * @return CaveSurface — результат операции
	 */
	public static CaveSurface createHalfWithCeiling(int ceiling) {
		return new CaveSurface.Half(ceiling, false);
	}

	/**
	 * Создаёт half with ceiling expanded.
	 *
	 * @param i i
	 *
	 * @return CaveSurface — результат операции
	 */
	public static CaveSurface createHalfWithCeilingExpanded(int i) {
		return new CaveSurface.Half(i + 1, false);
	}

	/**
	 * Создаёт half with floor.
	 *
	 * @param floor floor
	 *
	 * @return CaveSurface — результат операции
	 */
	public static CaveSurface createHalfWithFloor(int floor) {
		return new CaveSurface.Half(floor, true);
	}

	/**
	 * Создаёт half with floor expanded.
	 *
	 * @param i i
	 *
	 * @return CaveSurface — результат операции
	 */
	public static CaveSurface createHalfWithFloorExpanded(int i) {
		return new CaveSurface.Half(i - 1, true);
	}

	/**
	 * Создаёт empty.
	 *
	 * @return CaveSurface — результат операции
	 */
	public static CaveSurface createEmpty() {
		return CaveSurface.Empty.INSTANCE;
	}

	/**
	 * Create.
	 *
	 * @param ceilingHeight ceiling height
	 * @param floorHeight floor height
	 *
	 * @return CaveSurface — результат операции
	 */
	public static CaveSurface create(OptionalInt ceilingHeight, OptionalInt floorHeight) {
		if (ceilingHeight.isPresent() && floorHeight.isPresent()) {
			return createBounded(ceilingHeight.getAsInt(), floorHeight.getAsInt());
		}
		else if (ceilingHeight.isPresent()) {
			return createHalfWithFloor(ceilingHeight.getAsInt());
		}
		else {
			return floorHeight.isPresent() ? createHalfWithCeiling(floorHeight.getAsInt()) : createEmpty();
		}
	}

	public abstract OptionalInt getCeilingHeight();

	public abstract OptionalInt getFloorHeight();

	public abstract OptionalInt getOptionalHeight();

	/**
	 * With floor.
	 *
	 * @param floor floor
	 *
	 * @return CaveSurface — результат операции
	 */
	public CaveSurface withFloor(OptionalInt floor) {
		return create(floor, this.getCeilingHeight());
	}

	/**
	 * With ceiling.
	 *
	 * @param ceiling ceiling
	 *
	 * @return CaveSurface — результат операции
	 */
	public CaveSurface withCeiling(OptionalInt ceiling) {
		return create(this.getFloorHeight(), ceiling);
	}

	public static Optional<CaveSurface> create(
			TestableWorld world,
			BlockPos pos,
			int height,
			Predicate<BlockState> canGenerate,
			Predicate<BlockState> canReplace
	) {
		BlockPos.Mutable mutable = pos.mutableCopy();
		if (!world.testBlockState(pos, canGenerate)) {
			return Optional.empty();
		}
		else {
			int i = pos.getY();
			OptionalInt optionalInt = getCaveSurface(world, height, canGenerate, canReplace, mutable, i, Direction.UP);
			OptionalInt
					optionalInt2 =
					getCaveSurface(world, height, canGenerate, canReplace, mutable, i, Direction.DOWN);
			return Optional.of(create(optionalInt2, optionalInt));
		}
	}

	private static OptionalInt getCaveSurface(
			TestableWorld world,
			int height,
			Predicate<BlockState> canGenerate,
			Predicate<BlockState> canReplace,
			BlockPos.Mutable mutablePos,
			int y,
			Direction direction
	) {
		mutablePos.setY(y);

		for (int i = 1; i < height && world.testBlockState(mutablePos, canGenerate); i++) {
			mutablePos.move(direction);
		}

		return world.testBlockState(mutablePos, canReplace) ? OptionalInt.of(mutablePos.getY()) : OptionalInt.empty();
	}

	/**
	 * {@code Bounded}.
	 */
	public static final class Bounded extends CaveSurface {

		private final int floor;
		private final int ceiling;

		protected Bounded(int floor, int ceiling) {
			this.floor = floor;
			this.ceiling = ceiling;
			if (this.getHeight() < 0) {
				throw new IllegalArgumentException("Column of negative height: " + this);
			}
		}

		@Override
		public OptionalInt getCeilingHeight() {
			return OptionalInt.of(this.ceiling);
		}

		@Override
		public OptionalInt getFloorHeight() {
			return OptionalInt.of(this.floor);
		}

		@Override
		public OptionalInt getOptionalHeight() {
			return OptionalInt.of(this.getHeight());
		}

		public int getCeiling() {
			return this.ceiling;
		}

		public int getFloor() {
			return this.floor;
		}

		public int getHeight() {
			return this.ceiling - this.floor - 1;
		}

		@Override
		public String toString() {
			return "C(" + this.ceiling + "-" + this.floor + ")";
		}
	}

	/**
	 * {@code Empty}.
	 */
	public static final class Empty extends CaveSurface {

		static final CaveSurface.Empty INSTANCE = new CaveSurface.Empty();

		private Empty() {
		}

		@Override
		public OptionalInt getCeilingHeight() {
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt getFloorHeight() {
			return OptionalInt.empty();
		}

		@Override
		public OptionalInt getOptionalHeight() {
			return OptionalInt.empty();
		}

		@Override
		public String toString() {
			return "C(-)";
		}
	}

	/**
	 * {@code Half}.
	 */
	public static final class Half extends CaveSurface {

		private final int height;
		private final boolean floor;

		public Half(int height, boolean floor) {
			this.height = height;
			this.floor = floor;
		}

		@Override
		public OptionalInt getCeilingHeight() {
			return this.floor ? OptionalInt.empty() : OptionalInt.of(this.height);
		}

		@Override
		public OptionalInt getFloorHeight() {
			return this.floor ? OptionalInt.of(this.height) : OptionalInt.empty();
		}

		@Override
		public OptionalInt getOptionalHeight() {
			return OptionalInt.empty();
		}

		@Override
		public String toString() {
			return this.floor ? "C(" + this.height + "-)" : "C(-" + this.height + ")";
		}
	}
}
