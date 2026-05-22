package net.minecraft.test;

import com.google.common.collect.Lists;
import net.minecraft.text.Text;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Последовательный исполнитель задач с привязкой к тикам теста.
 * Задачи выполняются одна за другой при каждом вызове {@link #runSilently(int)} или
 * {@link #runReported(int)}. Если у задачи задана длительность — проверяется,
 * что между предыдущей и текущей задачей прошло ровно столько тиков.
 */
public class TimedTaskRunner {

	final GameTestState test;
	private final List<TimedTask> tasks = Lists.newArrayList();
	private int tick;

	TimedTaskRunner(GameTestState gameTest) {
		test = gameTest;
		tick = gameTest.getTick();
	}

	public TimedTaskRunner createAndAdd(Runnable task) {
		tasks.add(TimedTask.create(task));
		return this;
	}

	public TimedTaskRunner createAndAdd(long duration, Runnable task) {
		tasks.add(TimedTask.create(duration, task));
		return this;
	}

	public TimedTaskRunner expectMinDuration(int minDuration) {
		return expectMinDurationAndRun(minDuration, () -> {});
	}

	public TimedTaskRunner createAndAddReported(Runnable task) {
		tasks.add(TimedTask.create(() -> tryRun(task)));
		return this;
	}

	/**
	 * Добавляет задачу, которая выполняется только если с момента предыдущей задачи
	 * прошло не менее {@code minDuration} тиков. Иначе — тест падает.
	 */
	public TimedTaskRunner expectMinDurationAndRun(int minDuration, Runnable task) {
		tasks.add(TimedTask.create(() -> {
			if (test.getTick() < tick + minDuration) {
				throw new GameTestException(
					Text.translatable("test.error.sequence.not_completed"),
					test.getTick()
				);
			}

			tryRun(task);
		}));
		return this;
	}

	/**
	 * Добавляет задачу, которая выполняется немедленно, но если с момента предыдущей задачи
	 * прошло менее {@code minDuration} тиков — тест падает после выполнения.
	 */
	public TimedTaskRunner expectMinDurationOrRun(int minDuration, Runnable task) {
		tasks.add(TimedTask.create(() -> {
			if (test.getTick() >= tick + minDuration) {
				return;
			}

			tryRun(task);
			throw new GameTestException(
				Text.translatable("test.error.sequence.not_completed"),
				test.getTick()
			);
		}));
		return this;
	}

	public void completeIfSuccessful() {
		tasks.add(TimedTask.create(test::completeIfSuccessful));
	}

	public void fail(Supplier<TestException> exceptionSupplier) {
		tasks.add(TimedTask.create(() -> test.fail(exceptionSupplier.get())));
	}

	public Trigger createAndAddTrigger() {
		Trigger trigger = new Trigger();
		tasks.add(TimedTask.create(() -> trigger.trigger(test.getTick())));
		return trigger;
	}

	public void runSilently(int currentTick) {
		try {
			runTasks(currentTick);
		} catch (GameTestException ignored) {
			// Ошибки подавляются намеренно — используется для "тихого" прогона без фиксации провала
		}
	}

	public void runReported(int currentTick) {
		try {
			runTasks(currentTick);
		} catch (GameTestException ex) {
			test.fail(ex);
		}
	}

	private void tryRun(Runnable task) {
		try {
			task.run();
		} catch (GameTestException ex) {
			test.fail(ex);
		}
	}

	private void runTasks(int currentTick) {
		Iterator<TimedTask> iterator = tasks.iterator();

		while (iterator.hasNext()) {
			TimedTask timedTask = iterator.next();
			timedTask.task.run();
			iterator.remove();

			int elapsed = currentTick - tick;
			int previousTick = tick;
			tick = currentTick;

			if (timedTask.duration != null && timedTask.duration != elapsed) {
				test.fail(new GameTestException(
					Text.translatable("test.error.sequence.invalid_tick", previousTick + timedTask.duration),
					currentTick
				));
				break;
			}
		}
	}

	/**
	 * Триггер, который должен быть активирован ровно в текущий тик теста.
	 * Используется для проверки точного момента наступления события.
	 */
	public class Trigger {

		private static final int UNTRIGGERED_TICK = -1;
		private int triggeredTick = UNTRIGGERED_TICK;

		void trigger(int currentTick) {
			if (triggeredTick != UNTRIGGERED_TICK) {
				throw new IllegalStateException("Condition already triggered at " + triggeredTick);
			}

			triggeredTick = currentTick;
		}

		public void checkTrigger() {
			int currentTick = TimedTaskRunner.this.test.getTick();

			if (triggeredTick == currentTick) {
				return;
			}

			if (triggeredTick == UNTRIGGERED_TICK) {
				throw new GameTestException(
					Text.translatable("test.error.sequence.condition_not_triggered"),
					currentTick
				);
			}

			throw new GameTestException(
				Text.translatable("test.error.sequence.condition_already_triggered", triggeredTick),
				currentTick
			);
		}
	}
}
