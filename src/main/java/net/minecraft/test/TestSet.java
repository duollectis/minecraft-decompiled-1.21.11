package net.minecraft.test;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Набор тестов одного батча с агрегированной статистикой и визуальным представлением прогресса.
 * Символы прогресса: {@code ' '} — не запущен, {@code '_'} — выполняется,
 * {@code '+'} — пройден, {@code 'x'} — опциональный провал, {@code 'X'} — обязательный провал.
 */
public class TestSet {

	private static final char NOT_STARTED = ' ';
	private static final char RUNNING = '_';
	private static final char PASS = '+';
	private static final char OPTIONAL_FAIL = 'x';
	private static final char REQUIRED_FAIL = 'X';

	private final Collection<GameTestState> tests = Lists.newArrayList();
	private final Collection<TestListener> listeners = Lists.newArrayList();

	public TestSet() {
	}

	public TestSet(Collection<GameTestState> tests) {
		this.tests.addAll(tests);
	}

	public void add(GameTestState test) {
		tests.add(test);
		listeners.forEach(test::addListener);
	}

	public void addListener(TestListener listener) {
		listeners.add(listener);
		tests.forEach(test -> test.addListener(listener));
	}

	public void addListener(Consumer<GameTestState> onFailed) {
		addListener(new TestListener() {
			@Override
			public void onStarted(GameTestState test) {
			}

			@Override
			public void onPassed(GameTestState test, TestRunContext context) {
			}

			@Override
			public void onFailed(GameTestState test, TestRunContext context) {
				onFailed.accept(test);
			}

			@Override
			public void onRetry(GameTestState lastState, GameTestState nextState, TestRunContext context) {
			}
		});
	}

	public int getFailedRequiredTestCount() {
		return (int) tests.stream().filter(GameTestState::isFailed).filter(GameTestState::isRequired).count();
	}

	public int getFailedOptionalTestCount() {
		return (int) tests.stream().filter(GameTestState::isFailed).filter(GameTestState::isOptional).count();
	}

	public int getCompletedTestCount() {
		return (int) tests.stream().filter(GameTestState::isCompleted).count();
	}

	public boolean failed() {
		return getFailedRequiredTestCount() > 0;
	}

	public boolean hasFailedOptionalTests() {
		return getFailedOptionalTestCount() > 0;
	}

	public Collection<GameTestState> getRequiredTests() {
		return tests.stream()
			.filter(GameTestState::isFailed)
			.filter(GameTestState::isRequired)
			.collect(Collectors.toList());
	}

	public Collection<GameTestState> getOptionalTests() {
		return tests.stream()
			.filter(GameTestState::isFailed)
			.filter(GameTestState::isOptional)
			.collect(Collectors.toList());
	}

	public int getTestCount() {
		return tests.size();
	}

	public boolean isDone() {
		return getCompletedTestCount() == getTestCount();
	}

	public String getResultString() {
		StringBuilder result = new StringBuilder();
		result.append('[');

		tests.forEach(test -> {
			if (!test.isStarted()) {
				result.append(NOT_STARTED);
			} else if (test.isPassed()) {
				result.append(PASS);
			} else if (test.isFailed()) {
				result.append(test.isRequired() ? REQUIRED_FAIL : OPTIONAL_FAIL);
			} else {
				result.append(RUNNING);
			}
		});

		result.append(']');
		return result.toString();
	}

	public void remove(GameTestState state) {
		tests.remove(state);
	}

	@Override
	public String toString() {
		return getResultString();
	}
}
