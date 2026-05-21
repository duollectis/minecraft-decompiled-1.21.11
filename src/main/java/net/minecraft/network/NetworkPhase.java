package net.minecraft.network;

/**
 * Фаза (состояние) сетевого протокола Minecraft.
 * <p>Определяет текущий этап жизненного цикла соединения между клиентом и сервером.
 * Переходы между фазами происходят в строго определённом порядке:
 * {@link #HANDSHAKING} → {@link #LOGIN} → {@link #CONFIGURATION} → {@link #PLAY}.
 */
public enum NetworkPhase {

	HANDSHAKING("handshake"),
	PLAY("play"),
	STATUS("status"),
	LOGIN("login"),
	CONFIGURATION("configuration");

	private final String id;

	NetworkPhase(String id) {
		this.id = id;
	}

	/**
	 * Возвращает строковый идентификатор фазы протокола.
	 *
	 * @return строковый идентификатор (например, {@code "play"})
	 */
	public String getId() {
		return id;
	}
}
