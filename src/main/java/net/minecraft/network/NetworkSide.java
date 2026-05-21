package net.minecraft.network;

/**
 * Сторона сетевого соединения — определяет направление передачи пакетов.
 * <p>{@link #SERVERBOUND} означает, что пакет идёт от клиента к серверу,
 * {@link #CLIENTBOUND} — от сервера к клиенту.
 */
public enum NetworkSide {

	SERVERBOUND("serverbound"),
	CLIENTBOUND("clientbound");

	private final String name;

	NetworkSide(String name) {
		this.name = name;
	}

	/**
	 * Возвращает противоположную сторону соединения.
	 *
	 * @return {@link #SERVERBOUND} для {@link #CLIENTBOUND} и наоборот
	 */
	public NetworkSide getOpposite() {
		return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
	}

	/**
	 * Возвращает строковый идентификатор стороны.
	 *
	 * @return строковое имя стороны (например, {@code "serverbound"})
	 */
	public String getName() {
		return name;
	}
}
