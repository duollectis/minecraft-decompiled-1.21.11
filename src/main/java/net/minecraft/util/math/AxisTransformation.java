package net.minecraft.util.math;

import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Arrays;

/**
 * {@code AxisTransformation}.
 */
public enum AxisTransformation {
	P123(0, 1, 2),
	P213(1, 0, 2),
	P132(0, 2, 1),
	P312(2, 0, 1),
	P231(1, 2, 0),
	P321(2, 1, 0);

	private final int xMapping;
	private final int yMapping;
	private final int zMapping;
	private final Matrix3fc matrix;
	private static final AxisTransformation[][] COMBINATIONS = Util.make(
			() -> {
				AxisTransformation[] axisTransformations = values();
				AxisTransformation[][]
						axisTransformations2 =
						new AxisTransformation[axisTransformations.length][axisTransformations.length];

				for (AxisTransformation axisTransformation : axisTransformations) {
					for (AxisTransformation axisTransformation2 : axisTransformations) {
						int i = axisTransformation.map(axisTransformation2.xMapping);
						int j = axisTransformation.map(axisTransformation2.yMapping);
						int k = axisTransformation.map(axisTransformation2.zMapping);
						AxisTransformation axisTransformation3 = Arrays.stream(axisTransformations)
						                                               .filter(transformation ->
								                                               transformation.xMapping == i
										                                               && transformation.yMapping == j
										                                               && transformation.zMapping == k)
						                                               .findFirst()
						                                               .get();
						axisTransformations2[axisTransformation.ordinal()][axisTransformation2.ordinal()] =
								axisTransformation3;
					}
				}

				return axisTransformations2;
			}
	);
	private static final AxisTransformation[] INVERSE = Util.make(
			() -> {
				AxisTransformation[] axisTransformations = values();
				return Arrays.stream(axisTransformations)
				             .map(a -> Arrays.stream(values()).filter(b -> a.prepend(b) == P123).findAny().get())
				             .toArray(AxisTransformation[]::new);
			}
	);

	private AxisTransformation(final int xMapping, final int yMapping, final int zMapping) {
		this.xMapping = xMapping;
		this.yMapping = yMapping;
		this.zMapping = zMapping;
		this.matrix =
				new Matrix3f().zero().set(this.map(0), 0, 1.0F).set(this.map(1), 1, 1.0F).set(this.map(2), 2, 1.0F);
	}

	/**
	 * Prepend.
	 *
	 * @param transformation transformation
	 *
	 * @return AxisTransformation — результат операции
	 */
	public AxisTransformation prepend(AxisTransformation transformation) {
		return COMBINATIONS[this.ordinal()][transformation.ordinal()];
	}

	public AxisTransformation getInverse() {
		return INVERSE[this.ordinal()];
	}

	/**
	 * Map.
	 *
	 * @param axis axis
	 *
	 * @return int — результат операции
	 */
	public int map(int axis) {
		return switch (axis) {
			case 0 -> this.xMapping;
			case 1 -> this.yMapping;
			case 2 -> this.zMapping;
			default -> throw new IllegalArgumentException("Must be 0, 1 or 2, but got " + axis);
		};
	}

	public Direction.Axis map(Direction.Axis axis) {
		return Direction.Axis.VALUES[this.map(axis.ordinal())];
	}

	/**
	 * Map.
	 *
	 * @param vec vec
	 *
	 * @return Vector3f — результат операции
	 */
	public Vector3f map(Vector3f vec) {
		float f = vec.get(this.xMapping);
		float g = vec.get(this.yMapping);
		float h = vec.get(this.zMapping);
		return vec.set(f, g, h);
	}

	/**
	 * Map.
	 *
	 * @param vec vec
	 *
	 * @return Vector3i — результат операции
	 */
	public Vector3i map(Vector3i vec) {
		int i = vec.get(this.xMapping);
		int j = vec.get(this.yMapping);
		int k = vec.get(this.zMapping);
		return vec.set(i, j, k);
	}

	public Matrix3fc getMatrix() {
		return this.matrix;
	}
}
