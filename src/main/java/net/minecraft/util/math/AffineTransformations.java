package net.minecraft.util.math;

import com.google.common.collect.Maps;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Утилитарный класс для работы с аффинными преобразованиями блоков:
 * UV-блокировка, трансформация блока и поворот по направлению.
 */
public class AffineTransformations {

	private static final Map<Direction, AffineTransformation> DIRECTION_ROTATIONS = Maps.newEnumMap(
			Map.of(
					Direction.SOUTH, AffineTransformation.identity(),
					Direction.EAST, new AffineTransformation(null, new Quaternionf().rotateY((float) (Math.PI / 2)), null, null),
					Direction.WEST, new AffineTransformation(null, new Quaternionf().rotateY((float) (-Math.PI / 2)), null, null),
					Direction.NORTH, new AffineTransformation(null, new Quaternionf().rotateY((float) Math.PI), null, null),
					Direction.UP, new AffineTransformation(null, new Quaternionf().rotateX((float) (-Math.PI / 2)), null, null),
					Direction.DOWN, new AffineTransformation(null, new Quaternionf().rotateX((float) (Math.PI / 2)), null, null)
			)
	);

	private static final Map<Direction, AffineTransformation> INVERTED_DIRECTION_ROTATIONS = Maps.newEnumMap(
			Util.transformMapValues(DIRECTION_ROTATIONS, AffineTransformation::invert)
	);

	public static AffineTransformation setupUvLock(AffineTransformation transformation) {
		Matrix4f matrix = new Matrix4f().translation(0.5F, 0.5F, 0.5F);
		matrix.mul(transformation.getMatrix());
		matrix.translate(-0.5F, -0.5F, -0.5F);
		return new AffineTransformation(matrix);
	}

	public static AffineTransformation setupBlockTransform(AffineTransformation transformation) {
		Matrix4f matrix = new Matrix4f().translation(-0.5F, -0.5F, -0.5F);
		matrix.mul(transformation.getMatrix());
		matrix.translate(0.5F, 0.5F, 0.5F);
		return new AffineTransformation(matrix);
	}

	public static AffineTransformation getTransformed(AffineTransformation affineTransformation, Direction direction) {
		if (MatrixUtil.isIdentity(affineTransformation.getMatrix())) {
			return affineTransformation;
		}

		AffineTransformation rotated = affineTransformation.multiply(DIRECTION_ROTATIONS.get(direction));
		Vector3f forward = rotated.getMatrix().transformDirection(new Vector3f(0.0F, 0.0F, 1.0F));
		Direction facing = Direction.getFacing(forward.x, forward.y, forward.z);
		return INVERTED_DIRECTION_ROTATIONS.get(facing).multiply(rotated);
	}
}
