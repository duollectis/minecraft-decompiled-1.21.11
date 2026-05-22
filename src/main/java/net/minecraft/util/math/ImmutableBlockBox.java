package net.minecraft.util.math;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.Iterator;

/**
 * Иммутабельный ограничивающий прямоугольник в блочных координатах.
 * В отличие от {@link BlockBox}, не допускает мутации и реализует {@link Iterable}
 * для перебора всех блочных позиций внутри.
 */
public record ImmutableBlockBox(BlockPos min, BlockPos max) implements Iterable<BlockPos> {

	public static final PacketCodec<ByteBuf, ImmutableBlockBox> PACKET_CODEC = new PacketCodec<>() {
		@Override
		public ImmutableBlockBox decode(ByteBuf byteBuf) {
			return new ImmutableBlockBox(
					PacketByteBuf.readBlockPos(byteBuf),
					PacketByteBuf.readBlockPos(byteBuf)
			);
		}

		@Override
		public void encode(ByteBuf byteBuf, ImmutableBlockBox box) {
			PacketByteBuf.writeBlockPos(byteBuf, box.min());
			PacketByteBuf.writeBlockPos(byteBuf, box.max());
		}
	};

	public ImmutableBlockBox(final BlockPos min, final BlockPos max) {
		this.min = BlockPos.min(min, max);
		this.max = BlockPos.max(min, max);
	}

	public static ImmutableBlockBox of(BlockPos pos) {
		return new ImmutableBlockBox(pos, pos);
	}

	public static ImmutableBlockBox of(BlockPos first, BlockPos second) {
		return new ImmutableBlockBox(first, second);
	}

	public ImmutableBlockBox encompass(BlockPos pos) {
		return new ImmutableBlockBox(BlockPos.min(min, pos), BlockPos.max(max, pos));
	}

	public boolean isSingleBlock() {
		return min.equals(max);
	}

	public boolean includes(BlockPos pos) {
		return pos.getX() >= min.getX()
				&& pos.getY() >= min.getY()
				&& pos.getZ() >= min.getZ()
				&& pos.getX() <= max.getX()
				&& pos.getY() <= max.getY()
				&& pos.getZ() <= max.getZ();
	}

	public Box enclosingBox() {
		return Box.enclosing(min, max);
	}

	@Override
	public Iterator<BlockPos> iterator() {
		return BlockPos.iterate(min, max).iterator();
	}

	public int getBlockCountX() {
		return max.getX() - min.getX() + 1;
	}

	public int getBlockCountY() {
		return max.getY() - min.getY() + 1;
	}

	public int getBlockCountZ() {
		return max.getZ() - min.getZ() + 1;
	}

	public ImmutableBlockBox expand(Direction direction, int offset) {
		if (offset == 0) {
			return this;
		}

		return direction.getDirection() == Direction.AxisDirection.POSITIVE
				? of(min, BlockPos.max(min, max.offset(direction, offset)))
				: of(BlockPos.min(min.offset(direction, offset), max), max);
	}

	public ImmutableBlockBox move(Direction direction, int offset) {
		return offset == 0
				? this
				: new ImmutableBlockBox(min.offset(direction, offset), max.offset(direction, offset));
	}

	public ImmutableBlockBox move(Vec3i offset) {
		return new ImmutableBlockBox(min.add(offset), max.add(offset));
	}
}
