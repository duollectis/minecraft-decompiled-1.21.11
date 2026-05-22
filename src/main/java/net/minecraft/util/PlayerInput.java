package net.minecraft.util;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Снимок состояния клавиш управления игрока в один игровой тик.
 * Сериализуется в один байт для эффективной сетевой передачи:
 * каждый бит соответствует одной клавише управления.
 */
public record PlayerInput(
	boolean forward,
	boolean backward,
	boolean left,
	boolean right,
	boolean jump,
	boolean sneak,
	boolean sprint
) {

	private static final byte BIT_FORWARD = 1;
	private static final byte BIT_BACKWARD = 2;
	private static final byte BIT_LEFT = 4;
	private static final byte BIT_RIGHT = 8;
	private static final byte BIT_JUMP = 16;
	private static final byte BIT_SNEAK = 32;
	private static final byte BIT_SPRINT = 64;

	public static final PlayerInput DEFAULT = new PlayerInput(false, false, false, false, false, false, false);

	public static final PacketCodec<PacketByteBuf, PlayerInput> PACKET_CODEC = new PacketCodec<>() {
		@Override
		public void encode(PacketByteBuf buf, PlayerInput input) {
			byte flags = 0;
			flags = (byte) (flags | (input.forward() ? BIT_FORWARD : 0));
			flags = (byte) (flags | (input.backward() ? BIT_BACKWARD : 0));
			flags = (byte) (flags | (input.left() ? BIT_LEFT : 0));
			flags = (byte) (flags | (input.right() ? BIT_RIGHT : 0));
			flags = (byte) (flags | (input.jump() ? BIT_JUMP : 0));
			flags = (byte) (flags | (input.sneak() ? BIT_SNEAK : 0));
			flags = (byte) (flags | (input.sprint() ? BIT_SPRINT : 0));
			buf.writeByte(flags);
		}

		@Override
		public PlayerInput decode(PacketByteBuf buf) {
			byte flags = buf.readByte();
			return new PlayerInput(
				(flags & BIT_FORWARD) != 0,
				(flags & BIT_BACKWARD) != 0,
				(flags & BIT_LEFT) != 0,
				(flags & BIT_RIGHT) != 0,
				(flags & BIT_JUMP) != 0,
				(flags & BIT_SNEAK) != 0,
				(flags & BIT_SPRINT) != 0
			);
		}
	};
}
