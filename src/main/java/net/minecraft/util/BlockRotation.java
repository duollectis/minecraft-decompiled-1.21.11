package net.minecraft.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Поворот блока при размещении структур.
 * Представляет четыре возможных угла поворота вокруг вертикальной оси Y:
 * 0°, 90°, 180° и 270° по часовой стрелке.
 */
public enum BlockRotation implements StringIdentifiable {
	NONE(0, "none", DirectionTransformation.IDENTITY),
	CLOCKWISE_90(1, "clockwise_90", DirectionTransformation.ROT_90_Y_NEG),
	CLOCKWISE_180(2, "180", DirectionTransformation.ROT_180_FACE_XZ),
	COUNTERCLOCKWISE_90(3, "counterclockwise_90", DirectionTransformation.ROT_90_Y_POS);

	public static final IntFunction<BlockRotation> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
		BlockRotation::getIndex, values(), ValueLists.OutOfBoundsHandling.WRAP
	);
	public static final Codec<BlockRotation> CODEC = StringIdentifiable.createCodec(BlockRotation::values);
	public static final PacketCodec<ByteBuf, BlockRotation> PACKET_CODEC = PacketCodecs.indexed(
		INDEX_MAPPER, BlockRotation::getIndex
	);
	@Deprecated
	public static final Codec<BlockRotation> ENUM_NAME_CODEC = Codecs.enumByName(BlockRotation::valueOf);

	private final int index;
	private final String id;
	private final DirectionTransformation directionTransformation;

	BlockRotation(int index, String id, DirectionTransformation directionTransformation) {
		this.index = index;
		this.id = id;
		this.directionTransformation = directionTransformation;
	}

	/**
	 * Складывает два поворота, возвращая результирующий поворот.
	 * Например, {@code CLOCKWISE_90.rotate(CLOCKWISE_90)} вернёт {@code CLOCKWISE_180}.
	 *
	 * @param rotation поворот, который нужно применить поверх текущего
	 * @return результирующий поворот
	 */
	public BlockRotation rotate(BlockRotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> switch (this) {
				case NONE -> CLOCKWISE_90;
				case CLOCKWISE_90 -> CLOCKWISE_180;
				case CLOCKWISE_180 -> COUNTERCLOCKWISE_90;
				case COUNTERCLOCKWISE_90 -> NONE;
			};
			case CLOCKWISE_180 -> switch (this) {
				case NONE -> CLOCKWISE_180;
				case CLOCKWISE_90 -> COUNTERCLOCKWISE_90;
				case CLOCKWISE_180 -> NONE;
				case COUNTERCLOCKWISE_90 -> CLOCKWISE_90;
			};
			case COUNTERCLOCKWISE_90 -> switch (this) {
				case NONE -> COUNTERCLOCKWISE_90;
				case CLOCKWISE_90 -> NONE;
				case CLOCKWISE_180 -> CLOCKWISE_90;
				case COUNTERCLOCKWISE_90 -> CLOCKWISE_180;
			};
			default -> this;
		};
	}

	public DirectionTransformation getDirectionTransformation() {
		return directionTransformation;
	}

	/**
	 * Применяет поворот к горизонтальному направлению.
	 * Вертикальные направления (UP/DOWN) возвращаются без изменений.
	 *
	 * @param direction исходное направление
	 * @return повёрнутое направление
	 */
	public Direction rotate(Direction direction) {
		if (direction.getAxis() == Direction.Axis.Y) {
			return direction;
		}

		return switch (this) {
			case CLOCKWISE_90 -> direction.rotateYClockwise();
			case CLOCKWISE_180 -> direction.getOpposite();
			case COUNTERCLOCKWISE_90 -> direction.rotateYCounterclockwise();
			default -> direction;
		};
	}

	/**
	 * Применяет поворот к числовому значению угла в диапазоне {@code [0, fullTurn)}.
	 *
	 * @param rotation  исходное значение угла
	 * @param fullTurn  полный оборот (например, 16 для блоков с 16 состояниями поворота)
	 * @return повёрнутое значение угла
	 */
	public int rotate(int rotation, int fullTurn) {
		return switch (this) {
			case CLOCKWISE_90 -> (rotation + fullTurn / 4) % fullTurn;
			case CLOCKWISE_180 -> (rotation + fullTurn / 2) % fullTurn;
			case COUNTERCLOCKWISE_90 -> (rotation + fullTurn * 3 / 4) % fullTurn;
			default -> rotation;
		};
	}

	/** @return случайный поворот из четырёх возможных */
	public static BlockRotation random(Random random) {
		return Util.getRandom(values(), random);
	}

	/** @return список всех четырёх поворотов в случайном порядке */
	public static List<BlockRotation> randomRotationOrder(Random random) {
		return Util.copyShuffled(values(), random);
	}

	@Override
	public String asString() {
		return id;
	}

	private int getIndex() {
		return index;
	}
}
