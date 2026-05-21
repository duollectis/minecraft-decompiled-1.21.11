package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * {@code SideChainPart}.
 */
public enum SideChainPart implements StringIdentifiable {
	UNCONNECTED("unconnected"),
	RIGHT("right"),
	CENTER("center"),
	LEFT("left");

	private final String id;

	private SideChainPart(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return this.asString();
	}

	@Override
	public String asString() {
		return this.id;
	}

	public boolean isConnected() {
		return this != UNCONNECTED;
	}

	public boolean isCenterOr(SideChainPart sideChainPart) {
		return this == CENTER || this == sideChainPart;
	}

	public boolean isNotCenter() {
		return this != CENTER;
	}

	/**
	 * Connect to right.
	 *
	 * @return SideChainPart — результат операции
	 */
	public SideChainPart connectToRight() {
		return switch (this) {
			case UNCONNECTED, LEFT -> LEFT;
			case RIGHT, CENTER -> CENTER;
		};
	}

	/**
	 * Connect to left.
	 *
	 * @return SideChainPart — результат операции
	 */
	public SideChainPart connectToLeft() {
		return switch (this) {
			case UNCONNECTED, RIGHT -> RIGHT;
			case CENTER, LEFT -> CENTER;
		};
	}

	/**
	 * Disconnect from right.
	 *
	 * @return SideChainPart — результат операции
	 */
	public SideChainPart disconnectFromRight() {
		return switch (this) {
			case UNCONNECTED, LEFT -> UNCONNECTED;
			case RIGHT, CENTER -> RIGHT;
		};
	}

	/**
	 * Disconnect from left.
	 *
	 * @return SideChainPart — результат операции
	 */
	public SideChainPart disconnectFromLeft() {
		return switch (this) {
			case UNCONNECTED, RIGHT -> UNCONNECTED;
			case CENTER, LEFT -> LEFT;
		};
	}
}
