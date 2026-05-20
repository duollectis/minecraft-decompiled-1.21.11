package net.minecraft.resource;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.slf4j.Logger;

public class ProfiledResourceReload extends SimpleResourceReload<ProfiledResourceReload.Summary> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Stopwatch reloadTimer = Stopwatch.createUnstarted();

   public static ResourceReload start(
      ResourceManager manager, List<ResourceReloader> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage
   ) {
      ProfiledResourceReload profiledResourceReload = new ProfiledResourceReload(reloaders);
      profiledResourceReload.start(
         prepareExecutor,
         applyExecutor,
         manager,
         reloaders,
         (store, reloadSynchronizer, reloader, prepare, apply) -> {
            AtomicLong atomicLong = new AtomicLong();
            AtomicLong atomicLong2 = new AtomicLong();
            AtomicLong atomicLong3 = new AtomicLong();
            AtomicLong atomicLong4 = new AtomicLong();
            CompletableFuture<Void> completableFuture = reloader.reload(
               store,
               getProfiledExecutor(prepare, atomicLong, atomicLong2, reloader.getName()),
               reloadSynchronizer,
               getProfiledExecutor(apply, atomicLong3, atomicLong4, reloader.getName())
            );
            return completableFuture.thenApplyAsync(v -> {
               LOGGER.debug("Finished reloading {}", reloader.getName());
               return new ProfiledResourceReload.Summary(reloader.getName(), atomicLong, atomicLong2, atomicLong3, atomicLong4);
            }, applyExecutor);
         },
         initialStage
      );
      return profiledResourceReload;
   }

   private ProfiledResourceReload(List<ResourceReloader> waitingReloaders) {
      super(waitingReloaders);
      this.reloadTimer.start();
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
      return super.startAsync(prepareExecutor, applyExecutor, manager, reloaders, factory, initialStage).thenApplyAsync(this::finish, applyExecutor);
   }

   private static Executor getProfiledExecutor(Executor executor, AtomicLong output, AtomicLong counter, String name) {
      return runnable -> executor.execute(() -> {
         Profiler profiler = Profilers.get();
         profiler.push(name);
         long l = Util.getMeasuringTimeNano();
         runnable.run();
         output.addAndGet(Util.getMeasuringTimeNano() - l);
         counter.incrementAndGet();
         profiler.pop();
      });
   }

   private List<ProfiledResourceReload.Summary> finish(List<ProfiledResourceReload.Summary> summaries) {
      this.reloadTimer.stop();
      long l = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.reloadTimer.elapsed(TimeUnit.MILLISECONDS));

      for (ProfiledResourceReload.Summary summary : summaries) {
         long m = TimeUnit.NANOSECONDS.toMillis(summary.prepareTimeMs.get());
         long n = summary.preparationCount.get();
         long o = TimeUnit.NANOSECONDS.toMillis(summary.applyTimeMs.get());
         long p = summary.reloadCount.get();
         long q = m + o;
         long r = n + p;
         String string = summary.name;
         LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", new Object[]{string, r, q, n, m, p, o});
         l += o;
      }

      LOGGER.info("Total blocking time: {} ms", l);
      return summaries;
   }

   public record Summary(String name, AtomicLong prepareTimeMs, AtomicLong preparationCount, AtomicLong applyTimeMs, AtomicLong reloadCount) {
   }
}
