package net.minecraft.util;

import com.mojang.serialization.Codec;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;

/**
 * Тип зеркального отражения блока при размещении структур.
 * Используется в системе структур для корректного отражения блоков
 * относительно осей X и Z.
 */
public enum BlockMirror implements StringIdentifiable {
	NONE("none", DirectionTransformation.IDENTITY),
	LEFT_RIGHT("left_right", DirectionTransformation.INVERT_Z),
	FRONT_BACK("front_back", DirectionTransformation.INVERT_X);

	public static final Codec<BlockMirror> CODEC = StringIdentifiable.createCodec(BlockMirror::values);
	@Deprecated
	public static final Codec<BlockMirror> ENUM_NAME_CODEC = Codecs.enumByName(BlockMirror::valueOf);

	private final String id;
	private final Text name;
	private final DirectionTransformation directionTransformation;

	BlockMirror(String id, DirectionTransformation directionTransformation) {
		this.id = id;
		this.name = Text.translatable("mirror." + id);
		this.directionTransformation = directionTransformation;
	}

	/**
	 * Применяет зеркальное отражение к числовому значению поворота.
	 * <p>
	 * Используется для корректного отражения угловых значений (например, поворот блока)
	 * в диапазоне {@code [0, fullTurn)}.
	 *
	 * @param rotation  исходное значение поворота
	 * @param fullTurn  полный оборот (например, 16 для блоков с 16 состояниями поворота)
	 * @return отражённое значение поворота
	 */
	public int mirror(int rotation, int fullTurn) {
		int half = fullTurn / 2;
		int normalized = rotation > half ? rotation - fullTurn : rotation;

		return switch (this) {
			case LEFT_RIGHT -> (half - normalized + fullTurn) % fullTurn;
			case FRONT_BACK -> (fullTurn - normalized) % fullTurn;
			default -> rotation;
		};
	}

	/**
	 * Возвращает поворот блока, необходимый для компенсации данного отражения
	 * при заданном направлении.
	 *
	 * @param direction направление, для которого вычисляется компенсирующий поворот
	 * @return {@link BlockRotation#CLOCKWISE_180} если отражение применимо к оси направления,
	 *         иначе {@link BlockRotation#NONE}
	 */
	public BlockRotation getRotation(Direction direction) {
		Direction.Axis axis = direction.getAxis();
		boolean isLeftRightOnZ = this == LEFT_RIGHT && axis == Direction.Axis.Z;
		boolean isFrontBackOnX = this == FRONT_BACK && axis == Direction.Axis.X;

		return isLeftRightOnZ || isFrontBackOnX ? BlockRotation.CLOCKWISE_180 : BlockRotation.NONE;
	}

	/**
	 * Применяет зеркальное отражение к направлению.
	 *
	 * @param direction исходное направление
	 * @return отражённое направление, или то же самое если отражение не применимо
	 */
	public Direction apply(Direction direction) {
		if (this == FRONT_BACK && direction.getAxis() == Direction.Axis.X) {
			return direction.getOpposite();
		}

		return this == LEFT_RIGHT && direction.getAxis() == Direction.Axis.Z
			? direction.getOpposite()
			: direction;
	}

	public DirectionTransformation getDirectionTransformation() {
		return directionTransformation;
	}

	public Text getName() {
		return name;
	}

	@Override
	public String asString() {
		return id;
	}
}
