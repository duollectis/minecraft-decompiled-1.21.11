package net.minecraft.util.thread;

/**
 * Расширение {@link ThreadExecutor}, поддерживающее реентрантное выполнение задач.
 * Отслеживает глубину вложенных вызовов {@link #executeTask}, чтобы корректно
 * определять, нужно ли откладывать задачу в очередь или выполнять её немедленно.
 */
public abstract class ReentrantThreadExecutor<R extends Runnable> extends ThreadExecutor<R> {

	private int runningTasks;

	public ReentrantThreadExecutor(String name) {
		super(name);
	}

	@Override
	public boolean shouldExecuteAsync() {
		return hasRunningTasks() || super.shouldExecuteAsync();
	}

	protected boolean hasRunningTasks() {
		return runningTasks != 0;
	}

	@Override
	public void executeTask(R task) {
		runningTasks++;

		try {
			super.executeTask(task);
		}
		finally {
			runningTasks--;
		}
	}
}
