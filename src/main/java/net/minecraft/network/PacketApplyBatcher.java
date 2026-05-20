package net.minecraft.network;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.crash.CrashException;
import org.slf4j.Logger;

public class PacketApplyBatcher implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private final Queue<PacketApplyBatcher.Entry<?>> entries = Queues.newConcurrentLinkedQueue();
   private final Thread thread;
   private boolean closed;

   public PacketApplyBatcher(Thread thread) {
      this.thread = thread;
   }

   public boolean isOnThread() {
      return Thread.currentThread() == this.thread;
   }

   public <T extends PacketListener> void add(T listener, Packet<T> packet) {
      if (this.closed) {
         throw new RejectedExecutionException("Server already shutting down");
      } else {
         this.entries.add(new PacketApplyBatcher.Entry<>(listener, packet));
      }
   }

   public void apply() {
      if (!this.closed) {
         while (!this.entries.isEmpty()) {
            this.entries.poll().apply();
         }
      }
   }

   @Override
   public void close() {
      this.closed = true;
   }

   record Entry<T extends PacketListener>(T listener, Packet<T> packet) {
      public void apply() {
         if (this.listener.accepts(this.packet)) {
            try {
               this.packet.apply(this.listener);
            } catch (Exception var3) {
               if (var3 instanceof CrashException crashException && crashException.getCause() instanceof OutOfMemoryError) {
                  throw NetworkThreadUtils.createCrashException(var3, this.packet, this.listener);
               }

               this.listener.onPacketException(this.packet, var3);
            }
         } else {
            PacketApplyBatcher.LOGGER.debug("Ignoring packet due to disconnection: {}", this.packet);
         }
      }
   }
}
