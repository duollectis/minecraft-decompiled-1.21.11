package net.minecraft.util.math;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * {@code AxisRotation}.
 */
public enum AxisRotation {
	R0(0, DirectionTransformation.IDENTITY, DirectionTransformation.IDENTITY, DirectionTransformation.IDENTITY),
	R90(
			1,
			DirectionTransformation.ROT_90_Y_POS,
			DirectionTransformation.ROT_90_Z_POS,
			DirectionTransformation.ROT_90_X_POS
	),
	R180(
			2,
			DirectionTransformation.ROT_180_FACE_XZ,
			DirectionTransformation.ROT_180_FACE_XY,
			DirectionTransformation.ROT_180_FACE_YZ
	),
	R270(
			3,
			DirectionTransformation.ROT_90_Y_NEG,
			DirectionTransformation.ROT_90_Z_NEG,
			DirectionTransformation.ROT_90_X_NEG
	);

	public static final Codec<AxisRotation> CODEC = Codec.INT.comapFlatMap(
			degrees -> {
				return switch (MathHelper.floorMod(degrees, 360)) {
					case 0 -> DataResult.success(R0);
					case 90 -> DataResult.success(R90);
					case 180 -> DataResult.success(R180);
					case 270 -> DataResult.success(R270);
					default ->
							DataResult.error(() -> "Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
				};
			}, rotation -> {
				return switch (rotation) {
					case R0 -> 0;
					case R90 -> 90;
					case R180 -> 180;
					case R270 -> 270;
				};
			}
	);
	public final int index;
	public final DirectionTransformation xAxisTransformation;
	public final DirectionTransformation yAxisTransformation;
	public final DirectionTransformation zAxisTransformation;

	private AxisRotation(
			final int index,
			final DirectionTransformation directionTransformation,
			final DirectionTransformation directionTransformation2,
			final DirectionTransformation directionTransformation3
	) {
		this.index = index;
		this.xAxisTransformation = directionTransformation;
		this.yAxisTransformation = directionTransformation2;
		this.zAxisTransformation = directionTransformation3;
	}

	@Deprecated
	public static AxisRotation fromDegrees(int degrees) {
		return switch (MathHelper.floorMod(degrees, 360)) {
			case 0 -> R0;
			case 90 -> R90;
			case 180 -> R180;
			case 270 -> R270;
			default ->
					throw new JsonParseException("Invalid rotation " + degrees + " found, only 0/90/180/270 allowed");
		};
	}

	public static DirectionTransformation combineXY(AxisRotation axisRotation, AxisRotation axisRotation2) {
		return axisRotation2.yAxisTransformation.prepend(axisRotation.xAxisTransformation);
	}

	public static DirectionTransformation combineXYZ(
			AxisRotation axisRotation,
			AxisRotation axisRotation2,
			AxisRotation axisRotation3
	) {
		return axisRotation3.zAxisTransformation.prepend(axisRotation2.yAxisTransformation.prepend(axisRotation.xAxisTransformation));
	}

	public int rotate(int index) {
		return (index + this.index) % 4;
	}
}
