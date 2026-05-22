package net.minecraft.util.math;

import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Arrays;

/**
 * Перестановка осей X, Y, Z без отражения.
 * Имена констант кодируют маппинг: P123 = тождественная, P213 = X↔Y и т.д.
 */
public enum AxisTransformation {
	P123(0, 1, 2),
	P213(1, 0, 2),
	P132(0, 2, 1),
	P312(2, 0, 1),
	P231(1, 2, 0),
	P321(2, 1, 0);

	private static final AxisTransformation[][] COMBINATIONS = Util.make(() -> {
		AxisTransformation[] all = values();
		AxisTransformation[][] table = new AxisTransformation[all.length][all.length];

		for (AxisTransformation a : all) {
			for (AxisTransformation b : all) {
				int x = a.map(b.xMapping);
				int y = a.map(b.yMapping);
				int z = a.map(b.zMapping);
				table[a.ordinal()][b.ordinal()] = Arrays.stream(all)
						.filter(t -> t.xMapping == x && t.yMapping == y && t.zMapping == z)
						.findFirst()
						.get();
			}
		}

		return table;
	});

	private static final AxisTransformation[] INVERSE = Util.make(() ->
			Arrays.stream(values())
					.map(a -> Arrays.stream(values()).filter(b -> a.prepend(b) == P123).findAny().get())
					.toArray(AxisTransformation[]::new)
	);

	private final int xMapping;
	private final int yMapping;
	private final int zMapping;
	private final Matrix3fc matrix;

	AxisTransformation(int xMapping, int yMapping, int zMapping) {
		this.xMapping = xMapping;
		this.yMapping = yMapping;
		this.zMapping = zMapping;
		matrix = new Matrix3f().zero()
				.set(map(0), 0, 1.0F)
				.set(map(1), 1, 1.0F)
				.set(map(2), 2, 1.0F);
	}

	public AxisTransformation prepend(AxisTransformation transformation) {
		return COMBINATIONS[ordinal()][transformation.ordinal()];
	}

	public AxisTransformation getInverse() {
		return INVERSE[ordinal()];
	}

	public int map(int axis) {
		return switch (axis) {
			case 0 -> xMapping;
			case 1 -> yMapping;
			case 2 -> zMapping;
			default -> throw new IllegalArgumentException("Must be 0, 1 or 2, but got " + axis);
		};
	}

	public Direction.Axis map(Direction.Axis axis) {
		return Direction.Axis.VALUES[map(axis.ordinal())];
	}

	public Vector3f map(Vector3f vec) {
		float x = vec.get(xMapping);
		float y = vec.get(yMapping);
		float z = vec.get(zMapping);
		return vec.set(x, y, z);
	}

	public Vector3i map(Vector3i vec) {
		int x = vec.get(xMapping);
		int y = vec.get(yMapping);
		int z = vec.get(zMapping);
		return vec.set(x, y, z);
	}

	public Matrix3fc getMatrix() {
		return matrix;
	}
}
