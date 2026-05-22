package net.minecraft.test;

import com.google.common.base.MoreObjects;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.TestInstanceBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Слушатель тестов, обновляющий блок-сущность структуры и рассылающий
 * сообщения игрокам о результатах. Поддерживает флакующие тесты и повторные прогоны.
 */
class StructureTestListener implements TestListener {

	private int attempt = 0;
	private int successes = 0;

	@Override
	public void onStarted(GameTestState test) {
		attempt++;
	}

	@Override
	public void onPassed(GameTestState test, TestRunContext context) {
		successes++;
		if (test.getTestAttemptConfig().needsMultipleAttempts()) {
			retry(test, context, true);
			return;
		}

		if (!test.isFlaky()) {
			passTest(
					test,
					test.getId() + " passed! (" + test.getElapsedMilliseconds() + "ms / " + test.getTick() + "gameticks)"
			);
			return;
		}

		if (successes >= test.getRequiredSuccesses()) {
			passTest(test, test + " passed " + successes + " times of " + attempt + " attempts.");
		} else {
			sendMessageToAllPlayers(
					test.getWorld(),
					Formatting.GREEN,
					"Flaky test " + test + " succeeded, attempt: " + attempt + " successes: " + successes
			);
			context.retry(test);
		}
	}

	@Override
	public void onFailed(GameTestState test, TestRunContext context) {
		if (!test.isFlaky()) {
			failTest(test, test.getThrowable());
			if (test.getTestAttemptConfig().needsMultipleAttempts()) {
				retry(test, context, false);
			}
			return;
		}

		TestInstance testInstance = test.getInstance();
		String message = "Flaky test " + test + " failed, attempt: " + attempt + "/" + testInstance.getMaxAttempts();
		if (testInstance.getRequiredSuccesses() > 1) {
			message += ", successes: " + successes + " (" + testInstance.getRequiredSuccesses() + " required)";
		}

		sendMessageToAllPlayers(test.getWorld(), Formatting.YELLOW, message);

		if (test.getMaxAttempts() - attempt + successes >= test.getRequiredSuccesses()) {
			context.retry(test);
		} else {
			failTest(test, new NotEnoughSuccessesError(attempt, successes, test));
		}
	}

	@Override
	public void onRetry(GameTestState lastState, GameTestState nextState, TestRunContext context) {
		nextState.addListener(this);
	}

	public static void passTest(GameTestState test, String output) {
		getTestInstanceBlockEntity(test).ifPresent(TestInstanceBlockEntity::setFinished);
		finishPassedTest(test, output);
	}

	protected static void failTest(GameTestState test, Throwable output) {
		Text errorText = output instanceof GameTestException gameTestException
				? gameTestException.getText()
				: Text.literal(Util.getInnermostMessage(output));

		getTestInstanceBlockEntity(test).ifPresent(entity -> entity.setErrorMessage(errorText));
		finishFailedTest(test, output);
	}

	protected static void finishFailedTest(GameTestState test, Throwable output) {
		String causeInfo = output.getCause() == null
				? ""
				: " cause: " + Util.getInnermostMessage(output.getCause());
		String fullMessage = (test.isRequired() ? "" : "(optional) ")
				+ test.getId() + " failed! " + output.getMessage() + causeInfo;

		Formatting formatting = test.isRequired() ? Formatting.RED : Formatting.YELLOW;
		sendMessageToAllPlayers(test.getWorld(), formatting, fullMessage);

		Throwable rootCause = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(output), output);
		if (rootCause instanceof PositionedException positionedException) {
			test.getTestInstanceBlockEntity()
					.addError(positionedException.getPos(), positionedException.getDebugMessage());
		}

		TestFailureLogger.failTest(test);
	}

	protected static void sendMessageToAllPlayers(ServerWorld world, Formatting formatting, String message) {
		world.getPlayers(player -> true)
				.forEach(player -> player.sendMessage(Text.literal(message).formatted(formatting)));
	}

	private void retry(GameTestState state, TestRunContext context, boolean lastPassed) {
		TestAttemptConfig config = state.getTestAttemptConfig();
		String stats = String.format(
				Locale.ROOT,
				"[Run: %4d, Ok: %4d, Fail: %4d",
				attempt,
				successes,
				attempt - successes
		);

		if (!config.isDisabled()) {
			stats += String.format(Locale.ROOT, ", Left: %4d", config.numberOfTries() - attempt);
		}

		stats += "]";
		String summary = state.getId() + " " + (lastPassed ? "passed" : "failed") + "! "
				+ state.getElapsedMilliseconds() + "ms";
		String line = String.format(Locale.ROOT, "%-53s%s", stats, summary);

		if (lastPassed) {
			passTest(state, line);
		} else {
			sendMessageToAllPlayers(state.getWorld(), Formatting.RED, line);
		}

		if (config.shouldTestAgain(attempt, successes)) {
			context.retry(state);
		}
	}

	private static void finishPassedTest(GameTestState test, String output) {
		sendMessageToAllPlayers(test.getWorld(), Formatting.GREEN, output);
		TestFailureLogger.passTest(test);
	}

	private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(GameTestState state) {
		ServerWorld world = state.getWorld();
		return Optional.ofNullable(state.getPos())
				.flatMap(pos -> world.getBlockEntity(pos, BlockEntityType.TEST_INSTANCE_BLOCK));
	}
}
