package net.minecraft.test;

import net.minecraft.text.Text;

/**
 * Исключение, выбрасываемое когда тест превысил максимально допустимое количество тиков.
 */
public class TickLimitExceededException extends TestException {

	protected final Text message;

	public TickLimitExceededException(Text message) {
		super(message.getString());
		this.message = message;
	}

	@Override
	public Text getText() {
		return message;
	}
}
