package net.minecraft.test;

import net.minecraft.text.Text;

/**
 * Исключение, возникающее в ходе выполнения игрового теста.
 * Содержит текстовое сообщение об ошибке и номер тика, на котором произошёл сбой.
 */
public class GameTestException extends TestException {

	protected final Text message;
	protected final int tick;

	public GameTestException(Text message, int tick) {
		super(message.getString());
		this.message = message;
		this.tick = tick;
	}

	@Override
	public Text getText() {
		return Text.translatable("test.error.tick", message, tick);
	}

	@Override
	public String getMessage() {
		return getText().getString();
	}
}
