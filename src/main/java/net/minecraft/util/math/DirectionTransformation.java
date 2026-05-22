package net.minecraft.util.math;

import net.minecraft.block.enums.Orientation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Vector3i;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Перечисление всех 48 ортогональных преобразований направлений в 3D-пространстве
 * (группа симметрий куба O_h). Каждое преобразование задаётся перестановкой осей
 * ({@link AxisTransformation}) и набором флагов инверсии по каждой оси.
 */
public enum DirectionTransformation implements StringIdentifiable {
	IDENTITY("identity", AxisTransformation.P123, false, false, false),
	ROT_180_FACE_XY("rot_180_face_xy", AxisTransformation.P123, true, true, false),
	ROT_180_FACE_XZ("rot_180_face_xz", AxisTransformation.P123, true, false, true),
	ROT_180_FACE_YZ("rot_180_face_yz", AxisTransformation.P123, false, true, true),
	ROT_120_NNN("rot_120_nnn", AxisTransformation.P231, false, false, false),
	ROT_120_NNP("rot_120_nnp", AxisTransformation.P312, true, false, true),
	ROT_120_NPN("rot_120_npn", AxisTransformation.P312, false, true, true),
	ROT_120_NPP("rot_120_npp", AxisTransformation.P231, true, false, true),
	ROT_120_PNN("rot_120_pnn", AxisTransformation.P312, true, true, false),
	ROT_120_PNP("rot_120_pnp", AxisTransformation.P231, true, true, false),
	ROT_120_PPN("rot_120_ppn", AxisTransformation.P231, false, true, true),
	ROT_120_PPP("rot_120_ppp", AxisTransformation.P312, false, false, false),
	ROT_180_EDGE_XY_NEG("rot_180_edge_xy_neg", AxisTransformation.P213, true, true, true),
	ROT_180_EDGE_XY_POS("rot_180_edge_xy_pos", AxisTransformation.P213, false, false, true),
	ROT_180_EDGE_XZ_NEG("rot_180_edge_xz_neg", AxisTransformation.P321, true, true, true),
	ROT_180_EDGE_XZ_POS("rot_180_edge_xz_pos", AxisTransformation.P321, false, true, false),
	ROT_180_EDGE_YZ_NEG("rot_180_edge_yz_neg", AxisTransformation.P132, true, true, true),
	ROT_180_EDGE_YZ_POS("rot_180_edge_yz_pos", AxisTransformation.P132, true, false, false),
	ROT_90_X_NEG("rot_90_x_neg", AxisTransformation.P132, false, false, true),
	ROT_90_X_POS("rot_90_x_pos", AxisTransformation.P132, false, true, false),
	ROT_90_Y_NEG("rot_90_y_neg", AxisTransformation.P321, true, false, false),
	ROT_90_Y_POS("rot_90_y_pos", AxisTransformation.P321, false, false, true),
	ROT_90_Z_NEG("rot_90_z_neg", AxisTransformation.P213, false, true, false),
	ROT_90_Z_POS("rot_90_z_pos", AxisTransformation.P213, true, false, false),
	INVERSION("inversion", AxisTransformation.P123, true, true, true),
	INVERT_X("invert_x", AxisTransformation.P123, true, false, false),
	INVERT_Y("invert_y", AxisTransformation.P123, false, true, false),
	INVERT_Z("invert_z", AxisTransformation.P123, false, false, true),
	ROT_60_REF_NNN("rot_60_ref_nnn", AxisTransformation.P312, true, true, true),
	ROT_60_REF_NNP("rot_60_ref_nnp", AxisTransformation.P231, true, false, false),
	ROT_60_REF_NPN("rot_60_ref_npn", AxisTransformation.P231, false, false, true),
	ROT_60_REF_NPP("rot_60_ref_npp", AxisTransformation.P312, false, false, true),
	ROT_60_REF_PNN("rot_60_ref_pnn", AxisTransformation.P231, false, true, false),
	ROT_60_REF_PNP("rot_60_ref_pnp", AxisTransformation.P312, true, false, false),
	ROT_60_REF_PPN("rot_60_ref_ppn", AxisTransformation.P312, false, true, false),
	ROT_60_REF_PPP("rot_60_ref_ppp", AxisTransformation.P231, true, true, true),
	SWAP_XY("swap_xy", AxisTransformation.P213, false, false, false),
	SWAP_YZ("swap_yz", AxisTransformation.P132, false, false, false),
	SWAP_XZ("swap_xz", AxisTransformation.P321, false, false, false),
	SWAP_NEG_XY("swap_neg_xy", AxisTransformation.P213, true, true, false),
	SWAP_NEG_YZ("swap_neg_yz", AxisTransformation.P132, false, true, true),
	SWAP_NEG_XZ("swap_neg_xz", AxisTransformation.P321, true, false, true),
	ROT_90_REF_X_NEG("rot_90_ref_x_neg", AxisTransformation.P132, true, false, true),
	ROT_90_REF_X_POS("rot_90_ref_x_pos", AxisTransformation.P132, true, true, false),
	ROT_90_REF_Y_NEG("rot_90_ref_y_neg", AxisTransformation.P321, true, true, false),
	ROT_90_REF_Y_POS("rot_90_ref_y_pos", AxisTransformation.P321, false, true, true),
	ROT_90_REF_Z_NEG("rot_90_ref_z_neg", AxisTransformation.P213, false, true, true),
	ROT_90_REF_Z_POS("rot_90_ref_z_pos", AxisTransformation.P213, true, false, true);

