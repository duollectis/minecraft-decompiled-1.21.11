package net.minecraft.util.math;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Iterator;

/**
 * {@code ImmutableBlockBox}.
 */
public record ImmutableBlockBox(BlockPos min, BlockPos max) implements Iterable<BlockPos> {

	public static final PacketCodec<ByteBuf, ImmutableBlockBox>
			PACKET_CODEC =
			new PacketCodec<ByteBuf, ImmutableBlockBox>() {
				/**
				 * Decode.
				 *
				 * @param byteBuf byte buf
				 *
				 * @return ImmutableBlockBox — результат операции
				 */
				public ImmutableBlockBox decode(ByteBuf byteBuf) {
					return new ImmutableBlockBox(
							PacketByteBuf.readBlockPos(byteBuf),
							PacketByteBuf.readBlockPos(byteBuf)
					);
				}

				/**
				 * Encode.
				 *
				 * @param byteBuf byte buf
				 * @param immutableBlockBox immutable block box
				 */
				public void encode(ByteBuf byteBuf, ImmutableBlockBox immutableBlockBox) {
					PacketByteBuf.writeBlockPos(byteBuf, immutableBlockBox.min());
					PacketByteBuf.writeBlockPos(byteBuf, immutableBlockBox.max());
				}
			};

	public ImmutableBlockBox(final BlockPos min, final BlockPos max) {
		this.min = BlockPos.min(min, max);
		this.max = BlockPos.max(min, max);
	}

	/**
	 * Of.
	 *
	 * @param pos pos
	 *
	 * @return ImmutableBlockBox — результат операции
	 */
	public static ImmutableBlockBox of(BlockPos pos) {
		return new ImmutableBlockBox(pos, pos);
	}

	/**
	 * Of.
	 *
	 * @param first first
	 * @param second second
	 *
	 * @return ImmutableBlockBox — результат операции
	 */
	public static ImmutableBlockBox of(BlockPos first, BlockPos second) {
		return new ImmutableBlockBox(first, second);
	}

	/**
	 * Encompass.
	 *
	 * @param pos pos
	 *
	 * @return ImmutableBlockBox — результат операции
	 */
	public ImmutableBlockBox encompass(BlockPos pos) {
		return new ImmutableBlockBox(BlockPos.min(this.min, pos), BlockPos.max(this.max, pos));
	}

	public boolean isSingleBlock() {
		return this.min.equals(this.max);
	}

	/**
	 * Includes.
	 *
	 * @param pos pos
	 *
	 * @return boolean — результат операции
	 */
	public boolean includes(BlockPos pos) {
		return pos.getX() >= this.min.getX()
				&& pos.getY() >= this.min.getY()
				&& pos.getZ() >= this.min.getZ()
				&& pos.getX() <= this.max.getX()
				&& pos.getY() <= this.max.getY()
				&& pos.getZ() <= this.max.getZ();
	}

	/**
	 * Enclosing box.
	 *
	 * @return Box — результат операции
	 */
	public Box enclosingBox() {
		return Box.enclosing(this.min, this.max);
	}

	@Override
	public Iterator<BlockPos> iterator() {
		return BlockPos.iterate(this.min, this.max).iterator();
	}

	public int getBlockCountX() {
		return this.max.getX() - this.min.getX() + 1;
	}

	public int getBlockCountY() {
		return this.max.getY() - this.min.getY() + 1;
	}

	public int getBlockCountZ() {
		return this.max.getZ() - this.min.getZ() + 1;
	}

	/**
	 * Expand.
	 *
	 * @param direction direction
	 * @param offset offset
	 *
	 * @return ImmutableBlockBox — результат операции
	 */
	public ImmutableBlockBox expand(Direction direction, int offset) {
		if (offset == 0) {
			return this;
		}
		else {
			return direction.getDirection() == Direction.AxisDirection.POSITIVE
			       ? of(this.min, BlockPos.max(this.min, this.max.offset(direction, offset)))
			       : of(BlockPos.min(this.min.offset(direction, offset), this.max), this.max);
		}
	}

	/**
	 * Move.
	 *
	 * @param direction direction
	 * @param offset offset
	 *
	 * @return ImmutableBlockBox — результат операции
	 */
	public ImmutableBlockBox move(Direction direction, int offset) {
		return offset == 0 ? this : new ImmutableBlockBox(
				this.min.offset(direction, offset),
				this.max.offset(direction, offset)
		);
	}

	/**
	 * Move.
	 *
	 * @param offset offset
	 *
	 * @return ImmutableBlockBox — результат операции
	 */
	public ImmutableBlockBox move(Vec3i offset) {
		return new ImmutableBlockBox(this.min.add(offset), this.max.add(offset));
	}
}
