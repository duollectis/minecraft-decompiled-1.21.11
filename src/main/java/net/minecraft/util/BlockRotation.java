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
 * {@code BlockRotation}.
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
	public static final PacketCodec<ByteBuf, BlockRotation>
			PACKET_CODEC =
			PacketCodecs.indexed(INDEX_MAPPER, BlockRotation::getIndex);
	@Deprecated
	public static final Codec<BlockRotation> ENUM_NAME_CODEC = Codecs.enumByName(BlockRotation::valueOf);
	private final int index;
	private final String id;
	private final DirectionTransformation directionTransformation;

	private BlockRotation(final int index, final String id, final DirectionTransformation directionTransformation) {
		this.index = index;
		this.id = id;
		this.directionTransformation = directionTransformation;
	}

	/**
	 * Rotate.
	 *
	 * @param rotation rotation
	 *
	 * @return BlockRotation — результат операции
	 */
	public BlockRotation rotate(BlockRotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> {
				switch (this) {
					case NONE:
						yield CLOCKWISE_90;
					case CLOCKWISE_90:
						yield CLOCKWISE_180;
					case CLOCKWISE_180:
						yield COUNTERCLOCKWISE_90;
					case COUNTERCLOCKWISE_90:
						yield NONE;
					default:
						throw new MatchException(null, null);
				}
			}
			case CLOCKWISE_180 -> {
				switch (this) {
					case NONE:
						yield CLOCKWISE_180;
					case CLOCKWISE_90:
						yield COUNTERCLOCKWISE_90;
					case CLOCKWISE_180:
						yield NONE;
					case COUNTERCLOCKWISE_90:
						yield CLOCKWISE_90;
					default:
						throw new MatchException(null, null);
				}
			}
			case COUNTERCLOCKWISE_90 -> {
				switch (this) {
					case NONE:
						yield COUNTERCLOCKWISE_90;
					case CLOCKWISE_90:
						yield NONE;
					case CLOCKWISE_180:
						yield CLOCKWISE_90;
					case COUNTERCLOCKWISE_90:
						yield CLOCKWISE_180;
					default:
						throw new MatchException(null, null);
				}
			}
			default -> this;
		};
	}

	public DirectionTransformation getDirectionTransformation() {
		return this.directionTransformation;
	}

	/**
	 * Rotate.
	 *
	 * @param direction direction
	 *
	 * @return Direction — результат операции
	 */
	public Direction rotate(Direction direction) {
		if (direction.getAxis() == Direction.Axis.Y) {
			return direction;
		}
		else {
			return switch (this) {
				case CLOCKWISE_90 -> direction.rotateYClockwise();
				case CLOCKWISE_180 -> direction.getOpposite();
				case COUNTERCLOCKWISE_90 -> direction.rotateYCounterclockwise();
				default -> direction;
			};
		}
	}

	/**
	 * Rotate.
	 *
	 * @param rotation rotation
	 * @param fullTurn full turn
	 *
	 * @return int — результат операции
	 */
	public int rotate(int rotation, int fullTurn) {
		return switch (this) {
			case CLOCKWISE_90 -> (rotation + fullTurn / 4) % fullTurn;
			case CLOCKWISE_180 -> (rotation + fullTurn / 2) % fullTurn;
			case COUNTERCLOCKWISE_90 -> (rotation + fullTurn * 3 / 4) % fullTurn;
			default -> rotation;
		};
	}

	/**
	 * Random.
	 *
	 * @param random random
	 *
	 * @return BlockRotation — результат операции
	 */
	public static BlockRotation random(Random random) {
		return Util.getRandom(values(), random);
	}

	/**
	 * Random rotation order.
	 *
	 * @param random random
	 *
	 * @return List — результат операции
	 */
	public static List<BlockRotation> randomRotationOrder(Random random) {
		return Util.copyShuffled(values(), random);
	}

	@Override
	public String asString() {
		return this.id;
	}

	private int getIndex() {
		return this.index;
	}
}
