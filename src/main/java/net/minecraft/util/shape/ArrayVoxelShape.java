package net.minecraft.util.shape;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

/**
 * {@code ArrayVoxelShape}.
 */
public class ArrayVoxelShape extends VoxelShape {

	private final DoubleList xPoints;
	private final DoubleList yPoints;
	private final DoubleList zPoints;

	protected ArrayVoxelShape(VoxelSet shape, double[] xPoints, double[] yPoints, double[] zPoints) {
		this(
				shape,
				DoubleArrayList.wrap(Arrays.copyOf(xPoints, shape.getXSize() + 1)),
				DoubleArrayList.wrap(Arrays.copyOf(yPoints, shape.getYSize() + 1)),
				DoubleArrayList.wrap(Arrays.copyOf(zPoints, shape.getZSize() + 1))
		);
	}

	ArrayVoxelShape(VoxelSet shape, DoubleList xPoints, DoubleList yPoints, DoubleList zPoints) {
		super(shape);
		int i = shape.getXSize() + 1;
		int j = shape.getYSize() + 1;
		int k = shape.getZSize() + 1;
		if (i == xPoints.size() && j == yPoints.size() && k == zPoints.size()) {
			this.xPoints = xPoints;
			this.yPoints = yPoints;
			this.zPoints = zPoints;
		}
		else {
			throw (IllegalArgumentException) Util.getFatalOrPause(
					new IllegalArgumentException(
							"Lengths of point arrays must be consistent with the size of the VoxelShape.")
			);
		}
	}

	@Override
	public DoubleList getPointPositions(Direction.Axis axis) {
		return switch (axis) {
			case X -> this.xPoints;
			case Y -> this.yPoints;
			case Z -> this.zPoints;
		};
	}
}
