package net.minecraft.network.message;

/**
 * Запись acknowledged message.
 */
public record AcknowledgedMessage(MessageSignatureData signature, boolean pending) {

	/**
	 * Unmark as pending.
	 *
	 * @return AcknowledgedMessage — результат операции
	 */
	public AcknowledgedMessage unmarkAsPending() {
		return this.pending ? new AcknowledgedMessage(this.signature, false) : this;
	}
}
