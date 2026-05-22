package net.minecraft.command;

/**
 * Фрейм выполнения команды, хранящий глубину стека, потребителя возвращаемого значения
 * и управляющий объект для досрочного прерывания цепочки.
 */
public record Frame(int depth, ReturnValueConsumer returnValueConsumer, Frame.Control frameControl) {

	public void succeed(int returnValue) {
		returnValueConsumer.onSuccess(returnValue);
	}

	public void fail() {
		returnValueConsumer.onFailure();
	}

	public void doReturn() {
		frameControl.discard();
	}

	/**
	 * Управляющий интерфейс для сброса (прерывания) текущего фрейма.
	 */
	@FunctionalInterface
	public interface Control {

		void discard();
	}
}
