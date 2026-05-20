package net.minecraft.client.sound;

import java.util.concurrent.locks.LockSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.thread.ThreadExecutor;

@Environment(EnvType.CLIENT)
public class SoundExecutor extends ThreadExecutor<Runnable> {
   private Thread thread = this.createThread();
   private volatile boolean stopped;

   public SoundExecutor() {
      super("Sound executor");
   }

   private Thread createThread() {
      Thread thread = new Thread(this::waitForStop);
      thread.setDaemon(true);
      thread.setName("Sound engine");
      thread.setUncaughtExceptionHandler(
         (threadx, throwable) -> MinecraftClient.getInstance()
            .setCrashReportSupplierAndAddDetails(CrashReport.create(throwable, "Uncaught exception on thread: " + threadx.getName()))
      );
      thread.start();
      return thread;
   }

   @Override
   public Runnable createTask(Runnable runnable) {
      return runnable;
   }

   @Override
   public void send(Runnable runnable) {
      if (!this.stopped) {
         super.send(runnable);
      }
   }

   @Override
   protected boolean canExecute(Runnable task) {
      return !this.stopped;
   }

   @Override
   protected Thread getThread() {
      return this.thread;
   }

   private void waitForStop() {
      while (!this.stopped) {
         this.runTasks(() -> this.stopped);
      }
   }

   @Override
   protected void waitForTasks() {
      LockSupport.park("waiting for tasks");
   }

   public void stop() {
      this.stopped = true;
      this.cancelTasks();
      this.thread.interrupt();

      try {
         this.thread.join();
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }
   }

   public void restart() {
      this.stopped = false;
      this.thread = this.createThread();
   }
}
