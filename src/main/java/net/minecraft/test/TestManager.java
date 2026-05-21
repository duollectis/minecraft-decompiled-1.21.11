package net.minecraft.test;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;

/**
 * {@code TestManager}.
 */
public class TestManager {

	public static final TestManager INSTANCE = new TestManager();
	private static final Logger LOGGER = LogUtils.getLogger();
	private final Collection<GameTestState> tests = Lists.newCopyOnWriteArrayList();
	private @Nullable TestRunContext runContext;
	private TestManager.State state = TestManager.State.IDLE;

	private TestManager() {
	}

	public void start(GameTestState test) {
		this.tests.add(test);
	}

	public void clear() {
		if (this.state != TestManager.State.IDLE) {
			this.state = TestManager.State.HALTING;
		}
		else {
			this.tests.clear();
			if (this.runContext != null) {
				this.runContext.clear();
				this.runContext = null;
			}
		}
	}

	public void setRunContext(TestRunContext runContext) {
		if (this.runContext != null) {
			Util.logErrorOrPause("The runner was already set in GameTestTicker");
		}

		this.runContext = runContext;
	}

	public void tick() {
		if (this.runContext != null) {
			this.state = TestManager.State.RUNNING;
			this.tests.forEach(test -> test.tick(this.runContext));
			this.tests.removeIf(GameTestState::isCompleted);
			TestManager.State state = this.state;
			this.state = TestManager.State.IDLE;
			if (state == TestManager.State.HALTING) {
				this.clear();
			}
		}
	}

	/**
	 * {@code State}.
	 */
	static enum State {
		IDLE,
		RUNNING,
		HALTING;
	}
}
