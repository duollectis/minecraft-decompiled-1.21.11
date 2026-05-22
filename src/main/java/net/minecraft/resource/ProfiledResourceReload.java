package net.minecraft.resource;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Расширение {@link SimpleResourceReload}, собирающее статистику времени выполнения
 * каждого перезагрузчика и выводящее итоговый отчёт в лог.
 */
public class ProfiledResourceReload extends SimpleResourceReload<ProfiledResourceReload.Summary> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final Stopwatch reloadTimer = Stopwatch.createUnstarted();

	/**
	 * Запускает профилируемую перезагрузку ресурсов.
	 *
	 * @param manager        менеджер ресурсов
	 * @param reloaders      список перезагрузчиков
	 * @param prepareExecutor исполнитель фазы подготовки
	 * @param applyExecutor  исполнитель фазы применения
	 * @param initialStage   начальная стадия (барьер синхронизации)
	 * @return объект отслеживания прогресса перезагрузки
	 */
	public static ResourceReload start(
		ResourceManager manager,
		List<ResourceReloader> reloaders,
		Executor prepareExecutor,
		Executor applyExecutor,
		CompletableFuture<Unit> initialStage
	) {
		ProfiledResourceReload reload = new ProfiledResourceReload(reloaders);
		reload.start(
			prepareExecutor,
			applyExecutor,
			manager,
			reloaders,
			(store, synchronizer, reloader, prepare, apply) -> {
				AtomicLong prepareTime = new AtomicLong();
				AtomicLong prepareCount = new AtomicLong();
				AtomicLong applyTime = new AtomicLong();
				AtomicLong applyCount = new AtomicLong();

				CompletableFuture<Void> future = reloader.reload(
					store,
					getProfiledExecutor(prepare, prepareTime, prepareCount, reloader.getName()),
					synchronizer,
					getProfiledExecutor(apply, applyTime, applyCount, reloader.getName())
				);

				return future.thenApplyAsync(
					ignored -> {
						LOGGER.debug("Finished reloading {}", reloader.getName());
						return new Summary(reloader.getName(), prepareTime, prepareCount, applyTime, applyCount);
					},
					applyExecutor
				);
			},
			initialStage
		);
		return reload;
	}

	private ProfiledResourceReload(List<ResourceReloader> waitingReloaders) {
		super(waitingReloaders);
		reloadTimer.start();
	}

	@Override
	protected CompletableFuture<List<ProfiledResourceReload.Summary>> startAsync(
		Executor prepareExecutor,
		Executor applyExecutor,
		ResourceManager manager,
		List<ResourceReloader> reloaders,
		SimpleResourceReload.Factory<ProfiledResourceReload.Summary> factory,
		CompletableFuture<?> initialStage
	) {
		return super.startAsync(prepareExecutor, applyExecutor, manager, reloaders, factory, initialStage)
			.thenApplyAsync(this::finish, applyExecutor);
	}

	private static Executor getProfiledExecutor(
		Executor executor,
		AtomicLong timeAccumulator,
		AtomicLong counter,
		String name
	) {
		return runnable -> executor.execute(() -> {
			Profiler profiler = Profilers.get();
			profiler.push(name);
			long startNano = Util.getMeasuringTimeNano();
			runnable.run();
			timeAccumulator.addAndGet(Util.getMeasuringTimeNano() - startNano);
			counter.incrementAndGet();
			profiler.pop();
		});
	}

	private List<ProfiledResourceReload.Summary> finish(List<ProfiledResourceReload.Summary> summaries) {
		reloadTimer.stop();
		long totalApplyMs = 0L;

		LOGGER.info("Resource reload finished after {} ms", reloadTimer.elapsed(TimeUnit.MILLISECONDS));

		for (Summary summary : summaries) {
			long prepareMs = TimeUnit.NANOSECONDS.toMillis(summary.prepareTimeMs.get());
			long prepareTasks = summary.preparationCount.get();
			long applyMs = TimeUnit.NANOSECONDS.toMillis(summary.applyTimeMs.get());
			long applyTasks = summary.reloadCount.get();
			long totalMs = prepareMs + applyMs;
			long totalTasks = prepareTasks + applyTasks;

			LOGGER.info(
				"{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)",
				summary.name, totalTasks, totalMs, prepareTasks, prepareMs, applyTasks, applyMs
			);

			totalApplyMs += applyMs;
		}

		LOGGER.info("Total blocking time: {} ms", totalApplyMs);
		return summaries;
	}

	/**
	 * Итоговая статистика перезагрузки одного {@link ResourceReloader}.
	 */
	public record Summary(
		String name,
		AtomicLong prepareTimeMs,
		AtomicLong preparationCount,
		AtomicLong applyTimeMs,
		AtomicLong reloadCount
	) {}
}
