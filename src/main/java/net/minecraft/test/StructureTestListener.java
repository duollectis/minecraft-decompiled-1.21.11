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
 * {@code StructureTestListener}.
 */
class StructureTestListener implements TestListener {

	private int attempt = 0;
	private int successes = 0;

	public StructureTestListener() {
	}

	@Override
	public void onStarted(GameTestState test) {
		this.attempt++;
	}

	private void retry(GameTestState state, TestRunContext context, boolean lastPassed) {
		TestAttemptConfig testAttemptConfig = state.getTestAttemptConfig();
		String
				string =
				String.format(
						Locale.ROOT,
						"[Run: %4d, Ok: %4d, Fail: %4d",
						this.attempt,
						this.successes,
						this.attempt - this.successes
				);
		if (!testAttemptConfig.isDisabled()) {
			string =
					string + String.format(
							Locale.ROOT,
							", Left: %4d",
							testAttemptConfig.numberOfTries() - this.attempt
					);
		}

		string = string + "]";
		String
				string2 =
				state.getId() + " " + (lastPassed ? "passed" : "failed") + "! " + state.getElapsedMilliseconds() + "ms";
		String string3 = String.format(Locale.ROOT, "%-53s%s", string, string2);
		if (lastPassed) {
			passTest(state, string3);
		}
		else {
			sendMessageToAllPlayers(state.getWorld(), Formatting.RED, string3);
		}

		if (testAttemptConfig.shouldTestAgain(this.attempt, this.successes)) {
			context.retry(state);
		}
	}

	@Override
	public void onPassed(GameTestState test, TestRunContext context) {
		this.successes++;
		if (test.getTestAttemptConfig().needsMultipleAttempts()) {
			this.retry(test, context, true);
		}
		else if (!test.isFlaky()) {
			passTest(
					test,
					test.getId() + " passed! (" + test.getElapsedMilliseconds() + "ms / " + test.getTick()
							+ "gameticks)"
			);
		}
		else {
			if (this.successes >= test.getRequiredSuccesses()) {
				passTest(test, test + " passed " + this.successes + " times of " + this.attempt + " attempts.");
			}
			else {
				sendMessageToAllPlayers(
						test.getWorld(),
						Formatting.GREEN,
						"Flaky test " + test + " succeeded, attempt: " + this.attempt + " successes: " + this.successes
				);
				context.retry(test);
			}
		}
	}

	@Override
	public void onFailed(GameTestState test, TestRunContext context) {
		if (!test.isFlaky()) {
			failTest(test, test.getThrowable());
			if (test.getTestAttemptConfig().needsMultipleAttempts()) {
				this.retry(test, context, false);
			}
		}
		else {
			TestInstance testInstance = test.getInstance();
			String
					string =
					"Flaky test " + test + " failed, attempt: " + this.attempt + "/" + testInstance.getMaxAttempts();
			if (testInstance.getRequiredSuccesses() > 1) {
				string =
						string + ", successes: " + this.successes + " (" + testInstance.getRequiredSuccesses()
								+ " required)";
			}

			sendMessageToAllPlayers(test.getWorld(), Formatting.YELLOW, string);
			if (test.getMaxAttempts() - this.attempt + this.successes >= test.getRequiredSuccesses()) {
				context.retry(test);
			}
			else {
				failTest(test, new NotEnoughSuccessesError(this.attempt, this.successes, test));
			}
		}
	}

	@Override
	public void onRetry(GameTestState lastState, GameTestState nextState, TestRunContext context) {
		nextState.addListener(this);
	}

	public static void passTest(GameTestState test, String output) {
		getTestInstanceBlockEntity(test).ifPresent(testInstanceBlockEntity -> testInstanceBlockEntity.setFinished());
		finishPassedTest(test, output);
	}

	private static void finishPassedTest(GameTestState test, String output) {
		sendMessageToAllPlayers(test.getWorld(), Formatting.GREEN, output);
		TestFailureLogger.passTest(test);
	}

	protected static void failTest(GameTestState test, Throwable output) {
		Text text;
		if (output instanceof GameTestException gameTestException) {
			text = gameTestException.getText();
		}
		else {
			text = Text.literal(Util.getInnermostMessage(output));
		}

		getTestInstanceBlockEntity(test).ifPresent(testInstanceBlockEntity -> testInstanceBlockEntity.setErrorMessage(
				text));
		finishFailedTest(test, output);
	}

	protected static void finishFailedTest(GameTestState test, Throwable output) {
		String
				string =
				output.getMessage() + (output.getCause() == null ? "" : " cause: "
				                                                        + Util.getInnermostMessage(output.getCause())
				);
		String string2 = (test.isRequired() ? "" : "(optional) ") + test.getId() + " failed! " + string;
		sendMessageToAllPlayers(test.getWorld(), test.isRequired() ? Formatting.RED : Formatting.YELLOW, string2);
		Throwable throwable = (Throwable) MoreObjects.firstNonNull(ExceptionUtils.getRootCause(output), output);
		if (throwable instanceof PositionedException positionedException) {
			test
					.getTestInstanceBlockEntity()
					.addError(positionedException.getPos(), positionedException.getDebugMessage());
		}

		TestFailureLogger.failTest(test);
	}

	private static Optional<TestInstanceBlockEntity> getTestInstanceBlockEntity(GameTestState state) {
		ServerWorld serverWorld = state.getWorld();
		Optional<BlockPos> optional = Optional.ofNullable(state.getPos());
		return optional.flatMap(pos -> serverWorld.getBlockEntity(pos, BlockEntityType.TEST_INSTANCE_BLOCK));
	}

	protected static void sendMessageToAllPlayers(ServerWorld world, Formatting formatting, String message) {
		world
				.getPlayers(player -> true)
				.forEach(player -> player.sendMessage(Text.literal(message).formatted(formatting)));
	}
}
