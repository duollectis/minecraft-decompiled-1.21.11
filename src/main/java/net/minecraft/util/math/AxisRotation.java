package net.minecraft.util.math;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Дискретное вращение вокруг оси на кратный 90° угол.
 */
public enum AxisRotation {
	R0(0, DirectionTransformation.IDENTITY, DirectionTransformation.IDENTITY, DirectionTransformation.IDENTITY),
	R90(
			1,
			DirectionTransformation.ROT_90_X_NEG,
			DirectionTransformation.ROT_90_Y_NEG,
			DirectionTransformation.ROT_90_Z_NEG
	),
	R180(
			2,
			DirectionTransformation.ROT_180_FACE_YZ,
			DirectionTransformation.ROT_180_FACE_XZ,
			DirectionTransformation.ROT_180_FACE_XY
	),
	R270(
			3,
			DirectionTransformation.ROT_90_X_POS,
			DirectionTransformation.ROT_90_Y_POS,
			DirectionTransformation.ROT_90_Z_POS
	);

	public static final Codec<AxisRotation> CODEC = Codec.INT.comapFlatMap(
			degrees -> switch (MathHelper.floorMod(degrees, 360)) {
				case 0 -> DataResult.success(R0);
				case 90 -> DataResult.success(R90);
				case 180 -> DataResult.success(R180);
				case 270 -> DataResult.success(R270);
				default -> DataResult.error(() -> "Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
			},
			rotation -> switch (rotation) {
				case R0 -> 0;
				case R90 -> 90;
				case R180 -> 180;
				case R270 -> 270;
			}
	);

	public final int index;
	public final DirectionTransformation xAxisTransformation;
	public final DirectionTransformation yAxisTransformation;
	public final DirectionTransformation zAxisTransformation;

	AxisRotation(
			int index,
			DirectionTransformation xTransform,
			DirectionTransformation yTransform,
			DirectionTransformation zTransform
	) {
		this.index = index;
		xAxisTransformation = xTransform;
		yAxisTransformation = yTransform;
		zAxisTransformation = zTransform;
	}

	/**
	 * @deprecated Используй {@link #CODEC} для десериализации.
	 */
	@Deprecated
	public static AxisRotation fromDegrees(int degrees) {
		return switch (MathHelper.floorMod(degrees, 360)) {
			case 0 -> R0;
			case 90 -> R90;
			case 180 -> R180;
			case 270 -> R270;
			default -> throw new JsonParseException("Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
		};
	}

	public static DirectionTransformation combineXY(AxisRotation rotX, AxisRotation rotY) {
		return rotY.yAxisTransformation.prepend(rotX.xAxisTransformation);
	}

	public static DirectionTransformation combineXYZ(AxisRotation rotX, AxisRotation rotY, AxisRotation rotZ) {
		return rotZ.zAxisTransformation.prepend(rotY.yAxisTransformation.prepend(rotX.xAxisTransformation));
	}

	public int rotate(int index) {
		return (index + this.index) % 4;
	}
}
