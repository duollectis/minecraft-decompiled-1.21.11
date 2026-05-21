package net.minecraft.network.message;

public record AcknowledgedMessage(MessageSignatureData signature, boolean pending) {

	public AcknowledgedMessage unmarkAsPending() {
		return this.pending ? new AcknowledgedMessage(this.signature, false) : this;
	}
}
