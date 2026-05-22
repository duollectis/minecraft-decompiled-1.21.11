package net.minecraft.block.enums;

import net.minecraft.util.StringIdentifiable;

/**
 * Состояние подключения боковой цепи блока (например, цепочки блоков команд).
 * Описывает, к каким соседям подключён данный блок в цепочке:
 * только к правому, только к левому, к обоим или ни к одному.
 */
public enum SideChainPart implements StringIdentifiable {
	/** Блок не подключён ни к одному соседу. */
	UNCONNECTED("unconnected"),
	/** Блок подключён только к правому соседу. */
	RIGHT("right"),
	/** Блок подключён к обоим соседям (центральный элемент цепи). */
	CENTER("center"),
	/** Блок подключён только к левому соседу. */
	LEFT("left");

	private final String id;

	SideChainPart(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return asString();
	}

	@Override
	public String asString() {
		return id;
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
	 * Возвращает новое состояние после добавления подключения к правому соседу.
	 * Если уже подключён к левому — становится {@link #CENTER}, иначе — {@link #LEFT}.
	 */
	public SideChainPart connectToRight() {
		return switch (this) {
			case UNCONNECTED, LEFT -> LEFT;
			case RIGHT, CENTER -> CENTER;
		};
	}

	/**
	 * Возвращает новое состояние после добавления подключения к левому соседу.
	 * Если уже подключён к правому — становится {@link #CENTER}, иначе — {@link #RIGHT}.
	 */
	public SideChainPart connectToLeft() {
		return switch (this) {
			case UNCONNECTED, RIGHT -> RIGHT;
			case CENTER, LEFT -> CENTER;
		};
	}

	/**
	 * Возвращает новое состояние после удаления подключения к правому соседу.
	 * Если был {@link #CENTER} — становится {@link #RIGHT}, иначе — {@link #UNCONNECTED}.
	 */
	public SideChainPart disconnectFromRight() {
		return switch (this) {
			case UNCONNECTED, LEFT -> UNCONNECTED;
			case RIGHT, CENTER -> RIGHT;
		};
	}

	/**
	 * Возвращает новое состояние после удаления подключения к левому соседу.
	 * Если был {@link #CENTER} — становится {@link #LEFT}, иначе — {@link #UNCONNECTED}.
	 */
	public SideChainPart disconnectFromLeft() {
		return switch (this) {
			case UNCONNECTED, RIGHT -> UNCONNECTED;
			case CENTER, LEFT -> LEFT;
		};
	}
}
