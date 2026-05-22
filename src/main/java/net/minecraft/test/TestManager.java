package net.minecraft.test;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * Глобальный менеджер активных тестов. Тикает все запущенные тесты каждый игровой тик.
 * Поддерживает безопасную остановку через {@link State#HALTING}: если {@link #clear()} вызван
 * во время тика, очистка откладывается до его завершения.
 */
public class TestManager {

	public static final TestManager INSTANCE = new TestManager();
	private static final Logger LOGGER = LogUtils.getLogger();

	private final Collection<GameTestState> tests = Lists.newCopyOnWriteArrayList();
	private @Nullable TestRunContext runContext;
	private State state = State.IDLE;

	private TestManager() {
	}

	public void start(GameTestState test) {
		tests.add(test);
	}

	public void clear() {
		if (state != State.IDLE) {
			state = State.HALTING;
			return;
		}

		tests.clear();

		if (runContext != null) {
			runContext.clear();
			runContext = null;
		}
	}

	public void setRunContext(TestRunContext runContext) {
		if (this.runContext != null) {
			Util.logErrorOrPause("The runner was already set in GameTestTicker");
		}

		this.runContext = runContext;
	}

	public void tick() {
		if (runContext == null) {
			return;
		}

		state = State.RUNNING;
		tests.forEach(test -> test.tick(runContext));
		tests.removeIf(GameTestState::isCompleted);

		State previousState = state;
		state = State.IDLE;

		if (previousState == State.HALTING) {
			clear();
		}
	}

	enum State {
		IDLE,
		RUNNING,
		HALTING
	}
}
