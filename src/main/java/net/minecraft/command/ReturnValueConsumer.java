package net.minecraft.command;

/**
 * Потребитель возвращаемого значения команды.
 * Вызывается при успешном завершении или провале выполнения команды.
 */
@FunctionalInterface
public interface ReturnValueConsumer {

	ReturnValueConsumer EMPTY = new ReturnValueConsumer() {
		@Override
		public void onResult(boolean successful, int returnValue) {
		}

		@Override
		public String toString() {
			return "<empty>";
		}
	};

	void onResult(boolean successful, int returnValue);

	default void onSuccess(int returnValue) {
		onResult(true, returnValue);
	}

	default void onFailure() {
		onResult(false, 0);
	}

	/**
	 * Объединяет двух потребителей в один: оба получат уведомление об одном результате.
	 * Если один из них {@link #EMPTY}, возвращается другой без создания обёртки.
	 */
	static ReturnValueConsumer chain(ReturnValueConsumer first, ReturnValueConsumer second) {
		if (first == EMPTY) {
			return second;
		}

		return second == EMPTY
				? first
				: (successful, returnValue) -> {
					first.onResult(successful, returnValue);
					second.onResult(successful, returnValue);
				};
	}
}
