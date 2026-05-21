package net.minecraft.server.function;

import net.minecraft.text.Text;

/**
 * {@code MacroException}.
 */
public class MacroException extends Exception {

	private final Text message;

	public MacroException(Text message) {
		super(message.getString());
		this.message = message;
	}

	public Text getTextMessage() {
		return this.message;
	}
}
