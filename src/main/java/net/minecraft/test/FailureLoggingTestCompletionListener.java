package net.minecraft.test;

import com.mojang.logging.LogUtils;
import net.minecraft.util.Util;
import org.slf4j.Logger;

/**
 * Слушатель завершения тестов, логирующий провалы через SLF4J.
 * Обязательные тесты логируются как {@code ERROR}, опциональные — как {@code WARN}.
 */
public class FailureLoggingTestCompletionListener implements TestCompletionListener {

	private static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void onTestFailed(GameTestState test) {
		String position = test.getPos().toShortString();
		String message = Util.getInnermostMessage(test.getThrowable());

		if (test.isRequired()) {
			LOGGER.error("{} failed at {}! {}", test.getId(), position, message);
		} else {
			LOGGER.warn("(optional) {} failed at {}. {}", test.getId(), position, message);
		}
	}

	@Override
	public void onTestPassed(GameTestState test) {
	}
}