	public static final DirectionTransformation ROT_X_POS_270 = ROT_90_X_POS;
	public static final DirectionTransformation ROT_X_180 = ROT_180_FACE_YZ;
	public static final DirectionTransformation ROT_X_NEG_270 = ROT_90_X_NEG;
	public static final DirectionTransformation ROT_Y_POS_270 = ROT_90_Y_POS;
	public static final DirectionTransformation ROT_Y_180 = ROT_180_FACE_XZ;
	public static final DirectionTransformation ROT_Y_NEG_270 = ROT_90_Y_NEG;
	public static final DirectionTransformation ROT_Z_POS_270 = ROT_90_Z_POS;
	public static final DirectionTransformation ROT_Z_180 = ROT_180_FACE_XY;
	public static final DirectionTransformation ROT_Z_NEG_270 = ROT_90_Z_NEG;

	private static final DirectionTransformation[][] COMBINATIONS = Util.make(
			() -> {
				DirectionTransformation[] all = values();
				DirectionTransformation[][] result = new DirectionTransformation[all.length][all.length];
				Map<Integer, DirectionTransformation> byIndex = Arrays.stream(all)
						.collect(Collectors.toMap(DirectionTransformation::getIndex, t -> t));

				for (DirectionTransformation a : all) {
					for (DirectionTransformation b : all) {
						AxisTransformation combined = b.axisTransformation.prepend(a.axisTransformation);
						boolean flipX = a.shouldFlipDirection(Direction.Axis.X)
								^ b.shouldFlipDirection(a.axisTransformation.map(Direction.Axis.X));
						boolean flipY = a.shouldFlipDirection(Direction.Axis.Y)
								^ b.shouldFlipDirection(a.axisTransformation.map(Direction.Axis.Y));
						boolean flipZ = a.shouldFlipDirection(Direction.Axis.Z)
								^ b.shouldFlipDirection(a.axisTransformation.map(Direction.Axis.Z));
						result[a.ordinal()][b.ordinal()] = byIndex.get(toIndex(flipX, flipY, flipZ, combined));
					}
				}

				return result;
			}
	);

	private static final DirectionTransformation[] INVERSES = Arrays.stream(values())
			.map(a -> Arrays.stream(values())
					.filter(b -> a.prepend(b) == IDENTITY)
					.findAny()
					.get())
			.toArray(DirectionTransformation[]::new);

	private final Matrix3fc matrix;
	private final String name;
	private @Nullable Map<Direction, Direction> mappings;
	private final boolean flipX;
	private final boolean flipY;
	private final boolean flipZ;
	private final AxisTransformation axisTransformation;

	DirectionTransformation(
			final String name,
			final AxisTransformation axisTransformation,
			final boolean flipX,
			final boolean flipY,
			final boolean flipZ
	) {
		this.name = name;
		this.flipX = flipX;
		this.flipY = flipY;
		this.flipZ = flipZ;
		this.axisTransformation = axisTransformation;
		matrix = new Matrix3f()
				.scaling(flipX ? -1.0F : 1.0F, flipY ? -1.0F : 1.0F, flipZ ? -1.0F : 1.0F)
				.mul(axisTransformation.getMatrix());
	}

	private static int toIndex(boolean flipX, boolean flipY, boolean flipZ, AxisTransformation axisTransformation) {
		int flags = (flipZ ? 4 : 0) + (flipY ? 2 : 0) + (flipX ? 1 : 0);
		return axisTransformation.ordinal() << 3 | flags;
	}

	private int getIndex() {
		return toIndex(flipX, flipY, flipZ, axisTransformation);
	}

	public DirectionTransformation prepend(DirectionTransformation transformation) {
		return COMBINATIONS[ordinal()][transformation.ordinal()];
	}

	public DirectionTransformation inverse() {
		return INVERSES[ordinal()];
	}

	public Matrix3fc getMatrix() {
		return matrix;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public String asString() {
		return name;
	}

	public Direction map(Direction direction) {
		if (mappings == null) {
			mappings = Util.mapEnum(
					Direction.class, d -> {
						Direction.Axis axis = d.getAxis();
						Direction.AxisDirection axisDirection = d.getDirection();
						Direction.Axis mappedAxis = axisTransformation.getInverse().map(axis);
						Direction.AxisDirection mappedAxisDirection = shouldFlipDirection(mappedAxis)
								? axisDirection.getOpposite()
								: axisDirection;
						return Direction.from(mappedAxis, mappedAxisDirection);
					}
			);
		}

		return mappings.get(direction);
	}

	public Vector3i map(Vector3i vec) {
		axisTransformation.map(vec);
		vec.x = vec.x * (flipX ? -1 : 1);
		vec.y = vec.y * (flipY ? -1 : 1);
		vec.z = vec.z * (flipZ ? -1 : 1);
		return vec;
	}

	public boolean shouldFlipDirection(Direction.Axis axis) {
		return switch (axis) {
			case X -> flipX;
			case Y -> flipY;
			case Z -> flipZ;
		};
	}

	public AxisTransformation getAxisTransformation() {
		return axisTransformation;
	}

	public Orientation mapJigsawOrientation(Orientation orientation) {
		return Orientation.byDirections(map(orientation.getFacing()), map(orientation.getRotation()));
	}
}
