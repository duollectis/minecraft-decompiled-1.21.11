package net.minecraft.util.math;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Поле гравитации, моделирующее притяжение нескольких точечных масс.
 * Используется в алгоритмах генерации структур для оценки «притяжения» к определённым блокам.
 */
public class GravityField {

	private final List<Point> points = Lists.newArrayList();

	public void addPoint(BlockPos pos, double mass) {
		if (mass != 0.0) {
			points.add(new Point(pos, mass));
		}
	}

	public double calculate(BlockPos pos, double mass) {
		if (mass == 0.0) {
			return 0.0;
		}

		double total = 0.0;

		for (Point point : points) {
			total += point.getGravityFactor(pos);
		}

		return total * mass;
	}

	private static class Point {

		private final BlockPos pos;
		private final double mass;

		Point(BlockPos pos, double mass) {
			this.pos = pos;
			this.mass = mass;
		}

		double getGravityFactor(BlockPos target) {
			double squaredDistance = pos.getSquaredDistance(target);
			return squaredDistance == 0.0 ? Double.POSITIVE_INFINITY : mass / Math.sqrt(squaredDistance);
		}
	}
}
