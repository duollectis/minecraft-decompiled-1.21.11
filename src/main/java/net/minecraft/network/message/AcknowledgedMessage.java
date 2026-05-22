package net.minecraft.network.message;

/**
 * Подтверждённое сообщение в скользящем окне последних просмотренных сообщений.
 * Флаг {@code pending} означает, что сообщение ещё не было явно подтверждено клиентом.
 */
public record AcknowledgedMessage(MessageSignatureData signature, boolean pending) {

	/**
	 * Снимает флаг ожидания подтверждения, если он был установлен.
	 * Используется при сборке снимка окна последних просмотренных сообщений.
	 *
	 * @return новый экземпляр без флага {@code pending}, либо {@code this} если флаг уже снят
	 */
	public AcknowledgedMessage unmarkAsPending() {
		return pending
				? new AcknowledgedMessage(signature, false)
				: this;
	}
}
