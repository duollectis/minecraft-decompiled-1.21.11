package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.EnumMap;
import java.util.Map;

/**
 * Грань куба блочной модели: описывает четыре угла грани через {@link Corner},
 * каждый из которых задаётся тремя {@link AxisBound} — по одному на каждую ось.
 */
@Environment(EnvType.CLIENT)
public enum CubeFace {
	DOWN(
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MAX_Z)
	),
	UP(
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MIN_Z)
	),
	NORTH(
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MIN_Z)
	),
	SOUTH(
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MAX_Z)
	),
	WEST(
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MIN_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MAX_Z)
	),
	EAST(
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MAX_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MIN_Y, CubeFace.AxisBound.MIN_Z),
			new CubeFace.Corner(CubeFace.AxisBound.MAX_X, CubeFace.AxisBound.MAX_Y, CubeFace.AxisBound.MIN_Z)
	);

	private static final Map<Direction, CubeFace> DIRECTION_LOOKUP = Util.make(
			new EnumMap<>(Direction.class), map -> {
				map.put(Direction.DOWN, DOWN);
				map.put(Direction.UP, UP);
				map.put(Direction.NORTH, NORTH);
				map.put(Direction.SOUTH, SOUTH);
				map.put(Direction.WEST, WEST);
				map.put(Direction.EAST, EAST);
			}
	);

	private final CubeFace.Corner[] corners;

	CubeFace(CubeFace.Corner... corners) {
		this.corners = corners;
	}

	public static CubeFace getFace(Direction direction) {
		return DIRECTION_LOOKUP.get(direction);
	}

	public CubeFace.Corner getCorner(int corner) {
		return corners[corner];
	}

	/**
	 * Угол грани куба: задаётся тремя {@link AxisBound} — по одному на каждую ось.
	 * Метод {@link #resolve} вычисляет реальную позицию угла из AABB блока.
	 */
	@Environment(EnvType.CLIENT)
	public record Corner(CubeFace.AxisBound xSide, CubeFace.AxisBound ySide, CubeFace.AxisBound zSide) {

		/**
		 * Вычисляет позицию угла, выбирая min или max координату по каждой оси
		 * из переданного AABB (min = {@code from}, max = {@code to}).
		 */
		public Vector3f resolve(Vector3fc from, Vector3fc to) {
			return new Vector3f(
					xSide.select(from, to),
					ySide.select(from, to),
					zSide.select(from, to)
			);
		}
	}

	/**
	 * Граница оси куба: определяет, берётся ли минимальная или максимальная
	 * координата по данной оси при вычислении позиции угла.
	 */
	@Environment(EnvType.CLIENT)
	public enum AxisBound {
		MIN_X,
		MIN_Y,
		MIN_Z,
		MAX_X,
		MAX_Y,
		MAX_Z;

		public float select(Vector3fc from, Vector3fc to) {
			return switch (this) {
				case MIN_X -> from.x();
				case MIN_Y -> from.y();
				case MIN_Z -> from.z();
				case MAX_X -> to.x();
				case MAX_Y -> to.y();
				case MAX_Z -> to.z();
			};
		}

		public float selectFloat(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
			return switch (this) {
				case MIN_X -> minX;
				case MIN_Y -> minY;
				case MIN_Z -> minZ;
				case MAX_X -> maxX;
				case MAX_Y -> maxY;
				case MAX_Z -> maxZ;
			};
		}
	}
}
