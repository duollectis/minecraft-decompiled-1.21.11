package net.minecraft.network.packet.s2c.play;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Перечисление position flag.
 */
public enum PositionFlag {
	X(0),
	Y(1),
	Z(2),
	Y_ROT(3),
	X_ROT(4),
	DELTA_X(5),
	DELTA_Y(6),
	DELTA_Z(7),
	ROTATE_DELTA(8);

	public static final Set<PositionFlag> VALUES = Set.of(values());
	public static final Set<PositionFlag> ROT = Set.of(X_ROT, Y_ROT);
	public static final Set<PositionFlag> DELTA = Set.of(DELTA_X, DELTA_Y, DELTA_Z, ROTATE_DELTA);
	public static final PacketCodec<ByteBuf, Set<PositionFlag>>
			PACKET_CODEC =
			PacketCodecs.INTEGER.xmap(PositionFlag::getFlags, PositionFlag::getBitfield);
	private final int shift;

	@SafeVarargs
	/**
	 * Combine.
	 *
	 * @param sets sets
	 *
	 * @return Set — результат операции
	 */
	public static Set<PositionFlag> combine(Set<PositionFlag>... sets) {
		HashSet<PositionFlag> hashSet = new HashSet<>();

		for (Set<PositionFlag> set : sets) {
			hashSet.addAll(set);
		}

		return hashSet;
	}

	/**
	 * Of rot.
	 *
	 * @param yRot y rot
	 * @param xRot x rot
	 *
	 * @return Set — результат операции
	 */
	public static Set<PositionFlag> ofRot(boolean yRot, boolean xRot) {
		Set<PositionFlag> set = EnumSet.noneOf(PositionFlag.class);
		if (yRot) {
			set.add(Y_ROT);
		}

		if (xRot) {
			set.add(X_ROT);
		}

		return set;
	}

	/**
	 * Of pos.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return Set — результат операции
	 */
	public static Set<PositionFlag> ofPos(boolean x, boolean y, boolean z) {
		Set<PositionFlag> set = EnumSet.noneOf(PositionFlag.class);
		if (x) {
			set.add(X);
		}

		if (y) {
			set.add(Y);
		}

		if (z) {
			set.add(Z);
		}

		return set;
	}

	/**
	 * Of delta pos.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 *
	 * @return Set — результат операции
	 */
	public static Set<PositionFlag> ofDeltaPos(boolean x, boolean y, boolean z) {
		Set<PositionFlag> set = EnumSet.noneOf(PositionFlag.class);
		if (x) {
			set.add(DELTA_X);
		}

		if (y) {
			set.add(DELTA_Y);
		}

		if (z) {
			set.add(DELTA_Z);
		}

		return set;
	}

	private PositionFlag(final int shift) {
		this.shift = shift;
	}

	private int getMask() {
		return 1 << this.shift;
	}

	private boolean isSet(int mask) {
		return (mask & this.getMask()) == this.getMask();
	}

	public static Set<PositionFlag> getFlags(int mask) {
		Set<PositionFlag> set = EnumSet.noneOf(PositionFlag.class);

		for (PositionFlag positionFlag : values()) {
			if (positionFlag.isSet(mask)) {
				set.add(positionFlag);
			}
		}

		return set;
	}

	public static int getBitfield(Set<PositionFlag> flags) {
		int i = 0;

		for (PositionFlag positionFlag : flags) {
			i |= positionFlag.getMask();
		}

		return i;
	}
}
