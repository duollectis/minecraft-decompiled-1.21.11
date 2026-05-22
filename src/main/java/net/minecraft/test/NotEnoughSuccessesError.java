package net.minecraft.test;

/**
 * Ошибка, возникающая когда флакующий тест не набрал достаточного количества
 * успешных прогонов за отведённое число попыток.
 */
class NotEnoughSuccessesError extends Throwable {

	public NotEnoughSuccessesError(int attempts, int successes, GameTestState test) {
		super(
				"Not enough successes: "
						+ successes
						+ " out of "
						+ attempts
						+ " attempts. Required successes: "
						+ test.getRequiredSuccesses()
						+ ". max attempts: "
						+ test.getMaxAttempts()
						+ ".",
				test.getThrowable()
		);
	}
}
